package com.usj.adhoc.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.usj.adhoc.AppViewModel
import com.usj.adhoc.model.DoorStatus

/**
 * Screen 2 – Sensor Dashboard
 * Displays Temperature, Humidity and Distance received from the Arduino via HC-05.
 * Data format: "25.00,60.00,30.00"
 */
@Composable
fun DashboardScreen(
    viewModel: AppViewModel,
    onNavigateToControl: () -> Unit,
    onNavigateToAdHoc: () -> Unit
) {
    val data by viewModel.sensorData.collectAsState()
    val rawData by viewModel.rawData.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Dashboard", style = MaterialTheme.typography.headlineMedium)
        HorizontalDivider()

        SensorCard(label = "🌡 Temperatura", value = data.temperature.formatSensor("°C"))
        SensorCard(label = "💧 Humedad", value = data.humidity.formatSensor("%"))
        SensorCard(label = "📏 Distancia", value = data.distance.formatSensor("cm"))
        DoorCard(status = data.doorStatus)

        if (rawData.isNotEmpty()) {
            Text(
                text = "Raw: $rawData",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        } else {
            Text(
                text = "Esperando datos del Arduino…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onNavigateToControl,
                modifier = Modifier.weight(1f)
            ) {
                Text("Control")
            }
            Button(
                onClick = onNavigateToAdHoc,
                modifier = Modifier.weight(1f)
            ) {
                Text("WiFi AdHoc")
            }
        }
    }
}

@Composable
fun DoorCard(status: DoorStatus) {
    val (icon, label, color) = when (status) {
        DoorStatus.OPEN    -> Triple("🟢", "OPEN",    Color(0xFF2E7D32))
        DoorStatus.CLOSED  -> Triple("🔴", "CLOSED", Color(0xFFC62828))
        DoorStatus.UNKNOWN -> Triple("⚪", "--",     Color(0xFF9E9E9E))
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🚪 Puerta", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "$icon $label",
                style = MaterialTheme.typography.headlineSmall,
                color = color
            )
        }
    }
}

@Composable
fun SensorCard(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

/** Shows two decimal places with unit, or "N/A" (no unit) when the sensor reports NaN. */
private fun Float.formatSensor(unit: String): String =
    if (this.isNaN()) "N/A" else "%.2f".format(this) + " $unit"