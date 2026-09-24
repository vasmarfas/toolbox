package com.vasmarfas.card.core

import android.content.Context
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.Size
import androidx.media3.effect.MultipleInputVideoGraph
import androidx.media3.transformer.CompositionPlayer
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.delay

private const val PREVIEW_SIDE = 1280

// a multi-input CompositionPlayer stalls after a second composition or a seek, so every play starts
// a new player at the playhead
@Composable
actual fun MediaPreview(project: MediaProject?, state: PreviewState, modifier: Modifier) {
    val context = LocalContext.current
    val infos = remember { HashMap<PlatformFile, MediaInfo>() }
    var run by remember { mutableIntStateOf(0) }
    val structure = remember(project) { project?.structure() }
    LaunchedEffect(state.playing) {
        if (state.playing) run++
    }
    LaunchedEffect(structure) {
        state.playing = false
        project?.tracks?.flatMap { it.clips }?.map { it.file }?.distinct()?.filter { it !in infos }?.forEach { file ->
            runCatching { MediaEngine.probe(file) }.onSuccess { infos[file] = it }
        }
    }
    var surface by remember { mutableStateOf<Pair<Surface, Size>?>(null) }
    Box(modifier) {
        if (run > 0) key(run) { Playback(project, state, infos, context, surface) }
        AndroidView(
            factory = { viewContext ->
                TextureView(viewContext).apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
                            surface = Surface(texture) to Size(width, height)
                        }

                        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
                            surface = surface?.let { it.first to Size(width, height) }
                        }

                        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                            surface?.first?.release()
                            surface = null
                            return true
                        }

                        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (!state.playing && project != null) StillFrame(project, state.positionMs, infos, Modifier.fillMaxSize())
    }
}

@Composable
private fun Playback(project: MediaProject?, state: PreviewState, infos: Map<PlatformFile, MediaInfo>, context: Context, surface: Pair<Surface, Size>?) {
    val player = remember { CompositionPlayer.Builder(context).setVideoGraphFactory(MultipleInputVideoGraph.Factory()).build() }
    var built by remember { mutableStateOf<MediaEngine.Built?>(null) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    state.report(player.currentPosition)
                    state.playing = false
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    LaunchedEffect(surface) {
        if (surface == null) player.clearVideoSurface() else player.setVideoSurface(surface.first, surface.second)
    }
    LaunchedEffect(Unit) {
        val current = project ?: return@LaunchedEffect
        built = runCatching {
            MediaEngine.composition(current, infos, video = true, audio = true, maxSide = PREVIEW_SIDE, clock = false).also {
                player.setComposition(it.composition, state.positionMs.coerceIn(0, current.durationMs))
                player.prepare()
                player.play()
            }
        }.getOrNull()
        if (built == null) state.playing = false
    }
    LaunchedEffect(project, built) {
        built?.layers?.layers = project?.pictures?.asReversed().orEmpty()
    }
    LaunchedEffect(state.playing, built) {
        if (built == null) return@LaunchedEffect
        if (!state.playing) {
            player.pause()
            return@LaunchedEffect
        }
        while (state.playing) {
            state.report(player.currentPosition)
            delay(33)
        }
    }
}
