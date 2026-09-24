package com.vasmarfas.card.core

object Mp3Stream {
    class Frame(
        val offset: Int,
        val size: Int,
        val mpeg1: Boolean,
        val kbps: Int,
        val sampleRate: Int,
        val padding: Boolean,
        val mode: Int,
        val modeExtension: Int,
    ) {
        val sideInfoSize: Int get() = if (mpeg1) (if (mode == 3) 17 else 32) else (if (mode == 3) 9 else 17)
    }

    private val mpeg1Kbps = intArrayOf(0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320)
    private val mpeg2Kbps = intArrayOf(0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160)

    fun frames(data: ByteArray): List<Frame> {
        val frames = mutableListOf<Frame>()
        var p = 0
        while (p + 4 <= data.size) {
            val b1 = data[p + 1].toInt() and 0xFF
            val b2 = data[p + 2].toInt() and 0xFF
            val b3 = data[p + 3].toInt() and 0xFF
            check(data[p].toInt() and 0xFF == 0xFF && b1 and 0xE0 == 0xE0) { "no frame sync at $p" }
            val version = (b1 ushr 3) and 3
            check(version == 3 || version == 2) { "not MPEG-1 or MPEG-2 at $p" }
            check((b1 ushr 1) and 3 == 1) { "not Layer III at $p" }
            check(b1 and 1 == 1) { "CRC protection at $p" }
            val mpeg1 = version == 3
            val kbps = (if (mpeg1) mpeg1Kbps else mpeg2Kbps)[b2 ushr 4]
            val rateIndex = (b2 ushr 2) and 3
            check(rateIndex < 3 && kbps > 0) { "reserved header fields at $p" }
            val sampleRate = (if (mpeg1) intArrayOf(44100, 48000, 32000) else intArrayOf(22050, 24000, 16000))[rateIndex]
            val padding = b2 and 2 != 0
            val size = (if (mpeg1) 144_000 else 72_000) * kbps / sampleRate + if (padding) 1 else 0
            frames += Frame(p, size, mpeg1, kbps, sampleRate, padding, b3 ushr 6, (b3 ushr 4) and 3)
            p += size
        }
        check(p == data.size) { "stream ends inside a frame" }
        return frames
    }

    fun blockTypes(data: ByteArray, frame: Frame): List<Int> {
        var bit = (frame.offset + 4) * 8
        fun read(count: Int): Int {
            var value = 0
            repeat(count) {
                value = (value shl 1) or ((data[bit ushr 3].toInt() ushr (7 - (bit and 7))) and 1)
                bit++
            }
            return value
        }
        val channels = if (frame.mode == 3) 1 else 2
        val granules = if (frame.mpeg1) 2 else 1
        if (frame.mpeg1) read(9 + (if (channels == 1) 5 else 3) + 4 * channels) else read(8 + channels)
        val types = mutableListOf<Int>()
        repeat(granules * channels) {
            read(12 + 9 + 8 + if (frame.mpeg1) 4 else 9)
            if (read(1) == 1) {
                types += read(2)
                read(1 + 10 + 9)
            } else {
                types += 0
                read(15 + 4 + 3)
            }
            read(if (frame.mpeg1) 3 else 2)
        }
        return types
    }

    fun u32(data: ByteArray, at: Int): Long =
        ((data[at].toLong() and 0xFF) shl 24) or ((data[at + 1].toLong() and 0xFF) shl 16) or
            ((data[at + 2].toLong() and 0xFF) shl 8) or (data[at + 3].toLong() and 0xFF)

    fun crc16(data: ByteArray, from: Int, to: Int): Int {
        var crc = 0
        for (i in from until to) {
            crc = crc xor (data[i].toInt() and 0xFF)
            repeat(8) { crc = if (crc and 1 != 0) (crc ushr 1) xor 0xA001 else crc ushr 1 }
        }
        return crc
    }
}
