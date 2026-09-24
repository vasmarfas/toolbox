package com.vasmarfas.card.tools.media

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

class Palette(val colors: IntArray, val transparentIndex: Int) {
    val size: Int get() = colors.size
}

// Wu-style greedy variance splitting over a 5-bit-per-channel histogram, then a few k-means passes.
// Translucent pixels are handled premultiplied, so nearly invisible colours do not take palette slots
object Quantizer {
    private const val REFINE_PASSES = 3
    private const val LARGE_HISTOGRAM = 200_000

    // fully transparent pixels share entry 0, entries are ordered by alpha, then luma
    fun palette(pixels: IntArray, maxColors: Int = 256): Palette {
        require(maxColors in 1..256) { "maxColors must be within 1..256" }
        var transparent = false
        var translucent = false
        val distinct = ColorSet(maxColors + 1)
        for (p in pixels) {
            val a = p ushr 24
            if (a == 0) {
                transparent = true
            } else {
                if (a != 255) translucent = true
                if (distinct.size <= maxColors) distinct.add(p)
            }
        }
        val budget = if (transparent) maxColors - 1 else maxColors
        val colors = if (distinct.size <= budget) distinct.toIntArray() else reduce(pixels, budget, translucent)
        val ordered = colors.sortedWith(compareBy<Int>({ it ushr 24 }, { luma(it) }))
        if (!transparent) return Palette(ordered.toIntArray(), -1)
        return Palette(IntArray(ordered.size + 1) { if (it == 0) 0 else ordered[it - 1] }, 0)
    }

    fun remap(pixels: IntArray, width: Int, height: Int, palette: Palette, dither: Boolean): ByteArray {
        require(width > 0 && height > 0 && pixels.size >= width * height) { "pixel buffer is smaller than width × height" }
        require(palette.size in 1..256) { "palette must have 1..256 entries" }
        val matcher = ColorMatcher(palette)
        val out = ByteArray(width * height)
        if (!dither) {
            var last = pixels[0]
            var lastIndex = matcher.map(last)
            for (i in out.indices) {
                val p = pixels[i]
                if (p != last) {
                    last = p
                    lastIndex = matcher.map(p)
                }
                out[i] = lastIndex.toByte()
            }
            return out
        }
        val transparent = matcher.transparent
        val translucent = matcher.translucent
        val coords = matcher.coords
        val limit = matcher.errorLimit
        val stride = (width + 2) * 4
        var cur = IntArray(stride)
        var next = IntArray(stride)
        for (y in 0 until height) {
            val dir = if (y and 1 == 0) 1 else -1
            val step = dir * 4
            var x = if (dir > 0) 0 else width - 1
            repeat(width) {
                val i = y * width + x
                val p = pixels[i]
                val e = (x + 1) * 4
                if (p ushr 24 == 0 && transparent >= 0) {
                    out[i] = transparent.toByte()
                } else {
                    val q = if (translucent) premultiply(p) else p
                    val r = clamp(((q shr 16) and 0xFF) + ((cur[e] + 8) shr 4).coerceIn(-limit, limit))
                    val g = clamp(((q shr 8) and 0xFF) + ((cur[e + 1] + 8) shr 4).coerceIn(-limit, limit))
                    val b = clamp((q and 0xFF) + ((cur[e + 2] + 8) shr 4).coerceIn(-limit, limit))
                    val a = if (translucent) clamp((q ushr 24) + ((cur[e + 3] + 8) shr 4).coerceIn(-limit, limit)) else 255
                    val j = matcher.nearest(r, g, b, a)
                    out[i] = j.toByte()
                    val o = j * 4
                    diffuse(cur, next, e, step, 0, r - coords[o])
                    diffuse(cur, next, e, step, 1, g - coords[o + 1])
                    diffuse(cur, next, e, step, 2, b - coords[o + 2])
                    if (translucent) diffuse(cur, next, e, step, 3, a - coords[o + 3])
                }
                x += dir
            }
            val t = cur
            cur = next
            next = t
            next.fill(0)
        }
        return out
    }

    private fun diffuse(cur: IntArray, next: IntArray, e: Int, step: Int, channel: Int, error: Int) {
        if (error == 0) return
        cur[e + step + channel] += error * 7
        next[e - step + channel] += error * 3
        next[e + channel] += error * 5
        next[e + step + channel] += error
    }

    private fun reduce(pixels: IntArray, count: Int, translucent: Boolean): IntArray {
        if (count == 0) return IntArray(0)
        val histogram = Histogram(pixels, translucent)
        val centroids = VarianceSplit(histogram, count).run()
        refine(histogram, centroids)
        return IntArray(centroids.size / 4) { toArgb(centroids, it * 4) }
    }

    private fun refine(h: Histogram, centroids: FloatArray) {
        val k = centroids.size / 4
        if (k < 2) return
        val sums = DoubleArray(k * 4)
        val weights = DoubleArray(k)
        repeat(if (h.size > LARGE_HISTOGRAM) 1 else REFINE_PASSES) {
            sums.fill(0.0)
            weights.fill(0.0)
            val search = CentroidSearch(centroids, k, h.dims)
            for (e in 0 until h.size) {
                val j = search.nearest(h.coords, e * 4)
                val w = h.weight[e].toDouble()
                weights[j] += w
                for (d in 0 until 4) sums[j * 4 + d] += w * h.coords[e * 4 + d]
            }
            for (j in 0 until k) {
                if (weights[j] > 0.0) for (d in 0 until 4) centroids[j * 4 + d] = (sums[j * 4 + d] / weights[j]).toFloat()
            }
        }
    }

    private fun toArgb(c: FloatArray, o: Int): Int {
        val a = c[o + 3].roundToInt().coerceIn(0, 255)
        if (a == 0) return 0
        val scale = 255f / a
        val r = (c[o] * scale).roundToInt().coerceIn(0, 255)
        val g = (c[o + 1] * scale).roundToInt().coerceIn(0, 255)
        val b = (c[o + 2] * scale).roundToInt().coerceIn(0, 255)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun luma(c: Int): Int = 299 * ((c shr 16) and 0xFF) + 587 * ((c shr 8) and 0xFF) + 114 * (c and 0xFF)

    private fun clamp(v: Int): Int = if (v < 0) 0 else if (v > 255) 255 else v
}

// an alpha error shows in all three channels against a contrasting background, a colour error in one
private const val ALPHA_WEIGHT = 3

private fun premultiply(p: Int): Int {
    val a = p ushr 24
    if (a == 255) return p
    val r = (((p shr 16) and 0xFF) * a + 127) / 255
    val g = (((p shr 8) and 0xFF) * a + 127) / 255
    val b = ((p and 0xFF) * a + 127) / 255
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

private fun cellKey(r: Int, g: Int, b: Int, a: Int, translucent: Boolean): Int {
    val rgb = ((r shr 3) shl 10) or ((g shr 3) shl 5) or (b shr 3)
    return if (translucent) ((a shr 3) shl 15) or rgb else rgb
}

private class ColorSet(private val limit: Int) {
    private val bits = 32 - (limit * 2 - 1).countLeadingZeroBits()
    private val table = IntArray(1 shl bits)
    var size = 0
        private set

    fun add(c: Int) {
        val mask = table.size - 1
        var slot = (c * -0x61c88647) ushr (32 - bits)
        while (true) {
            val v = table[slot]
            if (v == c) return
            if (v == 0) {
                if (size == limit) return
                table[slot] = c
                size++
                return
            }
            slot = (slot + 1) and mask
        }
    }

    fun toIntArray(): IntArray {
        val out = IntArray(size)
        var n = 0
        for (v in table) if (v != 0) out[n++] = v
        return out
    }
}

private class Histogram(pixels: IntArray, translucent: Boolean) {
    val dims = if (translucent) 4 else 3
    val size: Int
    val weight: IntArray
    val coords: FloatArray

    init {
        val slots = IntArray(1 shl (if (translucent) 20 else 15))
        for (p in pixels) {
            if (p ushr 24 == 0) continue
            val q = premultiply(p)
            slots[cellKey((q shr 16) and 0xFF, (q shr 8) and 0xFF, q and 0xFF, q ushr 24, translucent)] = 1
        }
        var n = 0
        for (i in slots.indices) if (slots[i] != 0) slots[i] = ++n
        size = n
        weight = IntArray(n)
        val sums = LongArray(n * 4)
        for (p in pixels) {
            if (p ushr 24 == 0) continue
            val q = premultiply(p)
            val r = (q shr 16) and 0xFF
            val g = (q shr 8) and 0xFF
            val b = q and 0xFF
            val a = q ushr 24
            val e = slots[cellKey(r, g, b, a, translucent)] - 1
            weight[e]++
            val o = e * 4
            sums[o] += r.toLong()
            sums[o + 1] += g.toLong()
            sums[o + 2] += b.toLong()
            sums[o + 3] += a.toLong()
        }
        coords = FloatArray(n * 4) { (sums[it].toDouble() / weight[it shr 2]).toFloat() }
    }
}

private class VarianceSplit(private val h: Histogram, private val count: Int) {
    private val dims = h.dims
    private val moments = dims + 1
    private val order = IntArray(h.size) { it }
    private val lo = IntArray(count)
    private val hi = IntArray(count)
    private val gain = DoubleArray(count)
    private val axis = IntArray(count)
    private val cut = IntArray(count)
    private val levels = DoubleArray(dims * 256 * moments)
    private val total = DoubleArray(moments)
    private val left = DoubleArray(moments)
    private val weight = DoubleArray(moments) { if (it == 4) ALPHA_WEIGHT.toDouble() else 1.0 }

    fun run(): FloatArray {
        var boxes = 1
        hi[0] = h.size
        evaluate(0)
        while (boxes < count) {
            var best = -1
            for (i in 0 until boxes) if (gain[i] > 0.0 && (best < 0 || gain[i] > gain[best])) best = i
            if (best < 0) break
            val mid = partition(lo[best], hi[best], axis[best], cut[best])
            lo[boxes] = mid
            hi[boxes] = hi[best]
            hi[best] = mid
            evaluate(best)
            evaluate(boxes)
            boxes++
        }
        val centroids = FloatArray(boxes * 4)
        for (b in 0 until boxes) {
            var w = 0.0
            var r = 0.0
            var g = 0.0
            var bl = 0.0
            var a = 0.0
            for (i in lo[b] until hi[b]) {
                val e = order[i]
                val ew = h.weight[e].toDouble()
                val o = e * 4
                w += ew
                r += ew * h.coords[o]
                g += ew * h.coords[o + 1]
                bl += ew * h.coords[o + 2]
                a += ew * h.coords[o + 3]
            }
            centroids[b * 4] = (r / w).toFloat()
            centroids[b * 4 + 1] = (g / w).toFloat()
            centroids[b * 4 + 2] = (bl / w).toFloat()
            centroids[b * 4 + 3] = (a / w).toFloat()
        }
        return centroids
    }

    private fun evaluate(box: Int) {
        gain[box] = 0.0
        if (hi[box] - lo[box] < 2) return
        levels.fill(0.0)
        for (i in lo[box] until hi[box]) {
            val e = order[i]
            val w = h.weight[e].toDouble()
            val o = e * 4
            for (ax in 0 until dims) {
                val base = (ax * 256 + h.coords[o + ax].toInt()) * moments
                levels[base] += w
                for (d in 0 until dims) levels[base + 1 + d] += w * h.coords[o + d]
            }
        }
        total.fill(0.0)
        for (t in 0 until 256) for (m in 0 until moments) total[m] += levels[t * moments + m]
        val whole = total[0]
        var base = 0.0
        for (d in 1 until moments) base += weight[d] * total[d] * total[d] / whole
        var bestGain = 0.0
        for (ax in 0 until dims) {
            left.fill(0.0)
            for (t in 0 until 255) {
                val at = (ax * 256 + t) * moments
                if (levels[at] == 0.0) continue
                for (m in 0 until moments) left[m] += levels[at + m]
                val wl = left[0]
                val wr = whole - wl
                if (wr <= 0.0) break
                var g = -base
                for (d in 1 until moments) {
                    val sl = left[d]
                    val sr = total[d] - sl
                    g += weight[d] * (sl * sl / wl + sr * sr / wr)
                }
                if (g > bestGain) {
                    bestGain = g
                    axis[box] = ax
                    cut[box] = t
                }
            }
        }
        gain[box] = bestGain
    }

    private fun partition(from: Int, to: Int, ax: Int, level: Int): Int {
        var i = from
        var j = to - 1
        while (i <= j) {
            if (h.coords[order[i] * 4 + ax].toInt() <= level) {
                i++
            } else {
                val t = order[i]
                order[i] = order[j]
                order[j] = t
                j--
            }
        }
        return i
    }
}

private class CentroidSearch(private val c: FloatArray, private val k: Int, private val dims: Int) {
    private val axis: Int
    private val axisWeight: Float
    private val order: IntArray
    private val keys: FloatArray

    init {
        var best = 0
        var bestSpread = -1f
        for (ax in 0 until dims) {
            var min = Float.MAX_VALUE
            var max = -Float.MAX_VALUE
            for (j in 0 until k) {
                val v = c[j * 4 + ax]
                if (v < min) min = v
                if (v > max) max = v
            }
            if (max - min > bestSpread) {
                bestSpread = max - min
                best = ax
            }
        }
        axis = best
        axisWeight = if (best == 3) ALPHA_WEIGHT.toFloat() else 1f
        order = (0 until k).sortedBy { c[it * 4 + best] }.toIntArray()
        keys = FloatArray(k) { c[order[it] * 4 + best] }
    }

    fun nearest(p: FloatArray, o: Int): Int {
        val v = p[o + axis]
        var up = lowerBound(v)
        var down = up - 1
        var best = order[if (up < k) up else down]
        var bestD = distance(p, o, best)
        while (up < k || down >= 0) {
            if (up < k) {
                val dv = keys[up] - v
                if (dv * dv * axisWeight >= bestD) {
                    up = k
                } else {
                    val j = order[up++]
                    val d = distance(p, o, j)
                    if (d < bestD) {
                        bestD = d
                        best = j
                    }
                }
            }
            if (down >= 0) {
                val dv = v - keys[down]
                if (dv * dv * axisWeight >= bestD) {
                    down = -1
                } else {
                    val j = order[down--]
                    val d = distance(p, o, j)
                    if (d < bestD) {
                        bestD = d
                        best = j
                    }
                }
            }
        }
        return best
    }

    private fun distance(p: FloatArray, o: Int, j: Int): Float {
        var d = 0f
        for (ax in 0 until dims) {
            val t = p[o + ax] - c[j * 4 + ax]
            d += if (ax == 3) t * t * ALPHA_WEIGHT else t * t
        }
        return d
    }

    private fun lowerBound(v: Float): Int {
        var a = 0
        var b = k
        while (a < b) {
            val m = (a + b) ushr 1
            if (keys[m] < v) a = m + 1 else b = m
        }
        return a
    }
}

// Heckbert's locally sorted search: each 8x8x8 (x8 alpha) cell keeps the entries that can be nearest
// to some point in it, built on first use
private class ColorMatcher(palette: Palette) {
    val transparent = palette.transparentIndex
    val translucent = palette.colors.any { (it ushr 24) in 1..254 }
    val coords = IntArray(palette.size * 4)
    private val dims = if (translucent) 4 else 3
    private val eligible: IntArray

    // four times the mean distance between neighbouring entries: enough to mix any two of them, and error
    // does not pile up in empty regions of colour space as isolated off-colour pixels
    val errorLimit: Int
    private val cells = IntArray(1 shl (if (translucent) 20 else 15)).also { it.fill(-1) }
    private val minDistance: IntArray
    private var pool = IntArray(4096)
    private var poolSize = 0

    init {
        for (j in 0 until palette.size) {
            val q = if (translucent) premultiply(palette.colors[j]) else palette.colors[j]
            coords[j * 4] = (q shr 16) and 0xFF
            coords[j * 4 + 1] = (q shr 8) and 0xFF
            coords[j * 4 + 2] = q and 0xFF
            coords[j * 4 + 3] = if (translucent) q ushr 24 else 255
        }
        eligible = (0 until palette.size).filter { translucent || it != transparent }.toIntArray()
        minDistance = IntArray(eligible.size)
        errorLimit = spacingLimit()
    }

    private fun spacingLimit(): Int {
        if (eligible.size < 2) return 255
        var sum = 0.0
        for (i in eligible) {
            var best = Int.MAX_VALUE
            for (j in eligible) {
                if (i == j) continue
                var d = 0
                for (c in 0 until dims) {
                    val t = coords[i * 4 + c] - coords[j * 4 + c]
                    d += t * t
                }
                if (d < best) best = d
            }
            sum += sqrt(best.toDouble())
        }
        return max(16, (4 * sum / eligible.size).roundToInt())
    }

    fun map(p: Int): Int {
        val a = p ushr 24
        if (a == 0 && transparent >= 0) return transparent
        if (!translucent) return nearest((p shr 16) and 0xFF, (p shr 8) and 0xFF, p and 0xFF, 255)
        val q = premultiply(p)
        return nearest((q shr 16) and 0xFF, (q shr 8) and 0xFF, q and 0xFF, a)
    }

    fun nearest(r: Int, g: Int, b: Int, a: Int): Int {
        if (eligible.isEmpty()) return 0
        val key = cellKey(r, g, b, a, translucent)
        var start = cells[key]
        if (start < 0) {
            start = fill(r and 0xF8, g and 0xF8, b and 0xF8, a and 0xF8)
            cells[key] = start
        }
        val n = pool[start]
        var best = pool[start + 1]
        if (n == 1) return best
        var bestD = Int.MAX_VALUE
        for (i in start + 1..start + n) {
            val j = pool[i]
            val o = j * 4
            val dr = r - coords[o]
            val dg = g - coords[o + 1]
            val db = b - coords[o + 2]
            var d = dr * dr + dg * dg + db * db
            if (translucent) {
                val da = a - coords[o + 3]
                d += da * da * ALPHA_WEIGHT
            }
            if (d < bestD) {
                bestD = d
                best = j
            }
        }
        return best
    }

    private fun fill(r0: Int, g0: Int, b0: Int, a0: Int): Int {
        var limit = Int.MAX_VALUE
        for (i in eligible.indices) {
            val o = eligible[i] * 4
            var far = farthest(coords[o], r0) + farthest(coords[o + 1], g0) + farthest(coords[o + 2], b0)
            var near = closest(coords[o], r0) + closest(coords[o + 1], g0) + closest(coords[o + 2], b0)
            if (dims == 4) {
                far += farthest(coords[o + 3], a0) * ALPHA_WEIGHT
                near += closest(coords[o + 3], a0) * ALPHA_WEIGHT
            }
            minDistance[i] = near
            if (far < limit) limit = far
        }
        if (poolSize + eligible.size + 1 > pool.size) pool = pool.copyOf(max(pool.size * 2, poolSize + eligible.size + 1))
        val start = poolSize
        var n = 0
        for (i in eligible.indices) if (minDistance[i] <= limit) pool[start + 1 + n++] = eligible[i]
        pool[start] = n
        poolSize += n + 1
        return start
    }

    private fun closest(v: Int, low: Int): Int {
        val d = if (v < low) low - v else if (v > low + 7) v - low - 7 else 0
        return d * d
    }

    private fun farthest(v: Int, low: Int): Int {
        val d = max(abs(v - low), abs(v - low - 7))
        return d * d
    }
}
