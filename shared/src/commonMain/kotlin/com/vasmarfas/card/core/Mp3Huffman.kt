package com.vasmarfas.card.core

import kotlin.math.max
import kotlin.math.min

private val GROUP_TABLES = arrayOf(
    intArrayOf(1),
    intArrayOf(2, 3),
    intArrayOf(5, 6),
    intArrayOf(7, 8, 9),
    intArrayOf(10, 11, 12),
    intArrayOf(13, 15),
    intArrayOf(16, 24),
)
private val GROUP_LIMIT = intArrayOf(1, 2, 3, 5, 7, 15, MAX_VALUE)
private const val ESCAPE_GROUP = 6
private const val MAX_VALUE = 8206

// code length plus sign bits, one 16-bit field per table of the group
private val PACKED = Array(GROUP_TABLES.size) { group ->
    val tables = GROUP_TABLES[group]
    val size = Mp3Tables.tableSize[tables[0]]
    LongArray(size * size) { index ->
        val signs = (if (index / size != 0) 1 else 0) + (if (index % size != 0) 1 else 0)
        var packed = 0L
        for (field in tables.indices) packed = packed or ((Mp3Tables.lengths[tables[field]][index] + signs).toLong() shl (16 * field))
        packed
    }
}

private fun groupOf(max: Int): Int = when {
    max <= 3 -> max - 1
    max <= 5 -> 3
    max <= 7 -> 4
    max <= 15 -> 5
    else -> ESCAPE_GROUP
}

private fun escapeTable(first: Int, max: Int): Int {
    var table = first
    while ((1 shl Mp3Tables.linbits[table]) <= max - 15) table++
    return table
}

internal class Mp3Huffman {
    private var chosen = 0
    private var bands = 0
    private val bandMax = IntArray(23)
    private val prefix = Array(GROUP_TABLES.size) { LongArray(23) }
    private val escapes = IntArray(23)
    private val tailBits = IntArray(23)
    private val tailTable = IntArray(23)

    fun count(values: IntArray, granule: Mp3Granule, edges: IntArray): Int {
        val big = layout(values, granule, edges)
        val count1 = count1Bits(values, big, big + 4 * granule.count1, granule)
        if (granule.blockType != NORMAL_BLOCK) return count1 + switchedBits(values, granule, edges, big)
        if (big == 0) {
            setRegions(granule, 1, 1, 0, 0, 0)
            return count1
        }
        val first = ((bands + 1) / 3).coerceIn(1, 16)
        val second = min(bands, first + ((bands + 2) / 3).coerceIn(1, 8))
        val r1 = min(edges[first], big)
        val r2 = min(edges[second], big)
        var bits = rangeBits(values, 0, r1)
        val t0 = chosen
        bits += rangeBits(values, r1, r2)
        val t1 = chosen
        bits += rangeBits(values, r2, big)
        setRegions(granule, first, second, t0, t1, chosen)
        return count1 + bits
    }

    fun countBest(values: IntArray, granule: Mp3Granule, edges: IntArray): Int {
        val big = layout(values, granule, edges)
        val count1 = count1Bits(values, big, big + 4 * granule.count1, granule)
        if (granule.blockType != NORMAL_BLOCK) return count1 + switchedBits(values, granule, edges, big)
        if (big == 0) {
            setRegions(granule, 1, 1, 0, 0, 0)
            return count1
        }
        prepareBands(values, edges, big)
        for (c in 1..bands) {
            tailBits[c] = bandBits(c, bands)
            tailTable[c] = chosen
        }
        var best = Int.MAX_VALUE
        var bestFirst = 1
        var bestSecond = 1
        var tables = 0
        for (a in 1..min(16, bands)) {
            val bits0 = bandBits(0, a)
            val t0 = chosen
            if (a == bands) {
                if (bits0 < best) {
                    best = bits0
                    bestFirst = a
                    bestSecond = a
                    tables = t0 shl 10
                }
                continue
            }
            for (c in a + 1..min(a + 8, bands)) {
                val bits = bits0 + bandBits(a, c) + tailBits[c]
                if (bits < best) {
                    best = bits
                    bestFirst = a
                    bestSecond = c
                    tables = (t0 shl 10) or (chosen shl 5) or tailTable[c]
                }
            }
        }
        setRegions(granule, bestFirst, bestSecond, tables ushr 10, (tables ushr 5) and 31, tables and 31)
        return count1 + best
    }

    fun write(writer: Mp3BitWriter, values: IntArray, lines: DoubleArray, granule: Mp3Granule, edges: IntArray) {
        val big = granule.bigValues * 2
        val switched = granule.blockType != NORMAL_BLOCK
        val r1 = min(if (switched) switchedRegion(granule, edges) else edges[granule.region0Count + 1], big)
        val r2 = if (switched) big else min(edges[granule.region0Count + granule.region1Count + 2], big)
        writePairs(writer, values, lines, 0, r1, granule.tableSelect[0])
        writePairs(writer, values, lines, r1, r2, granule.tableSelect[1])
        writePairs(writer, values, lines, r2, big, granule.tableSelect[2])
        val end = big + 4 * granule.count1
        var i = big
        while (i < end) {
            val quad = values[i] * 8 + values[i + 1] * 4 + values[i + 2] * 2 + values[i + 3]
            if (granule.count1Table == 0) writer.write(Mp3Tables.count1Codes[quad], Mp3Tables.count1Lengths[quad]) else writer.write(15 - quad, 4)
            for (k in i until i + 4) {
                if (values[k] != 0) writer.write(if (lines[k] < 0) 1 else 0, 1)
            }
            i += 4
        }
    }

    private fun layout(values: IntArray, granule: Mp3Granule, edges: IntArray): Int {
        var zero = 576
        while (zero > 1 && values[zero - 1] == 0 && values[zero - 2] == 0) zero -= 2
        var big = zero
        while (big > 3 && values[big - 1] <= 1 && values[big - 2] <= 1 && values[big - 3] <= 1 && values[big - 4] <= 1) big -= 4
        granule.bigValues = big / 2
        granule.count1 = (zero - big) / 4
        bands = 0
        while (edges[bands] < big) bands++
        return big
    }

    // with window switching there are only two regions and the first is fixed: 36 lines for short blocks,
    // eight long bands for start and stop blocks (ISO/IEC 11172-3 2.4.2.7)
    private fun switchedRegion(granule: Mp3Granule, edges: IntArray): Int = if (granule.blockType == SHORT_BLOCK) 36 else edges[8]

    private fun switchedBits(values: IntArray, granule: Mp3Granule, edges: IntArray, big: Int): Int {
        val r1 = min(switchedRegion(granule, edges), big)
        var bits = rangeBits(values, 0, r1)
        val t0 = chosen
        bits += rangeBits(values, r1, big)
        setRegions(granule, 1, 1, t0, chosen, 0)
        return bits
    }

    private fun setRegions(granule: Mp3Granule, first: Int, second: Int, t0: Int, t1: Int, t2: Int) {
        granule.region0Count = first - 1
        granule.region1Count = max(0, second - first - 1)
        granule.tableSelect[0] = t0
        granule.tableSelect[1] = t1
        granule.tableSelect[2] = t2
    }

    private fun count1Bits(values: IntArray, from: Int, to: Int, granule: Mp3Granule): Int {
        var a = 0
        var signs = 0
        var i = from
        while (i < to) {
            a += Mp3Tables.count1Lengths[values[i] * 8 + values[i + 1] * 4 + values[i + 2] * 2 + values[i + 3]]
            signs += values[i] + values[i + 1] + values[i + 2] + values[i + 3]
            i += 4
        }
        val b = to - from
        granule.count1Table = if (b < a) 1 else 0
        return min(a, b) + signs
    }

    private fun rangeBits(values: IntArray, start: Int, end: Int): Int {
        var max = 0
        for (i in start until end) if (values[i] > max) max = values[i]
        if (max == 0) {
            chosen = 0
            return 0
        }
        val group = groupOf(max)
        val packed = PACKED[group]
        var sum = 0L
        var escaped = 0
        var i = start
        if (group == ESCAPE_GROUP) {
            while (i < end) {
                var x = values[i]
                var y = values[i + 1]
                if (x >= 15) {
                    x = 15
                    escaped++
                }
                if (y >= 15) {
                    y = 15
                    escaped++
                }
                sum += packed[x * 16 + y]
                i += 2
            }
        } else {
            val size = Mp3Tables.tableSize[GROUP_TABLES[group][0]]
            while (i < end) {
                sum += packed[values[i] * size + values[i + 1]]
                i += 2
            }
        }
        return pick(group, sum, escaped, max)
    }

    private fun prepareBands(values: IntArray, edges: IntArray, big: Int) {
        var peak = 0
        for (k in 0 until bands) {
            var max = 0
            for (i in edges[k] until min(edges[k + 1], big)) if (values[i] > max) max = values[i]
            bandMax[k] = max
            if (max > peak) peak = max
        }
        val groups = groupOf(max(peak, 1))
        for (group in 0..groups) prefix[group][0] = 0L
        escapes[0] = 0
        for (k in 0 until bands) {
            val start = edges[k]
            val end = min(edges[k + 1], big)
            for (group in 0..groups) {
                var sum = 0L
                if (bandMax[k] <= GROUP_LIMIT[group]) {
                    val packed = PACKED[group]
                    var i = start
                    if (group == ESCAPE_GROUP) {
                        var escaped = 0
                        while (i < end) {
                            val x = values[i]
                            val y = values[i + 1]
                            if (x >= 15) escaped++
                            if (y >= 15) escaped++
                            sum += packed[min(x, 15) * 16 + min(y, 15)]
                            i += 2
                        }
                        escapes[k + 1] = escapes[k] + escaped
                    } else {
                        val size = Mp3Tables.tableSize[GROUP_TABLES[group][0]]
                        while (i < end) {
                            sum += packed[values[i] * size + values[i + 1]]
                            i += 2
                        }
                    }
                }
                prefix[group][k + 1] = prefix[group][k] + sum
            }
        }
    }

    private fun bandBits(from: Int, to: Int): Int {
        var max = 0
        for (k in from until to) if (bandMax[k] > max) max = bandMax[k]
        if (max == 0) {
            chosen = 0
            return 0
        }
        val group = groupOf(max)
        val escaped = if (group == ESCAPE_GROUP) escapes[to] - escapes[from] else 0
        return pick(group, prefix[group][to] - prefix[group][from], escaped, max)
    }

    private fun pick(group: Int, sum: Long, escaped: Int, max: Int): Int {
        val tables = GROUP_TABLES[group]
        var best = Int.MAX_VALUE
        for (field in tables.indices) {
            var table = tables[field]
            var bits = ((sum ushr (16 * field)) and 0xFFFF).toInt()
            if (group == ESCAPE_GROUP) {
                table = escapeTable(table, max)
                bits += escaped * Mp3Tables.linbits[table]
            }
            if (bits < best) {
                best = bits
                chosen = table
            }
        }
        return best
    }

    private fun writePairs(writer: Mp3BitWriter, values: IntArray, lines: DoubleArray, start: Int, end: Int, table: Int) {
        if (table == 0) return
        val size = Mp3Tables.tableSize[table]
        val linbits = Mp3Tables.linbits[table]
        val codes = Mp3Tables.codes[table]
        val lengths = Mp3Tables.lengths[table]
        var i = start
        while (i < end) {
            val x = values[i]
            val y = values[i + 1]
            val index = min(x, 15) * size + min(y, 15)
            writer.write(codes[index], lengths[index])
            if (linbits > 0 && x >= 15) writer.write(x - 15, linbits)
            if (x != 0) writer.write(if (lines[i] < 0) 1 else 0, 1)
            if (linbits > 0 && y >= 15) writer.write(y - 15, linbits)
            if (y != 0) writer.write(if (lines[i + 1] < 0) 1 else 0, 1)
            i += 2
        }
    }
}
