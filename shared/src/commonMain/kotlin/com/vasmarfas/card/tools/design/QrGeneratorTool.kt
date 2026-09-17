package com.vasmarfas.card.tools.design

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.CopyIconButton
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.MonoText
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SegmentedChoice
import com.vasmarfas.card.ui.components.SwitchRow
import com.vasmarfas.card.ui.components.ToolInputField
import com.vasmarfas.card.ui.components.ToolSection
import com.vasmarfas.card.ui.components.expandedSquare
import io.github.alexzhirkevich.qrose.options.QrBallShape
import io.github.alexzhirkevich.qrose.options.QrBrush
import io.github.alexzhirkevich.qrose.options.QrErrorCorrectionLevel
import io.github.alexzhirkevich.qrose.options.QrFrameShape
import io.github.alexzhirkevich.qrose.options.QrPixelShape
import io.github.alexzhirkevich.qrose.options.roundCorners
import io.github.alexzhirkevich.qrose.options.solid
import io.github.alexzhirkevich.qrose.rememberQrCodePainter

private enum class QrPreset { TEXT, URL, WIFI, VCARD, EMAIL, PHONE, SMS, GEO, TELEGRAM }

private val qrColorPresets = listOf(
    "#000000" to "#FFFFFF",
    "#00696D" to "#FFFFFF",
    "#3F51B5" to "#FFFFFF",
    "#B3261E" to "#FFF8F7",
    "#FFFFFF" to "#101014",
)

val qrGeneratorTool = Tool(
    id = "qr-generator",
    category = ToolCategory.DESIGN,
    title = Res.string.qr_code,
    description = Res.string.qr_codes_for_text_links_wi_fi_contacts_email,
    icon = Icons.Filled.QrCode2,
    keywords = listOf("qr", "qrcode", "wifi", "vcard", "link", "кьюар", "куар", "вайфай", "визитка", "ссылка"),
    expandable = true,
) { QrGeneratorScreen() }

@Composable
private fun QrGeneratorScreen() {
    var preset by rememberSaveable { mutableStateOf(QrPreset.TEXT) }
    var text by rememberSaveable { mutableStateOf("https://vasmarfas.dev") }
    var ssid by rememberSaveable { mutableStateOf("") }
    var wifiPassword by rememberSaveable { mutableStateOf("") }
    var wifiSecurity by rememberSaveable { mutableStateOf("WPA") }
    var wifiHidden by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var org by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }
    var subject by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var lat by rememberSaveable { mutableStateOf("44.95") }
    var lon by rememberSaveable { mutableStateOf("34.10") }
    var colorIndex by rememberSaveable { mutableStateOf(0) }
    var rounded by rememberSaveable { mutableStateOf(true) }
    var ecLevel by rememberSaveable { mutableStateOf(QrErrorCorrectionLevel.Medium) }

    ChoiceChips(
        options = QrPreset.entries,
        selected = preset,
        onSelect = { preset = it },
        label = {
            when (it) {
                QrPreset.TEXT -> Res.string.text.str()
                QrPreset.URL -> Res.string.link.str()
                QrPreset.WIFI -> "Wi-Fi"
                QrPreset.VCARD -> Res.string.contact.str()
                QrPreset.EMAIL -> Res.string.email.str()
                QrPreset.PHONE -> Res.string.phone.str()
                QrPreset.SMS -> "SMS"
                QrPreset.GEO -> Res.string.location.str()
                QrPreset.TELEGRAM -> "Telegram"
            }
        },
    )
    when (preset) {
        QrPreset.TEXT -> ToolInputField(text, { text = it }, Res.string.text.str(), singleLine = false, minLines = 3)
        QrPreset.URL -> ToolInputField(text, { text = it }, Res.string.link.str(), placeholder = "https://example.com")
        QrPreset.TELEGRAM -> ToolInputField(text, { text = it }, Res.string.username.str(), placeholder = "@durov")
        QrPreset.PHONE -> ToolInputField(phone, { phone = it }, Res.string.phone_number.str(), placeholder = "+79781234567")
        QrPreset.WIFI -> ToolSection("Wi-Fi") {
            ToolInputField(ssid, { ssid = it }, "SSID")
            ToolInputField(wifiPassword, { wifiPassword = it }, Res.string.password.str())
            SegmentedChoice(
                options = listOf("WPA", "WEP", "nopass"),
                selected = wifiSecurity,
                onSelect = { wifiSecurity = it },
                label = { if (it == "nopass") Res.string.open_2.str() else it },
            )
            SwitchRow(Res.string.hidden_network.str(), wifiHidden, { wifiHidden = it })
        }

        QrPreset.VCARD -> ToolSection(Res.string.contact.str()) {
            ToolInputField(name, { name = it }, Res.string.name.str())
            ToolInputField(org, { org = it }, Res.string.company.str())
            ToolInputField(phone, { phone = it }, Res.string.phone.str())
            ToolInputField(email, { email = it }, Res.string.email.str())
            ToolInputField(url, { url = it }, Res.string.website.str())
        }

        QrPreset.EMAIL -> ToolSection(Res.string.email_2.str()) {
            ToolInputField(email, { email = it }, Res.string.address.str(), placeholder = "hi@example.com")
            ToolInputField(subject, { subject = it }, Res.string.subject.str())
            ToolInputField(body, { body = it }, Res.string.body.str(), singleLine = false, minLines = 2)
        }

        QrPreset.SMS -> ToolSection("SMS") {
            ToolInputField(phone, { phone = it }, Res.string.phone_number.str())
            ToolInputField(body, { body = it }, Res.string.message.str(), singleLine = false, minLines = 2)
        }

        QrPreset.GEO -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ToolInputField(lat, { lat = it }, Res.string.latitude.str(), Modifier.weight(1f), monospace = true)
            ToolInputField(lon, { lon = it }, Res.string.longitude.str(), Modifier.weight(1f), monospace = true)
        }
    }

    val payload = when (preset) {
        QrPreset.TEXT, QrPreset.URL -> text
        QrPreset.TELEGRAM -> QrPayload.telegram(text)
        QrPreset.WIFI -> QrPayload.wifi(ssid, wifiPassword, wifiSecurity, wifiHidden)
        QrPreset.VCARD -> QrPayload.vcard(name, org, phone, email, url)
        QrPreset.EMAIL -> QrPayload.mailto(email, subject, body)
        QrPreset.PHONE -> QrPayload.tel(phone)
        QrPreset.SMS -> QrPayload.sms(phone, body)
        QrPreset.GEO -> QrPayload.geo(lat, lon)
    }

    ChoiceChips(
        options = qrColorPresets.indices.toList(),
        selected = colorIndex,
        onSelect = { colorIndex = it },
        label = { qrColorPresets[it].first },
    )
    SwitchRow(Res.string.rounded_modules.str(), rounded, { rounded = it })
    SegmentedChoice(
        options = listOf(QrErrorCorrectionLevel.Low, QrErrorCorrectionLevel.Medium, QrErrorCorrectionLevel.High),
        selected = ecLevel,
        onSelect = { ecLevel = it },
        label = {
            when (it) {
                QrErrorCorrectionLevel.Low -> "L 7%"
                QrErrorCorrectionLevel.Medium -> "M 15%"
                else -> "H 30%"
            }
        },
    )

    if (payload.isBlank()) {
        ErrorText(Res.string.fill_in_the_fields_above.str())
        return
    }
    val darkColor = ColorMath.parse(qrColorPresets[colorIndex].first)?.toColor() ?: Color.Black
    val lightColor = ColorMath.parse(qrColorPresets[colorIndex].second)?.toColor() ?: Color.White
    val painter = rememberQrCodePainter(data = payload) {
        shapes {
            if (rounded) {
                ball = QrBallShape.roundCorners(.25f)
                frame = QrFrameShape.roundCorners(.25f)
                darkPixel = QrPixelShape.roundCorners()
            }
        }
        colors {
            dark = QrBrush.solid(darkColor)
            light = QrBrush.solid(lightColor)
        }
        errorCorrectionLevel = ecLevel
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(lightColor)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(painter, contentDescription = null, modifier = Modifier.size(expandedSquare(normal = 280.dp, reserved = 260.dp)))
    }
    ResultCard(Res.string.encoded_string.str()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MonoText(payload, Modifier.weight(1f))
            CopyIconButton(payload)
        }
    }
}
