package com.ornek.r60ble.data

import com.ornek.r60ble.data.ble.BleManager
import com.ornek.r60ble.data.db.*
import com.ornek.r60ble.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val dao: WorkoutDao,
    private val ble: BleManager
) {
    val connection: StateFlow<ConnectionState> = ble.state
    val live: SharedFlow<LiveMetric> = ble.metrics
    fun scan() = ble.scan()
    fun connect(address: String) = ble.connect(address)
    fun disconnect() = ble.disconnect()
    fun setAutoReconnect(v: Boolean) { ble.autoReconnect = v }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val buffer = ArrayList<LiveMetric>()
    private val _recordingStart = MutableStateFlow<Long?>(null)
    val recordingStart: StateFlow<Long?> = _recordingStart.asStateFlow()

    init {
        scope.launch {
            ble.metrics.collect { m -> if (_recordingStart.value != null) synchronized(buffer) { buffer.add(m) } }
        }
        scope.launch {
            ble.state.filterIsInstance<ConnectionState.Connected>().collect {
                dao.upsertDevice(DeviceEntity(it.address, it.name, System.currentTimeMillis()))
            }
        }
    }

    fun startRecording() { synchronized(buffer) { buffer.clear() }; _recordingStart.value = System.currentTimeMillis() }

    /** Oturumu durdurur ve kaydeder; veri yoksa null döner. */
    suspend fun stopRecording(): Long? {
        val start = _recordingStart.value ?: return null
        _recordingStart.value = null
        val data = synchronized(buffer) { buffer.toList().also { buffer.clear() } }
        if (data.isEmpty()) return null
        return saveSession(SessionCalculator.build(start, System.currentTimeMillis(), data))
    }

    suspend fun saveSession(s: WorkoutSession): Long = dao.saveSession(
        SessionEntity(startTime = s.startTime, endTime = s.endTime, distanceKm = s.distanceKm,
            totalCalories = s.totalCalories, avgPower = s.avgPower, maxPower = s.maxPower),
        s.metrics.map { MetricEntity(sessionId = 0, timestamp = it.timestamp, speed = it.speed,
            cadence = it.cadence, power = it.power, distance = it.distance) }
    )

    fun sessions(): Flow<List<WorkoutSession>> = dao.observeSessions().map { l -> l.map { it.toDomain() } }

    suspend fun session(id: Long): WorkoutSession? = dao.getSession(id)?.toDomain(
        dao.getMetrics(id).map { LiveMetric(it.timestamp, it.speed, it.cadence, it.power, it.distance) }
    )

    suspend fun delete(id: Long) = dao.deleteSession(id)

    private fun SessionEntity.toDomain(m: List<LiveMetric> = emptyList()) =
        WorkoutSession(id, startTime, endTime, distanceKm, totalCalories, avgPower, maxPower, m)
}
