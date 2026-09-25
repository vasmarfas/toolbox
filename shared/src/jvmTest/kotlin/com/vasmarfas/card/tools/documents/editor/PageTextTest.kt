package com.vasmarfas.card.tools.documents.editor

import com.vasmarfas.card.tools.documents.pdf.PdfDocument
import com.vasmarfas.card.tools.documents.pdf.PdfText
import com.vasmarfas.card.tools.documents.pdf.arial
import com.vasmarfas.card.tools.documents.pdf.helvetica
import com.vasmarfas.card.tools.documents.pdf.helveticaBold
import com.vasmarfas.card.tools.documents.pdf.pdf
import com.vasmarfas.card.tools.documents.pdf.textPage
import com.vasmarfas.card.tools.documents.pdf.times
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.util.Matrix

class PageTextTest {
    private val top = PDRectangle.A4.height - 60.0

    private fun text(bytes: ByteArray) = PageText(PdfText.glyphs(PdfDocument.parse(bytes), 0))

    @Test
    fun linesAndSpacesComeBack() {
        val page = text(pdf { doc -> doc.textPage(helvetica(), listOf("Alpha beta gamma", "Second line here")) })
        assertEquals("Alpha beta gamma\nSecond line here", page.text)
        assertEquals(2, page.lines.size)
    }

    @Test
    fun searchFindsTextWhereItIsDrawn() {
        val page = text(pdf { doc -> doc.textPage(doc.arial(), listOf("Первая строка", "вторая строка")) })
        val matches = page.search("СТРОКА")
        assertEquals(2, matches.size)
        val first = matches[0].bounds
        assertTrue(first.left > 90 && first.left < 140, "${first.left}")
        assertTrue(first.bottom < top && first.top > top, "$first")
        assertTrue(matches[1].bounds.top < first.bottom)
        assertEquals("строка", matches[0].text)
    }

    @Test
    fun rangeSpansLines() {
        val page = text(pdf { doc -> doc.textPage(helvetica(), listOf("one two", "three four")) })
        val start = page.text.indexOf("two")
        val match = assertNotNull(page.range(start, page.text.indexOf("four")))
        assertEquals(2, match.quads.size)
        assertEquals("two three", match.text)
    }

    @Test
    fun pointsFindTheirLineAndCharacter() {
        val page = text(pdf { doc -> doc.textPage(helvetica(), listOf("first", "second")) })
        assertEquals("second", page.lineAt(60.0, top - 18 + 3)?.text)
        val at = assertNotNull(page.offsetAt(52.0, top + 3))
        assertEquals('f', page.text[at])
    }

    @Test
    fun styleAndColourTravelWithTheGlyphs() {
        val bytes = pdf { doc ->
            doc.textPage(helveticaBold(), listOf("Bold"))
            PDPageContentStream(doc, doc.getPage(0), PDPageContentStream.AppendMode.APPEND, false).use { cs ->
                cs.setNonStrokingColor(1f, 0f, 0f)
                cs.beginText()
                cs.setFont(times(), 20f)
                cs.newLineAtOffset(50f, 400f)
                cs.showText("Red")
                cs.endText()
            }
        }
        val glyphs = PdfText.glyphs(PdfDocument.parse(bytes), 0)
        assertTrue(glyphs.first().style.bold)
        val red = glyphs.first { it.text == "R" }
        assertEquals(0xFFFF0000.toInt(), red.color)
        assertTrue(red.style.serif)
        assertEquals(20.0, red.size, 1e-6)
    }

    @Test
    fun linesKnowTheirQuarterTurn() {
        val bytes = pdf { doc ->
            val page = PDPage(PDRectangle.A4)
            doc.addPage(page)
            PDPageContentStream(doc, page).use { cs ->
                cs.beginText()
                cs.setFont(helvetica(), 12f)
                cs.setTextMatrix(Matrix.getRotateInstance(PI / 2, 300f, 300f))
                cs.showText("Upward")
                cs.setTextMatrix(Matrix.getRotateInstance(PI / 6, 100f, 100f))
                cs.showText("Slanted")
                cs.endText()
            }
        }
        val lines = text(bytes).lines
        assertEquals(listOf(90, null), lines.map { it.angle })
    }

    @Test
    fun replacementStartsOnTheOldBaseline() {
        val inset = 2.4
        val baseline = 9.6
        for (angle in listOf(0, 90, 180, 270)) {
            val box = lineBox(100.0, 200.0, angle, inset, baseline, 80.0, 19.2)
            val (width, height) = Affine.frameSize(box, angle)
            assertEquals(80.0, width, 1e-9)
            assertEquals(19.2, height, 1e-9)
            val frame = Affine.frameOf(box, angle)
            assertEquals(100.0, frame.x(inset, height - inset - baseline), 1e-9, "x at $angle")
            assertEquals(200.0, frame.y(inset, height - inset - baseline), 1e-9, "y at $angle")
        }
    }
}
