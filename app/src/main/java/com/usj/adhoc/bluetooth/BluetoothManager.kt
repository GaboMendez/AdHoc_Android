package com.usj.adhoc.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.currentCoroutineContext
import java.io.IOException
import java.util.UUID

/**
 * Manages a Classic Bluetooth RFCOMM connection to an HC-05 module.
 * All I/O runs on Dispatchers.IO; callbacks are posted to the Main dispatcher.
 */
@SuppressLint("MissingPermission")
class BluetoothManager(
    private val bluetoothAdapter: BluetoothAdapter,
    private val onDataReceived: (String) -> Unit,
    private val onConnectionChanged: (connected: Boolean) -> Unit
) {
    companion object {
        private const val TAG = "BluetoothManager"
        /** Standard Serial Port Profile UUID used by HC-05 */
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var socket: BluetoothSocket? = null
    private var connectJob: Job? = null

    // ── Paired Devices ────────────────────────────────────────────────────────

    fun getPairedDevices(): List<BluetoothDevice> =
        bluetoothAdapter.bondedDevices?.toList() ?: emptyList()

    // ── Connection ────────────────────────────────────────────────────────────

    fun connect(device: BluetoothDevice) {
        connectJob?.cancel()
        connectJob = scope.launch {
            closeSocket()
            try {
                val newSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket = newSocket
                bluetoothAdapter.cancelDiscovery()
                newSocket.connect()
                Log.i(TAG, "Connected to ${device.name} (${device.address})")
                withContext(Dispatchers.Main) { onConnectionChanged(true) }
                readLoop(newSocket)
            } catch (e: IOException) {
                Log.e(TAG, "Connection failed: ${e.message}")
                closeSocket()
                withContext(Dispatchers.Main) { onConnectionChanged(false) }
            }
        }
    }

    fun disconnect() {
        connectJob?.cancel()
        closeSocket()
        scope.launch(Dispatchers.Main) { onConnectionChanged(false) }
    }

    // ── Data Sending ──────────────────────────────────────────────────────────

    fun sendData(data: String) {
        scope.launch {
            try {
                socket?.outputStream?.write((data + "\n").toByteArray(Charsets.UTF_8))
                socket?.outputStream?.flush()
            } catch (_: IOException) { }
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private suspend fun readLoop(s: BluetoothSocket) {
        // BufferedReader.readLine() handles \r\n, \r, and \n — covers all HC-05 firmware variants
        Log.d(TAG, "readLoop started")
        val reader = s.inputStream.bufferedReader(Charsets.UTF_8)
        while (currentCoroutineContext().isActive) {
            try {
                val line = reader.readLine()
                if (line == null) {
                    Log.w(TAG, "readLine() returned null — remote closed stream")
                    break
                }
                val trimmed = line.trim()
                Log.d(TAG, "RAW received: '$trimmed'")
                if (trimmed.isNotEmpty()) {
                    withContext(Dispatchers.Main) { onDataReceived(trimmed) }
                }
            } catch (e: IOException) {
                Log.e(TAG, "readLoop IOException: ${e.message}")
                withContext(Dispatchers.Main) { onConnectionChanged(false) }
                break
            }
        }
        Log.d(TAG, "readLoop ended")
    }

    private fun closeSocket() {
        try { socket?.close() } catch (_: IOException) { }
        socket = null
    }

    fun dispose() {
        connectJob?.cancel()
        closeSocket()
        scope.cancel()
    }
}
