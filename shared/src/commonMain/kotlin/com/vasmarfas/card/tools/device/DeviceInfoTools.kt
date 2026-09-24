package com.vasmarfas.card.tools.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.APP_VERSION
import com.vasmarfas.card.core.BatteryInfo
import com.vasmarfas.card.core.InterfaceInfo
import com.vasmarfas.card.core.NetCapabilities
import com.vasmarfas.card.core.availableSensors
import com.vasmarfas.card.core.batteryInfo
import com.vasmarfas.card.core.displayExtras
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.core.networkInterfaces
import com.vasmarfas.card.core.platformInfo
import com.vasmarfas.card.core.str
import com.vasmarfas.card.core.systemTimeZoneId
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.LoadingRow
import com.vasmarfas.card.ui.components.ResultCard
import kotlinx.coroutines.delay

val deviceInfoTool = Tool(
    id = "device-info",
    category = ToolCategory.DEVICE,
    title = Res.string.device_info,
    description = Res.string.device_info_description,
    icon = Icons.Filled.Info,
    keywords = listOf("system", "hardware", "model", "android version", "browser", "устройство", "система", "модель"),
) { DeviceInfoScreen() }

@Composable
private fun DeviceInfoScreen() {
    val info = remember { platformInfo() }
    val sensors = remember { availableSensors() }
    ResultCard(title = Res.string.system_title.str()) {
        KeyValueRow(Res.string.platform.str(), info.kind.title.str(), mono = false)
        KeyValueRow("OS", "${info.osName} ${info.osVersion}", mono = false)
        KeyValueRow(Res.string.model.str(), info.deviceModel, mono = false)
        KeyValueRow(Res.string.locale.str(), info.locale)
        KeyValueRow(Res.string.time_zone.str(), systemTimeZoneId())
        info.extra.forEach { (k, v) -> KeyValueRow(fieldName(k), fieldValue(v), mono = false) }
        KeyValueRow(Res.string.app_version.str(), APP_VERSION, copyable = false)
    }
    ResultCard(title = Res.string.sensors.str()) {
        if (sensors.isEmpty()) Text(Res.string.device_no_sensors_are_exposed.str(), style = MaterialTheme.typography.bodyMedium)
        else Text(sensors.map { sensorName(it) }.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
    }
    if (NetCapabilities.interfaces) {
        val interfaces by produceState(emptyList<InterfaceInfo>()) { value = networkInterfaces() }
        ResultCard(title = Res.string.network.str()) {
            interfaces.filter { it.isUp && !it.isLoopback && it.addresses.isNotEmpty() }.forEach { nif ->
                KeyValueRow(nif.name, nif.addresses.joinToString("\n"))
            }
        }
    }
}

val batteryTool = Tool(
    id = "battery",
    category = ToolCategory.DEVICE,
    title = Res.string.battery,
    description = Res.string.battery_description,
    icon = Icons.Filled.BatteryChargingFull,
    keywords = listOf("charge", "power", "voltage", "health", "cycles", "заряд", "аккумулятор", "питание"),
) { BatteryScreen() }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BatteryScreen() {
    var info by remember { mutableStateOf<BatteryInfo?>(null) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            info = batteryInfo()
            loaded = true
            delay(5000)
        }
    }
    if (!loaded) {
        LoadingRow()
        return
    }
    val b = info
    if (b == null) {
        Text(Res.string.device_no_battery_information.str())
        return
    }
    b.level?.let { level ->
        Text("$level%", style = MaterialTheme.typography.displayLarge)
        LinearWavyProgressIndicator(progress = { level / 100f }, modifier = Modifier.fillMaxWidth())
    }
    ResultCard {
        if (b.details.none { it.first == "Status" }) {
            b.charging?.let { KeyValueRow(Res.string.device_charging.str(), if (it) Res.string.yes.str() else Res.string.no.str(), mono = false, copyable = false) }
        }
        b.details.forEach { (k, v) -> KeyValueRow(fieldName(k), fieldValue(v), mono = false, copyable = false) }
    }
}

val displayInfoTool = Tool(
    id = "display-info",
    category = ToolCategory.DEVICE,
    title = Res.string.display_info,
    description = Res.string.display_info_description,
    icon = Icons.Filled.Monitor,
    keywords = listOf("screen", "resolution", "dpi", "density", "refresh rate", "экран", "разрешение", "плотность"),
) { DisplayInfoScreen() }

@Composable
private fun DisplayInfoScreen() {
    val density = LocalDensity.current
    val size = LocalWindowInfo.current.containerSize
    val extras = remember { displayExtras() }
    ResultCard {
        KeyValueRow(Res.string.window.str(), "${size.width} × ${size.height} px", copyable = false)
        KeyValueRow(Res.string.window_dp.str(), "${(size.width / density.density).toDouble().fmt(0)} × ${(size.height / density.density).toDouble().fmt(0)} dp", copyable = false)
        KeyValueRow(Res.string.density.str(), "${density.density}x", copyable = false)
        KeyValueRow(Res.string.font_scale.str(), "${density.fontScale}x", copyable = false)
        extras.forEach { (k, v) -> KeyValueRow(fieldName(k), fieldValue(v), mono = false, copyable = false) }
    }
}

val clipboardTool = Tool(
    id = "clipboard-inspector",
    category = ToolCategory.DEVICE,
    title = Res.string.clipboard_inspector,
    description = Res.string.clipboard_inspector_description,
    icon = Icons.Filled.ContentPaste,
    keywords = listOf("paste", "copy", "буфер", "вставить"),
) { ClipboardScreen() }

@Composable
private fun ClipboardScreen() {
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    var text by remember { mutableStateOf<String?>(null) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionButton(text = Res.string.paste.str(), onClick = { text = clipboard.getText()?.text ?: "" })
        ActionButton(text = Res.string.clear.str(), onClick = { clipboard.setText(AnnotatedString("")); text = "" })
    }
    text?.let { t ->
        ResultCard {
            KeyValueRow(Res.string.characters_count.str(), t.length.toString(), copyable = false)
            KeyValueRow(Res.string.lines.str(), if (t.isEmpty()) "0" else t.lines().size.toString(), copyable = false)
            KeyValueRow(Res.string.utf_8_bytes.str(), t.encodeToByteArray().size.toString(), copyable = false)
            KeyValueRow(Res.string.text.str(), if (t.length > 4000) t.take(4000) + "…" else t, mono = false)
            KeyValueRow("Hex", t.encodeToByteArray().take(64).joinToString(" ") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') })
        }
    }
}
