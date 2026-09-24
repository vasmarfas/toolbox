package com.vasmarfas.card.core

import kotlin.math.roundToInt

internal class Resampler(private val from: Int, private val to: Int, private val channels: Int) {
    private var position = 0.0
    private val last = ShortArray(channels)
    private var hasLast = false

    fun process(input: ShortArray, count: Int): ShortArray {
        val frames = count / channels
        if (from == to || frames == 0) return input.copyOf(count)
        val offset = if (hasLast) 1 else 0
        val length = frames + offset
        val step = from.toDouble() / to
        val out = ShortArray((((length - position) / step).toInt() + 2) * channels)
        var written = 0
        while (position < length - 1) {
            val i = position.toInt()
            val frac = position - i
            for (c in 0 until channels) {
                val a = if (i < offset) last[c].toInt() else input[(i - offset) * channels + c].toInt()
                val b = input[(i + 1 - offset) * channels + c].toInt()
                out[written++] = (a + (b - a) * frac).roundToInt().toShort()
            }
            position += step
        }
        position -= length - 1
        for (c in 0 until channels) last[c] = input[(frames - 1) * channels + c]
        hasLast = true
        return out.copyOf(written)
    }
}
