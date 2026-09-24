package com.vasmarfas.card.tools.documents

import kotlin.math.abs
import kotlin.math.min

object ImageHeader {
    // a JPEG with EXIF orientation 5..8 is displayed rotated, so the sides are swapped
    fun size(bytes: ByteArray): Pair<Int, Int>? {
        val size = when (mimeType(bytes)) {
            "image/png" -> png(bytes)
            "image/jpeg" -> jpeg(bytes)
            "image/gif" -> if (bytes.size >= 10) u16le(bytes, 6) to u16le(bytes, 8) else null
            "image/bmp" -> bmp(bytes)
            "image/webp" -> webp(bytes)
            "image/tiff" -> tiff(bytes, 0, bytes.size)?.let { it.width to it.height }
            else -> null
        }
        return size?.takeIf { it.first > 0 && it.second > 0 }
    }

    fun mimeType(bytes: ByteArray): String? = when {
        starts(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) -> "image/png"
        starts(bytes, 0xFF, 0xD8, 0xFF) -> "image/jpeg"
        starts(bytes, 0x47, 0x49, 0x46, 0x38) -> "image/gif"
        starts(bytes, 0x42, 0x4D) && bytes.size >= 26 -> "image/bmp"
        starts(bytes, 0x52, 0x49, 0x46, 0x46) && bytes.size >= 16 && ascii(bytes, 8, "WEBP") -> "image/webp"
        starts(bytes, 0x49, 0x49, 0x2A, 0x00) || starts(bytes, 0x4D, 0x4D, 0x00, 0x2A) -> "image/tiff"
        starts(bytes, 0x01, 0x00, 0x00, 0x00) && bytes.size >= 44 && ascii(bytes, 40, " EMF") -> "image/emf"
        starts(bytes, 0xD7, 0xCD, 0xC6, 0x9A) || starts(bytes, 0x01, 0x00, 0x09, 0x00) || starts(bytes, 0x02, 0x00, 0x09, 0x00) -> "image/wmf"
        isSvg(bytes) -> "image/svg+xml"
        else -> null
    }

    private class Tiff(val width: Int, val height: Int, val orientation: Int)

    private fun png(b: ByteArray): Pair<Int, Int>? {
        var p = 8
        while (p + 16 <= b.size) {
            val length = u32be(b, p)
            if (ascii(b, p + 4, "IHDR")) return u32be(b, p + 8) to u32be(b, p + 12)
            if (length < 0 || length > b.size) return null
            p += 12 + length
        }
        return null
    }

    private fun jpeg(b: ByteArray): Pair<Int, Int>? {
        var p = 2
        var orientation = 1
        while (p + 4 <= b.size) {
            if (b[p].toInt() and 0xFF != 0xFF) return null
            val marker = b[p + 1].toInt() and 0xFF
            when {
                marker == 0xFF -> {
                    p++
                    continue
                }
                marker == 0x01 || marker in 0xD0..0xD8 -> {
                    p += 2
                    continue
                }
                marker == 0xD9 || marker == 0xDA -> return null
            }
            val length = u16be(b, p + 2)
            if (length < 2) return null
            val end = min(b.size, p + 2 + length)
            if (marker == 0xE1 && end - p >= 10 && ascii(b, p + 4, "Exif")) {
                tiff(b, p + 10, end)?.let { orientation = it.orientation }
            }
            if (marker in 0xC0..0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC) {
                if (p + 9 > b.size) return null
                val height = u16be(b, p + 5)
                val width = u16be(b, p + 7)
                return if (orientation in 5..8) height to width else width to height
            }
            p += 2 + length
        }
        return null
    }

    private fun bmp(b: ByteArray): Pair<Int, Int>? {
        val header = u32le(b, 14)
        return if (header == 12) u16le(b, 18) to u16le(b, 20) else abs(u32le(b, 18)) to abs(u32le(b, 22))
    }

    private fun webp(b: ByteArray): Pair<Int, Int>? = when {
        ascii(b, 12, "VP8 ") && b.size >= 30 -> {
            if (b[23].toInt() and 0xFF != 0x9D || b[24].toInt() and 0xFF != 0x01 || b[25].toInt() and 0xFF != 0x2A) {
                null
            } else {
                (u16le(b, 26) and 0x3FFF) to (u16le(b, 28) and 0x3FFF)
            }
        }
        ascii(b, 12, "VP8L") && b.size >= 25 -> {
            val bits = u32le(b, 21)
            if (b[20].toInt() and 0xFF != 0x2F) null else ((bits and 0x3FFF) + 1) to (((bits ushr 14) and 0x3FFF) + 1)
        }
        ascii(b, 12, "VP8X") && b.size >= 30 -> (u24le(b, 24) + 1) to (u24le(b, 27) + 1)
        else -> null
    }

    private fun tiff(b: ByteArray, start: Int, end: Int): Tiff? {
        if (end - start < 8) return null
        val little = when {
            b[start].toInt() == 0x49 && b[start + 1].toInt() == 0x49 -> true
            b[start].toInt() == 0x4D && b[start + 1].toInt() == 0x4D -> false
            else -> return null
        }
        fun u16(at: Int) = if (little) u16le(b, at) else u16be(b, at)
        fun u32(at: Int) = if (little) u32le(b, at) else u32be(b, at)
        val ifd = u32(start + 4)
        if (ifd < 8 || ifd > end - start - 2) return null
        val count = u16(start + ifd)
        var width = 0
        var height = 0
        var orientation = 1
        for (i in 0 until count) {
            val entry = start + ifd + 2 + i * 12
            if (entry + 12 > end) break
            val value = if (u16(entry + 2) == 3) u16(entry + 8) else u32(entry + 8)
            when (u16(entry)) {
                256 -> width = value
                257 -> height = value
                274 -> orientation = value
            }
        }
        return Tiff(width, height, orientation)
    }

    private fun isSvg(b: ByteArray): Boolean {
        val head = CharArray(min(b.size, 2048)) { (b[it].toInt() and 0xFF).toChar() }.concatToString()
        var i = if (starts(b, 0xEF, 0xBB, 0xBF)) 3 else 0
        while (i < head.length) {
            when {
                head[i].isWhitespace() -> i++
                head.startsWith("<?", i) -> i = head.indexOf("?>", i).let { if (it < 0) return false else it + 2 }
                head.startsWith("<!--", i) -> i = head.indexOf("-->", i).let { if (it < 0) return false else it + 3 }
                head.startsWith("<!", i) -> i = head.indexOf('>', i).let { if (it < 0) return false else it + 1 }
                else -> return head.startsWith("<svg", i) || head.startsWith("<svg:svg", i)
            }
        }
        return false
    }

    private fun starts(b: ByteArray, vararg prefix: Int): Boolean {
        if (b.size < prefix.size) return false
        for (i in prefix.indices) if (b[i].toInt() and 0xFF != prefix[i]) return false
        return true
    }

    private fun ascii(b: ByteArray, at: Int, text: String): Boolean {
        if (at < 0 || at + text.length > b.size) return false
        for (i in text.indices) if (b[at + i].toInt() != text[i].code) return false
        return true
    }

    private fun u16le(b: ByteArray, at: Int): Int = if (at + 2 > b.size) 0 else (b[at].toInt() and 0xFF) or ((b[at + 1].toInt() and 0xFF) shl 8)

    private fun u16be(b: ByteArray, at: Int): Int = if (at + 2 > b.size) 0 else ((b[at].toInt() and 0xFF) shl 8) or (b[at + 1].toInt() and 0xFF)

    private fun u24le(b: ByteArray, at: Int): Int = u16le(b, at) or (if (at + 3 > b.size) 0 else (b[at + 2].toInt() and 0xFF) shl 16)

    private fun u32le(b: ByteArray, at: Int): Int = if (at + 4 > b.size) 0 else u16le(b, at) or (u16le(b, at + 2) shl 16)

    private fun u32be(b: ByteArray, at: Int): Int = if (at + 4 > b.size) 0 else (u16be(b, at) shl 16) or u16be(b, at + 2)
}
