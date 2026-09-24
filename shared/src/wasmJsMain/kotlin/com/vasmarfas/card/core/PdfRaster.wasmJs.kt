package com.vasmarfas.card.core

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.await
import org.khronos.webgl.Int32Array
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.khronos.webgl.toInt8Array
import kotlin.js.Promise

private fun jsImportPdf(): Promise<JsAny> = js("window.vasmarfasImport('pdf.mjs')")

private fun jsOpen(module: JsAny, bytes: Int8Array): Promise<JsAny> = js("module.open(new Uint8Array(bytes.buffer, bytes.byteOffset, bytes.length))")

private fun jsCount(document: JsAny): Int = js("document.count")

private fun jsRender(document: JsAny, index: Int, width: Int): Promise<JsAny> = js("document.render(index, width)")

private fun jsClose(document: JsAny): Unit = js("document.close()")

private fun jsWidth(page: JsAny): Int = js("page.width")

private fun jsHeight(page: JsAny): Int = js("page.height")

private fun jsPixels(page: JsAny): Int32Array = js("page.pixels")

private var pdfModule: JsAny? = null

actual class PdfRaster private constructor(private val document: JsAny) {
    actual val pageCount: Int = jsCount(document)

    actual suspend fun render(page: Int, width: Int): ImageBitmap {
        val rendered = jsRender(document, page, width).await<JsAny>()
        val w = jsWidth(rendered)
        val h = jsHeight(rendered)
        val rgba = jsPixels(rendered)
        val pixels = IntArray(w * h) {
            val v = rgba[it]
            (v and 0xFF00FF00.toInt()) or ((v and 0xFF) shl 16) or ((v ushr 16) and 0xFF)
        }
        return imageBitmapOf(pixels, w, h)
    }

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
