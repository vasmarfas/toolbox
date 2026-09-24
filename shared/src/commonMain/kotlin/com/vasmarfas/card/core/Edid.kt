package com.vasmarfas.card.core

import kotlin.math.hypot

class DisplayPanel(val name: String, val widthPx: Int, val heightPx: Int, val widthMm: Int, val heightMm: Int) {
    val diagonalInches: Double get() = hypot(widthMm.toDouble(), heightMm.toDouble()) / 25.4
}

private val EDID_HEADER = byteArrayOf(0, -1, -1, -1, -1, -1, -1, 0)

// first detailed timing descriptor: native mode and size in mm, descriptor 0xFC: model name, bytes 8-9:
// maker. Projectors and some TVs put zeros or just the aspect ratio into the size, those are dropped
fun parseEdid(edid: ByteArray): DisplayPanel? {
    if (edid.size < 128 || !edid.copyOfRange(0, 8).contentEquals(EDID_HEADER)) return null
    fun u(i: Int) = edid[i].toInt() and 0xFF
    if (u(54) == 0 && u(55) == 0) return null
    val widthPx = u(56) or (u(58) and 0xF0 shl 4)
    val heightPx = u(59) or (u(61) and 0xF0 shl 4)
    val widthMm = u(66) or (u(68) and 0xF0 shl 4)
    val heightMm = u(67) or (u(68) and 0x0F shl 8)
    if (widthPx == 0 || heightPx == 0 || widthMm == 0 || heightMm == 0) return null
    if (widthPx * 25.4 / widthMm !in 40.0..800.0) return null
    val maker = u(8) shl 8 or u(9)
    val code = CharArray(3) { i -> 'A' + ((maker shr (10 - 5 * i)) and 0x1F) - 1 }.concatToString()
    val name = (54..108 step 18).firstNotNullOfOrNull { at ->
        if (u(at) == 0 && u(at + 1) == 0 && u(at + 3) == 0xFC) {
            (at + 5 until at + 18).map { u(it).toChar() }.takeWhile { it != '\n' }.joinToString("").trim().ifEmpty { null }
        } else {
            null
        }
    } ?: code
    return DisplayPanel(name, widthPx, heightPx, widthMm, heightMm)
}
