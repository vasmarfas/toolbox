package com.vasmarfas.card.tools.media

internal class ByteSink(capacity: Int = 256) {
    private var buf = ByteArray(maxOf(capacity, 16))
    var size = 0
        private set

    fun byte(v: Int) {
        if (size == buf.size) grow(1)
        buf[size++] = v.toByte()
    }

    fun bytes(src: ByteArray, from: Int = 0, to: Int = src.size) {
        val n = to - from
        if (size + n > buf.size) grow(n)
        src.copyInto(buf, size, from, to)
        size += n
    }

    fun shortLE(v: Int) {
        byte(v)
        byte(v ushr 8)
    }

    fun intLE(v: Int) {
        shortLE(v)
        shortLE(v ushr 16)
    }

    fun shortBE(v: Int) {
        byte(v ushr 8)
        byte(v)
    }

    fun intBE(v: Int) {
        shortBE(v ushr 16)
        shortBE(v)
    }

    fun ascii(text: String) {
        for (c in text) byte(c.code)
    }

    operator fun get(index: Int): Int = buf[index].toInt() and 0xFF

    operator fun set(index: Int, v: Int) {
        buf[index] = v.toByte()
    }

    fun toByteArray(): ByteArray = buf.copyOf(size)

    private fun grow(extra: Int) {
        buf = buf.copyOf(maxOf(buf.size * 2, size + extra))
    }
}

internal fun ByteArray.u8(i: Int): Int = this[i].toInt() and 0xFF

internal fun ByteArray.u16be(i: Int): Int = (u8(i) shl 8) or u8(i + 1)

internal fun ByteArray.u16le(i: Int): Int = u8(i) or (u8(i + 1) shl 8)

internal fun ByteArray.u32be(i: Int): Long = (u16be(i).toLong() shl 16) or u16be(i + 2).toLong()

internal fun ByteArray.u32le(i: Int): Long = u16le(i).toLong() or (u16le(i + 2).toLong() shl 16)

internal fun ByteArray.matches(offset: Int, ascii: String): Boolean {
    if (offset < 0 || offset + ascii.length > size) return false
    for (i in ascii.indices) if (u8(offset + i) != ascii[i].code) return false
    return true
}
