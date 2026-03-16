package com.usj.adhoc.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.usj.adhoc.AppViewModel
import com.usj.adhoc.model.DoorStatus
import com.usj.adhoc.bluetooth.BluetoothManager

/**
 * Screen 3 – Control Panel
 * Sends LED_ON / LED_OFF / BUZZER_ON / BUZZER_OFF over the Bluetooth serial link.
 */
@Composable
fun ControlScreen(
    viewModel: AppViewModel,
    bluetoothManager: BluetoothManager?,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Control", style = MaterialTheme.typography.headlineMedium)
        HorizontalDivider()

        // ── LED Section ───────────────────────────────────────────────────────
        Text("💡 LED", style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth())
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    bluetoothManager?.sendData("LED_ON")
                    viewModel.setLedState(true)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("LED ON")
            }
            OutlinedButton(
                onClick = {
                    bluetoothManager?.sendData("LED_OFF")
                    viewModel.setLedState(false)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("LED OFF")
            }
        }

        HorizontalDivider()
        // ── Door Section ──────────────────────────────────────────────────────────
        Text("🚪 Puerta", style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth())
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    bluetoothManager?.sendData("DOOR_OPEN")
                    viewModel.setDoorState(DoorStatus.OPEN)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Abrir")
            }
            OutlinedButton(
                onClick = {
                    bluetoothManager?.sendData("DOOR_CLOSE")
                    viewModel.setDoorState(DoorStatus.CLOSED)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Cerrar")
            }
        }

        HorizontalDivider()
        // ── Buzzer Section ────────────────────────────────────────────────────
        Text("🔔 Buzzer", style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth())
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    bluetoothManager?.sendData("BUZZER_ON")
                    viewModel.setBuzzerState(true)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text("BUZZER ON")
            }
            OutlinedButton(
                onClick = {
                    bluetoothManager?.sendData("BUZZER_OFF")
                    viewModel.setBuzzerState(false)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("BUZZER OFF")
            }
        }

        if (bluetoothManager == null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    "Bluetooth no conectado — los comandos no se enviarán.",
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("← Volver")
        }
    }
}
