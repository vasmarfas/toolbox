package com.vasmarfas.card.core

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer

actual class PdfRaster private constructor(private val document: PDDocument) {
    private val renderer = PDFRenderer(document)
    private val gate = RenderGate { document.close() }

    actual val pageCount: Int = document.numberOfPages

    actual suspend fun render(page: Int, width: Int): ImageBitmap = gate.use {
        withContext(Dispatchers.IO) {
            val source = document.getPage(page)
            val points = if (source.rotation % 180 == 0) source.cropBox.width else source.cropBox.height
            renderer.renderImage(page, width / points.coerceAtLeast(1f)).toComposeImageBitmap()
        }
    }

    actual fun close() {
        gate.close()
    }

    actual companion object {
        actual suspend fun open(bytes: ByteArray): PdfRaster = withContext(Dispatchers.IO) { PdfRaster(Loader.loadPDF(bytes)) }
    }
}
