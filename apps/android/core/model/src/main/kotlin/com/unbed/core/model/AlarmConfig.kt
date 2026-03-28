package com.unbed.core.model

import java.time.DayOfWeek
import java.time.LocalTime

data class AlarmConfig(
    val id: Long,
    val time: LocalTime,
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val enabled: Boolean = true,
    val soundType: SoundType = SoundType.FIXED,
    val releaseCondition: ReleaseConditionType = ReleaseConditionType.ManualRelease,
) {
    val isRepeating: Boolean
        get() = repeatDays.isNotEmpty()

    fun terminalStateAfterSession(): AlarmState {
        return if (isRepeating) {
            AlarmState.SCHEDULED
        } else {
            AlarmState.IDLE
        }
    }
}
