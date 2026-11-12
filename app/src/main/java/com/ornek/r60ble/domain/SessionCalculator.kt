package com.ornek.r60ble.domain

object SessionCalculator {
    fun build(start: Long, end: Long, metrics: List<LiveMetric>): WorkoutSession {
        if (metrics.isEmpty()) return WorkoutSession(0, start, end, 0.0, 0.0, 0.0, 0)
        val distM = (metrics.last().distance - metrics.first().distance).coerceAtLeast(0.0)
        var kJ = 0.0 // mekanik iş (kJ); ~%25 verimle 1 kJ ≈ 1 kcal
        metrics.zipWithNext().forEach { (a, b) ->
            kJ += a.power * ((b.timestamp - a.timestamp) / 1000.0) / 1000.0
        }
        return WorkoutSession(
            startTime = start, endTime = end,
            distanceKm = distM / 1000.0,
            totalCalories = kJ,
            avgPower = metrics.map { it.power }.average(),
            maxPower = metrics.maxOf { it.power },
            metrics = metrics
        )
    }
}
