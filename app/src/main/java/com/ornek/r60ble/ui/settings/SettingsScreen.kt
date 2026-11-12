package com.ornek.r60ble.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ornek.r60ble.data.WorkoutRepository
import com.ornek.r60ble.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val repo: WorkoutRepository
) : ViewModel() {
    val dark = settings.darkTheme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val auto = settings.autoReconnect.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    init { viewModelScope.launch { settings.autoReconnect.collect { repo.setAutoReconnect(it) } } }

    fun setDark(v: Boolean) = viewModelScope.launch { settings.setDark(v) }
    fun setAuto(v: Boolean) = viewModelScope.launch { settings.setAutoReconnect(v) }
}

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val dark by vm.dark.collectAsStateWithLifecycle()
    val auto by vm.auto.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Ayarlar", style = MaterialTheme.typography.headlineSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Koyu tema", Modifier.weight(1f)); Switch(dark, vm::setDark)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Otomatik yeniden bağlan", Modifier.weight(1f)); Switch(auto, vm::setAuto)
        }
        Text(
            "Dil: uygulama sistem dilini kullanır. (Çoklu dil için res/values-xx/strings.xml ekleyin.)",
            style = MaterialTheme.typography.bodySmall
        )
        Text("Gizlilik: veriler yalnızca cihazda saklanır; yedekleme kapalıdır; CSV yalnızca siz paylaşınca dışarı çıkar.",
            style = MaterialTheme.typography.bodySmall)
    }
}
