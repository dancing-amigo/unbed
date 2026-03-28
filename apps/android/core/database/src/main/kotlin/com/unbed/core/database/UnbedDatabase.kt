package com.unbed.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.unbed.core.database.dao.AlarmConfigDao
import com.unbed.core.database.dao.AlarmSessionDao
import com.unbed.core.database.entity.AlarmConfigEntity
import com.unbed.core.database.entity.AlarmSessionEntity

@Database(
    entities = [
        AlarmConfigEntity::class,
        AlarmSessionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class UnbedDatabase : RoomDatabase() {
    abstract fun alarmConfigDao(): AlarmConfigDao

    abstract fun alarmSessionDao(): AlarmSessionDao
}
