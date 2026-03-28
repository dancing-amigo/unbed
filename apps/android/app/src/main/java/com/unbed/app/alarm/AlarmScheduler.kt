package com.unbed.app.alarm

import com.unbed.domain.alarm.NextTrigger

interface AlarmScheduler {
    suspend fun reschedule(): NextTrigger?

    fun canScheduleExactAlarms(): Boolean

    fun openExactAlarmSettings()
}
