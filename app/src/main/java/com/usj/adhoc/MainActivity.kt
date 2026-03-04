package com.usj.adhoc

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.usj.adhoc.bluetooth.BluetoothManager as BtManager
import com.usj.adhoc.screens.ConnectionScreen
import com.usj.adhoc.screens.ControlScreen
import com.usj.adhoc.screens.DashboardScreen
import com.usj.adhoc.screens.WifiAdHocScreen
import com.usj.adhoc.ui.theme.AdHocTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()
    private var btManager: BtManager? = null

    // ── Permission request ────────────────────────────────────────────────────

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.all { it }) {
            initBluetooth()
        }
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
                                bluetoothManager = btManager,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // Screen 4 – WiFi AdHoc (HTTP server / client)
                        composable("adhoc") {
                            WifiAdHocScreen(
                                viewModel = viewModel,
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
        val needed = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.INTERNET
        )
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
            }
        )
    }
}
