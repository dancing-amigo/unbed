package com.unbed.domain.alarm

import com.google.common.truth.Truth.assertThat
import com.unbed.core.model.AlarmConfig
import com.unbed.core.model.AlarmState
import com.unbed.core.model.ReleaseConditionType
import com.unbed.core.model.SessionEndedReason
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

class AlarmStateMachineTest {
    private val stateMachine = AlarmStateMachine()
    private val baseInstant = Instant.parse("2026-03-28T14:00:00Z")

    @Test
    fun `single alarm clears to idle after manual release`() {
        val config = singleAlarmConfig()
        val session = stateMachine.createScheduledSession(config, "s1", baseInstant)
        val ringing = stateMachine.startRinging(session)
        val scanning = stateMachine.beginQrScan(ringing)
        val snoozed = stateMachine.validateQr(scanning, baseInstant.plusSeconds(30))
        val cleared = stateMachine.completeManualRelease(snoozed, baseInstant.plusSeconds(40))
        val finalized = stateMachine.finalizeReleasedSession(config, cleared)

        assertThat(finalized.state).isEqualTo(AlarmState.IDLE)
        assertThat(finalized.sessionEndedReason).isEqualTo(SessionEndedReason.MANUAL_RELEASE)
    }

    @Test
    fun `repeating alarm clears to scheduled after manual release`() {
        val config = repeatingAlarmConfig()
        val session = stateMachine.createScheduledSession(config, "s1", baseInstant)
        val finalized =
            stateMachine.finalizeReleasedSession(
                config = config,
                session =
                    stateMachine.completeManualRelease(
                        session =
                            stateMachine.validateQr(
                                session =
                                    stateMachine.beginQrScan(
                                        session = stateMachine.startRinging(session),
                                    ),
                                validatedAt = baseInstant.plusSeconds(10),
                            ),
                        releasedAt = baseInstant.plusSeconds(20),
                    ),
            )

        assertThat(finalized.state).isEqualTo(AlarmState.SCHEDULED)
    }

    @Test
    fun `snooze expiry re-rings until cap then ends session`() {
        val config = singleAlarmConfig()
        var session =
            stateMachine.validateQr(
                session =
                    stateMachine.beginQrScan(
                        session =
                            stateMachine.startRinging(
                                stateMachine.createScheduledSession(config, "s1", baseInstant),
                            ),
                    ),
                validatedAt = baseInstant,
            )

        repeat(10) { index ->
            session = stateMachine.onSnoozeExpired(config, session)
            assertThat(session.state).isEqualTo(AlarmState.RINGING)
            assertThat(session.snoozeCycleCount).isEqualTo(index + 1)
            session =
                stateMachine.validateQr(
                    session = stateMachine.beginQrScan(session),
                    validatedAt = baseInstant.plusSeconds((index + 1L) * 60L),
                )
        }

        val capped = stateMachine.onSnoozeExpired(config, session)

        assertThat(capped.state).isEqualTo(AlarmState.IDLE)
        assertThat(capped.sessionEndedReason).isEqualTo(SessionEndedReason.RERING_CAP_REACHED)
    }

    @Test(expected = InvalidAlarmTransitionException::class)
    fun `invalid transition throws`() {
        val session = stateMachine.createScheduledSession(singleAlarmConfig(), "s1", baseInstant)
        stateMachine.validateQr(session, baseInstant)
    }

    private fun singleAlarmConfig(): AlarmConfig {
        return AlarmConfig(
            id = 1L,
            time = LocalTime.of(7, 0),
            repeatDays = emptySet(),
            releaseCondition = ReleaseConditionType.ManualRelease,
        )
    }

    private fun repeatingAlarmConfig(): AlarmConfig {
        return AlarmConfig(
            id = 1L,
            time = LocalTime.of(7, 0),
            repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
            releaseCondition = ReleaseConditionType.ManualRelease,
        )
    }
}
