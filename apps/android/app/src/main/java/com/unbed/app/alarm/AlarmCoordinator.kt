package com.unbed.app.alarm

import com.unbed.core.model.AlarmConfig
import com.unbed.core.model.AlarmSession
import com.unbed.core.model.AlarmState
import com.unbed.domain.alarm.AlarmConfigRepository
import com.unbed.domain.alarm.AlarmSessionRepository
import com.unbed.domain.alarm.AlarmStateMachine
import com.unbed.domain.alarm.NextTrigger
import com.unbed.domain.alarm.NextTriggerCalculator
import com.unbed.domain.alarm.NextTriggerType
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

@Suppress("TooManyFunctions")
class AlarmCoordinator(
    private val configRepository: AlarmConfigRepository,
    private val sessionRepository: AlarmSessionRepository,
    private val stateMachine: AlarmStateMachine,
    private val alarmScheduler: AndroidAlarmScheduler,
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

    companion object {
        const val DEFAULT_ALARM_ID: Long = 1L
        val DEFAULT_ALARM_TIME: LocalTime = LocalTime.of(7, 0)
    }
}
