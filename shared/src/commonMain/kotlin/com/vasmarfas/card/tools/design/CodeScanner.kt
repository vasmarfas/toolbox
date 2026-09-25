package com.vasmarfas.card.tools.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.AppPermission
import com.vasmarfas.card.core.CameraScanner
import com.vasmarfas.card.core.Lang
import com.vasmarfas.card.core.LocalLang
import com.vasmarfas.card.core.PickKind
import com.vasmarfas.card.core.ScannedCode
import com.vasmarfas.card.core.cameraScanning
import com.vasmarfas.card.core.ensurePermission
import com.vasmarfas.card.core.openUrl
import com.vasmarfas.card.core.scanCodes
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.media.PickButton
import com.vasmarfas.card.tools.media.imageExtensions
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.CopyIconButton
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.Hint
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.LoadingRow
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SegmentedChoice
import io.github.vinceglb.filekit.readBytes
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.launch

private enum class ScanSource { CAMERA, FILE }

private val productFormats = setOf("EAN-13", "EAN-8", "UPC-A", "UPC-E", "ISBN")

private val openableSchemes = listOf("http://", "https://", "mailto:", "tel:")

@Composable
fun CreateOrScan(scanning: Boolean, onChange: (Boolean) -> Unit) {
    SegmentedChoice(
        options = listOf(false, true),
        selected = scanning,
        onSelect = onChange,
        label = { if (it) Res.string.scan_scan.str() else Res.string.scan_create.str() },
    )
}

@Composable
fun CodeScanner() {
    var source by rememberSaveable { mutableStateOf(if (cameraScanning) ScanSource.CAMERA else ScanSource.FILE) }
    var codes by remember { mutableStateOf<List<ScannedCode>?>(null) }
    if (cameraScanning) {
        SegmentedChoice(
            options = ScanSource.entries,
            selected = source,
            onSelect = {
                source = it
                codes = null
            },
            label = { if (it == ScanSource.CAMERA) Res.string.scan_camera.str() else Res.string.scan_file.str() },
        )
    }
    when (source) {
        ScanSource.CAMERA -> CameraSource(codes) { codes = it }
        ScanSource.FILE -> FileSource(codes) { codes = it }
    }
}

@Composable
private fun CameraSource(codes: List<ScannedCode>?, onCodes: (List<ScannedCode>?) -> Unit) {
    if (codes != null) {
        codes.forEach { ScannedCard(it) }
        ActionButton(Res.string.scan_again.str(), onClick = { onCodes(null) }, icon = Icons.Filled.QrCodeScanner)
        return
    }
    var allowed by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) { allowed = ensurePermission(AppPermission.CAMERA) }
    when (allowed) {
        null -> LoadingRow()
        false -> ErrorText(Res.string.scan_camera_denied.str())
        true -> {
            CameraScanner(
                onFound = { onCodes(listOf(it)) },
                modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp).aspectRatio(1f).clip(RoundedCornerShape(16.dp)),
            )
            Hint(Res.string.scan_aim_hint.str())
        }
    }
}

@Composable
private fun FileSource(codes: List<ScannedCode>?, onCodes: (List<ScannedCode>?) -> Unit) {
    val scope = rememberCoroutineScope()
    var reading by remember { mutableStateOf(false) }
    PickButton(Res.string.scan_open_image.str(), imageExtensions, PickKind.IMAGE, icon = Icons.Filled.ImageSearch, empty = codes == null && !reading) { files ->
        val file = files.firstOrNull() ?: return@PickButton
        reading = true
        onCodes(null)
        scope.launch {
            onCodes(runCatching { scanCodes(file.readBytes()) }.getOrDefault(emptyList()))
            reading = false
        }
    }
    if (reading) LoadingRow()
    when {
        codes == null -> Hint(Res.string.scan_file_hint.str())
        codes.isEmpty() -> ErrorText(Res.string.scan_nothing_found.str())
        else -> codes.forEach { ScannedCard(it) }
    }
}

@Composable
private fun ScannedCard(code: ScannedCode) {
    val wifi = remember(code.text) { QrPayload.parseWifi(code.text) }
    val lang = LocalLang.current
    val link = openableSchemes.any { code.text.startsWith(it, ignoreCase = true) }
    val product = code.format in productFormats
    ResultCard(code.format) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SelectionContainer(Modifier.weight(1f)) { Text(code.text, style = MaterialTheme.typography.bodyLarge) }
            CopyIconButton(code.text)
        }
        if (wifi != null) {
            KeyValueRow(Res.string.scan_wifi_network.str(), wifi.ssid)
            if (wifi.password.isNotEmpty()) KeyValueRow(Res.string.password.str(), wifi.password)
            KeyValueRow(Res.string.scan_wifi_security.str(), if (wifi.security == "nopass") Res.string.qr_open.str() else wifi.security, copyable = false)
        }
        if (link || product) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (link) TextButton(onClick = { openUrl(code.text) }) { Text(Res.string.scan_open_link.str()) }
                if (product) {
                    val query = code.text.encodeURLParameter()
                    val search = if (lang == Lang.RU) "https://yandex.ru/search/?text=$query" else "https://www.google.com/search?q=$query"
                    TextButton(onClick = { openUrl(search) }) { Text(Res.string.scan_search_product.str()) }
                }
            }
        }
    }
}
