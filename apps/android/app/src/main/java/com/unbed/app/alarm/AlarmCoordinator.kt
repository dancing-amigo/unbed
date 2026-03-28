package com.unbed.app.alarm

import com.unbed.core.model.AlarmConfig
import com.unbed.core.model.AlarmSession
import com.unbed.core.model.AlarmState
import com.unbed.domain.alarm.AlarmConfigRepository
import com.unbed.domain.alarm.AlarmPolicy
import com.unbed.domain.alarm.AlarmSessionRepository
import com.unbed.domain.alarm.AlarmStateMachine
import com.unbed.domain.alarm.NextTrigger
import com.unbed.domain.alarm.NextTriggerCalculator
import com.unbed.domain.alarm.NextTriggerType
import com.unbed.domain.alarm.ReleaseConditionHandlerRegistry
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

@Suppress("LongParameterList", "TooManyFunctions")
class AlarmCoordinator(
    private val configRepository: AlarmConfigRepository,
    private val sessionRepository: AlarmSessionRepository,
    private val stateMachine: AlarmStateMachine,
    private val alarmScheduler: AlarmScheduler,
    private val releaseConditionHandlerRegistry: ReleaseConditionHandlerRegistry,
    private val calculator: NextTriggerCalculator,
    private val clock: Clock,
) {
    fun observeConfig(): Flow<AlarmConfig?> = configRepository.observeConfig()

    suspend fun getOrCreateConfig(): AlarmConfig {
        return configRepository.getConfig() ?: defaultConfig()
    }

    suspend fun saveConfig(config: AlarmConfig): NextTrigger? {
        configRepository.upsertConfig(config)
        return alarmScheduler.reschedule()
    }

    suspend fun getSession(sessionId: String): AlarmSession? {
        return sessionRepository.getSession(sessionId)
    }

    suspend fun peekNextTrigger(config: AlarmConfig? = null): NextTrigger? {
        return calculator.calculate(
            config = config ?: configRepository.getConfig(),
            activeSession = sessionRepository.getActiveSession(),
            now = clock.instant(),
        )
    }

    fun canScheduleExactAlarms(): Boolean = alarmScheduler.canScheduleExactAlarms()

    fun openExactAlarmSettings() {
        alarmScheduler.openExactAlarmSettings()
    }

    suspend fun beginQrScan(sessionId: String): AlarmSession? {
        val session = sessionRepository.getSession(sessionId) ?: return null
        val updatedSession =
            if (session.state == AlarmState.SCANNING_QR) {
                session
            } else {
                stateMachine.beginQrScan(session)
            }
        sessionRepository.upsertSession(updatedSession)
        return updatedSession
    }

    suspend fun cancelQrScan(sessionId: String): AlarmSession? {
        val session = sessionRepository.getSession(sessionId) ?: return null
        val updatedSession =
            if (session.state == AlarmState.SCANNING_QR) {
                stateMachine.cancelQrScan(session)
            } else {
                session
            }
        sessionRepository.upsertSession(updatedSession)
        return updatedSession
    }

    suspend fun submitQrCode(
        sessionId: String,
        scannedValue: String,
    ): QrSubmissionResult {
        val session = sessionRepository.getSession(sessionId)
        val result =
            when {
                session == null -> QrSubmissionResult.SessionUnavailable
                session.state != AlarmState.SCANNING_QR -> QrSubmissionResult.InvalidState(session.state)
                scannedValue != AlarmPolicy.DEFAULT_QR_CONFIG.fixedValue -> QrSubmissionResult.Mismatch
                else -> {
                    val snoozedSession = stateMachine.validateQr(session, clock.instant())
                    sessionRepository.upsertSession(snoozedSession)
                    alarmScheduler.reschedule()
                    QrSubmissionResult.Matched(snoozedSession)
                }
            }
        return result
    }

    suspend fun completeRelease(sessionId: String): AlarmSession? {
        val config = configRepository.getConfig()
        val session = sessionRepository.getSession(sessionId)
        val finalizedSession =
            if (config == null || session == null) {
                null
            } else {
                releaseConditionHandlerRegistry.complete(config, session, clock.instant())
            }
        finalizedSession?.let {
            sessionRepository.upsertSession(it)
            alarmScheduler.reschedule()
        }
        return finalizedSession
    }

    suspend fun handleTrigger(
        triggerType: NextTriggerType,
        triggerAt: Instant,
        sessionId: String?,
    ): AlarmSession? {
        val config = configRepository.getConfig() ?: return null
        val updatedSession =
            when (triggerType) {
                NextTriggerType.REGULAR_ALARM -> handleRegularAlarmTrigger(config, triggerAt)
                NextTriggerType.SESSION_RERING -> handleSessionRering(config, sessionId)
            }
        alarmScheduler.reschedule()
        return updatedSession?.takeIf { it.state == AlarmState.RINGING }
    }

    suspend fun restoreAfterSystemEvent(now: Instant = clock.instant()): RestoreAlarmResult {
        val config = configRepository.getConfig()
        val activeSession = sessionRepository.getActiveSession()
        val restoredSession =
            when {
                activeSession == null -> null
                shouldForceRinging(activeSession, now) -> activeSession.forceRingingRecovery()
                shouldFinalizeRecoveredSession(activeSession) && config != null ->
                    stateMachine.finalizeReleasedSession(config, activeSession)
                else -> activeSession
            }

        if (restoredSession != null && restoredSession != activeSession) {
            sessionRepository.upsertSession(restoredSession)
        }

        val nextTrigger = alarmScheduler.reschedule()
        return restoredSession
            ?.takeIf { it.state == AlarmState.RINGING }
            ?.let(RestoreAlarmResult::Ringing)
            ?: RestoreAlarmResult.Rescheduled(nextTrigger)
    }

    private suspend fun handleRegularAlarmTrigger(
        config: AlarmConfig,
        triggerAt: Instant,
    ): AlarmSession {
        sessionRepository.getActiveSession()
            ?.let { activeSession ->
                sessionRepository.upsertSession(
                    stateMachine.supersedeByNextSchedule(config, activeSession),
                )
            }

        val newSession =
            stateMachine.startRinging(
                stateMachine.createScheduledSession(
                    config = config,
                    sessionId = UUID.randomUUID().toString(),
                    scheduledAt = triggerAt,
                ),
            )
        sessionRepository.upsertSession(newSession)

        if (!config.isRepeating) {
            configRepository.setEnabled(config.id, false)
        }
        return newSession
    }

    private suspend fun handleSessionRering(
        config: AlarmConfig,
        sessionId: String?,
    ): AlarmSession? {
        val activeSession = resolveReringSession(sessionId) ?: return null
        val updatedSession = stateMachine.onSnoozeExpired(config, activeSession)
        sessionRepository.upsertSession(updatedSession)
        return updatedSession
    }

    private suspend fun resolveReringSession(sessionId: String?): AlarmSession? {
        return if (sessionId == null) {
            sessionRepository.getActiveSession()
        } else {
            sessionRepository.getSession(sessionId)
        }
    }

    private fun defaultConfig(): AlarmConfig {
        return AlarmConfig(
            id = DEFAULT_ALARM_ID,
            time = DEFAULT_ALARM_TIME,
            enabled = false,
        )
    }

    private fun shouldForceRinging(
        session: AlarmSession,
        now: Instant,
    ): Boolean {
        return when (session.state) {
            AlarmState.SCHEDULED,
            AlarmState.RINGING,
            AlarmState.SCANNING_QR,
            -> true

            AlarmState.SNOOZED_WAITING_RELEASE -> {
                val snoozeUntil = session.snoozeUntil
                snoozeUntil == null || !snoozeUntil.isAfter(now)
            }

            AlarmState.CLEARED,
            AlarmState.IDLE,
            -> false
        }
    }

    private fun shouldFinalizeRecoveredSession(session: AlarmSession): Boolean {
        return session.state == AlarmState.CLEARED
    }

    private fun AlarmSession.forceRingingRecovery(): AlarmSession {
        return copy(
            state = AlarmState.RINGING,
            qrValidatedAt = null,
            snoozeUntil = null,
        )
    }

    companion object {
        const val DEFAULT_ALARM_ID: Long = 1L
        val DEFAULT_ALARM_TIME: LocalTime = LocalTime.of(7, 0)
    }
}

sealed interface RestoreAlarmResult {
    data class Ringing(val session: AlarmSession) : RestoreAlarmResult

    data class Rescheduled(val nextTrigger: NextTrigger?) : RestoreAlarmResult
}

sealed interface QrSubmissionResult {
    data class Matched(val session: AlarmSession) : QrSubmissionResult

    data object Mismatch : QrSubmissionResult

    data object SessionUnavailable : QrSubmissionResult

    data class InvalidState(val state: AlarmState) : QrSubmissionResult
}
