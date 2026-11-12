package com.ornek.r60ble.ui.live

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ornek.r60ble.data.WorkoutRepository
import com.ornek.r60ble.domain.LiveMetric
import com.ornek.r60ble.util.formatDuration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LiveUiState(
    val latest: LiveMetric? = null,
    val history: List<LiveMetric> = emptyList(),
    val recordingStart: Long? = null,
    val message: String? = null
)

@HiltViewModel
class LiveViewModel @Inject constructor(private val repo: WorkoutRepository) : ViewModel() {
    private val window = MutableStateFlow<List<LiveMetric>>(emptyList())
    private val message = MutableStateFlow<String?>(null)

    val ui: StateFlow<LiveUiState> = combine(window, repo.recordingStart, message) { w, rec, msg ->
        LiveUiState(w.lastOrNull(), w, rec, msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LiveUiState())

    init {
        viewModelScope.launch { repo.live.collect { m -> window.update { (it + m).takeLast(120) } } }
    }

    fun toggleRecording() {
        if (repo.recordingStart.value == null) { repo.startRecording(); message.value = null }
        else viewModelScope.launch {
            val id = repo.stopRecording()
            message.value = if (id != null) "Oturum kaydedildi" else "Kaydedilecek veri yok"
        }
    }
}

@Composable
fun LiveScreen(vm: LiveViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val m = ui.latest
    val elapsed = ui.recordingStart?.let { ((m?.timestamp ?: it) - it).coerceAtLeast(0) / 1000 } ?: 0L

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Canlı", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Hız", "%.1f".format(m?.speed ?: 0.0), "km/h", Modifier.weight(1f))
            MetricCard("Kadans", "%.0f".format(m?.cadence ?: 0.0), "rpm", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Güç", "${m?.power ?: 0}", "W", Modifier.weight(1f))
            MetricCard("Mesafe", "%.2f".format((m?.distance ?: 0.0) / 1000), "km", Modifier.weight(1f))
        }
        Button(onClick = vm::toggleRecording, modifier = Modifier.fillMaxWidth()) {
            Text(if (ui.recordingStart == null) "Oturumu başlat" else "Durdur ve kaydet (${formatDuration(elapsed)})")
        }
        ui.message?.let { Text(it) }
        LineChart("Hız (km/h)", ui.history.map { it.speed.toFloat() })
        LineChart("Kadans (rpm)", ui.history.map { it.cadence.toFloat() })
        LineChart("Güç (W)", ui.history.map { it.power.toFloat() })
    }
}

@Composable
fun MetricCard(title: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.displaySmall)
            Text(unit, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun LineChart(label: String, values: List<Float>, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Canvas(Modifier.fillMaxWidth().height(80.dp)) {
            if (values.size < 2) return@Canvas
            val max = values.max().coerceAtLeast(1f)
            val path = Path()
            values.forEachIndexed { i, v ->
                val x = size.width * i / (values.size - 1)
                val y = size.height * (1 - v / max)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color, style = Stroke(width = 3f))
        }
    }
}
