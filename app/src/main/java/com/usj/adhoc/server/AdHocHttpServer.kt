package com.usj.adhoc.server

import com.usj.adhoc.model.SensorData
import fi.iki.elonen.NanoHTTPD

/**
 * Simple HTTP server (runs on a background thread via NanoHTTPD).
 * Mobile A starts this server; Mobiles B and C GET /data to receive JSON.
 */
class AdHocHttpServer(
    port: Int = 8080,
    private val getSensorData: () -> SensorData
) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        val data = getSensorData()
        val json = buildString {
            append("{")
            append("\"temp\": ${data.temperature},")
            append("\"hum\": ${data.humidity},")
            append("\"dist\": ${data.distance}")
            append("}")
        }
        val response = newFixedLengthResponse(Response.Status.OK, "application/json", json)
        response.addHeader("Access-Control-Allow-Origin", "*")
        return response
    }
}
