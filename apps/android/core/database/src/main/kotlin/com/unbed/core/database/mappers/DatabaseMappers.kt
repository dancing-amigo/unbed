package com.unbed.core.database.mappers

import com.unbed.core.database.entity.AlarmConfigEntity
import com.unbed.core.database.entity.AlarmSessionEntity
import com.unbed.core.model.AlarmConfig
import com.unbed.core.model.AlarmSession

fun AlarmConfigEntity.toDomain(): AlarmConfig {
    return AlarmConfig(
        id = id,
        time = time,
        repeatDays = repeatDays,
        enabled = enabled,
        soundType = soundType,
        releaseCondition = releaseCondition,
    )
}

fun AlarmConfig.toEntity(): AlarmConfigEntity {
    return AlarmConfigEntity(
        id = id,
        hour = time.hour,
        minute = time.minute,
        repeatDays = repeatDays,
        enabled = enabled,
        soundType = soundType,
        releaseCondition = releaseCondition,
    )
}

fun AlarmSessionEntity.toDomain(): AlarmSession {
    return AlarmSession(
        sessionId = sessionId,
        alarmId = alarmId,
        scheduledAt = scheduledAt,
        state = state,
        qrValidatedAt = qrValidatedAt,
        snoozeUntil = snoozeUntil,
        releasedAt = releasedAt,
        snoozeCycleCount = snoozeCycleCount,
        sessionEndedReason = sessionEndedReason,
    )
}

fun AlarmSession.toEntity(): AlarmSessionEntity {
    return AlarmSessionEntity(
        sessionId = sessionId,
        alarmId = alarmId,
        scheduledAt = scheduledAt,
        state = state,
        qrValidatedAt = qrValidatedAt,
        snoozeUntil = snoozeUntil,
        releasedAt = releasedAt,
        snoozeCycleCount = snoozeCycleCount,
        sessionEndedReason = sessionEndedReason,
    )
}
