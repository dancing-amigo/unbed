package com.unbed.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.unbed.core.database.entity.AlarmConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmConfigDao {
    @Query("SELECT * FROM alarm_config ORDER BY id LIMIT 1")
    fun observeSingleConfig(): Flow<AlarmConfigEntity?>

    @Query("SELECT * FROM alarm_config ORDER BY id LIMIT 1")
    suspend fun getSingleConfig(): AlarmConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: AlarmConfigEntity)

    @Query("UPDATE alarm_config SET enabled = :enabled WHERE id = :alarmId")
    suspend fun setEnabled(
        alarmId: Long,
        enabled: Boolean,
    )
}
