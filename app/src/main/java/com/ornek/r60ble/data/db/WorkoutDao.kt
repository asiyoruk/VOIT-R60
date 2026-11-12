package com.ornek.r60ble.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WorkoutDao {
    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    abstract fun observeSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    abstract suspend fun getSession(id: Long): SessionEntity?

    @Query("SELECT * FROM metrics WHERE sessionId = :id ORDER BY timestamp")
    abstract suspend fun getMetrics(id: Long): List<MetricEntity>

    @Insert abstract suspend fun insertSession(s: SessionEntity): Long
    @Insert abstract suspend fun insertMetrics(m: List<MetricEntity>)

    @Transaction
    open suspend fun saveSession(s: SessionEntity, metrics: List<MetricEntity>): Long {
        val id = insertSession(s)
        insertMetrics(metrics.map { it.copy(sessionId = id) })
        return id
    }

    @Query("DELETE FROM sessions WHERE id = :id") abstract suspend fun deleteSession(id: Long)

    @Upsert abstract suspend fun upsertDevice(d: DeviceEntity)
}
