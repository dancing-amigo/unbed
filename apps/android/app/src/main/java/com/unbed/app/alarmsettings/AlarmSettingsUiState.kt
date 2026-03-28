package com.unbed.app.alarmsettings

import com.unbed.app.alarm.AlarmCoordinator
import com.unbed.domain.alarm.NextTrigger
import java.time.DayOfWeek
import java.time.LocalTime

data class AlarmSettingsUiState(
    val enabled: Boolean = false,
    val time: LocalTime = AlarmCoordinator.DEFAULT_ALARM_TIME,
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val isSaving: Boolean = false,
    val isLoaded: Boolean = false,
    val nextTrigger: NextTrigger? = null,
    val canScheduleExactAlarms: Boolean = true,
)
