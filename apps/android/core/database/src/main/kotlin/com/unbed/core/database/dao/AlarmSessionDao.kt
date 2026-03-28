package com.unbed.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.unbed.core.database.entity.AlarmSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmSessionDao {
    @Query(
        """
        SELECT * FROM alarm_session
        WHERE sessionEndedReason IS NULL
        ORDER BY scheduledAt DESC
        LIMIT 1
        """,
    )
    fun observeActiveSession(): Flow<AlarmSessionEntity?>

    @Query(
        """
        SELECT * FROM alarm_session
        WHERE sessionEndedReason IS NULL
        ORDER BY scheduledAt DESC
        LIMIT 1
        """,
    )
    suspend fun getActiveSession(): AlarmSessionEntity?

    @Query("SELECT * FROM alarm_session WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getById(sessionId: String): AlarmSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: AlarmSessionEntity)
}
