package com.vasmarfas.card.tools.developer

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
object Base64Tools {
    fun encode(bytes: ByteArray, urlSafe: Boolean, padding: Boolean): String {
        val base = if (urlSafe) Base64.UrlSafe else Base64.Default
        return base.withPadding(if (padding) Base64.PaddingOption.PRESENT else Base64.PaddingOption.ABSENT).encode(bytes)
    }

    fun decode(text: String): ByteArray? {
        val cleaned = text.filterNot { it.isWhitespace() }
        if (cleaned.isEmpty()) return ByteArray(0)
        val base = if (cleaned.any { it == '-' || it == '_' }) Base64.UrlSafe else Base64.Default
        return try {
            base.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL).decode(cleaned)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun utf8OrNull(bytes: ByteArray): String? = try {
        bytes.decodeToString(throwOnInvalidSequence = true)
    } catch (e: Exception) {
        null
    }

    fun hexDump(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (offset in bytes.indices step 16) {
            val row = bytes.copyOfRange(offset, minOf(offset + 16, bytes.size))
            sb.append(offset.toString(16).padStart(8, '0')).append("  ")
            for (i in 0 until 16) {
                if (i < row.size) sb.append((row[i].toInt() and 0xFF).toString(16).padStart(2, '0')) else sb.append("  ")
                sb.append(if (i == 7) "  " else " ")
            }
            sb.append(" |")
            for (b in row) {
                val v = b.toInt() and 0xFF
                sb.append(if (v in 0x20..0x7E) v.toChar() else '.')
            }
            sb.append("|\n")
        }
        return sb.toString().trimEnd()
    }
}
