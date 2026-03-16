package com.usj.adhoc

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.usj.adhoc.bluetooth.BluetoothManager as BtManager
import com.usj.adhoc.screens.ConnectionScreen
import com.usj.adhoc.screens.ControlScreen
import com.usj.adhoc.screens.DashboardScreen
import com.usj.adhoc.screens.WifiAdHocScreen
import com.usj.adhoc.ui.theme.AdHocTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()
    private var btManager: BtManager? by mutableStateOf(null)
    // Sends periodic CLIENTS:N heartbeat while BT is connected so the Arduino watchdog stays alive
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var heartbeatJob: Job? = null

    // ── Permission request ────────────────────────────────────────────────────

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Always attempt init — paired-device use doesn't need scan/location granted
        initBluetooth()
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkAndRequestPermissions()

        setContent {
            AdHocTheme {
                val navController = rememberNavController()
                Scaffold { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "connection",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // Screen 1 – Bluetooth Connection
                        composable("connection") {
                            ConnectionScreen(
                                viewModel = viewModel,
                                bluetoothManager = btManager,
                                onNavigateToDashboard = {
                                    navController.navigate("dashboard") {
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateToAdHoc = {
                                    viewModel.setDeviceRole(DeviceRole.CLIENT)
                                    navController.navigate("adhoc") {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        // Screen 2 – Sensor Dashboard
                        composable("dashboard") {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToControl = { navController.navigate("control") },
                                onNavigateToAdHoc = { navController.navigate("adhoc") }
                            )
                        }

                        // Screen 3 – LED / Buzzer Control
                        composable("control") {
                            ControlScreen(
                                viewModel = viewModel,
                                bluetoothManager = btManager,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // Screen 4 – WiFi AdHoc (HTTP server / client)
                        composable("adhoc") {
                            WifiAdHocScreen(
                                viewModel = viewModel,
                                bluetoothManager = btManager,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        btManager?.dispose()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun checkAndRequestPermissions() {
        // On API < 31 BLUETOOTH / BLUETOOTH_ADMIN are normal (auto-granted).
        // Only ACCESS_FINE_LOCATION needs a runtime request on those versions.
        val needed: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val allGranted = needed.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) initBluetooth() else requestPermissions.launch(needed)
    }

    private fun initBluetooth() {
        val sysManager = getSystemService(BluetoothManager::class.java) ?: return
        val adapter = sysManager.adapter ?: return
        btManager = BtManager(
            bluetoothAdapter = adapter,
            onDataReceived = { data -> viewModel.parseSensorData(data) },
            onConnectionChanged = { connected ->
                viewModel.setBtState(
                    if (connected) BtConnectionState.CONNECTED else BtConnectionState.DISCONNECTED
                )
                if (connected) {
                    // Immediately update display to 1 (this phone = 1 device)
                    viewModel.setActiveWifiClients(0)
                    btManager?.sendData("CLIENTS:1")
                    // Heartbeat every 3s: keeps Arduino watchdog alive and polls WiFi clients
                    // even when the user is on a different screen.
                    heartbeatJob?.cancel()
                    heartbeatJob = mainScope.launch {
                        while (true) {
                            delay(3_000)
                            val wifiClients = if (
                                viewModel.serverRunning.value &&
                                viewModel.deviceRole.value == DeviceRole.SERVER
                            ) {
                                viewModel.httpServer?.getActiveClientCount() ?: 0
                            } else {
                                0
                            }
                            viewModel.setActiveWifiClients(wifiClients)
                            val total = (1 + wifiClients).coerceIn(0, 9)
                            btManager?.sendData("CLIENTS:$total")
                        }
                    }
                } else {
                    viewModel.setActiveWifiClients(0)
                    heartbeatJob?.cancel()
                    heartbeatJob = null
                }
            }
        )
    }
}
