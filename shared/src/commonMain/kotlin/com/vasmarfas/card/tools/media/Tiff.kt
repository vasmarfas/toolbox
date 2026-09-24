package com.vasmarfas.card.tools.media

import com.vasmarfas.card.core.DeflateException
import com.vasmarfas.card.core.Zlib
import kotlin.math.min

class DecodedImage(val width: Int, val height: Int, val pixels: IntArray)

class ImageFormatException(message: String) : Exception(message)

object Tiff {
    private const val MAX_PAGES = 10_000

    fun pageCount(bytes: ByteArray): Int = pages(open(bytes)).size

    fun decode(bytes: ByteArray, page: Int = 0): DecodedImage {
        val tiff = open(bytes)
        val pages = pages(tiff)
        if (pages.isEmpty()) throw ImageFormatException("TIFF file has no readable image directory")
        require(page in pages.indices) { "page $page does not exist, the file has ${pages.size}" }
        val ifd = tiff.ifd(pages[page]) ?: throw ImageFormatException("TIFF image directory lies outside the file")
        return TiffPage(tiff, ifd.entries).decode()
    }

    private fun open(bytes: ByteArray): TiffReader {
        if (bytes.matches(0, "II+\u0000") || bytes.matches(0, "MM\u0000+")) throw ImageFormatException("BigTIFF files are not supported")
        return TiffReader.of(bytes, 0, bytes.size) ?: throw ImageFormatException("Not a TIFF file")
    }

    private fun pages(tiff: TiffReader): List<Long> {
        val offsets = ArrayList<Long>()
        val seen = HashSet<Long>()
        var offset = tiff.firstIfd
        while (offset != 0L && seen.add(offset) && offsets.size < MAX_PAGES) {
            val ifd = tiff.ifd(offset) ?: break
            offsets += offset
            offset = ifd.next
        }
        return offsets
    }
}

internal class TiffEntry(val tag: Int, val type: Int, val count: Int, val offset: Int)

internal class TiffIfd(val entries: List<TiffEntry>, val next: Long)

// offsets count from base, as inside EXIF blocks. Entries whose values fall outside are dropped
internal class TiffReader private constructor(val bytes: ByteArray, private val base: Int, private val end: Int) {
    val littleEndian = bytes[base] == 'I'.code.toByte()
    val firstIfd: Long get() = u32(base + 4)

    fun u16(pos: Int): Int = if (littleEndian) bytes.u16le(pos) else bytes.u16be(pos)

    fun u32(pos: Int): Long = if (littleEndian) bytes.u32le(pos) else bytes.u32be(pos)

    fun ifd(offset: Long): TiffIfd? {
        if (offset < 8 || offset > end - base - 2L) return null
        val start = base + offset.toInt()
        val count = u16(start)
        val entries = ArrayList<TiffEntry>(min(count, (end - start - 2) / 12))
        for (i in 0 until count) {
            val e = start + 2 + i * 12
            if (e + 12 > end) break
            val type = u16(e + 2)
            val size = typeSize(type)
            val n = u32(e + 4)
            if (size == 0 || n == 0L || n > Int.MAX_VALUE / 8) continue
            val total = size * n
            val data = if (total <= 4) e + 8L else base + u32(e + 8)
            if (data + total > end) continue
            entries += TiffEntry(u16(e), type, n.toInt(), data.toInt())
        }
        val nextAt = start + 2 + count * 12
        return TiffIfd(entries, if (nextAt + 4 <= end) u32(nextAt) else 0L)
    }

    fun int(e: TiffEntry, i: Int = 0): Long {
        val p = e.offset + i * typeSize(e.type)
        return when (e.type) {
            BYTE, ASCII, UNDEFINED -> bytes.u8(p).toLong()
            SBYTE -> bytes[p].toLong()
            SHORT -> u16(p).toLong()
            SSHORT -> u16(p).toShort().toLong()
            LONG, IFD -> u32(p)
            SLONG -> u32(p).toInt().toLong()
            RATIONAL, SRATIONAL -> {
                val d = denominator(e, i)
                if (d == 0L) 0L else numerator(e, i) / d
            }
            else -> double(e, i).toLong()
        }
    }

    fun numerator(e: TiffEntry, i: Int): Long {
        val v = u32(e.offset + i * 8)
        return if (e.type == SRATIONAL) v.toInt().toLong() else v
    }

    fun denominator(e: TiffEntry, i: Int): Long {
        val v = u32(e.offset + i * 8 + 4)
        return if (e.type == SRATIONAL) v.toInt().toLong() else v
    }

    fun double(e: TiffEntry, i: Int = 0): Double = when (e.type) {
        RATIONAL, SRATIONAL -> {
            val d = denominator(e, i)
            if (d == 0L) Double.NaN else numerator(e, i).toDouble() / d
        }
        FLOAT -> Float.fromBits(u32(e.offset + i * 4).toInt()).toDouble()
        DOUBLE -> {
            val p = e.offset + i * 8
            val hi = u32(if (littleEndian) p + 4 else p)
            val lo = u32(if (littleEndian) p else p + 4)
            Double.fromBits((hi shl 32) or lo)
        }
        else -> int(e, i).toDouble()
    }

    fun ints(e: TiffEntry): LongArray = LongArray(e.count) { int(e, it) }

    fun text(e: TiffEntry): String {
        var stop = e.offset
        val limit = e.offset + e.count
        while (stop < limit && bytes[stop] != 0.toByte()) stop++
        return bytes.decodeToString(e.offset, stop).trim()
    }

    companion object {
        const val BYTE = 1
        const val ASCII = 2
        const val SHORT = 3
        const val LONG = 4
        const val RATIONAL = 5
        const val SBYTE = 6
        const val UNDEFINED = 7
        const val SSHORT = 8
        const val SLONG = 9
        const val SRATIONAL = 10
        const val FLOAT = 11
        const val DOUBLE = 12
        const val IFD = 13

        fun of(bytes: ByteArray, base: Int, end: Int): TiffReader? {
            if (base < 0 || end > bytes.size || end - base < 8) return null
            val valid = bytes.matches(base, "II*\u0000") || bytes.matches(base, "MM\u0000*")
            return if (valid) TiffReader(bytes, base, end) else null
        }

        fun typeSize(type: Int): Int = when (type) {
            BYTE, ASCII, SBYTE, UNDEFINED -> 1
            SHORT, SSHORT -> 2
            LONG, SLONG, FLOAT, IFD -> 4
            RATIONAL, SRATIONAL, DOUBLE -> 8
            else -> 0
        }
    }
}

private class TiffPage(private val tiff: TiffReader, entries: List<TiffEntry>) {
    private val tags = entries.associateBy { it.tag }
    private val bytes = tiff.bytes

    private fun int(tag: Int, default: Long): Long = tags[tag]?.let { tiff.int(it) } ?: default

    private fun ints(tag: Int): LongArray? = tags[tag]?.let { tiff.ints(it) }

    fun decode(): DecodedImage {
        val width = int(256, 0).toInt()
        val height = int(257, 0).toInt()
        if (width <= 0 || height <= 0) throw ImageFormatException("TIFF image has no size")
        if (width.toLong() * height > MAX_PIXELS) throw ImageFormatException("TIFF image of $width × $height is too large")
        val compression = int(259, 1).toInt()
        when (compression) {
            NONE, LZW, DEFLATE, OLD_DEFLATE, PACKBITS -> Unit
            2, 3, 4 -> throw ImageFormatException("CCITT fax compression is not supported")
            6, 7 -> throw ImageFormatException("JPEG-compressed TIFF is not supported")
            else -> throw ImageFormatException("TIFF compression $compression is not supported")
        }
        val format = int(339, 1).toInt()
        if (format == 3) throw ImageFormatException("Floating-point TIFF is not supported")
        if (format != 1) throw ImageFormatException("Signed-integer TIFF is not supported")
        val spp = int(277, 1).toInt()
        if (spp !in 1..8) throw ImageFormatException("TIFF with $spp samples per pixel is not supported")
        val depths = ints(258) ?: longArrayOf(1)
        val bps = depths[0].toInt()
        if (depths.any { it != depths[0] }) throw ImageFormatException("TIFF with different bit depths per channel is not supported")
        if (bps !in intArrayOf(1, 2, 4, 8, 16)) throw ImageFormatException("TIFF with $bps bits per sample is not supported")
        val photometric = int(262, if (spp >= 3) 2L else 1L).toInt()
        val colors = when (photometric) {
            WHITE_IS_ZERO, BLACK_IS_ZERO, PALETTE -> 1
            RGB -> 3
            SEPARATED -> {
                if (int(332, 1) != 1L) throw ImageFormatException("Only CMYK ink sets are supported in separated TIFF")
                4
            }
            6 -> throw ImageFormatException("YCbCr TIFF is not supported")
            8, 9, 10 -> throw ImageFormatException("CIE Lab TIFF is not supported")
            else -> throw ImageFormatException("TIFF photometric interpretation $photometric is not supported")
        }
        if (spp < colors) throw ImageFormatException("TIFF has fewer samples than its colour model needs")
        if (photometric == PALETTE && bps > 8) throw ImageFormatException("Palette TIFF with $bps-bit indices is not supported")
        val extra = if (spp > colors) ints(338)?.firstOrNull() ?: 0L else 0L
        val alpha = if (extra == 1L || extra == 2L) colors else -1
        val associated = extra == 1L
        val predictor = int(317, 1).toInt()
        if (predictor == 3) throw ImageFormatException("Floating-point predictor is not supported")
        if (predictor == 2 && bps < 8) throw ImageFormatException("Horizontal predictor needs 8 or 16 bits per sample")
        val planar = int(284, 1).toInt() == 2 && spp > 1
        val tiled = 322 in tags
        val chunkWidth = if (tiled) int(322, 0).toInt() else width
        val chunkHeight = if (tiled) int(323, 0).toInt() else int(278, height.toLong()).coerceIn(1, height.toLong()).toInt()
        if (tiled && (chunkWidth !in 1..MAX_TILE || chunkHeight !in 1..MAX_TILE)) {
            throw ImageFormatException("TIFF tile size $chunkWidth × $chunkHeight is not valid")
        }
        val offsets = ints(if (tiled) 324 else 273) ?: throw ImageFormatException("TIFF has no image data offsets")
        val counts = ints(if (tiled) 325 else 279)
        val across = (width + chunkWidth - 1) / chunkWidth
        val down = (height + chunkHeight - 1) / chunkHeight
        val planes = if (planar) spp else 1
        if (offsets.size < across.toLong() * down * planes) throw ImageFormatException("TIFF lists fewer strips or tiles than the image needs")
        val chunkSpp = if (planar) 1 else spp
        val rowBytes = ((chunkWidth.toLong() * chunkSpp * bps + 7) / 8).toInt()
        if (rowBytes.toLong() * chunkHeight > MAX_CHUNK) throw ImageFormatException("TIFF strips or tiles are too large")
        if (width.toLong() * height * spp > Int.MAX_VALUE) throw ImageFormatException("TIFF image of $width × $height is too large")
        val reverse = int(266, 1) == 2L
        val samples = ByteArray(width * height * spp)
        val scale = photometric != PALETTE
        for (plane in 0 until planes) {
            for (cy in 0 until down) {
                for (cx in 0 until across) {
                    val index = (plane * down + cy) * across + cx
                    val rows = if (tiled) chunkHeight else min(chunkHeight, height - cy * chunkHeight)
                    val expected = rows * rowBytes
                    val raw = slice(offsets[index], counts?.getOrNull(index), expected, compression)
                    if (reverse) for (i in raw.indices) raw[i] = reverseBits(raw[i])
                    val data = inflate(raw, expected, compression)
                    if (predictor == 2) undoPredictor(data, rows, rowBytes, chunkSpp, bps)
                    val x0 = cx * chunkWidth
                    val y0 = cy * chunkHeight
                    val region = Region(x0, y0, min(chunkWidth, width - x0), min(rows, height - y0), rowBytes)
                    store(data, region, samples, width, spp, if (planar) plane else 0, chunkSpp, bps, scale)
                }
            }
        }
        val pixels = toArgb(samples, width * height, spp, photometric, bps, alpha, associated)
        return oriented(pixels, width, height, int(274, 1).toInt())
    }

    private fun slice(offset: Long, count: Long?, expected: Int, compression: Int): ByteArray {
        if (offset < 0 || offset >= bytes.size) return ByteArray(0)
        val available = bytes.size - offset
        val length = min(count ?: if (compression == NONE) expected.toLong() else available, available).toInt()
        return bytes.copyOfRange(offset.toInt(), offset.toInt() + length)
    }

    private fun inflate(raw: ByteArray, expected: Int, compression: Int): ByteArray {
        val data = when (compression) {
            LZW -> TiffLzw.decode(raw, expected)
            PACKBITS -> unpackBits(raw, expected)
            DEFLATE, OLD_DEFLATE -> try {
                Zlib.decompress(raw)
            } catch (e: DeflateException) {
                throw ImageFormatException("TIFF Deflate data is corrupt: ${e.message}")
            }
            else -> raw
        }
        return if (data.size >= expected) data else data.copyOf(expected)
    }

    private fun unpackBits(src: ByteArray, expected: Int): ByteArray {
        val out = ByteArray(expected)
        var i = 0
        var o = 0
        while (i < src.size && o < expected) {
            val n = src[i++].toInt()
            if (n >= 0) {
                val len = min(n + 1, min(expected - o, src.size - i))
                src.copyInto(out, o, i, i + len)
                i += n + 1
                o += len
            } else if (n != -128 && i < src.size) {
                val len = min(1 - n, expected - o)
                out.fill(src[i++], o, o + len)
                o += len
            }
        }
        return out
    }

    private fun undoPredictor(data: ByteArray, rows: Int, rowBytes: Int, spp: Int, bps: Int) {
        if (bps == 8) {
            for (r in 0 until rows) {
                val o = r * rowBytes
                for (i in o + spp until o + rowBytes) data[i] = (data[i] + data[i - spp]).toByte()
            }
            return
        }
        val words = rowBytes / 2
        for (r in 0 until rows) {
            val o = r * rowBytes
            for (w in spp until words) {
                val p = o + w * 2
                val q = p - spp * 2
                val v = sample16(data, p) + sample16(data, q)
                if (tiff.littleEndian) {
                    data[p] = v.toByte()
                    data[p + 1] = (v shr 8).toByte()
                } else {
                    data[p] = (v shr 8).toByte()
                    data[p + 1] = v.toByte()
                }
            }
        }
    }

    private class Region(val x: Int, val y: Int, val width: Int, val height: Int, val rowBytes: Int)

    private fun store(data: ByteArray, r: Region, samples: ByteArray, width: Int, spp: Int, first: Int, chunkSpp: Int, bps: Int, scale: Boolean) {
        val max = (1 shl bps) - 1
        for (row in 0 until r.height) {
            val src = row * r.rowBytes
            val dst = ((r.y + row) * width + r.x) * spp + first
            if (bps == 8 && chunkSpp == spp) {
                data.copyInto(samples, dst, src, src + r.width * spp)
                continue
            }
            for (col in 0 until r.width) {
                for (s in 0 until chunkSpp) {
                    val k = col * chunkSpp + s
                    val v = when (bps) {
                        8 -> data[src + k].toInt() and 0xFF
                        16 -> (sample16(data, src + k * 2) * 255 + 32767) / 65535
                        else -> {
                            val bit = k * bps
                            val raw = ((data[src + (bit shr 3)].toInt() and 0xFF) shr (8 - bps - (bit and 7))) and max
                            if (scale) raw * 255 / max else raw
                        }
                    }
                    samples[dst + col * spp + s] = v.toByte()
                }
            }
        }
    }

    private fun sample16(data: ByteArray, p: Int): Int = if (tiff.littleEndian) data.u16le(p) else data.u16be(p)

    private fun toArgb(samples: ByteArray, count: Int, spp: Int, photometric: Int, bps: Int, alpha: Int, associated: Boolean): IntArray {
        val map = if (photometric == PALETTE) colorMap(bps) else IntArray(0)
        val pixels = IntArray(count)
        for (i in 0 until count) {
            val o = i * spp
            val s0 = samples[o].toInt() and 0xFF
            var r: Int
            var g: Int
            var b: Int
            when (photometric) {
                WHITE_IS_ZERO -> {
                    r = 255 - s0
                    g = r
                    b = r
                }
                BLACK_IS_ZERO -> {
                    r = s0
                    g = s0
                    b = s0
                }
                PALETTE -> {
                    val c = map[s0]
                    r = (c shr 16) and 0xFF
                    g = (c shr 8) and 0xFF
                    b = c and 0xFF
                }
                RGB -> {
                    r = s0
                    g = samples[o + 1].toInt() and 0xFF
                    b = samples[o + 2].toInt() and 0xFF
                }
                else -> {
                    val k = 255 - (samples[o + 3].toInt() and 0xFF)
                    r = ((255 - s0) * k + 127) / 255
                    g = ((255 - (samples[o + 1].toInt() and 0xFF)) * k + 127) / 255
                    b = ((255 - (samples[o + 2].toInt() and 0xFF)) * k + 127) / 255
                }
            }
            val a = if (alpha >= 0) samples[o + alpha].toInt() and 0xFF else 255
            if (associated && a < 255) {
                if (a == 0) {
                    r = 0
                    g = 0
                    b = 0
                } else {
                    r = min(255, (r * 255 + a / 2) / a)
                    g = min(255, (g * 255 + a / 2) / a)
                    b = min(255, (b * 255 + a / 2) / a)
                }
            }
            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        return pixels
    }

    // entries are 16-bit, but some writers store 8-bit values, libtiff detects them the same way
    private fun colorMap(bps: Int): IntArray {
        val entries = 1 shl bps
        val raw = ints(320)
        if (raw == null || raw.size < entries * 3) throw ImageFormatException("Palette TIFF has no colour map")
        val eightBit = raw.all { it < 256 }
        fun channel(v: Long): Int = if (eightBit) v.toInt() else (v shr 8).toInt()
        return IntArray(256) {
            if (it >= entries) {
                0
            } else {
                (channel(raw[it]) shl 16) or (channel(raw[entries + it]) shl 8) or channel(raw[2 * entries + it])
            }
        }
    }

    private companion object {
        const val MAX_PIXELS = 1L shl 28
        const val MAX_TILE = 65536
        const val MAX_CHUNK = 1L shl 30
        const val NONE = 1
        const val LZW = 5
        const val DEFLATE = 8
        const val OLD_DEFLATE = 32946
        const val PACKBITS = 32773
        const val WHITE_IS_ZERO = 0
        const val BLACK_IS_ZERO = 1
        const val RGB = 2
        const val PALETTE = 3
        const val SEPARATED = 5

        fun reverseBits(b: Byte): Byte {
            var v = b.toInt() and 0xFF
            var r = 0
            repeat(8) {
                r = (r shl 1) or (v and 1)
                v = v shr 1
            }
            return r.toByte()
        }
    }
}

// MSB-first, the code width grows one code early. Pre-6.0 libtiff wrote GIF-style LSB-first without
// the early change, such data starts with 0x00 0x01 instead of 0x80
private object TiffLzw {
    private const val CLEAR = 256
    private const val END = 257

    fun decode(src: ByteArray, expected: Int): ByteArray {
        val out = ByteArray(expected)
        val oldStyle = src.size >= 2 && src[0] == 0.toByte() && (src[1].toInt() and 1) != 0
        val prefix = IntArray(4096)
        val suffix = ByteArray(4096)
        val first = ByteArray(4096)
        val length = IntArray(4096)
        for (i in 0 until 256) {
            suffix[i] = i.toByte()
            first[i] = i.toByte()
            length[i] = 1
        }
        var pos = 0
        var acc = 0
        var bits = 0
        var width = 9
        var next = 258
        var old = -1
        var o = 0
        while (o < expected) {
            while (bits < width && pos < src.size) {
                val b = src[pos++].toInt() and 0xFF
                if (oldStyle) acc = acc or (b shl bits) else acc = (acc shl 8) or b
                bits += 8
            }
            if (bits < width) break
            val code: Int
            if (oldStyle) {
                code = acc and ((1 shl width) - 1)
                acc = acc ushr width
            } else {
                code = (acc ushr (bits - width)) and ((1 shl width) - 1)
                acc = acc and ((1 shl (bits - width)) - 1)
            }
            bits -= width
            if (code == END) break
            if (code == CLEAR) {
                width = 9
                next = 258
                old = -1
                continue
            }
            if (old < 0) {
                if (code > 255) break
                out[o++] = code.toByte()
                old = code
                continue
            }
            val known = code < next
            if (!known && code != next) break
            if (next < 4096) {
                prefix[next] = old
                suffix[next] = if (known) first[code] else first[old]
                first[next] = first[old]
                length[next] = length[old] + 1
                next++
                val limit = if (oldStyle) 1 shl width else (1 shl width) - 1
                if (next >= limit && width < 12) width++
            }
            var c = code
            val n = length[c]
            var at = o + n - 1
            while (c >= 0 && at >= o) {
                if (at < expected) out[at] = suffix[c]
                at--
                c = if (length[c] > 1) prefix[c] else -1
            }
            o += n
            old = code
        }
        return out
    }
}

private fun oriented(pixels: IntArray, width: Int, height: Int, orientation: Int): DecodedImage {
    if (orientation !in 2..8) return DecodedImage(width, height, pixels)
    val swap = orientation >= 5
    val outWidth = if (swap) height else width
    val outHeight = if (swap) width else height
    val out = IntArray(pixels.size)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val dst = when (orientation) {
                2 -> y * outWidth + width - 1 - x
                3 -> (height - 1 - y) * outWidth + width - 1 - x
                4 -> (height - 1 - y) * outWidth + x
                5 -> x * outWidth + y
                6 -> x * outWidth + height - 1 - y
                7 -> (width - 1 - x) * outWidth + height - 1 - y
                else -> (width - 1 - x) * outWidth + y
            }
            out[dst] = pixels[y * width + x]
        }
    }
    return DecodedImage(outWidth, outHeight, out)
}
