package com.vasmarfas.card.tools.documents

import com.vasmarfas.card.core.Zlib
import com.vasmarfas.card.tools.developer.Crc32

internal object TestImages {
    fun png(width: Int, height: Int, seed: Int = 0): ByteArray {
        val raw = ByteArray((width * 3 + 1) * height)
        for (y in 0 until height) {
            val row = y * (width * 3 + 1)
            for (x in 0 until width * 3) raw[row + 1 + x] = ((x * 31 + y * 17 + seed * 7) and 0xFF).toByte()
        }
        val header = be32(width) + be32(height) + byteArrayOf(8, 2, 0, 0, 0)
        return byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) +
            chunk("IHDR", header) + chunk("IDAT", Zlib.compress(raw)) + chunk("IEND", ByteArray(0))
    }

    fun jpeg(width: Int, height: Int, orientation: Int? = null): ByteArray {
        var out = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
        out += byteArrayOf(0xFF.toByte(), 0xE0.toByte(), 0, 16) + "JFIF".encodeToByteArray() + byteArrayOf(0, 1, 1, 0, 0, 1, 0, 1, 0, 0)
        if (orientation != null) {
            val tiff = "MM".encodeToByteArray() + byteArrayOf(0, 42) + be32(8) + be16(1) + be16(274) + be16(3) + be32(1) + be16(orientation) + be16(0) + be32(0)
            val exif = "Exif".encodeToByteArray() + byteArrayOf(0, 0) + tiff
            out += byteArrayOf(0xFF.toByte(), 0xE1.toByte()) + be16(exif.size + 2) + exif
        }
        out += byteArrayOf(0xFF.toByte(), 0xC0.toByte()) + be16(17) + byteArrayOf(8) + be16(height) + be16(width) +
            byteArrayOf(3, 1, 0x22, 0, 2, 0x11, 1, 3, 0x11, 1)
        out += byteArrayOf(0xFF.toByte(), 0xDA.toByte()) + be16(12) + byteArrayOf(3, 1, 0, 2, 0x11, 3, 0x11, 0, 0x3F, 0)
        out += byteArrayOf(0x00, 0x00, 0xFF.toByte(), 0xD9.toByte())
        return out
    }

    fun gif(width: Int, height: Int): ByteArray = "GIF89a".encodeToByteArray() + le16(width) + le16(height) + byteArrayOf(0, 0, 0, 0x3B)

    fun bmp(width: Int, height: Int): ByteArray =
        "BM".encodeToByteArray() + le32(54) + le32(0) + le32(54) + le32(40) + le32(width) + le32(-height) + le16(1) + le16(24) + ByteArray(24)

    fun webpLossless(width: Int, height: Int): ByteArray {
        val bits = (width - 1) or ((height - 1) shl 14)
        val body = "VP8L".encodeToByteArray() + le32(5) + byteArrayOf(0x2F) + le32(bits) + byteArrayOf(0)
        return "RIFF".encodeToByteArray() + le32(body.size + 4) + "WEBP".encodeToByteArray() + body
    }

    fun webpLossy(width: Int, height: Int): ByteArray {
        val body = "VP8 ".encodeToByteArray() + le32(10) + byteArrayOf(0, 0, 0, 0x9D.toByte(), 0x01, 0x2A) + le16(width) + le16(height)
        return "RIFF".encodeToByteArray() + le32(body.size + 4) + "WEBP".encodeToByteArray() + body
    }

    fun webpExtended(width: Int, height: Int): ByteArray {
        val body = "VP8X".encodeToByteArray() + le32(10) + byteArrayOf(0, 0, 0, 0) + le24(width - 1) + le24(height - 1)
        return "RIFF".encodeToByteArray() + le32(body.size + 4) + "WEBP".encodeToByteArray() + body
    }

    fun tiff(width: Int, height: Int): ByteArray =
        "II".encodeToByteArray() + le16(42) + le32(8) + le16(2) +
            le16(256) + le16(3) + le32(1) + le16(width) + le16(0) +
            le16(257) + le16(4) + le32(1) + le32(height) + le32(0)

    private fun chunk(type: String, data: ByteArray): ByteArray {
        val typed = type.encodeToByteArray() + data
        return be32(data.size) + typed + be32(Crc32.compute(typed))
    }

    private fun be32(v: Int) = byteArrayOf((v ushr 24).toByte(), (v ushr 16).toByte(), (v ushr 8).toByte(), v.toByte())

    private fun be16(v: Int) = byteArrayOf((v ushr 8).toByte(), v.toByte())

    private fun le16(v: Int) = byteArrayOf(v.toByte(), (v ushr 8).toByte())

    private fun le24(v: Int) = byteArrayOf(v.toByte(), (v ushr 8).toByte(), (v ushr 16).toByte())

    private fun le32(v: Int) = byteArrayOf(v.toByte(), (v ushr 8).toByte(), (v ushr 16).toByte(), (v ushr 24).toByte())
}
