package com.ornek.r60ble.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SessionEntity::class, MetricEntity::class, DeviceEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
}
