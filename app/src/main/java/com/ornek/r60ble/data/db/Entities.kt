package com.ornek.r60ble.data.db

import androidx.room.*

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long, val endTime: Long,
    val distanceKm: Double, val totalCalories: Double,
    val avgPower: Double, val maxPower: Int
)

@Entity(
    tableName = "metrics",
    foreignKeys = [ForeignKey(SessionEntity::class, ["id"], ["sessionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sessionId")]
)
data class MetricEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long, val timestamp: Long,
    val speed: Double, val cadence: Double, val power: Int, val distance: Double
)

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val address: String,
    val name: String, val lastConnected: Long
)
