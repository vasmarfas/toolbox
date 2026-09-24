package com.vasmarfas.card.core

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

actual fun ImageBitmap.encode(format: EncodedFormat, quality: Int): ByteArray {
    val target = when (format) {
        EncodedFormat.JPEG -> EncodedImageFormat.JPEG
        EncodedFormat.PNG -> EncodedImageFormat.PNG
        EncodedFormat.WEBP -> EncodedImageFormat.WEBP
    }
    val data = Image.makeFromBitmap(asSkiaBitmap()).encodeToData(target, quality.coerceIn(0, 100))
        ?: throw IllegalStateException("$format encoder produced nothing")
    return data.bytes
}

actual fun imageBitmapOf(pixels: IntArray, width: Int, height: Int): ImageBitmap {
    val bytes = ByteArray(pixels.size * 4)
    var o = 0
    for (p in pixels) {
        bytes[o] = p.toByte()
        bytes[o + 1] = (p shr 8).toByte()
        bytes[o + 2] = (p shr 16).toByte()
        bytes[o + 3] = (p ushr 24).toByte()
        o += 4
    }
    val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL)
    val bitmap = Bitmap()
    bitmap.allocPixels(info)
    bitmap.installPixels(info, bytes, width * 4)
    return bitmap.asComposeImageBitmap()
}

internal fun skiaDecode(bytes: ByteArray): ImageBitmap? {
    val image = runCatching { Image.makeFromEncoded(bytes) }.getOrNull() ?: return null
    return Bitmap.makeFromImage(image).asComposeImageBitmap()
}
