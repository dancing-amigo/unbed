package com.unbed.core.model

import java.time.Instant

data class AlarmSession(
    val sessionId: String,
    val alarmId: Long,
    val scheduledAt: Instant,
    val state: AlarmState,
    val qrValidatedAt: Instant? = null,
    val snoozeUntil: Instant? = null,
    val releasedAt: Instant? = null,
    val snoozeCycleCount: Int = 0,
    val sessionEndedReason: SessionEndedReason? = null,
)
