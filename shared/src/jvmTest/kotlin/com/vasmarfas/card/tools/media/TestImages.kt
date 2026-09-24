package com.vasmarfas.card.tools.media

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.CRC32
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

fun photo(width: Int, height: Int, seed: Int = 1, shift: Double = 0.0): IntArray {
    val rnd = Random(seed)
    val blobs = List(7) { DoubleArray(6) { rnd.nextDouble() } }
    val noise = Random(seed * 31 + 7)
    val out = IntArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val u = x.toDouble() / width + shift
            val v = y.toDouble() / height
            val horizon = 0.55 + 0.08 * sin(u * 6.0 + seed)
            var r: Double
            var g: Double
            var b: Double
            if (v < horizon) {
                r = 90 + 110 * v
                g = 140 + 90 * v
                b = 235 - 40 * v
                val dx = u - 0.75
                val dy = v - 0.2
                val glow = 255 * exp(-(dx * dx + dy * dy) * 40)
                r += glow
                g += glow * 0.9
                b += glow * 0.6
            } else {
                val t = valueNoise(u * 40, v * 40, seed) * 0.6 + valueNoise(u * 9, v * 9, seed + 1) * 0.4
                r = 60 + 90 * t + 40 * (v - horizon)
                g = 100 + 70 * t
                b = 35 + 40 * t
            }
            for (blob in blobs) {
                val cx = blob[0]
                val cy = 0.3 + blob[1] * 0.6
                val rad = 0.04 + blob[2] * 0.1
                val dx = (u - shift * 0.5 - cx) / rad
                val dy = (v - cy) / (rad * width / height)
                val d = dx * dx + dy * dy
                if (d < 1) {
                    val shade = 0.55 + 0.45 * (1 - d) - 0.2 * dx
                    r = 255 * blob[3] * shade + 20
                    g = 255 * blob[4] * shade + 20
                    b = 255 * blob[5] * shade + 20
                }
            }
            r += noise.nextInt(-4, 5)
            g += noise.nextInt(-4, 5)
            b += noise.nextInt(-4, 5)
            out[y * width + x] = argb(255, r.toInt(), g.toInt(), b.toInt())
        }
    }
    return out
}

private fun valueNoise(x: Double, y: Double, seed: Int): Double {
    val x0 = x.toInt()
    val y0 = y.toInt()
    val fx = x - x0
    val fy = y - y0
    fun corner(i: Int, j: Int): Double {
        var h = i * 374761393 + j * 668265263 + seed * 144269504
        h = (h xor (h ushr 13)) * 1274126177
        return ((h xor (h ushr 16)) and 0xFFFF) / 65535.0
    }
    val sx = fx * fx * (3 - 2 * fx)
    val sy = fy * fy * (3 - 2 * fy)
    val top = corner(x0, y0) * (1 - sx) + corner(x0 + 1, y0) * sx
    val bottom = corner(x0, y0 + 1) * (1 - sx) + corner(x0 + 1, y0 + 1) * sx
    return top * (1 - sy) + bottom * sy
}

fun argb(a: Int, r: Int, g: Int, b: Int): Int =
    (a.coerceIn(0, 255) shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

fun poster(width: Int, height: Int, colors: Int, seed: Int = 3): IntArray {
    val rnd = Random(seed)
    val palette = IntArray(colors) { argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)) }
    return IntArray(width * height) {
        val x = it % width
        val y = it / width
        palette[((x / 7) * 31 + (y / 5) * 17 + (x * y / 97)) % colors]
    }
}

fun image(pixels: IntArray, width: Int, height: Int, type: Int = BufferedImage.TYPE_INT_ARGB): BufferedImage {
    val img = BufferedImage(width, height, type)
    img.setRGB(0, 0, width, height, pixels, 0, width)
    return img
}

fun pixels(img: BufferedImage): IntArray = img.getRGB(0, 0, img.width, img.height, null, 0, img.width)

fun decode(bytes: ByteArray): BufferedImage = ImageIO.read(ByteArrayInputStream(bytes)) ?: error("ImageIO could not decode the image")

fun encodeWith(img: BufferedImage, format: String): ByteArray {
    val out = ByteArrayOutputStream()
    check(ImageIO.write(img, format, out)) { "no ImageIO writer for $format" }
    return out.toByteArray()
}

class ChannelError(val mean: Double, val max: Int)

fun channelError(expected: IntArray, actual: IntArray): ChannelError {
    require(expected.size == actual.size)
    var sum = 0L
    var worst = 0
    for (i in expected.indices) {
        for (shift in intArrayOf(0, 8, 16)) {
            val d = abs(((expected[i] shr shift) and 0xFF) - ((actual[i] shr shift) and 0xFF))
            sum += d
            worst = max(worst, d)
        }
    }
    return ChannelError(sum.toDouble() / (expected.size * 3), worst)
}

fun blockError(expected: IntArray, actual: IntArray, width: Int, height: Int): Double {
    var sum = 0.0
    var n = 0
    for (by in 0 until height / 4) {
        for (bx in 0 until width / 4) {
            for (shift in intArrayOf(0, 8, 16)) {
                var e = 0
                var a = 0
                for (y in by * 4 until by * 4 + 4) {
                    for (x in bx * 4 until bx * 4 + 4) {
                        e += (expected[y * width + x] shr shift) and 0xFF
                        a += (actual[y * width + x] shr shift) and 0xFF
                    }
                }
                sum += abs(e - a) / 16.0
                n++
            }
        }
    }
    return sum / n
}

fun ByteArray.indexOf(pattern: ByteArray, from: Int = 0): Int {
    outer@ for (i in from..size - pattern.size) {
        for (j in pattern.indices) if (this[i + j] != pattern[j]) continue@outer
        return i
    }
    return -1
}

fun ascii(text: String): ByteArray = text.toByteArray(Charsets.ISO_8859_1)

class PngChunk(val type: String, val data: ByteArray, val crcValid: Boolean)

fun pngChunks(png: ByteArray): List<PngChunk> {
    check(png.copyOf(8).contentEquals(Png.SIGNATURE)) { "not a PNG" }
    val chunks = ArrayList<PngChunk>()
    var pos = 8
    while (pos < png.size) {
        val length = ByteBuffer.wrap(png, pos, 4).int
        val type = String(png, pos + 4, 4, Charsets.ISO_8859_1)
        val crc = CRC32().apply { update(png, pos + 4, length + 4) }.value.toInt()
        chunks += PngChunk(type, png.copyOfRange(pos + 8, pos + 8 + length), crc == ByteBuffer.wrap(png, pos + 8 + length, 4).int)
        pos += 12 + length
    }
    return chunks
}

fun pngOf(chunks: List<Pair<String, ByteArray>>): ByteArray {
    val out = ByteArrayOutputStream()
    out.write(Png.SIGNATURE)
    for ((type, data) in chunks) {
        val typed = ascii(type) + data
        out.write(ByteBuffer.allocate(4).putInt(data.size).array())
        out.write(typed)
        out.write(ByteBuffer.allocate(4).putInt(CRC32().apply { update(typed) }.value.toInt()).array())
    }
    return out.toByteArray()
}
