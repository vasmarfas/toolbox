package com.vasmarfas.card.tools.text

data class AsciiEntry(val code: Int, val symbol: String, val name: String) {
    val hex: String get() = code.toString(16).uppercase().padStart(2, '0')
    val oct: String get() = code.toString(8).padStart(3, '0')
    val bin: String get() = code.toString(2).padStart(8, '0')
    val isControl: Boolean get() = code < 32 || code == 127
}

object AsciiTable {
    private val controls = listOf(
        "NUL" to "Null", "SOH" to "Start of heading", "STX" to "Start of text", "ETX" to "End of text",
        "EOT" to "End of transmission", "ENQ" to "Enquiry", "ACK" to "Acknowledge", "BEL" to "Bell",
        "BS" to "Backspace", "HT" to "Horizontal tab", "LF" to "Line feed", "VT" to "Vertical tab",
        "FF" to "Form feed", "CR" to "Carriage return", "SO" to "Shift out", "SI" to "Shift in",
        "DLE" to "Data link escape", "DC1" to "Device control 1 (XON)", "DC2" to "Device control 2",
        "DC3" to "Device control 3 (XOFF)", "DC4" to "Device control 4", "NAK" to "Negative acknowledge",
        "SYN" to "Synchronous idle", "ETB" to "End of transmission block", "CAN" to "Cancel", "EM" to "End of medium",
        "SUB" to "Substitute", "ESC" to "Escape", "FS" to "File separator", "GS" to "Group separator",
        "RS" to "Record separator", "US" to "Unit separator",
    )

    private val punctuation = mapOf(
        ' ' to "Space", '!' to "Exclamation mark", '"' to "Quotation mark", '#' to "Number sign", '$' to "Dollar sign",
        '%' to "Percent sign", '&' to "Ampersand", '\'' to "Apostrophe", '(' to "Left parenthesis", ')' to "Right parenthesis",
        '*' to "Asterisk", '+' to "Plus sign", ',' to "Comma", '-' to "Hyphen-minus", '.' to "Full stop", '/' to "Slash",
        ':' to "Colon", ';' to "Semicolon", '<' to "Less-than sign", '=' to "Equals sign", '>' to "Greater-than sign",
        '?' to "Question mark", '@' to "At sign", '[' to "Left square bracket", '\\' to "Backslash", ']' to "Right square bracket",
        '^' to "Caret", '_' to "Underscore", '`' to "Grave accent", '{' to "Left curly bracket", '|' to "Vertical bar",
        '}' to "Right curly bracket", '~' to "Tilde",
    )

    val entries: List<AsciiEntry> = (0..127).map { code ->
        when {
            code < 32 -> AsciiEntry(code, controls[code].first, controls[code].second)
            code == 127 -> AsciiEntry(code, "DEL", "Delete")
            else -> {
                val c = code.toChar()
                val name = when {
                    c.isDigit() -> "Digit $c"
                    c.isUpperCase() -> "Uppercase $c"
                    c.isLowerCase() -> "Lowercase $c"
                    else -> punctuation.getValue(c)
                }
                AsciiEntry(code, c.toString(), name)
            }
        }
    }

    fun search(query: String, includeControl: Boolean): List<AsciiEntry> {
        val q = query.trim()
        val lower = q.lowercase()
        val hexQuery = lower.removePrefix("0x")
        return entries.filter { e ->
            (includeControl || !e.isControl) && (
                q.isEmpty() ||
                    e.symbol == q ||
                    e.code.toString() == q ||
                    (hexQuery.length == 2 && e.hex.lowercase() == hexQuery) ||
                    e.name.lowercase().contains(lower) ||
                    (e.isControl && e.symbol.lowercase().contains(lower))
                )
        }
    }
}
