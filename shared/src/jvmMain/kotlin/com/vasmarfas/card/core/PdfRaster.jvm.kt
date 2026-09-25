package com.vasmarfas.card.core

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
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

    actual suspend fun render(page: Int, pageWidth: Int, x: Int, y: Int, w: Int, h: Int): ImageBitmap = gate.use {
        withContext(Dispatchers.IO) {
            val source = document.getPage(page)
            val points = if (source.rotation % 180 == 0) source.cropBox.width else source.cropBox.height
            val image = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
            val graphics = image.createGraphics()
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics.color = Color.WHITE
            graphics.fillRect(0, 0, w, h)
            graphics.translate(-x.toDouble(), -y.toDouble())
            renderer.renderPageToGraphics(page, graphics, pageWidth / points.coerceAtLeast(1f))
            graphics.dispose()
            image.toComposeImageBitmap()
        }
    }

    actual fun close() {
        gate.close()
    }

    actual companion object {
        actual suspend fun open(bytes: ByteArray): PdfRaster = withContext(Dispatchers.IO) { PdfRaster(Loader.loadPDF(bytes)) }
    }
}
