package com.vasmarfas.card.tools.documents.editor

import com.vasmarfas.card.tools.documents.pdf.PageContent
import com.vasmarfas.card.tools.documents.pdf.PdfDocument
import com.vasmarfas.card.tools.documents.pdf.helvetica
import com.vasmarfas.card.tools.documents.pdf.helveticaBold
import com.vasmarfas.card.tools.documents.pdf.pdf
import kotlin.test.Test
import kotlin.test.assertEquals
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont

class TextBlocksTest {
    private fun blocks(build: (PDPageContentStream) -> Unit): List<TextBlock> {
        val bytes = pdf { doc ->
            val page = PDPage(PDRectangle.A4)
            doc.addPage(page)
            PDPageContentStream(doc, page).use(build)
        }
        return TextBlocks.build(PageContent.of(PdfDocument.parse(bytes).page(0)).runs)
    }

    private fun PDPageContentStream.line(font: PDFont, size: Float, x: Float, y: Float, text: String) {
        beginText()
        setFont(font, size)
        newLineAtOffset(x, y)
        showText(text)
        endText()
    }

    @Test
    fun paragraphsHeadingsAndListItemsStayApart() {
        val blocks = blocks { cs ->
            cs.line(helveticaBold(), 18f, 50f, 760f, "Heading line")
            listOf("one two three", "four five six", "seven eight").forEachIndexed { i, text -> cs.line(helvetica(), 12f, 50f, 720f - 16f * i, text) }
            cs.line(helvetica(), 12f, 50f, 640f, "• first item")
            cs.line(helvetica(), 12f, 50f, 624f, "• second item")
        }
        assertEquals(listOf("Heading line", "one two three four five six seven eight", "• first item", "• second item"), blocks.map { it.text })
        val paragraph = blocks[1]
        assertEquals(3, paragraph.lines.size)
        assertEquals(16f / 12f, paragraph.spacing, 0.01f)
        assertEquals(MarkAlign.START, paragraph.align)
        assertEquals(50.0, paragraph.anchorX, 1e-6)
        assertEquals(720.0, paragraph.anchorY, 1e-6)
        assertEquals(0, paragraph.angle)
        assertEquals(true, blocks[0].style.bold)
    }

    @Test
    fun centredLinesAreCentred() {
        val font = helvetica()
        val blocks = blocks { cs ->
            listOf("a short one", "a considerably longer line of text", "mid length line").forEachIndexed { i, text ->
                val width = font.getStringWidth(text) / 1000 * 12
                cs.line(font, 12f, 300f - width / 2, 500f - 15f * i, text)
            }
        }
        assertEquals(1, blocks.size)
        assertEquals(MarkAlign.CENTER, blocks[0].align)
    }

    @Test
    fun aDashGoesOnWithTheParagraphAndABoldLeadInLeavesItPlain() {
        val blocks = blocks { cs ->
            cs.beginText()
            cs.setFont(helveticaBold(), 12f)
            cs.newLineAtOffset(50f, 700f)
            cs.showText("Lead: ")
            cs.setFont(helvetica(), 12f)
            cs.showText("a plain line that runs on")
            cs.endText()
            cs.line(helvetica(), 12f, 50f, 684f, "— and goes on here")
        }
        assertEquals(listOf("Lead: a plain line that runs on — and goes on here"), blocks.map { it.text })
        assertEquals(false, blocks[0].style.bold)
    }

    @Test
    fun oneOperatorForTwoLinesMakesOneBlock() {
        val blocks = blocks { cs ->
            cs.beginText()
            cs.setFont(helvetica(), 12f)
            cs.newLineAtOffset(50f, 400f)
            cs.showText("left column")
            cs.endText()
            cs.beginText()
            cs.setFont(helvetica(), 12f)
            cs.newLineAtOffset(50f, 300f)
            // a kerning jump of 20 em puts the rest far to the right, the operator is still one
            cs.showTextWithPositioning(arrayOf<Any>("far", -20000f, "right"))
            cs.endText()
        }
        assertEquals(listOf("left column", "far right"), blocks.map { it.text })
        assertEquals(1, blocks[1].ops.size)
    }
}
