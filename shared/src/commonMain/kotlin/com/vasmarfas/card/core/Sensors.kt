package com.vasmarfas.card.core

import kotlinx.coroutines.flow.Flow

enum class SensorType { ACCELEROMETER, GYROSCOPE, MAGNETOMETER, LIGHT, PRESSURE, PROXIMITY, ORIENTATION, STEP_COUNTER, HUMIDITY, TEMPERATURE, GRAVITY }

class SensorReading(val values: FloatArray, val timestampMs: Long) {
    val x: Float get() = values.getOrElse(0) { 0f }
    val y: Float get() = values.getOrElse(1) { 0f }
    val z: Float get() = values.getOrElse(2) { 0f }
}

data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val accuracy: Double?,
    val speed: Double?,
    val bearing: Double?,
    val timestampMs: Long,
    val provider: String,
)

data class BatteryInfo(
    val level: Int?,
    val charging: Boolean?,
    val details: List<Pair<String, String>>,
)

expect fun availableSensors(): Set<SensorType>

expect fun sensorFlow(type: SensorType): Flow<SensorReading>

expect suspend fun requestMotionAccess(): Boolean

expect fun locationSupported(): Boolean

expect fun locationFlow(): Flow<LocationFix>

expect suspend fun batteryInfo(): BatteryInfo?

expect fun torchSupported(): Boolean

expect fun setTorch(on: Boolean): Boolean

expect fun displayExtras(): List<Pair<String, String>>

expect fun screenDpi(): Float?

expect fun screenPixels(): Pair<Int, Int>?

expect fun appleScreen(): AppleScreen?

// only the desktop can read EDID
expect suspend fun displayPanels(): List<DisplayPanel>

expect fun microphoneSupported(): Boolean

expect fun microphoneLevelFlow(): Flow<Double>

expect fun setScreenBrightness(value: Float?)
