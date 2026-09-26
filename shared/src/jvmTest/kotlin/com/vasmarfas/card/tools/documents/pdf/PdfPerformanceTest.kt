package com.vasmarfas.card.tools.documents.pdf

import com.vasmarfas.card.tools.documents.DocFonts
import com.vasmarfas.card.tools.documents.FontFamily
import com.vasmarfas.card.tools.documents.editor.DocumentEdit
import com.vasmarfas.card.tools.documents.editor.EditPage
import com.vasmarfas.card.tools.documents.editor.MarkFonts
import com.vasmarfas.card.tools.documents.editor.ObjectEdit
import com.vasmarfas.card.tools.documents.editor.PdfEditWriter
import com.vasmarfas.card.tools.documents.editor.SourcePage
import com.vasmarfas.card.tools.documents.editor.TextBlocks
import java.awt.image.BufferedImage
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
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

    @Test
    fun aDenseTextPageSplitsQuickly() {
        val bytes = pdf { doc ->
            val page = PDPage(PDRectangle.A4)
            doc.addPage(page)
            PDPageContentStream(doc, page).use { cs ->
                cs.setFont(helvetica(), 6f)
                for (line in 0 until 100) {
                    cs.beginText()
                    cs.newLineAtOffset(20f, 820f - line * 8f)
                    for (word in 0 until 30) cs.showText("word$word ")
                    cs.endText()
                }
            }
        }
        fun font(name: String) = TrueTypeFont(File("src/commonMain/composeResources/files/fonts/$name.ttf").readBytes())
        val fonts = MarkFonts(
            DocFonts(
                body = FontFamily(font("ToolboxSerif-Regular"), font("ToolboxSerif-Bold"), font("ToolboxSerif-Italic"), font("ToolboxSerif-BoldItalic")),
                headings = FontFamily(font("ToolboxSans-Regular"), font("ToolboxSans-Bold"), font("ToolboxSans-Italic"), font("ToolboxSans-BoldItalic")),
                code = FontFamily(font("ToolboxMono-Regular"), font("ToolboxMono-Bold")),
            ),
        )
        val doc = PdfDocument.parse(bytes)
        fun millis(block: () -> Unit): Double {
            val started = System.nanoTime()
            block()
            return (System.nanoTime() - started) / 1e6
        }
        repeat(3) { TextBlocks.build(PageContent.of(doc.page(0)).runs) }
        lateinit var content: PageContent
        val parse = millis { content = PageContent.of(doc.page(0)) }
        val blocks = millis { TextBlocks.build(content.runs, content.graphics) }
        val glyphs = millis { PdfText.glyphs(doc, 0) }
        val edit = DocumentEdit(listOf(EditPage(1, SourcePage(doc.page(0)), objects = mapOf(content.runs[1500].op to ObjectEdit(removed = true)))))
        repeat(2) { PdfEditWriter.write(null, edit, fonts) }
        val rewrite = millis { PdfEditWriter.write(null, edit, fonts) }
        println("dense page, ${content.runs.size} runs: content $parse ms, paragraphs $blocks ms, glyphs $glyphs ms, rewritten page $rewrite ms")
        assertEquals(3000, content.runs.size)
        assertTrue(parse + blocks + glyphs + rewrite < 3000, "took ${parse + blocks + glyphs + rewrite} ms")
    }
}
