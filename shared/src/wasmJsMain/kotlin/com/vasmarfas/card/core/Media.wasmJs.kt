package com.vasmarfas.card.core

import androidx.compose.ui.graphics.ImageBitmap
import io.github.vinceglb.filekit.BrowserFile
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.WebFile
import io.github.vinceglb.filekit.toPlatformFile
import kotlinx.coroutines.await
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.khronos.webgl.Float32Array
import org.khronos.webgl.Int16Array
import org.khronos.webgl.Int32Array
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import org.khronos.webgl.toByteArray
import org.khronos.webgl.toFloatArray
import org.khronos.webgl.toShortArray
import kotlin.coroutines.cancellation.CancellationException
import kotlin.js.Promise
import kotlin.js.unsafeCast

private fun jsImport(name: String): Promise<JsAny> = js("window.vasmarfasImport(name)")

private fun jsHasVideoEncoder(): Boolean = js("typeof VideoEncoder !== 'undefined' && typeof OffscreenCanvas !== 'undefined'")

private fun jsProbe(module: JsAny, file: JsAny): Promise<JsAny> = js("module.probe(file)")

private fun jsFrame(module: JsAny, file: JsAny, timeMs: Double, maxSide: Int): Promise<JsAny?> = js("module.frame(file, timeMs, maxSide)")

private fun jsOpenFrames(module: JsAny, file: JsAny, fromMs: Double, toMs: Double, fps: Double, maxSide: Int): Promise<JsAny> =
    js("module.openFrames(file, fromMs, toMs, fps, maxSide)")

private fun jsNext(iterator: JsAny): Promise<JsAny?> = js("iterator.next()")

private fun jsClose(iterator: JsAny): Unit = js("iterator.close()")

private fun jsConvert(module: JsAny, file: JsAny, options: String): JsAny = js("module.convert(file, JSON.parse(options))")

private fun jsExportTimeline(module: JsAny, files: JsArray<JsAny>, project: String): JsAny = js("module.exportTimeline(files, JSON.parse(project))")

private fun jsPeaks(module: JsAny, file: JsAny, perSecond: Int, maxMs: Double): Promise<Float32Array> = js("module.peaks(file, perSecond, maxMs)")

private fun jsFile(blob: JsAny, name: String): BrowserFile = js("new File([blob], name, { type: blob.type })")

private fun jsDecodeAudio(module: JsAny, file: JsAny, maxMs: Double): Promise<JsAny> = js("module.decodeAudio(file, maxMs)")

private fun jsOpenAudioEncoder(module: JsAny, rate: Int, channels: Int, format: String, bitrate: Int): Promise<JsAny> =
    js("module.openAudioEncoder(rate, channels, format, bitrate)")

private fun jsPush(encoder: JsAny, samples: Int16Array): Promise<JsAny?> = js("encoder.push(samples)")

private fun jsFinish(encoder: JsAny): Promise<JsAny> = js("encoder.finish()")

private fun jsCancelEncoder(encoder: JsAny): Unit = js("encoder.cancel()")

private fun jsDownload(module: JsAny, blob: JsAny, name: String): Unit = js("module.download(blob, name)")

private fun jsBlobBytes(module: JsAny, blob: JsAny): Promise<Int8Array> = js("module.blobBytes(blob)")

private fun jsBlobSize(blob: JsAny): Double = js("blob.size")

private fun jsNumber(o: JsAny, key: String): Double = js("(+o[key]) || 0")

private fun jsText(o: JsAny, key: String): String? = js("o[key] == null ? null : String(o[key])")

private fun jsField(o: JsAny, key: String): JsAny = js("o[key]")

private fun jsTaskPromise(task: JsAny): Promise<JsAny?> = js("task.promise")

private fun jsTaskProgress(task: JsAny): Double = js("task.progress || 0")

private fun jsTaskCancel(task: JsAny): Unit = js("{ task.cancelled = true; if (task.cancel) task.cancel(); }")

private var mediaModule: JsAny? = null

internal suspend fun module(): JsAny = mediaModule ?: runJs { jsImport("media.mjs").await<JsAny>() }.also { mediaModule = it }

internal val PlatformFile.blob: JsAny
    get() = (webFile as WebFile.FileWrapper).file

private suspend fun <T> runJs(block: suspend () -> T): T = try {
    block()
} catch (e: JsException) {
    throw MediaException(e.message ?: "media error")
}

private suspend fun follow(task: JsAny, onProgress: (Float) -> Unit): JsAny = coroutineScope {
    val poll = launch {
        while (true) {
            onProgress(jsTaskProgress(task).toFloat().coerceIn(0f, 1f))
            delay(200)
        }
    }
    try {
        runJs { jsTaskPromise(task).await<JsAny?>() } ?: throw MediaException("empty result")
    } catch (e: CancellationException) {
        jsTaskCancel(task)
        throw e
    } finally {
        poll.cancel()
    }
}

private fun pixelsToBitmap(result: JsAny): ImageBitmap {
    val width = jsNumber(result, "width").toInt()
    val height = jsNumber(result, "height").toInt()
    val rgba = jsField(result, "pixels").unsafeCast<Int32Array>()
    val pixels = IntArray(width * height) {
        val v = rgba[it]
        (v and 0xFF00FF00.toInt()) or ((v and 0xFF) shl 16) or ((v ushr 16) and 0xFF)
    }
    return imageBitmapOf(pixels, width, height)
}

actual class MediaResult(val blob: JsAny) {
    actual val size: Long get() = jsBlobSize(blob).toLong()
}

actual suspend fun MediaResult.save(fileName: String): Boolean {
    jsDownload(module(), blob, fileName)
    return true
}

actual suspend fun MediaResult.readBytes(): ByteArray = runJs { jsBlobBytes(module(), blob).await<Int8Array>() }.toByteArray()

actual fun MediaResult.discard() = Unit

actual fun MediaResult.asFile(name: String): PlatformFile = WebFile.FileWrapper(jsFile(blob, name)).toPlatformFile()

internal fun MediaProject.toJs(width: Int, height: Int, fps: Int, videoBitrate: Long): Pair<JsArray<JsAny>, String> {
    val files = JsArray<JsAny>()
    val indices = HashMap<PlatformFile, Int>()
    fun index(file: PlatformFile): Int = indices.getOrPut(file) {
        files[files.length] = file.blob
        files.length - 1
    }
    val json = buildJsonObject {
        put(
            "tracks",
            buildJsonArray {
                tracks.forEach { track ->
                    add(
                        buildJsonObject {
                            put("kind", track.kind.name.lowercase())
                            put("muted", track.muted)
                            put("hidden", track.hidden)
                            put(
                                "clips",
                                buildJsonArray {
                                    track.clips.forEach { clip ->
                                        add(
                                            buildJsonObject {
                                                put("file", index(clip.file))
                                                put("kind", clip.kind.name.lowercase())
                                                put("startMs", clip.startMs)
                                                put("endMs", clip.endMs)
                                                put("atMs", clip.atMs)
                                                put("volume", clip.volume)
                                                put("fadeInMs", clip.fadeInMs)
                                                put("fadeOutMs", clip.fadeOutMs)
                                                put("opacity", clip.opacity)
                                                put("x", clip.box.x)
                                                put("y", clip.box.y)
                                                put("scale", clip.box.scale)
                                                put("fill", clip.fill)
                                            },
                                        )
                                    }
                                },
                            )
                        },
                    )
                }
            },
        )
        put(
            "spec",
            buildJsonObject {
                put("format", spec.format.extension)
                put("width", width)
                put("height", height)
                put("fps", fps)
                put("videoBitrate", videoBitrate)
                put("audioBitrate", spec.audioBitrateKbps * 1000)
                put("keepAudio", spec.keepAudio)
            },
        )
    }
    return files to json.toString()
}

actual object MediaEngine {
    actual val formats: Set<MediaFormat> = MediaFormat.entries.toSet()

    actual val canEdit: Boolean get() = jsHasVideoEncoder()

    actual suspend fun probe(file: PlatformFile): MediaInfo {
        val info = runJs { jsProbe(module(), file.blob).await<JsAny>() }
        return MediaInfo(
            durationMs = jsNumber(info, "durationMs").toLong(),
            width = jsNumber(info, "width").toInt(),
            height = jsNumber(info, "height").toInt(),
            frameRate = jsNumber(info, "frameRate"),
            videoCodec = jsText(info, "videoCodec"),
            audioCodec = jsText(info, "audioCodec"),
            sampleRate = jsNumber(info, "sampleRate").toInt(),
            channels = jsNumber(info, "channels").toInt(),
            bitrate = jsNumber(info, "bitrate").toLong(),
        )
    }

    actual suspend fun frame(file: PlatformFile, timeMs: Long, maxSide: Int): ImageBitmap? {
        val result = runCatching { runJs { jsFrame(module(), file.blob, timeMs.toDouble(), maxSide).await<JsAny?>() } }.getOrNull()
        return result?.let(::pixelsToBitmap)
    }

    actual suspend fun frames(file: PlatformFile, fromMs: Long, toMs: Long, fps: Double, maxSide: Int, onFrame: suspend (ImageBitmap) -> Unit) {
        val iterator = runJs { jsOpenFrames(module(), file.blob, fromMs.toDouble(), toMs.toDouble(), fps, maxSide).await<JsAny>() }
        try {
            while (true) {
                val next = runJs { jsNext(iterator).await<JsAny?>() } ?: break
                onFrame(pixelsToBitmap(next))
            }
        } finally {
            runCatching { jsClose(iterator) }
        }
    }

    actual suspend fun export(project: MediaProject, onProgress: (Float) -> Unit): MediaResult {
        val spec = project.spec
        val first = project.firstPicture?.let { probe(it.file) }
        val (width, height) = spec.frameSize(first)
        val fps = spec.frameRate(first)
        val videoBitrate = spec.videoBitrate(width, height, fps, project.durationMs)
        val single = project.single
        val plain = single != null && single.kind != ClipKind.IMAGE && project.tracks.single().let { !it.muted && !it.hidden } &&
            single.atMs == 0L && single.volume == 1f && single.fadeInMs == 0L && single.fadeOutMs == 0L &&
            single.opacity == 1f && single.box == ClipBox()
        val task = if (plain) {
            val options = buildJsonObject {
                put("format", spec.format.extension)
                put("startMs", single.startMs)
                put("endMs", single.endMs)
                put("width", if (spec.width > 0 || spec.height > 0) width else 0)
                put("height", if (spec.width > 0 || spec.height > 0) height else 0)
                put("fill", single.fill)
                put("fps", spec.frameRate)
                put("videoBitrate", videoBitrate)
                put("audioBitrate", spec.audioBitrateKbps * 1000)
                put("sampleRate", spec.sampleRate)
                put("channels", spec.channels)
                put("keepAudio", spec.keepAudio)
                put("copy", spec.copyStreams)
            }
            jsConvert(module(), single.file.blob, options.toString())
        } else {
            val (files, json) = project.toJs(width, height, fps, videoBitrate)
            jsExportTimeline(module(), files, json)
        }
        return MediaResult(follow(task, onProgress))
    }

    actual suspend fun decodeAudio(file: PlatformFile, maxDurationMs: Long): PcmAudio {
        val decoded = runJs { jsDecodeAudio(module(), file.blob, maxDurationMs.toDouble()).await<JsAny>() }
        val samples = jsField(decoded, "samples").unsafeCast<Int16Array>().toShortArray()
        return PcmAudio(jsNumber(decoded, "sampleRate").toInt(), jsNumber(decoded, "channels").toInt(), samples)
    }

    actual suspend fun peaks(file: PlatformFile, perSecond: Int, maxDurationMs: Long): FloatArray =
        runJs { jsPeaks(module(), file.blob, perSecond, maxDurationMs.toDouble()).await<Float32Array>() }.toFloatArray()

    actual suspend fun encodeAudio(source: PcmSource, format: MediaFormat, bitrateKbps: Int, onProgress: (Float) -> Unit): MediaResult {
        val encoder = runJs { jsOpenAudioEncoder(module(), source.sampleRate, source.channels, format.extension, bitrateKbps * 1000).await<JsAny>() }
        val chunk = ShortArray(source.sampleRate * source.channels)
        var frames = 0L
        try {
            while (true) {
                val n = source.read(chunk)
                if (n <= 0) break
                val samples = Int16Array(n)
                for (i in 0 until n) samples[i] = chunk[i]
                runJs { jsPush(encoder, samples).await<JsAny?>() }
                frames += n / source.channels
                onProgress((frames.toFloat() / source.frames.coerceAtLeast(1)).coerceIn(0f, 1f))
            }
            return MediaResult(runJs { jsFinish(encoder).await<JsAny>() })
        } catch (e: CancellationException) {
            jsCancelEncoder(encoder)
            throw e
        }
    }
}

