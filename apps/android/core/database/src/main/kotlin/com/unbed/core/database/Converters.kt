package com.unbed.core.database

import androidx.room.TypeConverter
import com.unbed.core.model.AlarmState
import com.unbed.core.model.ReleaseConditionType
import com.unbed.core.model.SessionEndedReason
import com.unbed.core.model.SoundType
import java.time.DayOfWeek
import java.time.Instant

@Suppress("TooManyFunctions")
class Converters {
    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun fromRepeatDays(days: Set<DayOfWeek>): String {
        return days
            .map(DayOfWeek::getValue)
            .sorted()
            .joinToString(separator = ",")
    }

    @TypeConverter
    fun toRepeatDays(value: String): Set<DayOfWeek> {
        if (value.isBlank()) {
            return emptySet()
        }
        return value.split(",")
            .filter(String::isNotBlank)
            .map { DayOfWeek.of(it.toInt()) }
            .toSet()
    }

    @TypeConverter
    fun fromAlarmState(state: AlarmState): String = state.name

    @TypeConverter
    fun toAlarmState(value: String): AlarmState = AlarmState.valueOf(value)

    @TypeConverter
    fun fromSessionEndedReason(reason: SessionEndedReason?): String? = reason?.name

    @TypeConverter
    fun toSessionEndedReason(value: String?): SessionEndedReason? = value?.let(SessionEndedReason::valueOf)

    @TypeConverter
    fun fromSoundType(soundType: SoundType): String = soundType.name

    @TypeConverter
    fun toSoundType(value: String): SoundType = SoundType.valueOf(value)

    @TypeConverter
    fun fromReleaseConditionType(type: ReleaseConditionType): String {
        return when (type) {
            ReleaseConditionType.ManualRelease -> "manual_release"
            is ReleaseConditionType.StepCountRelease -> "step_count_release:${type.stepsRequired}"
        }
    }

    @TypeConverter
    fun toReleaseConditionType(value: String): ReleaseConditionType {
        return if (value.startsWith("step_count_release:")) {
            ReleaseConditionType.StepCountRelease(
                stepsRequired = value.substringAfter(':').toInt(),
            )
        } else {
            ReleaseConditionType.ManualRelease
        }
    }
}
