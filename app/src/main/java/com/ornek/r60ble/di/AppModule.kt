package com.ornek.r60ble.di

import android.content.Context
import androidx.room.Room
import com.ornek.r60ble.data.db.AppDatabase
import com.ornek.r60ble.data.db.WorkoutDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun db(@ApplicationContext c: Context): AppDatabase =
        Room.databaseBuilder(c, AppDatabase::class.java, "r60.db").build()

    @Provides fun dao(db: AppDatabase): WorkoutDao = db.workoutDao()
}
