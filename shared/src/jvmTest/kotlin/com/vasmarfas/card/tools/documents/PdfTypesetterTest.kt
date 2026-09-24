package com.vasmarfas.card.tools.documents

import com.vasmarfas.card.tools.documents.pdf.PdfDocument
import com.vasmarfas.card.tools.documents.pdf.PdfText
import com.vasmarfas.card.tools.documents.pdf.TrueTypeFont
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PdfTypesetterTest {
    private fun font(name: String) = TrueTypeFont(File("src/commonMain/composeResources/files/fonts/$name.ttf").readBytes())

    private val fonts = DocFonts(
        body = FontFamily(font("ToolboxSerif-Regular"), font("ToolboxSerif-Bold"), font("ToolboxSerif-Italic"), font("ToolboxSerif-BoldItalic")),
        headings = FontFamily(font("ToolboxSans-Regular"), font("ToolboxSans-Bold"), font("ToolboxSans-Italic"), font("ToolboxSans-BoldItalic")),
        code = FontFamily(font("ToolboxMono-Regular"), font("ToolboxMono-Bold")),
    )

    private fun text(value: String) = Block.Paragraph(listOf(Inline.Text(value)))

    private fun jpeg(width: Int, height: Int): PreparedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until height) for (x in 0 until width) image.setRGB(x, y, (x * 255 / width shl 16) or (y * 255 / height shl 8) or 0x40)
        val out = ByteArrayOutputStream()
        ImageIO.write(image, "jpg", out)
        return PreparedImage(out.toByteArray(), width, height)
    }

    private val lorem = "Съешь же ещё этих мягких французских булок, да выпей чаю. The quick brown fox jumps over the lazy dog. "

    private fun sample(picture: Block.Picture): Doc = Doc(
        title = "Проверка вёрстки",
        author = "vasmarfas",
        language = "ru",
        blocks = listOf(
            Block.Heading(1, listOf(Inline.Text("Глава первая"))),
            Block.Paragraph(
                listOf(
                    Inline.Text("Обычный текст, "),
                    Inline.Text("жирный", bold = true),
                    Inline.Text(", "),
                    Inline.Text("курсив", italic = true),
                    Inline.Text(", ссылка на "),
                    Inline.Text("пример", link = "https://example.com/путь"),
                    Inline.Text(" и E = mc"),
                    Inline.Text("2", script = Script.SUPER),
                    Inline.Text(". " + lorem.repeat(6)),
                ),
                Align.JUSTIFY,
            ),
            Block.ListBlock(
                ordered = true,
                start = 3,
                items = listOf(
                    listOf(text("Третий пункт")),
                    listOf(text("Четвёртый пункт"), Block.ListBlock(ordered = false, items = listOf(listOf(text("вложенный")), listOf(text("ещё один"))))),
                ),
            ),
            Block.Quote(listOf(text("Цитата, которая " + lorem))),
            Block.Code("fun main() {\n    println(\"Привет\")\n}"),
            picture,
            Block.Heading(2, listOf(Inline.Text("Таблица"))),
            Block.Table(
                rows = listOf(listOf(Cell(listOf(text("Номер"))), Cell(listOf(text("Описание"))), Cell(listOf(text("Сумма"))))) +
                    (1..60).map { n -> listOf(Cell(listOf(text("$n"))), Cell(listOf(text("Строка номер $n, " + lorem.take(40 + n % 30))), colSpan = 1), Cell(listOf(text("${n * 125} ₽")))) },
                headerRows = 1,
            ),
            Block.PageBreak,
            Block.Heading(1, listOf(Inline.Text("Глава вторая"))),
            text("Отдельное слово: " + "длинноесловобезпробелов".repeat(8)),
            Block.Rule,
            text(lorem),
        ),
    )

    @Test
    fun documentSetsIntoAValidSearchablePdf() {
        val picture = Block.Picture(ByteArray(0), alt = "градиент")
        val pdf = PdfTypesetter.typeset(sample(picture), fonts, mapOf(picture to jpeg(400, 240)))
        Loader.loadPDF(pdf).use { doc ->
            assertTrue(doc.numberOfPages >= 3, "${doc.numberOfPages} pages")
            val text = PDFTextStripper().getText(doc)
            for (needle in listOf("Глава первая", "жирный", "французских", "Третий пункт", "вложенный", "Привет", "Строка номер 60", "Глава вторая", "125 ₽")) {
                assertTrue(needle in text, "missing \"$needle\"")
            }
            assertEquals("Проверка вёрстки", doc.documentInformation.title)
            val outline = doc.documentCatalog.documentOutline
            assertEquals(listOf("Глава первая", "Глава вторая"), outline.children().map { it.title })
            assertEquals(listOf("Таблица"), outline.children().first().children().map { it.title })
            val link = doc.getPage(0).annotations.filterIsInstance<PDAnnotationLink>().single()
            assertEquals("https://example.com/%D0%BF%D1%83%D1%82%D1%8C", (link.action as PDActionURI).uri)
            doc.pages.forEach { page -> page.resources.fontNames.forEach { assertTrue(page.resources.getFont(it).isEmbedded) } }
            val renderer = PDFRenderer(doc)
            for (i in 0 until doc.numberOfPages) renderer.renderImageWithDPI(i, 36f)
        }
        val parsed = PdfDocument.parse(pdf)
        assertTrue("Съешь же ещё" in PdfText.extract(parsed, 0))
    }

    @Test
    fun tableHeaderRepeatsOnEveryPageItSpans() {
        val picture = Block.Picture(ByteArray(0))
        Loader.loadPDF(PdfTypesetter.typeset(sample(picture), fonts, emptyMap())).use { doc ->
            val pages = (1..doc.numberOfPages).map { n -> PDFTextStripper().apply { startPage = n; endPage = n }.getText(doc) }
            val withRows = pages.filter { "Строка номер" in it }
            assertTrue(withRows.size >= 2, "the table should span pages")
            withRows.drop(1).forEach { assertTrue("Описание" in it, "header missing on a continued page") }
        }
    }
}
