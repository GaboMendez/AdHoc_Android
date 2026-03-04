package com.usj.adhoc

import androidx.lifecycle.ViewModel
import com.usj.adhoc.model.SensorData
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

    fun setBtState(state: BtConnectionState) { _btState.value = state }
    fun setConnectedDeviceName(name: String) { _connectedDeviceName.value = name }
    fun setDeviceRole(role: DeviceRole) { _deviceRole.value = role }
    fun setServerRunning(running: Boolean) { _serverRunning.value = running }
    fun setClientTargetIp(ip: String) { _clientTargetIp.value = ip }

    /** Parses a CSV string like "25.00,60.00,30.00" into SensorData.
     *  Handles Arduino 'nan' output (case-insensitive) gracefully as Float.NaN. */
    fun parseSensorData(raw: String) {
        val parts = raw.trim().split(",")
        if (parts.size >= 3) {
            _sensorData.value = SensorData(
                temperature = parts[0].trim().toFloatOrNan(),
                humidity    = parts[1].trim().toFloatOrNan(),
                distance    = parts[2].trim().toFloatOrNan()
            )
            _rawData.value = raw.trim()
        }
    }

    private fun String.toFloatOrNan(): Float =
        this.toFloatOrNull() ?: if (this.equals("nan", ignoreCase = true)) Float.NaN else Float.NaN

    fun updateSensorData(data: SensorData) { _sensorData.value = data }
}
