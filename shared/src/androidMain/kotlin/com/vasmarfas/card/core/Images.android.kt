package com.vasmarfas.card.core

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun decodeRawImage(bytes: ByteArray): RawImage? = withContext(Dispatchers.Default) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val decoded = runCatching {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB))
            }
        }.getOrNull()
        if (decoded != null) return@withContext RawImage(decoded.asImageBitmap(), oriented = true)
    }
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext null
    RawImage(bitmap.asImageBitmap(), oriented = false)
}

actual fun ImageBitmap.encode(format: EncodedFormat, quality: Int): ByteArray {
    val compress = when (format) {
        EncodedFormat.JPEG -> Bitmap.CompressFormat.JPEG
        EncodedFormat.PNG -> Bitmap.CompressFormat.PNG
        EncodedFormat.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
    }
    val out = ByteArrayOutputStream()
    asAndroidBitmap().compress(compress, quality.coerceIn(0, 100), out)
    return out.toByteArray()
}

actual fun imageBitmapOf(pixels: IntArray, width: Int, height: Int): ImageBitmap =
    Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
