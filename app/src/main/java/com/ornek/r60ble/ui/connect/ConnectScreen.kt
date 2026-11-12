package com.ornek.r60ble.ui.connect

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ornek.r60ble.data.WorkoutRepository
import com.ornek.r60ble.domain.ConnectionState
import com.ornek.r60ble.domain.ScanDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class ConnectViewModel @Inject constructor(private val repo: WorkoutRepository) : ViewModel() {
    val connection = repo.connection
    private val _devices = MutableStateFlow<List<ScanDevice>>(emptyList())
    val devices = _devices.asStateFlow()
    private val _scanning = MutableStateFlow(false)
    val scanning = _scanning.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private var job: Job? = null

    fun showError(msg: String) { _error.value = msg }

    fun startScan() {
        job?.cancel()
        _devices.value = emptyList(); _error.value = null; _scanning.value = true
        job = viewModelScope.launch {
            try {
                withTimeoutOrNull(15_000) {
                    repo.scan().collect { d ->
                        _devices.update { list ->
                            (list.filterNot { it.address == d.address } + d)
                                .sortedWith(compareByDescending<ScanDevice> { it.isTarget }.thenByDescending { it.rssi })
                        }
                    }
                }
            } catch (e: Exception) { _error.value = e.message } finally { _scanning.value = false }
        }
    }

    fun stopScan() { job?.cancel() }
    fun connect(d: ScanDevice) { stopScan(); repo.connect(d.address) }
    fun disconnect() = repo.disconnect()
}

@Composable
fun ConnectScreen(vm: ConnectViewModel = hiltViewModel()) {
    val ctx = LocalContext.current
    val state by vm.connection.collectAsStateWithLifecycle()
    val devices by vm.devices.collectAsStateWithLifecycle()
    val scanning by vm.scanning.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()

    val perms = remember {
        if (Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION) // Android 6-11: BLE taraması için konum izni
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res.values.all { it }) vm.startScan() else vm.showError("Tarama için izin gerekli")
    }
    fun scan() {
        if (perms.all { ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED }) vm.startScan()
        else launcher.launch(perms)
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Bağlantı", style = MaterialTheme.typography.headlineSmall)
        val statusText = when (val s = state) {
            ConnectionState.Disconnected -> "Bağlı değil"
            ConnectionState.Connecting -> "Bağlanıyor…"
            is ConnectionState.Connected -> "Bağlı: ${s.name}"
            is ConnectionState.Reconnecting -> "Yeniden bağlanıyor (deneme ${s.attempt})…"
            is ConnectionState.Error -> "Hata: ${s.message}"
        }
        Text(statusText, style = MaterialTheme.typography.titleMedium)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = ::scan, enabled = !scanning) { Text(if (scanning) "Taranıyor…" else "Tara") }
            OutlinedButton(
                onClick = vm::disconnect,
                enabled = state !is ConnectionState.Disconnected
            ) { Text("Bağlantıyı kapat") }
        }
        if (scanning) LinearProgressIndicator(Modifier.fillMaxWidth())

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(devices, key = { it.address }) { d ->
                ListItem(
                    headlineContent = { Text(d.name ?: "Bilinmeyen cihaz") },
                    supportingContent = { Text("${d.address} • ${d.rssi} dBm" + if (d.isTarget) " • R60/FTMS" else "") },
                    modifier = Modifier.clickable { vm.connect(d) }
                )
            }
        }
    }
}
