package com.vasmarfas.card.core

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.InputStream
import kotlin.math.roundToInt

private const val PREVIEW_FPS = 25
private const val PREVIEW_SIDE = 960

// the export graph run twice, for rawvideo and for PCM, frames wait for the sound card clock
@Composable
actual fun MediaPreview(project: MediaProject?, state: PreviewState, modifier: Modifier) {
    var frame by remember { mutableStateOf<ImageBitmap?>(null) }
    val infos = remember { HashMap<PlatformFile, MediaInfo>() }
    Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        frame?.let { Image(it, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()) }
    }
    LaunchedEffect(project, state.seekRequest, state.playing) {
        val current = project ?: return@LaunchedEffect
        current.tracks.flatMap { it.clips }.map { it.file }.distinct().filter { it !in infos }.forEach { file ->
            runCatching { MediaEngine.probe(file) }.onSuccess { infos[file] = it }
        }
        val first = current.firstPicture?.let { infos[it.file] }
        val (width, height) = previewSize(current.spec.frameSize(first))
        val total = current.durationMs
        if (state.playing) {
            runCatching {
                play(current, infos, state.positionMs.coerceIn(0, total), width, height) { image, ms ->
                    frame = image
                    state.report(ms)
                }
            }
            state.playing = false
        } else {
            delay(60)
            val at = state.positionMs.coerceIn(0, (total - 1000L / PREVIEW_FPS).coerceAtLeast(0))
            runCatching { still(current, infos, at, width, height) }.getOrNull()?.let { frame = it }
        }
    }
}

private fun previewSize(size: Pair<Int, Int>): Pair<Int, Int> {
    val (w, h) = size
    val scale = minOf(1.0, PREVIEW_SIDE.toDouble() / maxOf(w, h))
    return ((w * scale).roundToInt() / 2 * 2).coerceAtLeast(2) to ((h * scale).roundToInt() / 2 * 2).coerceAtLeast(2)
}

private fun command(graph: FfmpegArgs.Graph, output: String, format: List<String>): List<String> =
    listOf(MediaEngine.ffmpegPath, "-hide_banner", "-nostdin", "-loglevel", "error") + graph.inputs +
        listOf("-filter_complex", graph.filters, "-map", output) + format + "pipe:1"

private suspend fun still(project: MediaProject, infos: Map<PlatformFile, MediaInfo>, atMs: Long, width: Int, height: Int): ImageBitmap? {
    val graph = FfmpegArgs.graph(project, infos, width, height, PREVIEW_FPS, atMs, atMs + 1000L / PREVIEW_FPS, video = true, audio = false)
    var image: ImageBitmap? = null
    MediaEngine.execute(command(graph, "[vout]", listOf("-frames:v", "1", "-f", "rawvideo", "-pix_fmt", "rgba"))) { stdout ->
        val bytes = ByteArray(width * height * 4)
        if (MediaEngine.readFully(stdout, bytes)) image = rgbaBitmap(bytes, width, height)
    }
    return image
}

private suspend fun play(
    project: MediaProject,
    infos: Map<PlatformFile, MediaInfo>,
    fromMs: Long,
    width: Int,
    height: Int,
    onFrame: (ImageBitmap, Long) -> Unit,
) = coroutineScope {
    val total = project.durationMs
    if (fromMs >= total) return@coroutineScope
    val player = PcmPlayer()
    val started = System.nanoTime()
    fun clockMs(): Long =
        if (player.playing) fromMs + player.position * 1000 / FfmpegArgs.SAMPLE_RATE else fromMs + (System.nanoTime() - started) / 1_000_000
    val sound = FfmpegArgs.graph(project, infos, width, height, PREVIEW_FPS, fromMs, total, video = false, audio = true)
    launch {
        MediaEngine.execute(command(sound, "[aout]", listOf("-f", "s16le", "-ac", "2", "-ar", FfmpegArgs.SAMPLE_RATE.toString()))) { stdout ->
            val ended = CompletableDeferred<Unit>()
            val bytes = ByteArray(FfmpegArgs.SAMPLE_RATE / 10 * 4)
            player.start(FfmpegArgs.SAMPLE_RATE, 2) { samples ->
                val n = readFrames(stdout, bytes, minOf(bytes.size, samples.size * 2))
                if (n <= 0) ended.complete(Unit)
                for (i in 0 until n / 2) samples[i] = ((bytes[i * 2].toInt() and 0xFF) or (bytes[i * 2 + 1].toInt() shl 8)).toShort()
                n / 2
            }
            try {
                ended.await()
            } finally {
                player.stop()
            }
        }
    }
    val pictures = FfmpegArgs.graph(project, infos, width, height, PREVIEW_FPS, fromMs, total, video = true, audio = false)
    MediaEngine.execute(command(pictures, "[vout]", listOf("-f", "rawvideo", "-pix_fmt", "rgba"))) { stdout ->
        val bytes = ByteArray(width * height * 4)
        var index = 0L
        while (MediaEngine.readFully(stdout, bytes)) {
            val pts = fromMs + index++ * 1000 / PREVIEW_FPS
            val wait = pts - clockMs()
            if (wait < -120) continue
            if (wait > 0) delay(wait)
            onFrame(rgbaBitmap(bytes, width, height), pts)
        }
    }
}

private fun readFrames(stream: InputStream, buffer: ByteArray, limit: Int): Int {
    var read = 0
    while (read < limit) {
        val n = stream.read(buffer, read, limit - read)
        if (n < 0) break
        read += n
        if (read % 4 == 0) break
    }
    return read / 4 * 4
}
