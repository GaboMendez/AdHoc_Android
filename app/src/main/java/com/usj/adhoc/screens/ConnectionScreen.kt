package com.usj.adhoc.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.usj.adhoc.AppViewModel
import com.usj.adhoc.BtConnectionState
import com.usj.adhoc.bluetooth.BluetoothManager

/**
 * Screen 1 – Bluetooth Connection
 * Shows paired devices and allows connecting to the HC-05.
 */
@SuppressLint("MissingPermission")
@Composable
fun ConnectionScreen(
    viewModel: AppViewModel,
    bluetoothManager: BluetoothManager?,
    onNavigateToDashboard: () -> Unit,
    onNavigateToAdHoc: () -> Unit
) {
    val btState by viewModel.btState.collectAsState()
    val deviceName by viewModel.connectedDeviceName.collectAsState()

    // Snapshot of paired devices — stable across recompositions
    val pairedDevices: List<BluetoothDevice> = remember(bluetoothManager) {
        bluetoothManager?.getPairedDevices() ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Conexión Bluetooth", style = MaterialTheme.typography.headlineMedium)
        HorizontalDivider()

        // Móvil B/C: skip Bluetooth and go straight to WiFi AdHoc client
        // Hidden when already connected as host via Bluetooth
        if (btState == BtConnectionState.DISCONNECTED) {
            OutlinedButton(
                onClick = onNavigateToAdHoc,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📡 Entrar como Cliente WiFi AdHoc")
            }

            HorizontalDivider()
        }

        when (btState) {
            BtConnectionState.CONNECTED -> {
                Text(
                    "✅ Conectado a: $deviceName",
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyLarge
                )
                Button(onClick = onNavigateToDashboard, modifier = Modifier.fillMaxWidth()) {
                    Text("Ir al Dashboard")
                }
                OutlinedButton(
                    onClick = { bluetoothManager?.disconnect() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Desconectar")
                }
            }

            BtConnectionState.CONNECTING -> {
                CircularProgressIndicator()
                Text("Conectando con $deviceName…")
            }

            BtConnectionState.DISCONNECTED -> {
                if (bluetoothManager == null) {
                    Text(
                        "Inicializando Bluetooth…",
                        color = MaterialTheme.colorScheme.outline
                    )
                } else if (pairedDevices.isEmpty()) {
                    Text("No hay dispositivos emparejados.\nVe a Ajustes → Bluetooth, empareja tu dispositivo y vuelve aquí.")
                } else {
                    Text(
                        "Dispositivos ya emparejados:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(pairedDevices) { device ->
                            BluetoothDeviceCard(device) {
                                viewModel.setBtState(BtConnectionState.CONNECTING)
                                viewModel.setConnectedDeviceName(
                                    device.name ?: device.address
                                )
                                bluetoothManager.connect(device)
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun BluetoothDeviceCard(device: BluetoothDevice, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name ?: "Desconocido",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("Conectar →", color = Color.Black)
        }
    }
}
