package com.unbed.domain.alarm

import com.unbed.core.model.AlarmConfig
import com.unbed.core.model.AlarmSession
import com.unbed.core.model.AlarmState
import com.unbed.core.model.SessionEndedReason
import java.time.Duration
import java.time.Instant

class AlarmStateMachine(
    private val snoozeDuration: Duration = AlarmPolicy.DEFAULT_SNOOZE_DURATION,
    private val maxReringCount: Int = AlarmPolicy.MAX_RERING_COUNT,
) {
    fun createScheduledSession(
        config: AlarmConfig,
        sessionId: String,
        scheduledAt: Instant,
    ): AlarmSession {
        require(config.enabled) { "Cannot create a session from a disabled config." }
        return AlarmSession(
            sessionId = sessionId,
            alarmId = config.id,
            scheduledAt = scheduledAt,
            state = AlarmState.SCHEDULED,
        )
    }

    fun startRinging(session: AlarmSession): AlarmSession {
        session.requireState("start_ringing", AlarmState.SCHEDULED)
        return session.copy(state = AlarmState.RINGING)
    }

    fun beginQrScan(session: AlarmSession): AlarmSession {
        session.requireState("begin_qr_scan", AlarmState.RINGING)
        return session.copy(state = AlarmState.SCANNING_QR)
    }

    fun cancelQrScan(session: AlarmSession): AlarmSession {
        session.requireState("cancel_qr_scan", AlarmState.SCANNING_QR)
        return session.copy(state = AlarmState.RINGING)
    }

    fun validateQr(
        session: AlarmSession,
        validatedAt: Instant,
    ): AlarmSession {
        session.requireState("validate_qr", AlarmState.SCANNING_QR)
        return session.copy(
            state = AlarmState.SNOOZED_WAITING_RELEASE,
            qrValidatedAt = validatedAt,
            snoozeUntil = validatedAt.plus(snoozeDuration),
        )
    }

    fun completeManualRelease(
        session: AlarmSession,
        releasedAt: Instant,
    ): AlarmSession {
        session.requireState("complete_manual_release", AlarmState.SNOOZED_WAITING_RELEASE)
        return session.copy(
            state = AlarmState.CLEARED,
            releasedAt = releasedAt,
            sessionEndedReason = SessionEndedReason.MANUAL_RELEASE,
            snoozeUntil = null,
        )
    }

    fun finalizeReleasedSession(
        config: AlarmConfig,
        session: AlarmSession,
    ): AlarmSession {
        session.requireState("finalize_released_session", AlarmState.CLEARED)
        return session.copy(
            state = config.terminalStateAfterSession(),
        )
    }

    fun onSnoozeExpired(
        config: AlarmConfig,
        session: AlarmSession,
    ): AlarmSession {
        session.requireState("on_snooze_expired", AlarmState.SNOOZED_WAITING_RELEASE)
        val nextReringCount = session.snoozeCycleCount + 1
        return if (nextReringCount > maxReringCount) {
            session.copy(
                state = config.terminalStateAfterSession(),
                snoozeUntil = null,
                sessionEndedReason = SessionEndedReason.RERING_CAP_REACHED,
            )
        } else {
            session.copy(
                state = AlarmState.RINGING,
                qrValidatedAt = null,
                snoozeUntil = null,
                snoozeCycleCount = nextReringCount,
            )
        }
    }

    fun supersedeByNextSchedule(
        config: AlarmConfig,
        session: AlarmSession,
    ): AlarmSession {
        session.requireState(
            "supersede_by_next_schedule",
            AlarmState.SCHEDULED,
            AlarmState.RINGING,
            AlarmState.SCANNING_QR,
            AlarmState.SNOOZED_WAITING_RELEASE,
        )
        return session.copy(
            state = config.terminalStateAfterSession(),
            snoozeUntil = null,
            sessionEndedReason = SessionEndedReason.SUPERSEDED_BY_NEXT_SCHEDULE,
        )
    }
}

private fun AlarmSession.requireState(
    event: String,
    vararg allowedStates: AlarmState,
) {
    if (state !in allowedStates) {
        throw InvalidAlarmTransitionException(
            from = state,
            event = event,
            allowedStates = allowedStates.toSet(),
        )
    }
}
