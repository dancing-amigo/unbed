package com.unbed.core.database.repository

import com.unbed.core.database.dao.AlarmConfigDao
import com.unbed.core.database.dao.AlarmSessionDao
import com.unbed.core.database.mappers.toDomain
import com.unbed.core.database.mappers.toEntity
import com.unbed.core.model.AlarmConfig
import com.unbed.core.model.AlarmSession
import com.unbed.domain.alarm.AlarmConfigRepository
import com.unbed.domain.alarm.AlarmSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomAlarmStore(
    private val alarmConfigDao: AlarmConfigDao,
    private val alarmSessionDao: AlarmSessionDao,
) : AlarmConfigRepository, AlarmSessionRepository {
    override fun observeConfig(): Flow<AlarmConfig?> {
        return alarmConfigDao.observeSingleConfig().map { it?.toDomain() }
    }

    override suspend fun getConfig(): AlarmConfig? {
        return alarmConfigDao.getSingleConfig()?.toDomain()
    }

    override suspend fun upsertConfig(config: AlarmConfig) {
        alarmConfigDao.upsert(config.toEntity())
    }

    override suspend fun setEnabled(
        alarmId: Long,
        enabled: Boolean,
    ) {
        alarmConfigDao.setEnabled(alarmId, enabled)
    }

    override fun observeActiveSession(): Flow<AlarmSession?> {
        return alarmSessionDao.observeActiveSession().map { it?.toDomain() }
    }

    override suspend fun getActiveSession(): AlarmSession? {
        return alarmSessionDao.getActiveSession()?.toDomain()
    }

    override suspend fun getSession(sessionId: String): AlarmSession? {
        return alarmSessionDao.getById(sessionId)?.toDomain()
    }

    override suspend fun upsertSession(session: AlarmSession) {
        alarmSessionDao.upsert(session.toEntity())
    }
}
