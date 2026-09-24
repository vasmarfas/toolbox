package com.vasmarfas.card.tools.media

import com.vasmarfas.card.core.Zlib
import com.vasmarfas.card.tools.developer.Crc32
import kotlin.math.abs

object Png {
    internal val SIGNATURE = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)

    private const val GRAY = 0
    private const val RGB = 2
    private const val INDEXED = 3
    private const val GRAY_ALPHA = 4
    private const val RGBA = 6

    fun encode(pixels: IntArray, width: Int, height: Int, paletteColors: Int = 0, dither: Boolean = true, level: Int = 9): ByteArray {
        require(width > 0 && height > 0 && pixels.size == width * height) { "image must have width × height pixels" }
        require(paletteColors in 0..256) { "paletteColors must be within 0..256" }
        val out = ByteSink(pixels.size + 1024)
        out.bytes(SIGNATURE)
        val image = if (paletteColors > 0) indexed(out, pixels, width, height, paletteColors, dither) else truecolor(out, pixels, width, height)
        out.pngChunk("IDAT", Zlib.compress(image, level))
        out.pngChunk("IEND", ByteArray(0))
        return out.toByteArray()
    }

    private fun indexed(out: ByteSink, pixels: IntArray, width: Int, height: Int, colors: Int, dither: Boolean): ByteArray {
        val palette = Quantizer.palette(pixels, colors)
        val indices = Quantizer.remap(pixels, width, height, palette, dither)
        val depth = when {
            palette.size <= 2 -> 1
            palette.size <= 4 -> 2
            palette.size <= 16 -> 4
            else -> 8
        }
        out.pngChunk("IHDR", header(width, height, depth, INDEXED))
        val plte = ByteArray(palette.size * 3)
        var alphas = 0
        for ((i, c) in palette.colors.withIndex()) {
            plte[i * 3] = (c shr 16).toByte()
            plte[i * 3 + 1] = (c shr 8).toByte()
            plte[i * 3 + 2] = c.toByte()
            if (c ushr 24 != 255) alphas = i + 1
        }
        out.pngChunk("PLTE", plte)
        if (alphas > 0) out.pngChunk("tRNS", ByteArray(alphas) { (palette.colors[it] ushr 24).toByte() })
        val rowBytes = (width * depth + 7) / 8
        val perByte = 8 / depth
        val image = ByteArray(height * (rowBytes + 1))
        for (y in 0 until height) {
            val row = y * (rowBytes + 1) + 1
            val src = y * width
            if (depth == 8) {
                indices.copyInto(image, row, src, src + width)
            } else {
                for (x in 0 until width) {
                    val shift = 8 - depth * (x % perByte + 1)
                    val at = row + x / perByte
                    image[at] = (image[at].toInt() or ((indices[src + x].toInt() and 0xFF) shl shift)).toByte()
                }
            }
        }
        return image
    }

    private fun truecolor(out: ByteSink, pixels: IntArray, width: Int, height: Int): ByteArray {
        var opaque = true
        var gray = true
        for (p in pixels) {
            if (p ushr 24 != 255) opaque = false
            val g = (p shr 8) and 0xFF
            if (((p shr 16) and 0xFF) != g || (p and 0xFF) != g) gray = false
            if (!opaque && !gray) break
        }
        val colorType = when {
            gray && opaque -> GRAY
            gray -> GRAY_ALPHA
            opaque -> RGB
            else -> RGBA
        }
        val bpp = when (colorType) {
            GRAY -> 1
            GRAY_ALPHA -> 2
            RGB -> 3
            else -> 4
        }
        out.pngChunk("IHDR", header(width, height, 8, colorType))
        val rowBytes = width * bpp
        var prev = ByteArray(rowBytes)
        var cur = ByteArray(rowBytes)
        var best = ByteArray(rowBytes)
        var trial = ByteArray(rowBytes)
        val image = ByteArray(height * (rowBytes + 1))
        for (y in 0 until height) {
            var o = 0
            for (x in y * width until (y + 1) * width) {
                val p = pixels[x]
                when (colorType) {
                    GRAY -> cur[o++] = p.toByte()
                    GRAY_ALPHA -> {
                        cur[o++] = p.toByte()
                        cur[o++] = (p ushr 24).toByte()
                    }
                    RGB -> {
                        cur[o++] = (p shr 16).toByte()
                        cur[o++] = (p shr 8).toByte()
                        cur[o++] = p.toByte()
                    }
                    else -> {
                        cur[o++] = (p shr 16).toByte()
                        cur[o++] = (p shr 8).toByte()
                        cur[o++] = p.toByte()
                        cur[o++] = (p ushr 24).toByte()
                    }
                }
            }
            var bestType = 0
            var bestCost = Long.MAX_VALUE
            for (type in 0..4) {
                filter(type, cur, prev, bpp, trial)
                val cost = cost(trial)
                if (cost < bestCost) {
                    bestCost = cost
                    bestType = type
                    val t = best
                    best = trial
                    trial = t
                }
            }
            val row = y * (rowBytes + 1)
            image[row] = bestType.toByte()
            best.copyInto(image, row + 1)
            val t = prev
            prev = cur
            cur = t
        }
        return image
    }

    private fun filter(type: Int, cur: ByteArray, prev: ByteArray, bpp: Int, dst: ByteArray) {
        val n = cur.size
        when (type) {
            0 -> cur.copyInto(dst)
            1 -> {
                cur.copyInto(dst, 0, 0, bpp)
                for (i in bpp until n) dst[i] = (cur[i] - cur[i - bpp]).toByte()
            }
            2 -> for (i in 0 until n) dst[i] = (cur[i] - prev[i]).toByte()
            3 -> for (i in 0 until n) {
                val a = if (i >= bpp) cur[i - bpp].toInt() and 0xFF else 0
                dst[i] = (cur[i] - ((a + (prev[i].toInt() and 0xFF)) shr 1)).toByte()
            }
            else -> for (i in 0 until n) {
                val a = if (i >= bpp) cur[i - bpp].toInt() and 0xFF else 0
                val c = if (i >= bpp) prev[i - bpp].toInt() and 0xFF else 0
                dst[i] = (cur[i] - paeth(a, prev[i].toInt() and 0xFF, c)).toByte()
            }
        }
    }

    private fun cost(row: ByteArray): Long {
        var sum = 0L
        for (b in row) sum += abs(b.toInt())
        return sum
    }

    private fun paeth(a: Int, b: Int, c: Int): Int {
        val p = a + b - c
        val pa = abs(p - a)
        val pb = abs(p - b)
        val pc = abs(p - c)
        return if (pa <= pb && pa <= pc) a else if (pb <= pc) b else c
    }

    private fun header(width: Int, height: Int, depth: Int, colorType: Int): ByteArray {
        val h = ByteSink(13)
        h.intBE(width)
        h.intBE(height)
        h.byte(depth)
        h.byte(colorType)
        h.byte(0)
        h.byte(0)
        h.byte(0)
        return h.toByteArray()
    }
}

internal fun ByteSink.pngChunk(type: String, data: ByteArray) {
    val typed = ByteArray(4 + data.size)
    for (i in 0 until 4) typed[i] = type[i].code.toByte()
    data.copyInto(typed, 4)
    intBE(data.size)
    bytes(typed)
    intBE(Crc32.compute(typed))
}
