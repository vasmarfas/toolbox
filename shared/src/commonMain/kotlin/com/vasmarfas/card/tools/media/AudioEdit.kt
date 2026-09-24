package com.vasmarfas.card.tools.media

import com.vasmarfas.card.core.PcmAudio
import com.vasmarfas.card.core.PcmSource
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// source -1 is silence
data class AudioSegment(
    val source: Int,
    val from: Int,
    val to: Int,
    val gainStart: Float = 1f,
    val gainEnd: Float = 1f,
    val reversed: Boolean = false,
) {
    val length: Int get() = to - from

    fun gainAt(offset: Int): Float = if (length <= 1) gainStart else gainStart + (gainEnd - gainStart) * offset / (length - 1)
}

class AudioProject(val sampleRate: Int, val channels: Int) {
    val sources = mutableListOf<ShortArray>()
    private val peaks = mutableListOf<ShortArray>()

    fun add(pcm: PcmAudio): Int {
        val samples = remix(resample(pcm.samples, pcm.channels, pcm.sampleRate, sampleRate), pcm.channels, channels)
        sources += samples
        val frames = samples.size / channels
        val blocks = (frames + BLOCK - 1) / BLOCK
        val map = ShortArray(blocks)
        for (b in 0 until blocks) {
            var peak = 0
            val end = min(frames, (b + 1) * BLOCK) * channels
            for (i in b * BLOCK * channels until end) peak = max(peak, abs(samples[i].toInt()))
            map[b] = min(peak, Short.MAX_VALUE.toInt()).toShort()
        }
        peaks += map
        return sources.lastIndex
    }

    fun frames(source: Int): Int = sources[source].size / channels

    internal fun peak(source: Int, from: Int, to: Int): Int {
        if (to <= from) return 0
        val map = peaks[source]
        var peak = 0
        for (b in from / BLOCK..(to - 1) / BLOCK) peak = max(peak, map[b].toInt())
        return peak
    }

    companion object {
        const val BLOCK = 256
    }
}

object AudioEdit {
    fun length(timeline: List<AudioSegment>): Int = timeline.sumOf { it.length }

    fun splitAt(timeline: List<AudioSegment>, frame: Int): List<AudioSegment> {
        val out = ArrayList<AudioSegment>(timeline.size + 1)
        var position = 0
        for (segment in timeline) {
            val end = position + segment.length
            if (frame in (position + 1) until end) {
                val cut = frame - position
                val gain = segment.gainAt(cut)
                if (segment.reversed) {
                    out += segment.copy(from = segment.to - cut, gainEnd = gain)
                    out += segment.copy(to = segment.to - cut, gainStart = gain)
                } else {
                    out += segment.copy(to = segment.from + cut, gainEnd = gain)
                    out += segment.copy(from = segment.from + cut, gainStart = gain)
                }
            } else {
                out += segment
            }
            position = end
        }
        return out
    }

    private inline fun mapRange(
        timeline: List<AudioSegment>,
        from: Int,
        to: Int,
        transform: (AudioSegment, start: Int) -> List<AudioSegment>,
    ): List<AudioSegment> {
        val cut = splitAt(splitAt(timeline, from), to)
        val out = ArrayList<AudioSegment>(cut.size)
        var position = 0
        for (segment in cut) {
            val inside = position >= from && position + segment.length <= to && segment.length > 0
            if (inside) out += transform(segment, position) else out += segment
            position += segment.length
        }
        return out
    }

    fun delete(timeline: List<AudioSegment>, from: Int, to: Int): List<AudioSegment> = mapRange(timeline, from, to) { _, _ -> emptyList() }

    fun keep(timeline: List<AudioSegment>, from: Int, to: Int): List<AudioSegment> = slice(timeline, from, to)

    fun slice(timeline: List<AudioSegment>, from: Int, to: Int): List<AudioSegment> {
        val cut = splitAt(splitAt(timeline, from), to)
        val out = ArrayList<AudioSegment>()
        var position = 0
        for (segment in cut) {
            if (position >= from && position + segment.length <= to && segment.length > 0) out += segment
            position += segment.length
        }
        return out
    }

    fun silence(timeline: List<AudioSegment>, from: Int, to: Int): List<AudioSegment> =
        mapRange(timeline, from, to) { segment, _ -> listOf(AudioSegment(-1, 0, segment.length)) }

    fun insert(timeline: List<AudioSegment>, at: Int, clip: List<AudioSegment>): List<AudioSegment> {
        val cut = splitAt(timeline, at)
        val out = ArrayList<AudioSegment>(cut.size + clip.size)
        var position = 0
        var inserted = false
        for (segment in cut) {
            if (!inserted && position >= at) {
                out += clip
                inserted = true
            }
            out += segment
            position += segment.length
        }
        if (!inserted) out += clip
        return out
    }

    fun gain(timeline: List<AudioSegment>, from: Int, to: Int, factor: Float): List<AudioSegment> =
        mapRange(timeline, from, to) { segment, _ -> listOf(segment.copy(gainStart = segment.gainStart * factor, gainEnd = segment.gainEnd * factor)) }

    fun fade(timeline: List<AudioSegment>, from: Int, to: Int, rising: Boolean): List<AudioSegment> {
        val span = (to - from).coerceAtLeast(1).toFloat()
        return mapRange(timeline, from, to) { segment, start ->
            val a = (start - from) / span
            val b = (start + segment.length - from) / span
            val first = if (rising) a else 1 - a
            val last = if (rising) b else 1 - b
            listOf(segment.copy(gainStart = segment.gainStart * first, gainEnd = segment.gainEnd * last))
        }
    }

    fun reverse(timeline: List<AudioSegment>, from: Int, to: Int): List<AudioSegment> {
        val inside = slice(timeline, from, to).reversed().map { it.copy(reversed = !it.reversed, gainStart = it.gainEnd, gainEnd = it.gainStart) }
        return insert(delete(timeline, from, to), from, inside)
    }

    fun peak(timeline: List<AudioSegment>, project: AudioProject): Int {
        var peak = 0
        for (segment in timeline) {
            if (segment.source < 0) continue
            val gain = max(abs(segment.gainStart), abs(segment.gainEnd))
            peak = max(peak, (project.peak(segment.source, segment.from, segment.to) * gain).roundToInt())
        }
        return peak
    }

    fun normalize(timeline: List<AudioSegment>, project: AudioProject): List<AudioSegment> {
        val peak = peak(timeline, project)
        if (peak == 0) return timeline
        val factor = 32767 * 0.98f / peak
        return timeline.map { it.copy(gainStart = it.gainStart * factor, gainEnd = it.gainEnd * factor) }
    }

    fun render(timeline: List<AudioSegment>, project: AudioProject, startFrame: Int, out: ShortArray): Int {
        val channels = project.channels
        val wanted = out.size / channels
        var written = 0
        var position = 0
        for (segment in timeline) {
            val end = position + segment.length
            if (end <= startFrame + written) {
                position = end
                continue
            }
            if (written >= wanted) break
            val offset = startFrame + written - position
            val count = min(segment.length - offset, wanted - written)
            val samples = if (segment.source >= 0) project.sources[segment.source] else null
            for (k in 0 until count) {
                val local = offset + k
                val o = (written + k) * channels
                if (samples == null) {
                    for (c in 0 until channels) out[o + c] = 0
                    continue
                }
                val frame = if (segment.reversed) segment.to - 1 - local else segment.from + local
                val gain = segment.gainAt(local)
                for (c in 0 until channels) {
                    val v = samples[frame * channels + c] * gain
                    out[o + c] = v.roundToInt().coerceIn(-32768, 32767).toShort()
                }
            }
            written += count
            position = end
        }
        return written * channels
    }

    fun source(timeline: List<AudioSegment>, project: AudioProject, from: Int = 0, to: Int = length(timeline)): PcmSource {
        var position = from
        return PcmSource(project.sampleRate, project.channels, (to - from).toLong()) { buffer ->
            val frames = min(buffer.size / project.channels, to - position)
            if (frames <= 0) {
                0
            } else {
                val chunk = if (frames * project.channels == buffer.size) buffer else ShortArray(frames * project.channels)
                val n = render(timeline, project, position, chunk)
                if (chunk !== buffer) chunk.copyInto(buffer, 0, 0, n)
                position += n / project.channels
                n
            }
        }
    }

    fun waveform(timeline: List<AudioSegment>, project: AudioProject, from: Int, to: Int, columns: Int): FloatArray {
        val out = FloatArray(columns)
        if (to <= from || columns <= 0) return out
        val step = (to - from).toDouble() / columns
        var position = 0
        for (segment in timeline) {
            val start = position
            val end = position + segment.length
            position = end
            if (end <= from || start >= to || segment.source < 0) continue
            val first = ((max(start, from) - from) / step).toInt()
            val last = min(columns - 1, ((min(end, to) - 1 - from) / step).toInt())
            for (column in first..last) {
                val a = max(start, from + (column * step).toInt())
                val b = min(end, from + ((column + 1) * step).toInt().coerceAtLeast(a + 1))
                if (b <= a) continue
                val localA = a - start
                val localB = b - start
                val sourceA = if (segment.reversed) segment.to - localB else segment.from + localA
                val sourceB = if (segment.reversed) segment.to - localA else segment.from + localB
                val gain = max(abs(segment.gainAt(localA)), abs(segment.gainAt(localB - 1)))
                val level = project.peak(segment.source, sourceA, sourceB) * gain / 32768f
                if (level > out[column]) out[column] = level.coerceAtMost(1f)
            }
        }
        return out
    }
}

internal fun resample(samples: ShortArray, channels: Int, from: Int, to: Int): ShortArray {
    if (from == to || samples.isEmpty()) return samples
    val frames = samples.size / channels
    val outFrames = (frames.toLong() * to / from).toInt()
    val out = ShortArray(outFrames * channels)
    val step = from.toDouble() / to
    for (f in 0 until outFrames) {
        val position = f * step
        val i = position.toInt().coerceAtMost(frames - 1)
        val j = min(i + 1, frames - 1)
        val frac = position - i
        for (c in 0 until channels) {
            val a = samples[i * channels + c]
            val b = samples[j * channels + c]
            out[f * channels + c] = (a + (b - a) * frac).roundToInt().toShort()
        }
    }
    return out
}

internal fun remix(samples: ShortArray, from: Int, to: Int): ShortArray {
    if (from == to) return samples
    val frames = samples.size / from
    val out = ShortArray(frames * to)
    for (f in 0 until frames) {
        if (to == 1) {
            var sum = 0
            for (c in 0 until from) sum += samples[f * from + c]
            out[f] = (sum / from).toShort()
        } else {
            for (c in 0 until to) out[f * to + c] = samples[f * from + min(c, from - 1)]
        }
    }
    return out
}
