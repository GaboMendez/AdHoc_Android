package com.usj.adhoc.model

enum class DoorStatus { OPEN, CLOSED, UNKNOWN }

data class SensorData(
    val temperature: Float = 0f,
    val humidity: Float = 0f,
    val distance: Float = 0f,
    val doorStatus: DoorStatus = DoorStatus.UNKNOWN,
    val ledOn: Boolean = false,
    val buzzerOn: Boolean = false
)
