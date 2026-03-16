package com.usj.adhoc

import androidx.lifecycle.ViewModel
import com.usj.adhoc.model.DoorStatus
import com.usj.adhoc.model.SensorData
import com.usj.adhoc.server.AdHocHttpServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class BtConnectionState { DISCONNECTED, CONNECTING, CONNECTED }
enum class DeviceRole { SERVER, CLIENT }

class AppViewModel : ViewModel() {

    private val _btState = MutableStateFlow(BtConnectionState.DISCONNECTED)
    val btState: StateFlow<BtConnectionState> = _btState.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow("")
    val connectedDeviceName: StateFlow<String> = _connectedDeviceName.asStateFlow()

    private val _sensorData = MutableStateFlow(SensorData())
    val sensorData: StateFlow<SensorData> = _sensorData.asStateFlow()

    private val _rawData = MutableStateFlow("")
    val rawData: StateFlow<String> = _rawData.asStateFlow()

    private val _deviceRole = MutableStateFlow(DeviceRole.SERVER)
    val deviceRole: StateFlow<DeviceRole> = _deviceRole.asStateFlow()

    private val _serverRunning = MutableStateFlow(false)
    val serverRunning: StateFlow<Boolean> = _serverRunning.asStateFlow()

    private val _clientTargetIp = MutableStateFlow("192.168.43.1")
    val clientTargetIp: StateFlow<String> = _clientTargetIp.asStateFlow()

    private val _activeWifiClients = MutableStateFlow(0)
    val activeWifiClients: StateFlow<Int> = _activeWifiClients.asStateFlow()

    fun setBtState(state: BtConnectionState) { _btState.value = state }
    fun setConnectedDeviceName(name: String) { _connectedDeviceName.value = name }
    fun setDeviceRole(role: DeviceRole) { _deviceRole.value = role }
    fun setClientTargetIp(ip: String) { _clientTargetIp.value = ip }
    fun setActiveWifiClients(n: Int) { _activeWifiClients.value = n }
    fun setLedState(on: Boolean) { _sensorData.value = _sensorData.value.copy(ledOn = on) }
    fun setBuzzerState(on: Boolean) { _sensorData.value = _sensorData.value.copy(buzzerOn = on) }
    fun setDoorState(state: DoorStatus) { _sensorData.value = _sensorData.value.copy(doorStatus = state) }

    // ── HTTP Server (survives navigation) ─────────────────────────────────────
    /** Exposed so WifiAdHocScreen can poll getActiveClientCount(). */
    var httpServer: AdHocHttpServer? = null
        private set

    fun startHttpServer() {
        if (httpServer == null) {
            httpServer = AdHocHttpServer(8080) { sensorData.value }
            httpServer!!.start()
        }
        _serverRunning.value = true
    }

    fun stopHttpServer() {
        httpServer?.stop()
        httpServer = null
        _serverRunning.value = false
        _activeWifiClients.value = 0
    }

    fun setServerRunning(running: Boolean) {
        if (running) startHttpServer() else stopHttpServer()
    }

    override fun onCleared() {
        super.onCleared()
        httpServer?.stop()
        httpServer = null
    }

    /** Parses a CSV string like "25.00,60.00,30.00" into SensorData.
     *  Handles Arduino 'nan' output (case-insensitive) gracefully as Float.NaN. */
    fun parseSensorData(raw: String) {
        val parts = raw.trim().split(",")
        if (parts.size >= 3) {
            val current = _sensorData.value
            _sensorData.value = current.copy(
                temperature = parts[0].trim().toFloatOrNan(),
                humidity    = parts[1].trim().toFloatOrNan(),
                distance    = parts[2].trim().toFloatOrNan(),
                doorStatus  = if (parts.size >= 4) {
                    when (parts[3].trim().uppercase()) {
                        "OPEN"   -> DoorStatus.OPEN
                        "CLOSED" -> DoorStatus.CLOSED
                        else     -> DoorStatus.UNKNOWN
                    }
                } else DoorStatus.UNKNOWN,
                ledOn = if (parts.size >= 5) {
                    parts[4].trim().equals("ON", ignoreCase = true)
                } else current.ledOn,
                buzzerOn = if (parts.size >= 6) {
                    parts[5].trim().equals("ON", ignoreCase = true)
                } else current.buzzerOn
            )
            _rawData.value = raw.trim()
        }
    }

    private fun String.toFloatOrNan(): Float =
        this.toFloatOrNull() ?: if (this.equals("nan", ignoreCase = true)) Float.NaN else Float.NaN

    fun updateSensorData(data: SensorData) { _sensorData.value = data }
}
