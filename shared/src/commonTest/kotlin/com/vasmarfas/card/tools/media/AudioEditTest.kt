package com.vasmarfas.card.tools.media

import com.vasmarfas.card.core.PcmAudio
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AudioEditTest {
    // sample value equals the frame index, so every edit is readable in the output
    private fun project(frames: Int = 1000): Pair<AudioProject, List<AudioSegment>> {
        val project = AudioProject(sampleRate = 1000, channels = 1)
        val index = project.add(PcmAudio(1000, 1, ShortArray(frames) { it.toShort() }))
        return project to listOf(AudioSegment(index, 0, frames))
    }

    private fun renderAll(timeline: List<AudioSegment>, project: AudioProject): ShortArray {
        val out = ShortArray(AudioEdit.length(timeline) * project.channels)
        AudioEdit.render(timeline, project, 0, out)
        return out
    }

    @Test
    fun deleteAndKeepCutTheTimeline() {
        val (project, timeline) = project(100)
        val deleted = AudioEdit.delete(timeline, 10, 90)
        assertEquals(20, AudioEdit.length(deleted))
        assertContentEquals((0 until 10).map { it.toShort() } + (90 until 100).map { it.toShort() }, renderAll(deleted, project).toList())
        val kept = AudioEdit.keep(timeline, 40, 45)
        assertContentEquals(shortArrayOf(40, 41, 42, 43, 44), renderAll(kept, project))
    }

    @Test
    fun silenceKeepsTheLength() {
        val (project, timeline) = project(20)
        val silent = AudioEdit.silence(timeline, 5, 10)
        val out = renderAll(silent, project)
        assertEquals(20, out.size)
        assertTrue((5 until 10).all { out[it] == 0.toShort() })
        assertEquals(10, out[10].toInt())
    }

    @Test
    fun copyAndPaste() {
        val (project, timeline) = project(10)
        val clip = AudioEdit.slice(timeline, 2, 4)
        val pasted = AudioEdit.insert(timeline, 8, clip)
        assertContentEquals(shortArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 2, 3, 8, 9), renderAll(pasted, project))
        assertContentEquals(shortArrayOf(2, 3, 0, 1), renderAll(AudioEdit.insert(AudioEdit.slice(timeline, 0, 2), 0, clip), project))
    }

    @Test
    fun reverseFlipsOnlyTheRange() {
        val (project, timeline) = project(10)
        val reversed = AudioEdit.reverse(timeline, 3, 7)
        assertContentEquals(shortArrayOf(0, 1, 2, 6, 5, 4, 3, 7, 8, 9), renderAll(reversed, project))
        val split = AudioEdit.splitAt(reversed, 5)
        assertContentEquals(renderAll(reversed, project), renderAll(split, project))
        assertContentEquals(shortArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), renderAll(AudioEdit.reverse(reversed, 3, 7), project))
    }

    @Test
    fun gainAndFadesScaleSamples() {
        val project = AudioProject(1000, 1)
        val index = project.add(PcmAudio(1000, 1, ShortArray(101) { 1000 }))
        val timeline = listOf(AudioSegment(index, 0, 101))
        val louder = renderAll(AudioEdit.gain(timeline, 0, 101, 2f), project)
        assertEquals(2000, louder[50].toInt())
        val faded = renderAll(AudioEdit.fade(timeline, 0, 101, rising = true), project)
        assertEquals(0, faded[0].toInt())
        assertTrue(faded[50] in 480..520, "middle of a rising fade is about half, got ${faded[50]}")
        assertTrue(faded[100] > 980)
        val out = renderAll(AudioEdit.fade(timeline, 51, 101, rising = false), project)
        assertEquals(1000, out[40].toInt())
        assertTrue(out[100] < 30)
    }

    @Test
    fun normalizeBringsThePeakNearFullScale() {
        val project = AudioProject(1000, 1)
        val index = project.add(PcmAudio(1000, 1, ShortArray(600) { if (it == 300) 8000 else 100 }))
        val normalized = AudioEdit.normalize(listOf(AudioSegment(index, 0, 600)), project)
        val peak = renderAll(normalized, project).maxOf { it.toInt() }
        assertTrue(peak in 32000..32767, "peak $peak")
    }

    @Test
    fun streamingSourceMatchesTheFullRender() {
        val (project, timeline) = project(1000)
        val edited = AudioEdit.fade(AudioEdit.reverse(AudioEdit.delete(timeline, 100, 300), 50, 400), 0, 200, rising = true)
        val source = AudioEdit.source(edited, project)
        val streamed = ArrayList<Short>()
        val buffer = ShortArray(97)
        while (true) {
            val n = source.read(buffer)
            if (n == 0) break
            for (i in 0 until n) streamed += buffer[i]
        }
        assertContentEquals(renderAll(edited, project).toList(), streamed)
    }

    @Test
    fun sourcesAreBroughtToTheProjectFormat() {
        val project = AudioProject(sampleRate = 2000, channels = 2)
        val index = project.add(PcmAudio(1000, 1, ShortArray(100) { 500 }))
        assertEquals(200, project.frames(index))
        assertEquals(400, project.sources[index].size)
        assertTrue(project.sources[index].all { it == 500.toShort() })
    }

    @Test
    fun waveformFollowsTheEdits() {
        val project = AudioProject(1000, 1)
        val index = project.add(PcmAudio(1000, 1, ShortArray(4096) { if (it < 2048) 16384 else 0 }))
        val timeline = listOf(AudioSegment(index, 0, 4096))
        val columns = AudioEdit.waveform(timeline, project, 0, 4096, 16)
        assertTrue(columns.take(8).all { it in 0.45f..0.55f })
        assertTrue(columns.drop(8).all { it == 0f })
        val quiet = AudioEdit.waveform(AudioEdit.gain(timeline, 0, 4096, 0.5f), project, 0, 4096, 16)
        assertTrue(quiet[0] in 0.2f..0.3f)
    }
}
