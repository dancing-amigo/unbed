package com.unbed.domain.alarm

import com.unbed.core.model.AlarmSession
import kotlinx.coroutines.flow.Flow

interface AlarmSessionRepository {
    fun observeActiveSession(): Flow<AlarmSession?>

    suspend fun getActiveSession(): AlarmSession?

    suspend fun getSession(sessionId: String): AlarmSession?

    suspend fun upsertSession(session: AlarmSession)
}
