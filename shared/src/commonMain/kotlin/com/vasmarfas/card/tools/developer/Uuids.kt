package com.vasmarfas.card.tools.developer

import com.vasmarfas.card.core.secureRandomBytes

data class UuidInfo(
    val canonical: String,
    val version: Int,
    val variant: String,
    val timestampMs: Long?,
    val isNil: Boolean,
    val isMax: Boolean,
)

object Uuids {
    private const val GREGORIAN_OFFSET_100NS = 122192928000000000L

    fun format(bytes: ByteArray): String {
        val hex = bytes.toHex()
        return hex.substring(0, 8) + "-" + hex.substring(8, 12) + "-" + hex.substring(12, 16) + "-" + hex.substring(16, 20) + "-" + hex.substring(20, 32)
    }

    fun v4(random: ByteArray = secureRandomBytes(16)): String {
        val b = random.copyOf()
        b[6] = ((b[6].toInt() and 0x0F) or 0x40).toByte()
        b[8] = ((b[8].toInt() and 0x3F) or 0x80).toByte()
        return format(b)
    }

    fun v7(timeMs: Long, random: ByteArray = secureRandomBytes(16)): String {
        val b = random.copyOf()
        for (i in 0 until 6) b[i] = (timeMs ushr (8 * (5 - i))).toByte()
        b[6] = ((b[6].toInt() and 0x0F) or 0x70).toByte()
        b[8] = ((b[8].toInt() and 0x3F) or 0x80).toByte()
        return format(b)
    }

    fun parse(text: String): UuidInfo? {
        val hex = text.trim().lowercase().removePrefix("urn:uuid:").removePrefix("{").removeSuffix("}").replace("-", "")
        if (hex.length != 32 || !hex.all { it.digitToIntOrNull(16) != null }) return null
        val canonical = hex.substring(0, 8) + "-" + hex.substring(8, 12) + "-" + hex.substring(12, 16) + "-" + hex.substring(16, 20) + "-" + hex.substring(20, 32)
        val isNil = hex.all { it == '0' }
        val isMax = hex.all { it == 'f' }
        val version = hex[12].digitToInt(16)
        val variantNibble = hex[16].digitToInt(16)
        val variant = when {
            variantNibble < 8 -> "NCS (reserved)"
            variantNibble < 12 -> "RFC 4122 / RFC 9562"
            variantNibble < 14 -> "Microsoft (reserved)"
            else -> "Reserved for future"
        }
        val rfc = variantNibble in 8..11
        val timestampMs = when {
            !rfc -> null
            version == 7 -> hex.substring(0, 12).toLong(16)
            version == 1 -> {
                val low = hex.substring(0, 8).toLong(16)
                val mid = hex.substring(8, 12).toLong(16)
                val high = hex.substring(13, 16).toLong(16)
                gregorianToUnixMs((high shl 48) or (mid shl 32) or low)
            }
            version == 6 -> {
                val high = hex.substring(0, 8).toLong(16)
                val mid = hex.substring(8, 12).toLong(16)
                val low = hex.substring(13, 16).toLong(16)
                gregorianToUnixMs((high shl 28) or (mid shl 12) or low)
            }
            else -> null
        }
        return UuidInfo(canonical, if (rfc) version else 0, variant, timestampMs, isNil, isMax)
    }

    private fun gregorianToUnixMs(ticks: Long): Long = (ticks - GREGORIAN_OFFSET_100NS) / 10_000
}
