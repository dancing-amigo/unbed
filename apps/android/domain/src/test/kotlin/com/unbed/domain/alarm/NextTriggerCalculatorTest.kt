package com.unbed.domain.alarm

import com.google.common.truth.Truth.assertThat
import com.unbed.core.model.AlarmConfig
import com.unbed.core.model.AlarmSession
import com.unbed.core.model.AlarmState
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

class NextTriggerCalculatorTest {
    private val zoneId = ZoneId.of("America/Vancouver")
    private val calculator = NextTriggerCalculator(zoneId)

    @Test
    fun `single alarm schedules next occurrence on same day when still ahead`() {
        val now = Instant.parse("2026-03-28T13:00:00Z")
        val config =
            AlarmConfig(
                id = 1L,
                time = LocalTime.of(8, 0),
            )

        val next =
            calculator.calculateNextRegularOccurrence(
                config = config,
                now = now.atZone(zoneId),
            )

        assertThat(next?.hour).isEqualTo(8)
        assertThat(next?.dayOfMonth).isEqualTo(28)
    }

    @Test
    fun `repeating schedule picks matching weekday`() {
        val now = Instant.parse("2026-03-28T18:00:00Z")
        val config =
            AlarmConfig(
                id = 1L,
                time = LocalTime.of(7, 30),
                repeatDays = setOf(DayOfWeek.MONDAY),
            )

        val next =
            calculator.calculateNextRegularOccurrence(
                config = config,
                now = now.atZone(zoneId),
            )

        assertThat(next?.dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
        assertThat(next?.hour).isEqualTo(7)
        assertThat(next?.minute).isEqualTo(30)
    }

    @Test
    fun `regular alarm wins when it arrives before rering`() {
        val now = Instant.parse("2026-03-28T13:00:00Z")
        val config =
            AlarmConfig(
                id = 10L,
                time = LocalTime.of(7, 0),
            )
        val session =
            AlarmSession(
                sessionId = "session-1",
                alarmId = 10L,
                scheduledAt = now,
                state = AlarmState.SNOOZED_WAITING_RELEASE,
                snoozeUntil = Instant.parse("2026-03-28T16:30:00Z"),
            )

        val next = calculator.calculate(config, session, now)

        assertThat(next?.type).isEqualTo(NextTriggerType.REGULAR_ALARM)
    }

    @Test
    fun `session rering wins when it happens first`() {
        val now = Instant.parse("2026-03-28T13:00:00Z")
        val config =
            AlarmConfig(
                id = 10L,
                time = LocalTime.of(23, 0),
            )
        val session =
            AlarmSession(
                sessionId = "session-1",
                alarmId = 10L,
                scheduledAt = now,
                state = AlarmState.SNOOZED_WAITING_RELEASE,
                snoozeUntil = Instant.parse("2026-03-28T13:05:00Z"),
            )

        val next = calculator.calculate(config, session, now)

        assertThat(next?.type).isEqualTo(NextTriggerType.SESSION_RERING)
        assertThat(next?.sessionId).isEqualTo("session-1")
    }
}
