package com.vasmarfas.card.tools.documents

import kotlin.test.Test
import kotlin.test.assertEquals

class RoundTripTest {
    private val nbsp = Char(0xA0)

    @Test
    fun docx() = roundTrip(DocFormat.DOCX, Samples.rich())

    @Test
    fun odt() = roundTrip(DocFormat.ODT, Samples.rich())

    @Test
    fun epub() = roundTrip(DocFormat.EPUB, Samples.rich())

    @Test
    fun html() = roundTrip(DocFormat.HTML, Samples.rich())

    @Test
    fun markdown() = roundTrip(DocFormat.MARKDOWN, Samples.rich(), markdownView(Samples.rich()), sizes = false)

    @Test
    fun fb2() = roundTrip(DocFormat.FB2, Samples.rich(), fb2View(Samples.rich()), sizes = false)

    @Test
    fun txtKeepsTheText() {
        val doc = Samples.rich()
        val back = Documents.read(Documents.write(doc, DocFormat.TXT), DocFormat.TXT)
        val markers = Regex("\\d+\\.")
        val text = blocksText(back.blocks).replace("[Картинка]", "Картинка").replace("* * *", "")
        assertEquals(words(blocksText(doc.blocks)), words(text).filter { !markers.matches(it) })
        assertEquals(1, back.blocks.count { it == Block.PageBreak })
    }

    @Test
    fun structuresInsideListItems() {
        val doc = Doc(
            listOf(
                ol(
                    2,
                    item(p("первый абзац пункта"), p("второй абзац того же пункта"), Block.Code("x = 1\n\ty = 2")),
                    item(p("пункт с таблицей"), table(0, row(cell(p("a")), cell(p("b"))))),
                    item(p("пункт с вложенным"), ul(item(p("a")), item(p("b"), ul(item(p("глубже")))))),
                    item(p("после вложенного")),
                ),
            ),
        )
        roundTrip(DocFormat.DOCX, doc)
        roundTrip(DocFormat.HTML, doc)
        roundTrip(DocFormat.EPUB, doc)
        val markdown = Doc(
            listOf(
                ol(
                    2,
                    item(p("первый абзац пункта"), p("второй абзац того же пункта"), Block.Code("x = 1\n\ty = 2")),
                    item(p("пункт с таблицей"), table(1, row(cell(p("a")), cell(p("b"))))),
                    item(p("пункт с вложенным"), ul(item(p("a")), item(p("b"), ul(item(p("глубже")))))),
                    item(p("после вложенного")),
                ),
            ),
        )
        roundTrip(DocFormat.MARKDOWN, doc, markdown)
        val odt = Doc(
            listOf(
                ol(
                    2,
                    item(p("первый абзац пункта"), p("второй абзац того же пункта"), Block.Code("x = 1\n\ty = 2")),
                    item(p("пункт с таблицей")),
                    item(p("пункт с вложенным"), ul(item(p("a")), item(p("b"), ul(item(p("глубже")))))),
                    item(p("после вложенного")),
                ),
                table(0, row(cell(p("a")), cell(p("b")))),
            ),
        )
        roundTrip(DocFormat.ODT, doc, odt)
    }

    @Test
    fun emptyItemsAndCells() {
        val doc = Doc(listOf(ul(item(), item(p("x"))), table(1, row(cell(), cell(p("b"))), row(cell(p("c")), cell()))))
        for (format in listOf(DocFormat.HTML, DocFormat.EPUB, DocFormat.MARKDOWN, DocFormat.DOCX, DocFormat.ODT)) roundTrip(format, doc)
    }

    @Test
    fun nestedQuotes() {
        val doc = Doc(listOf(Block.Quote(listOf(p("внешняя"), Block.Quote(listOf(p("внутренняя"))), p("снова внешняя")))))
        roundTrip(DocFormat.HTML, doc)
        roundTrip(DocFormat.EPUB, doc)
        roundTrip(DocFormat.MARKDOWN, doc)
        val flat = Doc(listOf(Block.Quote(listOf(p("внешняя"), p("внутренняя"), p("снова внешняя")))))
        roundTrip(DocFormat.DOCX, doc, flat)
        roundTrip(DocFormat.ODT, doc, flat)
        roundTrip(DocFormat.FB2, doc, flat)
    }

    @Test
    fun headingLevels() {
        val doc = Doc((1..6).map { h(it, "Уровень $it") } + listOf(p("текст"), h(2, "Снова второй"), h(4, "Через уровень"), p("конец")))
        for (format in listOf(DocFormat.HTML, DocFormat.EPUB, DocFormat.MARKDOWN, DocFormat.DOCX, DocFormat.ODT, DocFormat.FB2)) roundTrip(format, doc)
    }

    @Test
    fun pictureFormats() {
        val jpeg = TestImages.jpeg(64, 48)
        val gif = TestImages.gif(10, 10)
        val webp = TestImages.webpLossless(5, 5)
        val doc = Doc(
            listOf(
                Block.Picture(Samples.png, alt = "png без размера"),
                Block.Picture(jpeg, 48f, 36f, "jpeg"),
                Block.Picture(gif, 7.5f, 7.5f, "gif"),
                Block.Picture(webp, 10f, 10f, "webp"),
                Block.Picture(Samples.png, 30f, 22.5f, "та же картинка ещё раз"),
            ),
        )
        roundTrip(DocFormat.HTML, doc, sizes = false)
        roundTrip(DocFormat.MARKDOWN, doc, sizes = false)
        val docxBack = Documents.read(Documents.write(doc, DocFormat.DOCX), DocFormat.DOCX)
        assertEquals(
            listOf("png без размера", "jpeg", "gif", "webp", "та же картинка ещё раз"),
            docxBack.blocks.map { (it as? Block.Picture)?.alt ?: (it as Block.Paragraph).content.plainText() },
        )
        val first = docxBack.blocks[0] as Block.Picture
        assertEquals(30f, first.widthPt)
        assertEquals(22.5f, first.heightPt)
        val epubBack = Documents.read(Documents.write(doc, DocFormat.EPUB), DocFormat.EPUB)
        assertEquals(4, epubBack.blocks.count { it is Block.Picture })
        assertEquals("webp", (epubBack.blocks[3] as Block.Paragraph).content.plainText())
        val fb2Back = Documents.read(Documents.write(doc, DocFormat.FB2), DocFormat.FB2)
        assertEquals(listOf(true, true, false, false, true), fb2Back.blocks.map { it is Block.Picture })
    }

    @Test
    fun linksWithAwkwardCharacters() {
        val doc = Doc(
            listOf(
                p(a("пробелы и скобки", "https://example.com/a b/(c)"), t(" "), a("mailto", "mailto:someone@example.com")),
                p(a("https://example.com/plain", "https://example.com/plain")),
            ),
        )
        val encoded = Doc(
            listOf(
                p(a("пробелы и скобки", "https://example.com/a%20b/(c)"), t(" "), a("mailto", "mailto:someone@example.com")),
                p(a("https://example.com/plain", "https://example.com/plain")),
            ),
        )
        roundTrip(DocFormat.HTML, doc, encoded)
        roundTrip(DocFormat.MARKDOWN, doc, encoded)
        roundTrip(DocFormat.DOCX, doc, encoded)
        roundTrip(DocFormat.ODT, doc, encoded)
        roundTrip(DocFormat.EPUB, doc, encoded)
        roundTrip(DocFormat.FB2, doc, encoded)
    }

    @Test
    fun markdownEscaping() {
        val tricky = listOf(
            "* not a list", "- not a list", "+ not a list", "1. not a list", "12) not a list", "# not a heading", "> not a quote",
            "=== not a heading", "--- not a rule", "a_b_c snake_case", "_under_ *star* **strong**", "`tick` ``double``",
            "[not](a link) ![not](an image)", "<not a tag> & &amp; &copy;", "http://example.com not a link", "www.example.com also not",
            "back\\slash \\* and pipe |", "trailing hash #", "~~not struck~~ ~single~",
        )
        val doc = Doc(tricky.map { p(it) } + h(2, "Heading ends with #"))
        roundTrip(DocFormat.MARKDOWN, doc)
    }

    @Test
    fun markdownEmphasisCombinations() {
        val doc = Doc(
            listOf(
                p(b("bold"), i("italic"), t(" plain")),
                p(t("word"), b("(bold)"), t("word")),
                p(i("a"), b("b"), i("c")),
                p(b("жирный "), Inline.Text("и курсив", bold = true, italic = true), b(" снова")),
                p(Inline.Text("всё", bold = true, italic = true, strike = true, underline = true)),
                p(c("code with ` tick"), t(" and "), c(" spaced "), t(" and "), b("bold code:"), Inline.Text("x", bold = true, code = true)),
                p(a("link ", "https://a.example"), a("same", "https://a.example"), t(" "), Inline.Text("bold link", bold = true, link = "https://b.example")),
                p(t("H"), sub("2"), t("O "), sup("[1]"), t(" x"), sup("2"), sub("i")),
                p(t("line one"), br, b("bold line two"), br, t("# three")),
            ),
        )
        roundTrip(DocFormat.MARKDOWN, doc)
        roundTrip(DocFormat.HTML, doc)
    }

    @Test
    fun adjacentListsStaySeparate() {
        val doc = Doc(listOf(ul(item(p("a"))), ul(item(p("b"))), ol(1, item(p("c"))), ol(1, item(p("d")))))
        for (format in listOf(DocFormat.HTML, DocFormat.MARKDOWN, DocFormat.DOCX, DocFormat.ODT, DocFormat.EPUB)) roundTrip(format, doc)
    }

    @Test
    fun codeBlocks() {
        val doc = Doc(
            listOf(
                Block.Code("line one\n\n    indented after an empty line\n\ttab\n```\nfence inside"),
                p("между"),
                Block.Code("  leading spaces kept"),
            ),
        )
        for (format in listOf(DocFormat.HTML, DocFormat.MARKDOWN, DocFormat.DOCX, DocFormat.ODT, DocFormat.EPUB)) roundTrip(format, doc)
        val fb2 = Documents.read(Documents.write(doc, DocFormat.FB2), DocFormat.FB2)
        assertEquals("line one\n \n    indented after an empty line\n\ttab\n```\nfence inside", (fb2.blocks[0] as Block.Code).text.replace(nbsp, ' '))
    }

    @Test
    fun emptyDocument() {
        val doc = Doc(emptyList())
        for (format in DocFormat.entries.filter { it.writable }) {
            val back = Documents.read(Documents.write(doc, format), format)
            assertEquals(emptyList(), back.blocks.filter { it != Block.PageBreak }, format.name)
        }
    }

    private fun roundTrip(format: DocFormat, doc: Doc, expected: Doc = doc, sizes: Boolean = true) {
        val back = Documents.read(Documents.write(doc, format), format)
        assertSameBlocks(expected.blocks.let { if (format == DocFormat.EPUB) epubView(it) else it }, back.blocks, "$format first pass", sizes)
        if (doc.title != null) assertEquals(doc.title, back.title, "$format title")
        if (doc.author != null && format != DocFormat.TXT) assertEquals(doc.author, back.author, "$format author")
        if (doc.language != null && format != DocFormat.TXT) assertEquals(doc.language, back.language, "$format language")
        val again = Documents.read(Documents.write(back, format), format)
        assertSameBlocks(back.blocks, again.blocks, "$format second pass", sizes)
    }

    private fun assertSameBlocks(expected: List<Block>, actual: List<Block>, message: String, sizes: Boolean) {
        assertEquals(dumpBlocks(expected, sizes), dumpBlocks(actual, sizes), message)
    }

    private fun epubView(blocks: List<Block>): List<Block> {
        val out = ArrayList<Block>()
        for (b in blocks) {
            if (b is Block.Heading && b.level == 1 && out.isNotEmpty() && out.last() != Block.PageBreak) out.add(Block.PageBreak)
            out.add(b)
        }
        while (out.lastOrNull() == Block.PageBreak) out.removeAt(out.lastIndex)
        return out
    }

    private fun markdownView(doc: Doc): Doc = Doc(
        doc.blocks.map { b ->
            when (b) {
                is Block.Paragraph -> Block.Paragraph(b.content)
                is Block.Table -> Block.Table(
                    b.rows.map { row -> row.flatMap { cell -> listOf(Cell(cell.blocks)) + List(cell.colSpan - 1) { Cell(emptyList()) } } },
                    b.headerRows,
                )
                else -> b
            }
        },
        doc.title,
        doc.author,
        doc.language,
    )

    private fun fb2View(doc: Doc): Doc {
        fun inlines(content: List<Inline>) = content.map { if (it is Inline.Text) it.copy(underline = false) else it }
        fun blocks(list: List<Block>, indent: String): List<Block> = list.flatMap { b ->
            when (b) {
                is Block.Paragraph -> listOf(Block.Paragraph(listOf(t(indent)) + inlines(b.content)))
                is Block.ListBlock -> b.items.flatMapIndexed { k, item ->
                    val marker = if (b.ordered) "${b.start + k}. " else "• "
                    val first = item.first() as Block.Paragraph
                    listOf(Block.Paragraph(listOf(t(indent + marker)) + inlines(first.content))) + blocks(item.drop(1), indent + "$nbsp$nbsp")
                }
                Block.PageBreak -> emptyList()
                else -> listOf(b)
            }
        }
        return Doc(blocks(doc.blocks, ""), doc.title, doc.author, doc.language)
    }

    private fun words(text: String): List<String> = text.split(' ', '\n', '\t', nbsp, '•', '\u000C').filter { it.isNotBlank() }
}
