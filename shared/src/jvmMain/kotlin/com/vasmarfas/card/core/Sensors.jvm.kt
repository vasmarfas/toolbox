package com.vasmarfas.card.core

import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.TargetDataLine
import kotlin.math.log10
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

private val osName = (System.getProperty("os.name") ?: "").lowercase()

actual fun availableSensors(): Set<SensorType> = emptySet()

actual fun sensorFlow(type: SensorType): Flow<SensorReading> = emptyFlow()

actual suspend fun requestMotionAccess(): Boolean = false

actual fun locationSupported(): Boolean = false

actual fun locationFlow(): Flow<LocationFix> = emptyFlow()

private fun runCommand(vararg command: String): String = runCatching {
    val process = ProcessBuilder(*command).redirectErrorStream(true).start()
    process.inputStream.bufferedReader().readText().also { process.waitFor() }
}.getOrDefault("")

actual suspend fun batteryInfo(): BatteryInfo? = withContext(Dispatchers.IO) {
    when {
        osName.contains("win") -> {
            val out = runCommand(
                "powershell", "-NoProfile", "-Command",
                "Get-CimInstance Win32_Battery | Select-Object EstimatedChargeRemaining,BatteryStatus,EstimatedRunTime,DesignVoltage,Name | ConvertTo-Json",
            )
            if (out.isBlank() || out.trim() == "null") return@withContext null
            val level = Regex("\"EstimatedChargeRemaining\":\\s*(\\d+)").find(out)?.groupValues?.get(1)?.toIntOrNull()
            val status = Regex("\"BatteryStatus\":\\s*(\\d+)").find(out)?.groupValues?.get(1)?.toIntOrNull()
            val runtime = Regex("\"EstimatedRunTime\":\\s*(\\d+)").find(out)?.groupValues?.get(1)?.toIntOrNull()
            val voltage = Regex("\"DesignVoltage\":\\s*(\\d+)").find(out)?.groupValues?.get(1)?.toIntOrNull()
            val name = Regex("\"Name\":\\s*\"([^\"]*)\"").find(out)?.groupValues?.get(1)
            if (level == null) return@withContext null
            val charging = status == 2 || status in 6..9
            BatteryInfo(
                level, charging,
                buildList {
                    add("Status" to when (status) { 1 -> "discharging"; 2 -> "on AC"; 3 -> "fully charged"; 4 -> "low"; 5 -> "critical"; 6 -> "charging"; 7 -> "charging (high)"; 8 -> "charging (low)"; 9 -> "charging (critical)"; else -> status.toString() })
                    if (runtime != null && runtime in 1..(60 * 24 * 7)) add("Estimated runtime" to "${runtime / 60} h ${runtime % 60} min")
                    if (voltage != null && voltage > 0) add("Design voltage" to "$voltage mV")
                    if (!name.isNullOrBlank()) add("Name" to name)
                },
            )
        }
        osName.contains("mac") -> {
            val out = runCommand("pmset", "-g", "batt")
            val level = Regex("(\\d+)%").find(out)?.groupValues?.get(1)?.toIntOrNull() ?: return@withContext null
            val charging = out.contains("charging") && !out.contains("discharging")
            BatteryInfo(level, charging, listOf("Raw" to out.lines().drop(1).joinToString(" ").trim()))
        }
        else -> {
            val dir = File("/sys/class/power_supply").listFiles()?.firstOrNull { it.name.startsWith("BAT") } ?: return@withContext null
            fun read(name: String) = runCatching { File(dir, name).readText().trim() }.getOrNull()
            val level = read("capacity")?.toIntOrNull() ?: return@withContext null
            val status = read("status") ?: ""
            BatteryInfo(
                level, status.equals("Charging", true) || status.equals("Full", true),
                buildList {
                    add("Status" to status)
                    read("voltage_now")?.toLongOrNull()?.let { add("Voltage" to "${it / 1000} mV") }
                    read("current_now")?.toLongOrNull()?.let { add("Current" to "${it / 1000} mA") }
                    read("energy_full")?.toLongOrNull()?.let { full -> read("energy_full_design")?.toLongOrNull()?.let { design -> add("Health" to "${full * 100 / design}% of design") } }
                    read("cycle_count")?.let { add("Cycle count" to it) }
                    read("technology")?.let { add("Technology" to it) }
                    read("manufacturer")?.let { add("Manufacturer" to it) }
                    read("model_name")?.let { add("Model" to it) }
                },
            )
        }
    }
}

actual fun torchSupported(): Boolean = false

actual fun setTorch(on: Boolean): Boolean = false

actual fun displayExtras(): List<Pair<String, String>> = runCatching {
    val env = GraphicsEnvironment.getLocalGraphicsEnvironment()
    val toolkit = Toolkit.getDefaultToolkit()
    buildList {
        env.screenDevices.forEachIndexed { index, device ->
            val mode = device.displayMode
            add("Display ${index + 1}" to "${mode.width} × ${mode.height} @ ${mode.refreshRate} Hz, ${mode.bitDepth} bit")
            val transform = device.defaultConfiguration.defaultTransform
            add("Scale ${index + 1}" to "${(transform.scaleX * 100).toInt()}%")
        }
        add("Logical DPI" to toolkit.screenResolution.toString())
    }
}.getOrDefault(emptyList())

actual fun screenDpi(): Float? = runCatching { Toolkit.getDefaultToolkit().screenResolution.toFloat() }.getOrNull()

actual fun screenPixels(): Pair<Int, Int>? = runCatching {
    val mode = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.displayMode
    mode.width to mode.height
}.getOrNull()

actual fun microphoneSupported(): Boolean = true

actual fun microphoneLevelFlow(): Flow<Double> = flow {
    val format = AudioFormat(44100f, 16, 1, true, false)
    val line = runCatching { AudioSystem.getTargetDataLine(format) as TargetDataLine }.getOrNull() ?: return@flow
    line.open(format)
    line.start()
    val buffer = ByteArray(4096)
    try {
        while (true) {
            val read = line.read(buffer, 0, buffer.size)
            if (read > 0) {
                var sum = 0.0
                var i = 0
                while (i + 1 < read) {
                    val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)).toShort().toDouble()
                    sum += sample * sample
                    i += 2
                }
                val rms = sqrt(sum / (read / 2))
                emit(20 * log10((rms / 32768.0).coerceAtLeast(1e-9)) + 90)
            }
            delay(50.milliseconds)
        }
    } finally {
        line.stop()
        line.close()
    }
}.flowOn(Dispatchers.IO)

actual fun setScreenBrightness(value: Float?) = Unit
