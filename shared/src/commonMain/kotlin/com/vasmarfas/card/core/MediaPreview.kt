package com.vasmarfas.card.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Stable
class PreviewState {
    var positionMs by mutableLongStateOf(0L)
        private set

    var playing by mutableStateOf(false)

    var seekRequest by mutableIntStateOf(0)
        private set

    fun seek(ms: Long) {
        positionMs = ms.coerceAtLeast(0)
        seekRequest++
    }

    internal fun report(ms: Long) {
        positionMs = ms
    }
}

// only what needs a new player: size and opacity reach a running one without a restart
internal fun MediaProject.structure(): MediaProject =
    copy(tracks = tracks.map { track -> track.copy(clips = track.clips.map { it.copy(opacity = 1f, box = ClipBox()) }) })

@Composable
expect fun MediaPreview(project: MediaProject?, state: PreviewState, modifier: Modifier = Modifier)
