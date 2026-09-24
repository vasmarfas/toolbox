package com.vasmarfas.card.core

import kotlinx.coroutines.await
import org.khronos.webgl.Int32Array
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.khronos.webgl.toInt8Array
import kotlin.js.Promise

private fun jsDecodeImage(bytes: Int8Array): Promise<JsAny?> = js(
    """(async () => {
        try {
            const bitmap = await createImageBitmap(new Blob([bytes]));
            const canvas = new OffscreenCanvas(bitmap.width, bitmap.height);
            const context = canvas.getContext('2d');
            context.drawImage(bitmap, 0, 0);
            const data = context.getImageData(0, 0, bitmap.width, bitmap.height).data;
            return { width: bitmap.width, height: bitmap.height, pixels: new Int32Array(data.buffer) };
        } catch (e) {
            return null;
        }
    })()""",
)

private fun jsImageWidth(image: JsAny): Int = js("image.width")

private fun jsImageHeight(image: JsAny): Int = js("image.height")

private fun jsImagePixels(image: JsAny): Int32Array = js("image.pixels")

// Skia in wasm knows the common formats, the browser's decoder adds AVIF, and HEIC in Safari
actual suspend fun decodeRawImage(bytes: ByteArray): RawImage? {
    skiaDecode(bytes)?.let { return RawImage(it, oriented = true) }
    val decoded = jsDecodeImage(bytes.toInt8Array()).await<JsAny?>() ?: return null
    val width = jsImageWidth(decoded)
    val height = jsImageHeight(decoded)
    val rgba = jsImagePixels(decoded)
    val pixels = IntArray(width * height) {
        val v = rgba[it]
        (v and 0xFF00FF00.toInt()) or ((v and 0xFF) shl 16) or ((v ushr 16) and 0xFF)
    }
    return RawImage(imageBitmapOf(pixels, width, height), oriented = true)
}
