package com.ornek.r60ble.data.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.store by preferencesDataStore("settings")

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val DARK = booleanPreferencesKey("dark")
    private val AUTO = booleanPreferencesKey("auto_reconnect")

    val darkTheme: Flow<Boolean> = context.store.data.map { it[DARK] ?: false }
    val autoReconnect: Flow<Boolean> = context.store.data.map { it[AUTO] ?: true }

    suspend fun setDark(v: Boolean) { context.store.edit { it[DARK] = v } }
    suspend fun setAutoReconnect(v: Boolean) { context.store.edit { it[AUTO] = v } }
}
