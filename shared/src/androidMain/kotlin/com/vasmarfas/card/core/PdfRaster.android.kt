package com.vasmarfas.card.core

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// PdfRenderer needs a file descriptor, hence the temporary copy
actual class PdfRaster private constructor(private val file: File, private val renderer: PdfRenderer) {
    private val gate = RenderGate {
        renderer.close()
        file.delete()
    }

    actual val pageCount: Int = renderer.pageCount

    actual suspend fun render(page: Int, width: Int): ImageBitmap = gate.use {
        withContext(Dispatchers.IO) {
            renderer.openPage(page).use { source ->
                val height = (width.toLong() * source.height / source.width.coerceAtLeast(1)).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                source.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap.asImageBitmap()
            }
        }
    }

    actual fun close() {
        gate.close()
    }

    actual companion object {
        actual suspend fun open(bytes: ByteArray): PdfRaster = withContext(Dispatchers.IO) {
            val file = File.createTempFile("render", ".pdf", AppContextHolder.context.cacheDir)
            file.writeBytes(bytes)
            try {
                PdfRaster(file, PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)))
            } catch (e: Exception) {
                file.delete()
                throw e
            }
        }
    }
}
