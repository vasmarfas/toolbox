package com.vasmarfas.card.core

expect class PcmPlayer() {
    fun start(sampleRate: Int, channels: Int, source: (ShortArray) -> Int)

    fun stop()

    val position: Long

    val playing: Boolean
}
