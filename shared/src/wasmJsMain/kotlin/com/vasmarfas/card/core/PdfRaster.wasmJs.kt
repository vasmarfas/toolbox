package com.vasmarfas.card.core

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.await
import org.khronos.webgl.Int32Array
import org.khronos.webgl.Int8Array
import org.khronos.webgl.toInt8Array
import kotlin.js.Promise

private fun jsImportPdf(): Promise<JsAny> = js("window.vasmarfasImport('pdf.mjs')")

private fun jsOpen(module: JsAny, bytes: Int8Array): Promise<JsAny> = js("module.open(new Uint8Array(bytes.buffer, bytes.byteOffset, bytes.length))")

private fun jsCount(document: JsAny): Int = js("document.count")

private fun jsRender(document: JsAny, index: Int, width: Int): Promise<JsAny> = js("document.render(index, width)")

private fun jsRenderRegion(document: JsAny, index: Int, width: Int, x: Int, y: Int, w: Int, h: Int): Promise<JsAny> =
    js("document.renderRegion(index, width, x, y, w, h)")

private fun jsClose(document: JsAny): Unit = js("document.close()")

private fun jsWidth(page: JsAny): Int = js("page.width")

private fun jsHeight(page: JsAny): Int = js("page.height")

private fun jsPixels(page: JsAny): Int32Array = js("page.pixels")

private var pdfModule: JsAny? = null

actual class PdfRaster private constructor(private val document: JsAny) {
    actual val pageCount: Int = jsCount(document)

    actual suspend fun render(page: Int, width: Int): ImageBitmap = bitmap(jsRender(document, page, width).await())

    actual suspend fun render(page: Int, pageWidth: Int, x: Int, y: Int, w: Int, h: Int): ImageBitmap =
        bitmap(jsRenderRegion(document, page, pageWidth, x, y, w, h).await())

    private fun bitmap(rendered: JsAny): ImageBitmap = rgbaBitmap(jsPixels(rendered), jsWidth(rendered), jsHeight(rendered))

    actual fun close() {
        jsClose(document)
    }

    actual companion object {
        actual suspend fun open(bytes: ByteArray): PdfRaster {
            val module = pdfModule ?: jsImportPdf().await<JsAny>().also { pdfModule = it }
            return PdfRaster(jsOpen(module, bytes.toInt8Array()).await())
        }
    }
}
