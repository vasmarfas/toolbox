package com.vasmarfas.card.tools.media

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import com.vasmarfas.card.core.MediaEngine
import com.vasmarfas.card.core.MediaInfo
import com.vasmarfas.card.core.PickKind
import com.vasmarfas.card.core.PreviewState
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.core.formatBytes
import com.vasmarfas.card.core.pixels
import com.vasmarfas.card.core.renamed
import com.vasmarfas.card.core.saveBytes
import com.vasmarfas.card.core.scaled
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SegmentedChoice
import com.vasmarfas.card.ui.components.SwitchRow
import com.vasmarfas.card.ui.components.ToolSection
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.pluralStringResource
import kotlin.math.min
import kotlin.math.roundToInt

val gifMakerTool = Tool(
    id = "gif-maker",
    category = ToolCategory.MEDIA,
    title = Res.string.gif_maker,
    description = Res.string.gif_maker_description,
    icon = Icons.Filled.Gif,
    keywords = listOf(
        "gif", "animated gif", "video to gif", "gif maker", "animation", "meme", "slideshow",
        "гиф", "гифка", "gif из видео", "видео в gif", "анимация", "сделать гифку", "мем",
    ),
) { GifMakerScreen() }

private class Gif(val name: String, val bytes: ByteArray, val width: Int, val height: Int, val frames: Int, val first: ImageBitmap)

private val gifWidths = listOf(240, 320, 480, 640)

@Composable
private fun GifMakerScreen() {
    var fromVideo by rememberSaveable { mutableStateOf(true) }
    var width by rememberSaveable { mutableStateOf(480) }
    var loop by rememberSaveable { mutableStateOf(true) }
    var result by remember { mutableStateOf<Gif?>(null) }
    val task = remember { TaskState() }

    SegmentedChoice(
        options = listOf(true, false),
        selected = fromVideo,
        onSelect = {
            fromVideo = it
            result = null
        },
        label = { if (it) Res.string.from_video.str() else Res.string.from_photos.str() },
    )
    val options: @Composable () -> Unit = {
        ToolSection(Res.string.width.str()) {
            ChoiceChips(options = gifWidths, selected = width, onSelect = { width = it }, label = { "$it px" })
        }
        SwitchRow(Res.string.loop_forever.str(), loop, { loop = it })
    }
    if (fromVideo) {
        FromVideo(task, width, loop, options) { result = it }
    } else {
        FromPhotos(task, width, loop, options) { result = it }
    }
    TaskProgress(task, Res.string.rendering.str())
    result?.let { gif ->
        ResultCard(title = Res.string.result.str()) {
            Preview(gif.first)
            Text(
                "${gif.width} × ${gif.height} · ${pluralStringResource(Res.plurals.frame_count, gif.frames, gif.frames)} · ${formatBytes(gif.bytes.size.toLong(), binary = false)}",
                style = MaterialTheme.typography.bodyLarge,
            )
            SaveButton { saveBytes(gif.bytes, gif.name) }
        }
    }
}

@Composable
private fun FromVideo(task: TaskState, width: Int, loop: Boolean, options: @Composable () -> Unit, onResult: (Gif?) -> Unit) {
    val scope = rememberCoroutineScope()
    var file by remember { mutableStateOf<PlatformFile?>(null) }
    var info by remember { mutableStateOf<MediaInfo?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var range by remember { mutableStateOf(0f..1f) }
    var fps by rememberSaveable { mutableStateOf(12) }

    PickButton(Res.string.choose_video.str(), videoExtensions, PickKind.VIDEO, icon = Icons.Filled.VideoLibrary, empty = file == null) { files ->
        val picked = files.first()
        file = picked
        info = null
        loadError = null
        onResult(null)
        scope.launch {
            runCatching { MediaEngine.probe(picked) }
                .onSuccess { probed ->
                    info = probed
                    range = 0f..min(1f, 5000f / probed.durationMs.coerceAtLeast(1))
                }
                .onFailure { loadError = it.message ?: it.toString() }
        }
    }
    loadError?.let { ErrorText(it) }
    val source = file ?: return
    val probed = info ?: return
    val player = remember(source) { PreviewState() }
    FilePreview(source, probed, player)
    TrimSlider(probed.durationMs, range) { changed ->
        val moved = if (changed.start != range.start) changed.start else changed.endInclusive
        range = changed
        player.playing = false
        player.seek((moved * probed.durationMs).toLong())
    }
    ToolSection(Res.string.frame_rate.str()) {
        ChoiceChips(options = listOf(5, 8, 12, 15, 20, 25), selected = fps, onSelect = { fps = it }, label = { "$it fps" })
    }
    options()
    Hint(Res.string.gif_size_hint.str())
    ActionButton(
        text = Res.string.make_gif.str(),
        icon = Icons.Filled.Gif,
        enabled = !task.running && probed.hasVideo,
        onClick = {
            onResult(null)
            val start = (range.start * probed.durationMs).toLong()
            val end = (range.endInclusive * probed.durationMs).toLong().coerceAtLeast(start + 200)
            val landscape = probed.width >= probed.height
            val longSide = if (landscape) width else (width.toLong() * probed.height / probed.width.coerceAtLeast(1)).toInt()
            val rate = fps
            task.launch(scope) { progress ->
                var writer: GifWriter? = null
                var first: ImageBitmap? = null
                var count = 0
                val total = ((end - start) * rate / 1000).coerceAtLeast(1)
                MediaEngine.frames(source, start, end, rate.toDouble(), longSide) { frame ->
                    val gif = writer ?: GifWriter(frame.width, frame.height, if (loop) 0 else -1).also {
                        writer = it
                        first = frame
                    }
                    withContext(Dispatchers.Default) { gif.addFrame(frame.pixels(), 1000 / rate) }
                    count++
                    progress((count.toFloat() / total).coerceAtMost(1f))
                }
                val gif = writer ?: return@launch
                val cover = first ?: return@launch
                onResult(Gif(renamed(source.name, "gif"), withContext(Dispatchers.Default) { gif.finish() }, gif.width, gif.height, count, cover))
            }
        },
    )
}

@Composable
private fun FromPhotos(task: TaskState, width: Int, loop: Boolean, options: @Composable () -> Unit, onResult: (Gif?) -> Unit) {
    val scope = rememberCoroutineScope()
    val files = remember { mutableStateListOf<PlatformFile>() }
    var frameMs by rememberSaveable { mutableStateOf(500) }
    val unreadable = Res.string.image_not_readable.str()

    PickButton(Res.string.choose_images.str(), imageExtensions, PickKind.IMAGE, multiple = true, icon = Icons.Filled.AddPhotoAlternate, empty = files.isEmpty()) { picked ->
        files.clear()
        files += picked
        onResult(null)
    }
    if (files.isEmpty()) return
    files.forEach { FileLine(it.name, formatBytes(it.size(), binary = false), icon = Icons.Filled.Image) }
    LabeledSlider(Res.string.frame_time.str(), (frameMs / 1000.0).fmt(1) + " s", frameMs.toFloat(), 100f..5000f) { frameMs = (it / 100).roundToInt() * 100 }
    options()
    ActionButton(
        text = Res.string.make_gif.str(),
        icon = Icons.Filled.Gif,
        enabled = !task.running,
        onClick = {
            onResult(null)
            val sources = files.toList()
            val delay = frameMs
            task.launch(scope) { progress ->
                var writer: GifWriter? = null
                var first: ImageBitmap? = null
                sources.forEachIndexed { index, file ->
                    val image = decodeImage(file.readBytes()) ?: throw IllegalStateException("${file.name}: $unreadable")
                    val gif = writer ?: GifWriter(width, (width.toLong() * image.height / image.width).toInt().coerceAtLeast(1), if (loop) 0 else -1).also { writer = it }
                    val frame = withContext(Dispatchers.Default) { fitted(image, gif.width, gif.height) }
                    if (first == null) first = frame
                    withContext(Dispatchers.Default) { gif.addFrame(frame.pixels(), delay) }
                    progress((index + 1f) / sources.size)
                }
                val gif = writer ?: return@launch
                val cover = first ?: return@launch
                onResult(Gif(renamed(sources.first().name, "gif"), withContext(Dispatchers.Default) { gif.finish() }, gif.width, gif.height, sources.size, cover))
            }
        },
    )
}

private fun fitted(image: ImageBitmap, width: Int, height: Int): ImageBitmap {
    val scale = min(width.toDouble() / image.width, height.toDouble() / image.height)
    val w = (image.width * scale).roundToInt().coerceIn(1, width)
    val h = (image.height * scale).roundToInt().coerceIn(1, height)
    if (w == width && h == height) return image.scaled(w, h)
    val out = ImageBitmap(width, height)
    Canvas(out).drawImage(image.scaled(w, h), Offset(((width - w) / 2).toFloat(), ((height - h) / 2).toFloat()), Paint())
    return out
}
