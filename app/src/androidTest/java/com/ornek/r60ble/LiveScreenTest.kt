package com.ornek.r60ble

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ornek.r60ble.ui.live.MetricCard
import org.junit.Rule
import org.junit.Test

class LiveScreenTest {
    @get:Rule val rule = createComposeRule()

    @Test fun metricCardShowsValue() {
        rule.setContent { MaterialTheme { MetricCard("Güç", "150", "W") } }
        rule.onNodeWithText("150").assertExists()
    }
}
