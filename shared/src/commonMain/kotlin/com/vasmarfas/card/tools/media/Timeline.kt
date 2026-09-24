package com.vasmarfas.card.tools.media

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vasmarfas.card.core.ClipKind
import com.vasmarfas.card.core.MediaClip
import com.vasmarfas.card.core.MediaProject
import com.vasmarfas.card.core.MediaSpec
import com.vasmarfas.card.core.MediaTrack
import com.vasmarfas.card.core.TrackKind
import kotlin.math.abs

internal data class EditClip(val id: Int, val clip: MediaClip, val sourceMs: Long) {
    val trimmable: Boolean get() = clip.kind != ClipKind.IMAGE
}

internal data class EditTrack(val id: Int, val kind: TrackKind, val clips: List<EditClip>, val muted: Boolean = false, val hidden: Boolean = false)

@Stable
internal class Timeline {
    var tracks by mutableStateOf(listOf(EditTrack(1, TrackKind.VIDEO, emptyList()), EditTrack(2, TrackKind.AUDIO, emptyList())))
        private set

    var selectedId by mutableStateOf<Int?>(null)

    var activeTrackId by mutableStateOf<Int?>(null)

    private val undoStack = ArrayDeque<List<EditTrack>>()
    private val redoStack = ArrayDeque<List<EditTrack>>()
    private var undoDepth by mutableIntStateOf(0)
    private var redoDepth by mutableIntStateOf(0)
    private var nextId = 3

    val canUndo: Boolean get() = undoDepth > 0

    val canRedo: Boolean get() = redoDepth > 0

    val durationMs: Long get() = tracks.maxOfOrNull { t -> t.clips.maxOfOrNull { it.clip.endAtMs } ?: 0 } ?: 0

    val isEmpty: Boolean get() = tracks.all { it.clips.isEmpty() }

    val selected: EditClip? get() = selectedId?.let { id -> tracks.firstNotNullOfOrNull { t -> t.clips.firstOrNull { it.id == id } } }

    fun trackOf(clipId: Int): EditTrack? = tracks.firstOrNull { t -> t.clips.any { it.id == clipId } }

    fun project(spec: MediaSpec): MediaProject =
        MediaProject(tracks.map { t -> MediaTrack(t.kind, t.clips.map { it.clip }, t.muted, t.hidden) }, spec)

    private fun commit(change: List<EditTrack>) {
        if (change == tracks) return
        undoStack.addLast(tracks)
        if (undoStack.size > 100) undoStack.removeFirst()
        redoStack.clear()
        tracks = change
        syncDepth()
    }

    private fun syncDepth() {
        undoDepth = undoStack.size
        redoDepth = redoStack.size
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(tracks)
        tracks = previous
        syncDepth()
        if (selected == null) selectedId = null
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(tracks)
        tracks = next
        syncDepth()
        if (selected == null) selectedId = null
    }

    private fun List<EditTrack>.edit(trackId: Int, change: (EditTrack) -> EditTrack) = map { if (it.id == trackId) change(it) else it }

    private fun EditTrack.sorted() = copy(clips = clips.sortedBy { it.clip.atMs })

    fun freeStart(track: EditTrack, durationMs: Long, wantedMs: Long, ignoreId: Int? = null): Long {
        val others = track.clips.filter { it.id != ignoreId }.sortedBy { it.clip.atMs }
        var best = -1L
        var gapStart = 0L
        fun consider(from: Long, to: Long) {
            if (to - from < durationMs) return
            val at = wantedMs.coerceIn(from, to - durationMs)
            if (best < 0 || abs(at - wantedMs) < abs(best - wantedMs)) best = at
        }
        others.forEach { clip ->
            consider(gapStart, clip.clip.atMs)
            gapStart = maxOf(gapStart, clip.clip.endAtMs)
        }
        consider(gapStart, Long.MAX_VALUE / 4)
        return best.coerceAtLeast(0)
    }

    fun trackFor(kind: TrackKind): EditTrack =
        tracks.firstOrNull { it.id == activeTrackId && it.kind == kind }
            ?: selectedId?.let { trackOf(it) }?.takeIf { it.kind == kind }
            ?: tracks.first { it.kind == kind }

    fun select(clipId: Int?) {
        selectedId = clipId
        clipId?.let { trackOf(it) }?.let { activeTrackId = it.id }
    }

    fun add(kind: TrackKind, clips: List<Pair<MediaClip, Long>>, atMs: Long) {
        if (clips.isEmpty()) return
        val track = trackFor(kind)
        var cursor = atMs
        var current = track
        var lastId = 0
        clips.forEach { (clip, sourceMs) ->
            val start = freeStart(current, clip.durationMs, cursor)
            val placed = EditClip(nextId++, clip.copy(atMs = start), sourceMs)
            lastId = placed.id
            current = current.copy(clips = current.clips + placed).sorted()
            cursor = placed.clip.endAtMs
        }
        commit(tracks.edit(track.id) { current })
        select(lastId)
    }

    fun move(clipId: Int, toTrackId: Int, atMs: Long) {
        val from = trackOf(clipId) ?: return
        val target = tracks.firstOrNull { it.id == toTrackId }?.takeIf { it.kind == from.kind } ?: from
        val clip = from.clips.first { it.id == clipId }
        val start = freeStart(target, clip.clip.durationMs, atMs, ignoreId = clipId)
        val moved = clip.copy(clip = clip.clip.copy(atMs = start))
        commit(
            tracks.map { t ->
                when (t.id) {
                    target.id -> t.copy(clips = t.clips.filter { it.id != clipId } + moved).sorted()
                    from.id -> t.copy(clips = t.clips.filter { it.id != clipId })
                    else -> t
                }
            },
        )
    }

    fun trimStart(clipId: Int, atMs: Long) {
        val track = trackOf(clipId) ?: return
        val edit = track.clips.first { it.id == clipId }
        val clip = edit.clip
        val previousEnd = track.clips.filter { it.clip.endAtMs <= clip.atMs && it.id != clipId }.maxOfOrNull { it.clip.endAtMs } ?: 0
        val earliest = if (edit.trimmable) maxOf(previousEnd, clip.atMs - clip.startMs) else previousEnd
        val start = atMs.coerceIn(earliest, clip.endAtMs - MIN_CLIP_MS)
        val shift = start - clip.atMs
        val trimmed = if (edit.trimmable) clip.copy(atMs = start, startMs = clip.startMs + shift) else clip.copy(atMs = start, endMs = clip.endMs - shift)
        commit(tracks.edit(track.id) { t -> t.copy(clips = t.clips.map { if (it.id == clipId) it.copy(clip = trimmed) else it }) })
    }

    fun trimEnd(clipId: Int, endAtMs: Long) {
        val track = trackOf(clipId) ?: return
        val edit = track.clips.first { it.id == clipId }
        val clip = edit.clip
        val nextStart = track.clips.filter { it.clip.atMs >= clip.endAtMs && it.id != clipId }.minOfOrNull { it.clip.atMs } ?: Long.MAX_VALUE
        val latest = if (edit.trimmable) minOf(nextStart, clip.atMs + edit.sourceMs - clip.startMs) else nextStart
        val end = endAtMs.coerceIn(clip.atMs + MIN_CLIP_MS, latest)
        val trimmed = clip.copy(endMs = clip.startMs + (end - clip.atMs))
        commit(tracks.edit(track.id) { t -> t.copy(clips = t.clips.map { if (it.id == clipId) it.copy(clip = trimmed) else it }) })
    }

    fun split(clipId: Int, atMs: Long): Boolean {
        val track = trackOf(clipId) ?: return false
        val edit = track.clips.first { it.id == clipId }
        val clip = edit.clip
        if (atMs - clip.atMs < MIN_CLIP_MS || clip.endAtMs - atMs < MIN_CLIP_MS) return false
        val cut = clip.startMs + (atMs - clip.atMs)
        val left = edit.copy(clip = clip.copy(endMs = cut, fadeOutMs = 0))
        val right = EditClip(nextId++, clip.copy(atMs = atMs, startMs = cut, fadeInMs = 0), edit.sourceMs)
        commit(tracks.edit(track.id) { t -> t.copy(clips = t.clips.flatMap { if (it.id == clipId) listOf(left, right) else listOf(it) }) })
        selectedId = right.id
        return true
    }

    fun duplicate(clipId: Int) {
        val track = trackOf(clipId) ?: return
        val edit = track.clips.first { it.id == clipId }
        val start = freeStart(track, edit.clip.durationMs, edit.clip.endAtMs)
        val copy = EditClip(nextId++, edit.clip.copy(atMs = start), edit.sourceMs)
        commit(tracks.edit(track.id) { t -> t.copy(clips = t.clips + copy).sorted() })
        selectedId = copy.id
    }

    fun delete(clipId: Int) {
        val track = trackOf(clipId) ?: return
        commit(tracks.edit(track.id) { t -> t.copy(clips = t.clips.filter { it.id != clipId }) })
        if (selectedId == clipId) selectedId = null
    }

    fun update(clipId: Int, change: (MediaClip) -> MediaClip) {
        val track = trackOf(clipId) ?: return
        commit(
            tracks.edit(track.id) { t ->
                t.copy(
                    clips = t.clips.map { edit ->
                        if (edit.id != clipId) return@map edit
                        val changed = change(edit.clip)
                        val nextStart = t.clips.filter { it.id != clipId && it.clip.atMs >= edit.clip.endAtMs }.minOfOrNull { it.clip.atMs } ?: Long.MAX_VALUE
                        edit.copy(clip = if (changed.endAtMs > nextStart) changed.copy(endMs = changed.startMs + (nextStart - changed.atMs)) else changed)
                    },
                )
            },
        )
    }

    fun addTrack(kind: TrackKind) {
        val track = EditTrack(nextId++, kind, emptyList())
        val lastOfKind = tracks.indexOfLast { it.kind == kind }
        commit(tracks.toMutableList().apply { add(lastOfKind + 1, track) })
        activeTrackId = track.id
    }

    fun removeTrack(trackId: Int) {
        val track = tracks.firstOrNull { it.id == trackId } ?: return
        if (tracks.count { it.kind == track.kind } < 2) return
        if (track.clips.any { it.id == selectedId }) selectedId = null
        commit(tracks.filter { it.id != trackId })
    }

    fun toggleMuted(trackId: Int) = commit(tracks.edit(trackId) { it.copy(muted = !it.muted) })

    fun toggleHidden(trackId: Int) = commit(tracks.edit(trackId) { it.copy(hidden = !it.hidden) })

    fun loopToEnd(clipId: Int) {
        val track = trackOf(clipId) ?: return
        val edit = track.clips.first { it.id == clipId }
        val end = tracks.filter { it.kind == TrackKind.VIDEO }.maxOfOrNull { t -> t.clips.maxOfOrNull { it.clip.endAtMs } ?: 0 } ?: 0
        var cursor = edit.clip.endAtMs
        val copies = mutableListOf<EditClip>()
        val taken = track.clips.filter { it.id != clipId }
        while (cursor < end && copies.size < 200) {
            if (taken.any { it.clip.atMs < cursor + edit.clip.durationMs && it.clip.endAtMs > cursor }) break
            val length = minOf(edit.clip.durationMs, end - cursor)
            if (length < MIN_CLIP_MS) break
            copies += EditClip(nextId++, edit.clip.copy(atMs = cursor, endMs = edit.clip.startMs + length, fadeInMs = 0, fadeOutMs = 0), edit.sourceMs)
            cursor += length
        }
        if (copies.isEmpty()) return
        val fadeOut = edit.clip.fadeOutMs
        copies[copies.lastIndex] = copies.last().let { it.copy(clip = it.clip.copy(fadeOutMs = fadeOut)) }
        val head = edit.copy(clip = edit.clip.copy(fadeOutMs = 0))
        commit(tracks.edit(track.id) { t -> t.copy(clips = t.clips.map { if (it.id == clipId) head else it } + copies).sorted() })
    }

    companion object {
        const val MIN_CLIP_MS = 200L
    }
}
