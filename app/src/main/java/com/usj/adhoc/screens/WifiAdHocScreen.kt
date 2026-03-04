package com.usj.adhoc.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.usj.adhoc.AppViewModel
import com.usj.adhoc.DeviceRole
import androidx.compose.ui.graphics.Color
import com.usj.adhoc.model.DoorStatus
import com.usj.adhoc.model.SensorData
import com.usj.adhoc.server.AdHocHttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Screen 4 – WiFi AdHoc
 *
 * Role SERVER (Móvil A):
 *   Starts a NanoHTTPD HTTP server on port 8080.
 *   GET /data  →  {"temp": X, "hum": Y, "dist": Z}
 *
 * Role CLIENT (Móvil B / C):
 *   Periodically sends GET requests to the server IP and displays received data.
 */
@Composable
fun WifiAdHocScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val role by viewModel.deviceRole.collectAsState()
    val serverRunning by viewModel.serverRunning.collectAsState()
    val sensorData by viewModel.sensorData.collectAsState()
    val targetIp by viewModel.clientTargetIp.collectAsState()

    // NanoHTTPD instance – only alive while serverRunning == true in SERVER mode
    var httpServer by remember { mutableStateOf<AdHocHttpServer?>(null) }

    // Client-side state
    var clientData by remember { mutableStateOf<SensorData?>(null) }
    var clientError by remember { mutableStateOf("") }
    var autoRefresh by remember { mutableStateOf(false) }
    var isFetching by remember { mutableStateOf(false) }
    var isServerStarting by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Start / stop server when flag changes
    LaunchedEffect(role, serverRunning) {
        if (role == DeviceRole.SERVER && serverRunning) {
            if (httpServer == null) {
                isServerStarting = true
                httpServer = AdHocHttpServer(8080) { viewModel.sensorData.value }
                httpServer!!.start()
                isServerStarting = false
            }
        } else {
            httpServer?.stop()
            httpServer = null
        }
    }

    // Auto-refresh loop for client
    LaunchedEffect(autoRefresh, role) {
        if (autoRefresh && role == DeviceRole.CLIENT) {
            while (autoRefresh) {
                isFetching = true
                fetchData(targetIp = viewModel.clientTargetIp.value,
                    onSuccess = { clientData = it; clientError = "" },
                    onError = { clientError = it })
                isFetching = false
                delay(3_000)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            httpServer?.stop()
            httpServer = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("WiFi AdHoc", style = MaterialTheme.typography.headlineMedium)
        HorizontalDivider()

        // ── Role Toggle ───────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Rol del dispositivo:  ", style = MaterialTheme.typography.bodyLarge)
            Button(
                onClick = {
                    autoRefresh = false
                    viewModel.setDeviceRole(
                        if (role == DeviceRole.SERVER) DeviceRole.CLIENT else DeviceRole.SERVER
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (role == DeviceRole.SERVER)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(if (role == DeviceRole.SERVER) "Servidor (Móvil A)" else "Cliente (Móvil B/C)")
            }
        }

        HorizontalDivider()

        if (role == DeviceRole.SERVER) {
            // ── SERVER UI ─────────────────────────────────────────────────────
            Text("Servidor HTTP – puerto 8080", style = MaterialTheme.typography.titleMedium)

            if (!serverRunning) {
                Button(
                    onClick = { viewModel.setServerRunning(true) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Iniciar Servidor") }
            } else if (isServerStarting) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Iniciando servidor…")
                }
            } else {
                OutlinedButton(
                    onClick = { viewModel.setServerRunning(false) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Detener Servidor") }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "🟢 Servidor activo",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("GET /data  →  JSON")
                        Text("Temp: ${sensorData.temperature.fmtServer()} °C")
                        Text("Hum:  ${sensorData.humidity.fmtServer()} %")
                        Text("Dist: ${sensorData.distance.fmtServer()} cm")
                        if (sensorData.doorStatus != DoorStatus.UNKNOWN) {
                            val doorText = if (sensorData.doorStatus == DoorStatus.OPEN)
                                "🟢 Puerta: ABIERTA" else "🔴 Puerta: CERRADA"
                            Text(
                                text = doorText,
                                color = if (sensorData.doorStatus == DoorStatus.OPEN)
                                    Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }                      
                    }
                }
            }
        } else {
            // ── CLIENT UI ─────────────────────────────────────────────────────
            Text("Cliente HTTP", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = targetIp,
                onValueChange = { viewModel.setClientTargetIp(it) },
                label = { Text("IP del Servidor (Móvil A)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        clientError = ""
                        scope.launch {
                            isFetching = true
                            fetchData(
                                targetIp = targetIp,
                                onSuccess = { clientData = it; clientError = "" },
                                onError = { clientError = it }
                            )
                            isFetching = false
                        }
                    },
                    enabled = !isFetching,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isFetching && !autoRefresh) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Solicitar Datos")
                    }
                }

                OutlinedButton(
                    onClick = { autoRefresh = !autoRefresh },
                    modifier = Modifier.weight(1f),
                    colors = if (autoRefresh) ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ) else ButtonDefaults.outlinedButtonColors()
                ) {
                    if (autoRefresh) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text("⏹ Auto")
                        }
                    } else {
                        Text("▶ Auto")
                    }
                }
            }

            clientData?.let { data ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Datos recibidos:", style = MaterialTheme.typography.titleSmall)
                            if (isFetching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("🌡 Temperatura: ${data.temperature.fmtServer()} °C")
                        Text("💧 Humedad:     ${data.humidity.fmtServer()} %")
                        Text("📏 Distancia:   ${data.distance.fmtServer()} cm")
                        if (data.doorStatus != DoorStatus.UNKNOWN) {
                            val isOpen = data.doorStatus == DoorStatus.OPEN
                            Text(
                                text = if (isOpen) "🟢 Puerta: ABIERTA" else "🔴 Puerta: CERRADA",
                                color = if (isOpen) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    }
                }
            }

            if (clientError.isNotEmpty()) {
                Text(
                    "⚠ $clientError",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("← Volver")
        }
    }
}

/** Formats a sensor float for display; shows "N/A" for NaN values. */
private fun Float.fmtServer(): String =
    if (this.isNaN()) "N/A" else "%.2f".format(this)

/** Suspending helper – fetches JSON from the server and parses it. */
private suspend fun fetchData(
    targetIp: String,
    onSuccess: (SensorData) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val result = withContext(Dispatchers.IO) {
            val url = URL("http://$targetIp:8080/data")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            val text = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            text
        }
        val json = JSONObject(result)
        onSuccess(
            SensorData(
                temperature = json.getDouble("temp").toFloat(),
                humidity = json.getDouble("hum").toFloat(),
                distance = json.getDouble("dist").toFloat(),
                doorStatus = when (json.optString("door", "UNKNOWN").uppercase()) {
                    "OPEN"   -> DoorStatus.OPEN
                    "CLOSED" -> DoorStatus.CLOSED
                    else     -> DoorStatus.UNKNOWN
                }
            )
        )
    } catch (e: Exception) {
        onError(e.message ?: "Error de conexión")
    }
}
