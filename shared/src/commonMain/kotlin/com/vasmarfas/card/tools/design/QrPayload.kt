package com.vasmarfas.card.tools.design

object QrPayload {
    fun escape(value: String): String = value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace(":", "\\:").replace("\"", "\\\"")

    fun wifi(ssid: String, password: String, security: String, hidden: Boolean): String =
        "WIFI:T:$security;S:${escape(ssid)};P:${escape(password)};H:$hidden;;"

    fun vcard(name: String, org: String, phone: String, email: String, url: String): String = buildString {
        append("BEGIN:VCARD\nVERSION:3.0\n")
        append("N:").append(name).append("\n")
        append("FN:").append(name).append("\n")
        if (org.isNotBlank()) append("ORG:").append(org).append("\n")
        if (phone.isNotBlank()) append("TEL:").append(phone).append("\n")
        if (email.isNotBlank()) append("EMAIL:").append(email).append("\n")
        if (url.isNotBlank()) append("URL:").append(url).append("\n")
        append("END:VCARD")
    }

    fun mailto(address: String, subject: String, body: String): String {
        val query = listOfNotNull(
            subject.takeIf { it.isNotBlank() }?.let { "subject=" + encode(it) },
            body.takeIf { it.isNotBlank() }?.let { "body=" + encode(it) },
        ).joinToString("&")
        return "mailto:$address" + if (query.isEmpty()) "" else "?$query"
    }

    fun sms(number: String, text: String): String = "SMSTO:$number:$text"

    fun tel(number: String): String = "tel:$number"

    fun geo(lat: String, lon: String): String = "geo:$lat,$lon"

    fun telegram(handle: String): String = "https://t.me/" + handle.trim().removePrefix("@").removePrefix("https://t.me/")

    fun encode(text: String): String = buildString {
        text.encodeToByteArray().forEach { byte ->
            val v = byte.toInt() and 0xFF
            val c = v.toChar()
            if (c.isLetterOrDigit() && v < 128 || c in "-_.~") append(c) else append('%').append(v.toString(16).uppercase().padStart(2, '0'))
        }
    }
}
