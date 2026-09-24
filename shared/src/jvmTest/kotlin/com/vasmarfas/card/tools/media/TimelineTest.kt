package com.vasmarfas.card.tools.media

import com.vasmarfas.card.core.ClipKind
import com.vasmarfas.card.core.MediaClip
import com.vasmarfas.card.core.TrackKind
import io.github.vinceglb.filekit.PlatformFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimelineTest {
    private val video = PlatformFile("/media/clip.mp4")
    private val song = PlatformFile("/media/song.mp3")

    private fun Timeline.clips(kind: TrackKind = TrackKind.VIDEO) = tracks.first { it.kind == kind }.clips.map { it.clip }

    private fun Timeline.withVideo(vararg lengths: Long): Timeline = apply {
        add(TrackKind.VIDEO, lengths.map { MediaClip(video, ClipKind.VIDEO, 0, it) to it }, 0)
    }

    @Test
    fun addedClipsFollowEachOtherFromThePlayhead() {
        val timeline = Timeline().withVideo(2000, 3000)
        assertEquals(listOf(0L to 2000L, 2000L to 5000L), timeline.clips().map { it.atMs to it.endAtMs })
        timeline.add(TrackKind.VIDEO, listOf(MediaClip(video, ClipKind.IMAGE, 0, 1000) to Long.MAX_VALUE), 1000)
        assertEquals(5000L, timeline.clips().last().atMs, "a clip must not land on top of another one")
        assertEquals(timeline.clips().last().atMs, timeline.selected?.clip?.atMs)
    }

    @Test
    fun aDroppedClipMovesToTheNearestGap() {
        val timeline = Timeline().withVideo(2000, 2000, 2000)
        val last = timeline.tracks.first().clips.last()
        timeline.move(last.id, timeline.tracks.first().id, 9000)
        assertEquals(9000L, timeline.clips().last().atMs)
        timeline.move(last.id, timeline.tracks.first().id, 1500)
        assertEquals(listOf(0L, 2000L, 4000L), timeline.clips().map { it.atMs }, "the gap after the clips is the nearest free place")
    }

    @Test
    fun trimmingStopsAtTheSourceAndTheNeighbours() {
        val timeline = Timeline().withVideo(4000, 4000)
        val first = timeline.tracks.first().clips.first()
        timeline.trimEnd(first.id, 10_000)
        assertEquals(4000L, timeline.clips().first().endAtMs, "a clip cannot run into the next one")
        timeline.trimStart(first.id, 1500)
        assertEquals(1500L, timeline.clips().first().atMs)
        assertEquals(1500L, timeline.clips().first().startMs)
        timeline.trimStart(first.id, -500)
        assertEquals(0L, timeline.clips().first().atMs, "the source has nothing before its first frame")
        assertEquals(0L, timeline.clips().first().startMs)
    }

    @Test
    fun splittingKeepsTheSourceContinuous() {
        val timeline = Timeline().withVideo(5000)
        val clip = timeline.tracks.first().clips.single()
        assertTrue(timeline.split(clip.id, 2000))
        val (left, right) = timeline.clips()
        assertEquals(2000L, left.endMs)
        assertEquals(2000L, right.startMs)
        assertEquals(2000L, right.atMs)
        assertFalse(timeline.split(timeline.selectedId!!, 2100), "a piece shorter than the minimum is refused")
    }

    @Test
    fun musicRepeatsUntilThePictureEnds() {
        val timeline = Timeline().withVideo(10_000)
        timeline.add(TrackKind.AUDIO, listOf(MediaClip(song, ClipKind.AUDIO, 0, 3000, fadeInMs = 500, fadeOutMs = 800) to 3000L), 0)
        timeline.loopToEnd(timeline.selectedId!!)
        val pieces = timeline.clips(TrackKind.AUDIO)
        assertEquals(listOf(0L, 3000L, 6000L, 9000L), pieces.map { it.atMs })
        assertEquals(10_000L, pieces.last().endAtMs)
        assertEquals(listOf(500L, 0L, 0L, 0L), pieces.map { it.fadeInMs })
        assertEquals(listOf(0L, 0L, 0L, 800L), pieces.map { it.fadeOutMs })
    }

    @Test
    fun undoAndRedoWalkThroughEveryChange() {
        val timeline = Timeline().withVideo(2000)
        val clip = timeline.tracks.first().clips.single()
        timeline.update(clip.id) { it.copy(volume = 0.5f) }
        timeline.delete(clip.id)
        assertTrue(timeline.isEmpty)
        timeline.undo()
        assertEquals(0.5f, timeline.clips().single().volume)
        timeline.undo()
        assertEquals(1f, timeline.clips().single().volume)
        timeline.redo()
        timeline.redo()
        assertTrue(timeline.isEmpty)
        assertFalse(timeline.canRedo)
    }

    @Test
    fun overlayTracksStackAboveTheMainOne() {
        val timeline = Timeline().withVideo(2000)
        timeline.addTrack(TrackKind.VIDEO)
        val kinds = timeline.tracks.map { it.kind }
        assertEquals(listOf(TrackKind.VIDEO, TrackKind.VIDEO, TrackKind.AUDIO), kinds)
        timeline.removeTrack(timeline.tracks.first { it.kind == TrackKind.AUDIO }.id)
        assertEquals(1, timeline.tracks.count { it.kind == TrackKind.AUDIO }, "the last audio track stays")
    }
}
