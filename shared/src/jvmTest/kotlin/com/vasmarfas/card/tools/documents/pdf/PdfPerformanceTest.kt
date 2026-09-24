package com.vasmarfas.card.tools.documents.pdf

import java.awt.image.BufferedImage
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory

class PdfPerformanceTest {
    private fun largeDocument(pages: Int): ByteArray {
        val random = Random(7)
        return pdf(SaveMode.COMPRESSED) { doc ->
            val font = helvetica()
            repeat(pages) { i ->
                val image = BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB)
                for (y in 0 until 128) for (x in 0 until 128) image.setRGB(x, y, random.nextInt())
                val picture = LosslessFactory.createFromImage(doc, image)
                val page = PDPage()
                doc.addPage(page)
                PDPageContentStream(doc, page).use { cs ->
                    cs.drawImage(picture, 100f, 400f)
                    cs.beginText()
                    cs.setFont(font, 12f)
                    cs.newLineAtOffset(72f, 720f)
                    cs.showText("Large document page $i")
                    cs.endText()
                }
            }
        }
    }

    @Test
    fun largeDocumentStaysLinear() {
        val bytes = largeDocument(1000)
        assertTrue(bytes.size > 45_000_000, "${bytes.size} bytes")
        val started = System.nanoTime()
        val doc = PdfDocument.parse(bytes)
        assertEquals(1000, doc.pageCount)
        val order = (0 until 1000).map { PageRef(doc, (it * 7) % 1000) }
        val output = PdfAssembler.assemble(order)
        for (i in 0 until 1000) assertEquals("Large document page $i", PdfText.extract(doc, i))
        val extracted = System.nanoTime()
        val reread = PdfDocument.parse(output)
        assertEquals(1000, reread.pageCount)
        assertEquals("Large document page 7", PdfText.extract(reread, 1))
        val seconds = { from: Long, to: Long -> (to - from) / 1e9 }
        assertTrue(seconds(started, extracted) < 30, "took ${seconds(started, extracted)} s")
        loadCleanly(output) { result ->
            assertEquals(1000, result.numberOfPages)
            assertEquals("Large document page 14", pdfboxText(result, 2).trim())
        }
        Loader.loadPDF(output).use { assertEquals(1000, it.numberOfPages) }
    }
}
