package com.usj.adhoc.server

import android.util.Log
import com.usj.adhoc.model.DoorStatus
import com.usj.adhoc.model.SensorData
import fi.iki.elonen.NanoHTTPD
import java.util.concurrent.ConcurrentHashMap

/**
 * Simple HTTP server (runs on a background thread via NanoHTTPD).
 * Mobile A starts this server; Mobiles B and C GET /data to receive JSON.
 * Also tracks unique client IPs so the app can report the count to the Arduino display.
 */
class AdHocHttpServer(
    port: Int = 8080,
    private val getSensorData: () -> SensorData
) : NanoHTTPD(port) {

    companion object { private const val TAG = "AdHocHttpServer" }

    // IP → last-seen timestamp (ms). Updated on every request.
    private val activeClients = ConcurrentHashMap<String, Long>()

    /** Returns the number of unique IPs that sent a request within [windowMs] milliseconds. */
    fun getActiveClientCount(windowMs: Long = 10_000L): Int {
        val cutoff = System.currentTimeMillis() - windowMs
        // Safe two-step removal — avoids ConcurrentModificationException under parallel NanoHTTPD threads
        val stale = activeClients.entries.filter { it.value < cutoff }.map { it.key }
        stale.forEach { activeClients.remove(it) }
        Log.d(TAG, "Active clients (${windowMs}ms window): ${activeClients.keys.toList()}")
        return activeClients.size
    }

    override fun serve(session: IHTTPSession): Response {
        // Prefer the explicit ?id= query param (sent by the app client) so emulators sharing
        // the same host IP are still counted as distinct devices.
        val queryId = session.parameters["id"]?.firstOrNull()?.trim()?.ifBlank { null }
        val ip = (queryId ?: run {
            val rawIp = session.remoteIpAddress
                ?: session.headers["remote-addr"]
                ?: session.headers["x-forwarded-for"]?.split(",")?.firstOrNull()?.trim()
                ?: session.headers["x-real-ip"]
            rawIp
                ?.removePrefix("::ffff:")
                ?.removeSuffix("/")
                ?.trim()
                ?.ifBlank { null }
        }) ?: "unknown"
        Log.d(TAG, "Request from key='$ip'  queryId=$queryId  remoteIp=${session.remoteIpAddress}")
        activeClients[ip] = System.currentTimeMillis()

        val data = getSensorData()
        val json = buildString {
            append("{")
            append("\"temp\": ${data.temperature},")
            append("\"hum\": ${data.humidity},")
            append("\"dist\": ${data.distance},")
            append("\"door\": \"${data.doorStatus.name}\",")
            append("\"control\": {")
            append("\"door\": \"${data.doorStatus.name}\",")
            append("\"led\": ${data.ledOn},")
            append("\"buzzer\": ${data.buzzerOn}")
            append("}")
            append("}")
        }
        val response = newFixedLengthResponse(Response.Status.OK, "application/json", json)
        response.addHeader("Access-Control-Allow-Origin", "*")
        return response
    }
}
