package com.unbed.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.unbed.core.model.ReleaseConditionType
import com.unbed.core.model.SoundType
import java.time.DayOfWeek
import java.time.LocalTime

@Entity(tableName = "alarm_config")
data class AlarmConfigEntity(
    @PrimaryKey val id: Long,
    val hour: Int,
    val minute: Int,
    val repeatDays: Set<DayOfWeek>,
    val enabled: Boolean,
    val soundType: SoundType,
    val releaseCondition: ReleaseConditionType,
) {
    val time: LocalTime
        get() = LocalTime.of(hour, minute)
}
