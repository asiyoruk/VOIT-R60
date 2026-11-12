package com.ornek.r60ble.ui.history

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ornek.r60ble.data.WorkoutRepository
import com.ornek.r60ble.domain.WorkoutSession
import com.ornek.r60ble.util.CsvExporter
import com.ornek.r60ble.util.formatDate
import com.ornek.r60ble.util.formatDuration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(private val repo: WorkoutRepository) : ViewModel() {
    val sessions = repo.sessions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun export(context: Context, id: Long) = viewModelScope.launch {
        val s = repo.session(id) ?: return@launch
        val uri = CsvExporter.export(context, s)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "CSV dışa aktar").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun delete(id: Long) = viewModelScope.launch { repo.delete(id) }
}

@Composable
fun HistoryScreen(vm: HistoryViewModel = hiltViewModel()) {
    val ctx = LocalContext.current
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<WorkoutSession?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Geçmiş oturumlar", style = MaterialTheme.typography.headlineSmall)
        if (sessions.isEmpty()) Text("Henüz kayıtlı oturum yok.", Modifier.padding(top = 12.dp))
        LazyColumn {
            items(sessions, key = { it.id }) { s ->
                ListItem(
                    headlineContent = { Text(formatDate(s.startTime)) },
                    supportingContent = { Text("${formatDuration(s.durationSec)} • %.2f km • ort. %.0f W".format(s.distanceKm, s.avgPower)) },
                    modifier = Modifier.clickable { selected = s }
                )
            }
        }
    }

    selected?.let { s ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(formatDate(s.startTime)) },
            text = {
                Text("Süre: ${formatDuration(s.durationSec)}\nMesafe: %.2f km\nKalori: %.0f kcal\nOrt. güç: %.0f W\nMaks. güç: %d W"
                    .format(s.distanceKm, s.totalCalories, s.avgPower, s.maxPower))
            },
            confirmButton = { TextButton(onClick = { vm.export(ctx, s.id); selected = null }) { Text("CSV dışa aktar") } },
            dismissButton = { TextButton(onClick = { vm.delete(s.id); selected = null }) { Text("Sil") } }
        )
    }
}
