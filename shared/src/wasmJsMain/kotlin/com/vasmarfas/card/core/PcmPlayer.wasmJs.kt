package com.vasmarfas.card.core

import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.khronos.webgl.Float32Array
import org.khronos.webgl.set

private fun jsContext(rate: Int): JsAny = js("{ const c = new (window.AudioContext || window.webkitAudioContext)({ sampleRate: rate }); c.resume(); return c; }")

private fun jsNow(context: JsAny): Double = js("context.currentTime")

private fun jsSchedule(context: JsAny, data: Float32Array, channels: Int, rate: Int, at: Double): Unit = js(
    """{
        const frames = data.length / channels;
        const buffer = context.createBuffer(channels, frames, rate);
        for (let c = 0; c < channels; c++) {
            const out = buffer.getChannelData(c);
            for (let i = 0; i < frames; i++) out[i] = data[i * channels + c];
        }
        const node = context.createBufferSource();
        node.buffer = buffer;
        node.connect(context.destination);
        node.start(at);
    }""",
)

private fun jsClose(context: JsAny): Unit = js("context.close()")

// Web Audio has no pull stream, so half-second buffers are queued about a second ahead of the clock
actual class PcmPlayer actual constructor() {
    private var job: Job? = null
    private var context: JsAny? = null
    private var startTime = 0.0
    private var rate = 48_000

    actual fun start(sampleRate: Int, channels: Int, source: (ShortArray) -> Int) {
        stop()
        val audio = jsContext(sampleRate)
        context = audio
        rate = sampleRate
        startTime = jsNow(audio) + 0.1
        job = MainScope().launch {
            val chunk = ShortArray(sampleRate / 2 * channels)
            var scheduled = 0L
            while (isActive) {
                val at = startTime + scheduled.toDouble() / sampleRate
                if (at - jsNow(audio) > 1.0) {
                    delay(100)
                    continue
                }
                val n = source(chunk)
                if (n <= 0) break
                val floats = Float32Array(n)
                for (i in 0 until n) floats[i] = chunk[i] / 32768f
                jsSchedule(audio, floats, channels, sampleRate, at)
                scheduled += n / channels
            }
            while (isActive && position < scheduled) delay(50)
            if (isActive) stop()
        }
    }

    actual fun stop() {
        job?.cancel()
        job = null
        context?.let { jsClose(it) }
        context = null
    }

    actual val position: Long
        get() = context?.let { ((jsNow(it) - startTime) * rate).toLong().coerceAtLeast(0) } ?: 0

    actual val playing: Boolean get() = job != null
}
