package com.vasmarfas.card.tools.documents.pdf

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.common.PDStream
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory

class PdfContentTest {
    private fun sample() = pdf { doc ->
        val page = PDPage(PDRectangle.A4)
        doc.addPage(page)
        val picture = LosslessFactory.createFromImage(doc, BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB))
        PDPageContentStream(doc, page).use { cs ->
            cs.setNonStrokingColor(0.9f, 0.2f, 0.2f)
            cs.addRect(300f, 500f, 120f, 60f)
            cs.fill()
            cs.drawImage(picture, 100f, 400f, 80f, 40f)
            cs.beginText()
            cs.setFont(helvetica(), 12f)
            cs.newLineAtOffset(50f, 700f)
            cs.showText("Keep this line")
            cs.newLineAtOffset(0f, -20f)
            cs.showText("Drop ")
            cs.showText("the tail")
            cs.endText()
            cs.saveGraphicsState()
            cs.addRect(0f, 0f, 10f, 10f)
            cs.clip()
            cs.restoreGraphicsState()
        }
    }

    private fun content(bytes: ByteArray) = PageContent.of(PdfDocument.parse(bytes).page(0))

    private fun replaced(bytes: ByteArray, content: ByteArray): ByteArray = Loader.loadPDF(bytes).use { doc ->
        doc.getPage(0).setContents(PDStream(doc, ByteArrayInputStream(content)))
        save(doc, SaveMode.CLASSIC)
    }

    @Test
    fun findsRunsPicturesAndPaintedPaths() {
        val content = content(sample())
        assertEquals(listOf("Keep this line", "Drop ", "the tail"), content.runs.map { run -> run.glyphs.joinToString("") { it.text } })
        val kinds = content.graphics.map { it.kind }
        assertEquals(listOf(GraphicKind.PATH, GraphicKind.IMAGE), kinds)
        val rect = content.graphics[0].bounds
        assertEquals(300.0, rect.left, 1e-6)
        assertEquals(560.0, rect.top, 1e-6)
        val image = content.graphics[1].bounds
        assertEquals(100.0, image.left, 1e-6)
        assertEquals(180.0, image.right, 1e-6)
        assertEquals(440.0, image.top, 1e-6)
    }

    @Test
    fun aRemovedRunLeavesTheRestWhereItWas() {
        val bytes = sample()
        val content = content(bytes)
        val drop = content.runs[1]
        val out = replaced(bytes, content.rewrite(setOf(drop.op), emptyMap()))
        assertEquals(listOf("Keep this line", "the tail"), pdfboxText(out, 0).trim().lines())
        val tail = content(out).runs.first { it.glyphs.firstOrNull()?.text == "t" }
        assertEquals(content.runs[2].glyphs.first().x0, tail.glyphs.first().x0, 1e-6)
    }

    @Test
    fun movedGraphicsAndTextLandWhereAsked() {
        val bytes = sample()
        val content = content(bytes)
        val image = content.graphics.first { it.kind == GraphicKind.IMAGE }
        val run = content.runs[0]
        val shift = doubleArrayOf(1.0, 0.0, 0.0, 1.0, 100.0, -50.0)
        val out = replaced(bytes, content.rewrite(emptySet(), mapOf(image.first to shift, run.op to shift)))
        val after = content(out)
        val moved = assertNotNull(after.graphics.firstOrNull { it.kind == GraphicKind.IMAGE })
        assertEquals(image.bounds.left + 100, moved.bounds.left, 1e-6)
        assertEquals(image.bounds.top - 50, moved.bounds.top, 1e-6)
        val again = after.runs.first { r -> r.glyphs.joinToString("") { it.text } == "Keep this line" }
        assertEquals(run.glyphs.first().x0 + 100, again.glyphs.first().x0, 1e-6)
        assertEquals(run.glyphs.first().y0 - 50, again.glyphs.first().y0, 1e-6)
        assertTrue(after.runs.none { r -> r.glyphs.isNotEmpty() && abs(r.glyphs.first().y0 - 700) < 0.01 })
        assertTrue("Keep this line" in pdfboxText(out, 0))
    }

    @Test
    fun scalingKeepsTheCornerPut() {
        val bytes = sample()
        val content = content(bytes)
        val rect = content.graphics.first { it.kind == GraphicKind.PATH }
        // twice as large around the bottom left corner
        val scale = doubleArrayOf(2.0, 0.0, 0.0, 2.0, -rect.bounds.left, -rect.bounds.bottom)
        val out = replaced(bytes, content.rewrite(emptySet(), mapOf(rect.first to scale)))
        val grown = content(out).graphics.first { it.kind == GraphicKind.PATH }
        assertEquals(rect.bounds.left, grown.bounds.left, 1e-6)
        assertEquals(rect.bounds.bottom, grown.bounds.bottom, 1e-6)
        assertEquals(rect.bounds.width * 2, grown.bounds.width, 1e-6)
    }
}
