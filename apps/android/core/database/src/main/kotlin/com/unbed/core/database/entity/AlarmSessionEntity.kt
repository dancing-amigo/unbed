package com.unbed.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.unbed.core.model.AlarmState
import com.unbed.core.model.SessionEndedReason
import java.time.Instant

@Entity(tableName = "alarm_session")
data class AlarmSessionEntity(
    @PrimaryKey val sessionId: String,
    val alarmId: Long,
    val scheduledAt: Instant,
    val state: AlarmState,
    val qrValidatedAt: Instant?,
    val snoozeUntil: Instant?,
    val releasedAt: Instant?,
    val snoozeCycleCount: Int,
    val sessionEndedReason: SessionEndedReason?,
)
