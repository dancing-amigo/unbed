package com.unbed.domain.alarm

import com.unbed.core.model.AlarmConfig
import kotlinx.coroutines.flow.Flow

interface AlarmConfigRepository {
    fun observeConfig(): Flow<AlarmConfig?>

    suspend fun getConfig(): AlarmConfig?

    suspend fun upsertConfig(config: AlarmConfig)

    suspend fun setEnabled(
        alarmId: Long,
        enabled: Boolean,
    )
}
