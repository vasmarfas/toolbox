package com.vasmarfas.card.tools.media

import kotlin.math.min

enum class ImageContainer { JPEG, PNG, WEBP, GIF, BMP, TIFF, HEIF, UNKNOWN }

object ImageMetadata {
    private const val XMP_FLAG = 0x04
    private const val EXIF_FLAG = 0x08
    private const val ICC_FLAG = 0x20

    private val HEIF_BRANDS = setOf("heic", "heix", "hevc", "hevx", "heim", "heis", "hevm", "hevs", "mif1", "msf1", "avif", "avis")

    fun detect(bytes: ByteArray): ImageContainer = when {
        bytes.size >= 3 && bytes.u8(0) == 0xFF && bytes.u8(1) == 0xD8 && bytes.u8(2) == 0xFF -> ImageContainer.JPEG
        bytes.matches(0, "\u0089PNG\r\n\u001A\n") -> ImageContainer.PNG
        bytes.matches(0, "RIFF") && bytes.matches(8, "WEBP") -> ImageContainer.WEBP
        bytes.matches(0, "GIF87a") || bytes.matches(0, "GIF89a") -> ImageContainer.GIF
        bytes.matches(0, "II*\u0000") || bytes.matches(0, "MM\u0000*") -> ImageContainer.TIFF
        bytes.matches(0, "BM") && bytes.size >= 26 -> ImageContainer.BMP
        isHeif(bytes) -> ImageContainer.HEIF
        else -> ImageContainer.UNKNOWN
    }

    fun strip(bytes: ByteArray, keepOrientation: Boolean = true, keepColorProfile: Boolean = true): ByteArray {
        val orientation = if (keepOrientation) Exif.orientation(bytes) else 1
        return when (detect(bytes)) {
            ImageContainer.JPEG -> jpeg(bytes, orientation, keepColorProfile)
            ImageContainer.PNG -> png(bytes, orientation, keepColorProfile)
            ImageContainer.WEBP -> webp(bytes, orientation, keepColorProfile)
            ImageContainer.GIF -> gif(bytes, keepColorProfile)
            else -> bytes
        }
    }

    // anything after EOI is dropped: MPF secondary images and motion-photo videos sit there, each with
    // metadata of its own
    private fun jpeg(bytes: ByteArray, orientation: Int, keepColorProfile: Boolean): ByteArray {
        val out = ByteSink(bytes.size)
        out.byte(0xFF)
        out.byte(0xD8)
        var pos = 2
        var orientationPending = orientation != 1
        while (true) {
            if (pos + 2 > bytes.size) throw ImageFormatException("JPEG ends before its image data")
            if (bytes.u8(pos) != 0xFF) throw ImageFormatException("JPEG marker expected at byte $pos")
            val marker = bytes.u8(pos + 1)
            if (marker == 0xFF) {
                pos++
                continue
            }
            if (marker == 0x01 || marker in 0xD0..0xD8) {
                pos += 2
                continue
            }
            if (marker == 0xD9) {
                out.byte(0xFF)
                out.byte(0xD9)
                break
            }
            if (pos + 4 > bytes.size) throw ImageFormatException("JPEG segment header is cut off")
            val end = pos + 2 + bytes.u16be(pos + 2)
            if (end > bytes.size || end < pos + 4) throw ImageFormatException("JPEG segment at byte $pos has a bad length")
            if (orientationPending && marker != 0xE0) {
                out.jpegOrientation(orientation)
                orientationPending = false
            }
            if (marker == 0xDA) {
                out.bytes(bytes, pos, scanEnd(bytes, end))
                break
            }
            val keep = when (marker) {
                0xE0 -> bytes.matches(pos + 4, "JFIF\u0000") || bytes.matches(pos + 4, "JFXX\u0000")
                0xE2 -> keepColorProfile && bytes.matches(pos + 4, "ICC_PROFILE\u0000")
                0xEE -> bytes.matches(pos + 4, "Adobe")
                in 0xE1..0xEF, 0xFE -> false
                else -> true
            }
            if (keep) out.bytes(bytes, pos, end)
            pos = end
        }
        return out.toByteArray()
    }

    private fun scanEnd(bytes: ByteArray, from: Int): Int {
        var i = from
        while (i + 1 < bytes.size) {
            if (bytes.u8(i) != 0xFF) {
                i++
                continue
            }
            val m = bytes.u8(i + 1)
            i += when {
                m == 0xD9 -> return i + 2
                m == 0xFF -> 1
                m == 0x00 || m in 0xD0..0xD7 -> 2
                i + 4 <= bytes.size -> 2 + bytes.u16be(i + 2)
                else -> return bytes.size
            }
        }
        return bytes.size
    }

    private fun ByteSink.jpegOrientation(orientation: Int) {
        val tiff = orientationTiff(orientation)
        byte(0xFF)
        byte(0xE1)
        shortBE(2 + 6 + tiff.size)
        ascii("Exif\u0000\u0000")
        bytes(tiff)
    }

    private fun png(bytes: ByteArray, orientation: Int, keepColorProfile: Boolean): ByteArray {
        val out = ByteSink(bytes.size)
        out.bytes(bytes, 0, 8)
        var pos = 8
        var orientationPending = orientation != 1
        while (pos + 12 <= bytes.size) {
            val length = bytes.u32be(pos)
            if (length > bytes.size - pos - 12) throw ImageFormatException("PNG chunk at byte $pos is cut off")
            val end = pos + 12 + length.toInt()
            val type = bytes.decodeToString(pos + 4, pos + 8)
            if (orientationPending && type == "IDAT") {
                out.pngChunk("eXIf", orientationTiff(orientation))
                orientationPending = false
            }
            val keep = when (type) {
                "tEXt", "zTXt", "iTXt", "eXIf", "tIME", "dSIG" -> false
                "iCCP" -> keepColorProfile
                else -> true
            }
            if (keep) out.bytes(bytes, pos, end)
            pos = end
            if (type == "IEND") break
        }
        return out.toByteArray()
    }

    private fun webp(bytes: ByteArray, orientation: Int, keepColorProfile: Boolean): ByteArray {
        val out = ByteSink(bytes.size)
        out.ascii("RIFF")
        out.intLE(0)
        out.ascii("WEBP")
        val end = min(bytes.size.toLong(), 8 + bytes.u32le(4)).toInt()
        var pos = 12
        var flagsAt = -1
        var exif = false
        var icc = false
        while (pos + 8 <= end) {
            val size = bytes.u32le(pos + 4)
            if (size > end - pos - 8) throw ImageFormatException("WebP chunk at byte $pos is cut off")
            val next = min(end, pos + 8 + size.toInt() + (size.toInt() and 1))
            when (bytes.decodeToString(pos, pos + 4)) {
                "EXIF" -> if (orientation != 1) {
                    val tiff = orientationTiff(orientation)
                    out.ascii("EXIF")
                    out.intLE(tiff.size)
                    out.bytes(tiff)
                    exif = true
                }
                "XMP " -> Unit
                "ICCP" -> if (keepColorProfile) {
                    out.bytes(bytes, pos, next)
                    icc = true
                }
                "VP8X" -> {
                    if (size > 0) flagsAt = out.size + 8
                    out.bytes(bytes, pos, next)
                }
                else -> out.bytes(bytes, pos, next)
            }
            pos = next
        }
        if (out.size and 1 != 0) out.byte(0)
        if (flagsAt >= 0) {
            var flags = out[flagsAt] and (ICC_FLAG or EXIF_FLAG or XMP_FLAG).inv()
            if (icc) flags = flags or ICC_FLAG
            if (exif) flags = flags or EXIF_FLAG
            out[flagsAt] = flags
        }
        val riff = out.size - 8
        out[4] = riff
        out[5] = riff shr 8
        out[6] = riff shr 16
        out[7] = riff shr 24
        return out.toByteArray()
    }

    private fun gif(bytes: ByteArray, keepColorProfile: Boolean): ByteArray {
        if (bytes.size < 13) throw ImageFormatException("GIF header is cut off")
        val screen = bytes.u8(10)
        var pos = 13 + if (screen and 0x80 != 0) 3 shl ((screen and 7) + 1) else 0
        if (pos > bytes.size) throw ImageFormatException("GIF colour table is cut off")
        val out = ByteSink(bytes.size)
        out.bytes(bytes, 0, pos)
        while (pos < bytes.size) {
            when (bytes.u8(pos)) {
                0x21 -> {
                    if (pos + 2 > bytes.size) throw ImageFormatException("GIF extension is cut off")
                    val end = skipSubBlocks(bytes, pos + 2)
                    val keep = when (bytes.u8(pos + 1)) {
                        0xFE -> false
                        0xFF -> {
                            val id = if (bytes.u8(pos + 2) == 11 && pos + 14 <= bytes.size) bytes.decodeToString(pos + 3, pos + 14) else ""
                            id == "NETSCAPE2.0" || id == "ANIMEXTS1.0" || (keepColorProfile && id == "ICCRGBG1012")
                        }
                        else -> true
                    }
                    if (keep) out.bytes(bytes, pos, end)
                    pos = end
                }
                0x2C -> {
                    if (pos + 11 > bytes.size) throw ImageFormatException("GIF image descriptor is cut off")
                    val flags = bytes.u8(pos + 9)
                    val table = if (flags and 0x80 != 0) 3 shl ((flags and 7) + 1) else 0
                    val end = skipSubBlocks(bytes, pos + 10 + table + 1)
                    out.bytes(bytes, pos, end)
                    pos = end
                }
                0x3B -> break
                else -> throw ImageFormatException("Unknown GIF block 0x${bytes.u8(pos).toString(16)} at byte $pos")
            }
        }
        out.byte(0x3B)
        return out.toByteArray()
    }

    private fun skipSubBlocks(bytes: ByteArray, from: Int): Int {
        var p = from
        while (p < bytes.size && bytes[p] != 0.toByte()) p += bytes.u8(p) + 1
        if (p >= bytes.size) throw ImageFormatException("GIF data block is cut off")
        return p + 1
    }

    private fun orientationTiff(orientation: Int): ByteArray {
        val tiff = ByteSink(26)
        tiff.ascii("MM")
        tiff.shortBE(42)
        tiff.intBE(8)
        tiff.shortBE(1)
        tiff.shortBE(Exif.ORIENTATION)
        tiff.shortBE(3)
        tiff.intBE(1)
        tiff.shortBE(orientation)
        tiff.shortBE(0)
        tiff.intBE(0)
        return tiff.toByteArray()
    }

    private fun isHeif(bytes: ByteArray): Boolean {
        if (!bytes.matches(4, "ftyp")) return false
        val size = min(bytes.u32be(0), bytes.size.toLong()).toInt()
        if (size < 16) return false
        if (bytes.decodeToString(8, 12) in HEIF_BRANDS) return true
        for (p in 16 until size - 3 step 4) if (bytes.decodeToString(p, p + 4) in HEIF_BRANDS) return true
        return false
    }
}
