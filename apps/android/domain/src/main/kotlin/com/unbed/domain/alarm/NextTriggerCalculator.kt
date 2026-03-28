package com.unbed.domain.alarm

import com.unbed.core.model.AlarmConfig
import com.unbed.core.model.AlarmSession
import com.unbed.core.model.AlarmState
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class NextTriggerCalculator(
    private val zoneIdProvider: () -> ZoneId = { ZoneId.systemDefault() },
) {
    constructor(zoneId: ZoneId) : this({ zoneId })

    private companion object {
        const val MAX_REPEAT_LOOKAHEAD_DAYS: Long = 7L
    }

    fun calculate(
        config: AlarmConfig?,
        activeSession: AlarmSession?,
        now: Instant,
    ): NextTrigger? {
        val zoneId = zoneIdProvider()
        val currentDateTime = ZonedDateTime.ofInstant(now, zoneId)
        val regularTrigger =
            config
                ?.takeIf { it.enabled }
                ?.let { enabledConfig ->
                    calculateNextRegularOccurrence(enabledConfig, currentDateTime)
                        ?.toInstant()
                        ?.let { triggerAt ->
                            NextTrigger(
                                type = NextTriggerType.REGULAR_ALARM,
                                triggerAt = triggerAt,
                                alarmId = enabledConfig.id,
                            )
                        }
                }

        val reringTrigger =
            activeSession
                ?.takeIf { it.state == AlarmState.SNOOZED_WAITING_RELEASE }
                ?.snoozeUntil
                ?.let { triggerAt ->
                    NextTrigger(
                        type = NextTriggerType.SESSION_RERING,
                        triggerAt = triggerAt,
                        alarmId = activeSession.alarmId,
                        sessionId = activeSession.sessionId,
                    )
                }

        return when {
            regularTrigger == null -> reringTrigger
            reringTrigger == null -> regularTrigger
            regularTrigger.triggerAt <= reringTrigger.triggerAt -> regularTrigger
            else -> reringTrigger
        }
    }

    fun calculateNextRegularOccurrence(
        config: AlarmConfig,
        now: ZonedDateTime,
    ): ZonedDateTime? {
        if (!config.enabled) {
            return null
        }

        return if (config.repeatDays.isEmpty()) {
            val today = now.toLocalDate().atTime(config.time).atZone(now.zone)
            if (today.isAfter(now)) {
                today
            } else {
                now.toLocalDate().plusDays(1).atTime(config.time).atZone(now.zone)
            }
        } else {
            nextRepeatingOccurrence(
                repeatDays = config.repeatDays,
                now = now,
                hourMinute = config.time.hour to config.time.minute,
            )
        }
    }

    private fun nextRepeatingOccurrence(
        repeatDays: Set<DayOfWeek>,
        now: ZonedDateTime,
        hourMinute: Pair<Int, Int>,
    ): ZonedDateTime? {
        return (0L..MAX_REPEAT_LOOKAHEAD_DAYS)
            .asSequence()
            .map { now.toLocalDate().plusDays(it) }
            .filter { it.dayOfWeek in repeatDays }
            .map { candidateDate ->
                candidateDate.atTime(hourMinute.first, hourMinute.second).atZone(now.zone)
            }
            .firstOrNull { it.isAfter(now) }
    }
}
