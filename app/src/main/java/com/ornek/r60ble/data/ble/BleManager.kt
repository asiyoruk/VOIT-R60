package com.ornek.r60ble.data.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import com.ornek.r60ble.domain.ConnectionState
import com.ornek.r60ble.domain.LiveMetric
import com.ornek.r60ble.domain.ScanDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@SuppressLint("MissingPermission") // izinler UI katmanında istenir
@Singleton
class BleManager @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        val FTMS_SERVICE: UUID = UUID.fromString("00001826-0000-1000-8000-00805f9b34fb")
        val INDOOR_BIKE_DATA: UUID = UUID.fromString("00002ad2-0000-1000-8000-00805f9b34fb")
        val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        const val TARGET_NAME = "R60"
        const val MAX_RETRIES = 5
    }

    private val adapter: BluetoothAdapter? get() =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _metrics = MutableSharedFlow<LiveMetric>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val metrics: SharedFlow<LiveMetric> = _metrics.asSharedFlow()

    @Volatile var autoReconnect = true
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var gatt: BluetoothGatt? = null
    private var address: String? = null
    private var userClosed = false
    private var attempts = 0
    private var last: LiveMetric? = null

    fun scan(): Flow<ScanDevice> = callbackFlow {
        val scanner = adapter?.takeIf { it.isEnabled }?.bluetoothLeScanner
        if (scanner == null) { close(IllegalStateException("Bluetooth kapalı")); return@callbackFlow }
        val cb = object : ScanCallback() {
            override fun onScanResult(type: Int, r: ScanResult) {
                val name = r.scanRecord?.deviceName ?: r.device.name
                val hasFtms = r.scanRecord?.serviceUuids?.contains(ParcelUuid(FTMS_SERVICE)) == true
                val byName = name?.contains(TARGET_NAME, ignoreCase = true) == true
                trySend(ScanDevice(name, r.device.address, r.rssi, hasFtms || byName))
            }
            override fun onScanFailed(errorCode: Int) { close(IllegalStateException("Tarama hatası: $errorCode")) }
        }
        scanner.startScan(null, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), cb)
        awaitClose { runCatching { scanner.stopScan(cb) } }
    }

    fun connect(addr: String) {
        userClosed = false; address = addr; attempts = 0; last = null
        open(addr)
    }

    private fun open(addr: String) {
        val a = adapter
        if (a == null || !a.isEnabled) { _state.value = ConnectionState.Error("Bluetooth kapalı"); return }
        _state.value = ConnectionState.Connecting
        gatt?.close()
        gatt = a.getRemoteDevice(addr).connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
    }

    fun disconnect() {
        userClosed = true
        gatt?.disconnect(); gatt?.close(); gatt = null
        _state.value = ConnectionState.Disconnected
    }

    private fun handleData(bytes: ByteArray) {
        val m = FtmsParser.parse(bytes, System.currentTimeMillis(), last) ?: return
        last = m
        _metrics.tryEmit(m)
    }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                attempts = 0
                g.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                g.close()
                val addr = address
                if (!userClosed && autoReconnect && addr != null && attempts < MAX_RETRIES) {
                    attempts++
                    _state.value = ConnectionState.Reconnecting(attempts)
                    scope.launch { delay(2000L * attempts); if (!userClosed) open(addr) }
                } else {
                    val cur = _state.value
                    _state.value = if (userClosed || cur is ConnectionState.Error) cur.takeIf { it is ConnectionState.Error } ?: ConnectionState.Disconnected
                    else ConnectionState.Error("Bağlantı koptu (kod $status)")
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val ch = g.getService(FTMS_SERVICE)?.getCharacteristic(INDOOR_BIKE_DATA)
            if (status != BluetoothGatt.GATT_SUCCESS || ch == null) {
                _state.value = ConnectionState.Error("Cihaz FTMS Indoor Bike Data sunmuyor")
                userClosed = true; g.disconnect(); return
            }
            g.setCharacteristicNotification(ch, true)
            val cccd = ch.getDescriptor(CCCD)
            if (Build.VERSION.SDK_INT >= 33) {
                g.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                @Suppress("DEPRECATION") run {
                    cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    g.writeDescriptor(cccd)
                }
            }
            _state.value = ConnectionState.Connected(g.device.name ?: "R60", g.device.address)
        }

        // Android 13+
        override fun onCharacteristicChanged(g: BluetoothGatt, ch: BluetoothGattCharacteristic, value: ByteArray) {
            if (ch.uuid == INDOOR_BIKE_DATA) handleData(value)
        }

        // Android 12 ve altı
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(g: BluetoothGatt, ch: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            if (ch.uuid == INDOOR_BIKE_DATA) ch.value?.let(::handleData)
        }
    }
}
