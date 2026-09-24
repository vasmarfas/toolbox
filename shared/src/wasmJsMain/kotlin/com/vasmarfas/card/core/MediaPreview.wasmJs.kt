package com.vasmarfas.card.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.HtmlElementView
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.browser.document
import kotlinx.coroutines.delay
import org.w3c.dom.HTMLCanvasElement

private fun jsCreatePreview(module: JsAny, canvas: HTMLCanvasElement): JsAny = js("module.createPreview(canvas)")

private fun jsLoad(preview: JsAny, files: JsArray<JsAny>, project: String?): Unit = js("preview.load(files, project == null ? null : JSON.parse(project))")

private fun jsSeek(preview: JsAny, ms: Double): Unit = js("preview.seek(ms)")

private fun jsPlay(preview: JsAny, ms: Double): Unit = js("preview.play(ms)")

private fun jsPause(preview: JsAny): Unit = js("preview.pause()")

private fun jsPosition(preview: JsAny): Double = js("preview.position()")

private fun jsEnded(preview: JsAny): Boolean = js("preview.ended()")

private fun jsDispose(preview: JsAny): Unit = js("preview.dispose()")

// pointer-events: none (styles.css), so the page still scrolls over the canvas
@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun MediaPreview(project: MediaProject?, state: PreviewState, modifier: Modifier) {
    val canvas = remember { (document.createElement("canvas") as HTMLCanvasElement).apply { className = "media-preview" } }
    var preview by remember { mutableStateOf<JsAny?>(null) }
    val infos = remember { HashMap<PlatformFile, MediaInfo>() }
    val structure = remember(project) { project?.structure() }
    HtmlElementView(factory = { canvas }, modifier = modifier)
    LaunchedEffect(canvas) {
        preview = runCatching { jsCreatePreview(module(), canvas) }.getOrNull()
    }
    DisposableEffect(preview) {
        val player = preview
        onDispose { player?.let(::jsDispose) }
    }
    LaunchedEffect(structure) {
        state.playing = false
    }
    LaunchedEffect(preview, project) {
        val player = preview ?: return@LaunchedEffect
        if (project == null) {
            jsLoad(player, JsArray(), null)
            return@LaunchedEffect
        }
        val first = project.firstPicture?.file?.let { file ->
            infos[file] ?: runCatching { MediaEngine.probe(file) }.getOrNull()?.also { infos[file] = it }
        }
        val (width, height) = project.spec.frameSize(first)
        val (files, json) = project.toJs(width, height, 0, 0)
        jsLoad(player, files, json)
    }
    LaunchedEffect(preview, state.seekRequest) {
        val player = preview ?: return@LaunchedEffect
        if (!state.playing) jsSeek(player, state.positionMs.toDouble())
    }
    LaunchedEffect(preview, state.playing) {
        val player = preview ?: return@LaunchedEffect
        if (!state.playing) {
            jsPause(player)
            return@LaunchedEffect
        }
        jsPlay(player, state.positionMs.toDouble())
        while (true) {
            delay(33)
            val ended = jsEnded(player)
            state.report(jsPosition(player).toLong())
            if (ended) break
        }
        state.playing = false
    }
}
