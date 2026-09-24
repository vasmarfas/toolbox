package com.vasmarfas.card.tools.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.ClipKind
import com.vasmarfas.card.core.MediaClip
import com.vasmarfas.card.core.MediaFormat
import com.vasmarfas.card.core.MediaInfo
import com.vasmarfas.card.core.MediaPreview
import com.vasmarfas.card.core.MediaProject
import com.vasmarfas.card.core.MediaSpec
import com.vasmarfas.card.core.PreviewState
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import io.github.vinceglb.filekit.PlatformFile

@Composable
internal fun PreviewFrame(project: MediaProject?, state: PreviewState, ratio: Float, maxHeight: Int = 360) {
    Box(Modifier.fillMaxWidth().heightIn(max = maxHeight.dp), contentAlignment = Alignment.Center) {
        MediaPreview(
            project,
            state,
            Modifier.aspectRatio(ratio.coerceIn(0.2f, 5f), matchHeightConstraintsFirst = ratio < 1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black),
        )
    }
}

@Composable
internal fun PlayButton(state: PreviewState, durationMs: Long) {
    FilledTonalIconButton(
        onClick = {
            if (!state.playing && state.positionMs >= durationMs - 50) state.seek(0)
            state.playing = !state.playing
        },
        enabled = durationMs > 0,
    ) {
        Icon(
            if (state.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (state.playing) Res.string.pause.str() else Res.string.play.str(),
        )
    }
}

@Composable
internal fun PlayerBar(state: PreviewState, durationMs: Long) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PlayButton(state, durationMs)
        Slider(
            value = if (durationMs > 0) (state.positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
            onValueChange = {
                state.playing = false
                state.seek((it * durationMs).toLong())
            },
            modifier = Modifier.weight(1f),
        )
        Text("${formatClock(state.positionMs)} / ${formatClock(durationMs)}", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
internal fun FilePreview(file: PlatformFile, info: MediaInfo, state: PreviewState = remember(file) { PreviewState() }) {
    val project = remember(file, info) {
        val kind = if (info.hasVideo) ClipKind.VIDEO else ClipKind.AUDIO
        MediaProject(MediaClip(file, kind, 0, info.durationMs), MediaSpec(MediaFormat.MP4, width = info.width, height = info.height))
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (info.hasVideo && info.width > 0 && info.height > 0) {
            PreviewFrame(project, state, info.width.toFloat() / info.height)
        } else {
            MediaPreview(project, state, Modifier.size(1.dp))
        }
        PlayerBar(state, info.durationMs)
    }
}
