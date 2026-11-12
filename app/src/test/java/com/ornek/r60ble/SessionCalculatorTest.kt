package com.ornek.r60ble

import com.ornek.r60ble.domain.LiveMetric
import com.ornek.r60ble.domain.SessionCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionCalculatorTest {
    @Test fun computesAggregates() {
        val m = listOf(
            LiveMetric(0, 20.0, 80.0, 100, 0.0),
            LiveMetric(10_000, 20.0, 80.0, 200, 55.0),
            LiveMetric(20_000, 20.0, 80.0, 300, 110.0)
        )
        val s = SessionCalculator.build(0, 20_000, m)
        assertEquals(0.11, s.distanceKm, 1e-6)
        assertEquals(200.0, s.avgPower, 1e-6)
        assertEquals(300, s.maxPower)
        assertEquals(3.0, s.totalCalories, 1e-6) // (100*10 + 200*10)/1000
    }
}
