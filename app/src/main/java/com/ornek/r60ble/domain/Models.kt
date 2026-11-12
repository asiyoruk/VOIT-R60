package com.ornek.r60ble.domain

data class LiveMetric(
    val timestamp: Long,
    val speed: Double,      // km/h
    val cadence: Double,    // rpm
    val power: Int,         // W
    val distance: Double    // metre (toplam)
)

data class WorkoutSession(
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val distanceKm: Double,
    val totalCalories: Double,
    val avgPower: Double,
    val maxPower: Int,
    val metrics: List<LiveMetric> = emptyList()
) {
    val durationSec: Long get() = (endTime - startTime) / 1000
}

data class ScanDevice(val name: String?, val address: String, val rssi: Int, val isTarget: Boolean)

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data class Connected(val name: String, val address: String) : ConnectionState
    data class Reconnecting(val attempt: Int) : ConnectionState
    data class Error(val message: String) : ConnectionState
}
