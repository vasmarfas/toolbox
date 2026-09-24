package com.vasmarfas.card.tools.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.PlatformKind
import com.vasmarfas.card.core.TlsInfo
import com.vasmarfas.card.core.Tr
import com.vasmarfas.card.core.str
import com.vasmarfas.card.core.tlsHandshake
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.LoadingRow
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.ToolInputField
import kotlinx.coroutines.launch

val tlsCertificateTool = Tool(
    id = "tls-certificate",
    category = ToolCategory.NETWORK,
    title = Res.string.tls_certificate,
    description = Res.string.tls_certificate_description,
    icon = Icons.Filled.Lock,
    keywords = listOf("ssl", "https", "certificate", "expiry", "x509", "сертификат", "срок", "https"),
    platforms = PlatformKind.jvm,
) { TlsCertificateScreen() }

@Composable
private fun TlsCertificateScreen() {
    var host by rememberSaveable { mutableStateOf("vasmarfas.com") }
    var port by rememberSaveable { mutableStateOf("443") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<TlsInfo?>(null) }
    val scope = rememberCoroutineScope()

    fun check() {
        val h = hostFrom(host)
        val p = port.toIntOrNull() ?: 443
        if (h.isEmpty()) return
        loading = true; error = null; info = null
        scope.launch {
            runCatching { tlsHandshake(h, p, 8000) }
                .onSuccess { info = it }
                .onFailure { error = it.message ?: it.toString() }
            loading = false
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ToolInputField(value = host, onValueChange = { host = it }, label = Res.string.host_or_ip_address.str(), modifier = Modifier.weight(3f), keyboardType = KeyboardType.Uri, monospace = true)
        NumberField(value = port, onValueChange = { port = it }, label = Res.string.port.str(), modifier = Modifier.weight(1f))
    }
    ActionButton(text = Res.string.check.str(), onClick = ::check, enabled = !loading)
    if (loading) LoadingRow()
    error?.let { ErrorText(it) }
    info?.let { tls ->
        ResultCard(title = "${tls.protocol} · ${tls.cipherSuite}") {
            val leaf = tls.chain.firstOrNull()
            if (leaf != null) {
                val status = when {
                    leaf.expired -> Res.string.expired.str()
                    leaf.daysLeft < 14 -> Tr("expires in ${leaf.daysLeft} days", "истекает через ${leaf.daysLeft} дн.").str()
                    else -> Tr("valid, ${leaf.daysLeft} days left", "действителен, осталось ${leaf.daysLeft} дн.").str()
                }
                Text(status, color = if (leaf.expired || leaf.daysLeft < 14) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            }
        }
        tls.chain.forEachIndexed { index, cert ->
            ResultCard(title = if (index == 0) Res.string.leaf_certificate.str() else Res.string.intermediate_root.str() + " #$index") {
                KeyValueRow(Res.string.cert_subject.str(), cert.subject, mono = false)
                KeyValueRow(Res.string.cert_issuer.str(), cert.issuer, mono = false)
                KeyValueRow(Res.string.valid_from.str(), cert.notBefore)
                KeyValueRow(Res.string.valid_until.str(), cert.notAfter)
                if (cert.subjectAltNames.isNotEmpty()) KeyValueRow("SAN", cert.subjectAltNames.joinToString("\n"))
                KeyValueRow(Res.string.public_key.str(), cert.publicKey, mono = false)
                KeyValueRow(Res.string.signature.str(), cert.signatureAlgorithm, mono = false)
                KeyValueRow(Res.string.cert_serial.str(), cert.serial)
                KeyValueRow("SHA-256", cert.sha256)
                KeyValueRow("SHA-1", cert.sha1)
            }
        }
    }
}
