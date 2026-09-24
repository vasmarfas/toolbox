package com.vasmarfas.card.tools.documents

import com.vasmarfas.card.core.XmlBuilder
import kotlin.math.roundToInt

internal const val BASE_CSS =
    "body { font-family: Georgia, 'Times New Roman', serif; line-height: 1.5; margin: 0 auto; max-width: 48em; padding: 1em; }\n" +
        "h1, h2, h3, h4, h5, h6 { line-height: 1.25; }\n" +
        "img { max-width: 100%; height: auto; }\n" +
        "div.image { text-align: center; margin: 1em 0; }\n" +
        "table { border-collapse: collapse; margin: 1em 0; }\n" +
        "td, th { border: 1px solid #999999; padding: 0.25em 0.5em; vertical-align: top; }\n" +
        "blockquote { border-left: 3px solid #cccccc; margin-left: 0; padding-left: 1em; }\n" +
        "pre { background: #f4f4f4; padding: 0.5em; white-space: pre-wrap; }\n" +
        "code, pre { font-family: Consolas, 'Courier New', monospace; }\n"

internal object HtmlWriter {
    fun write(doc: Doc): String {
        val xml = XmlBuilder(declaration = false)
        val lang = doc.language?.trim()?.takeIf { it.isNotEmpty() }
        if (lang != null) xml.start("html", "lang" to lang) else xml.start("html")
        xml.text("\n").start("head").text("\n")
        xml.leaf("meta", null, "charset" to "utf-8").text("\n")
        xml.leaf("meta", null, "name" to "viewport", "content" to "width=device-width, initial-scale=1").text("\n")
        xml.leaf("title", documentTitle(doc)).text("\n")
        doc.author?.takeIf { it.isNotBlank() }?.let { xml.leaf("meta", null, "name" to "author", "content" to it).text("\n") }
        xml.leaf("style", "\n$BASE_CSS").text("\n")
        xml.end().text("\n").start("body").text("\n")
        HtmlBody(xml, image = ::dataUri).blocks(doc.blocks)
        xml.end().text("\n").end()
        return "<!DOCTYPE html>\n$xml\n"
    }

    private fun dataUri(picture: Block.Picture): String? {
        val type = ImageHeader.mimeType(picture.bytes) ?: return null
        return "data:$type;base64,${encodeBase64(picture.bytes)}"
    }
}

// HTML needs a non-empty title
internal fun documentTitle(doc: Doc): String =
    doc.title?.takeIf { it.isNotBlank() }
        ?: doc.blocks.firstNotNullOfOrNull { (it as? Block.Heading)?.content?.plainText()?.collapseSpaces()?.takeIf(String::isNotBlank) }
        ?: "Document"

// polyglot markup: explicit end tags and />, so the output is HTML5 and well-formed XHTML, EPUB relies
// on the latter
internal class HtmlBody(
    private val xml: XmlBuilder,
    private val image: (Block.Picture) -> String?,
    private val headingId: ((Block.Heading) -> String?)? = null,
    private val link: (String) -> String? = { uriSafe(it) },
) {
    private data class Tag(val name: String, val attribute: Pair<String, String>? = null)

    fun blocks(blocks: List<Block>) {
        for (block in blocks) {
            block(block)
            xml.text("\n")
        }
    }

    private fun block(block: Block) {
        when (block) {
            is Block.Heading -> {
                val name = "h${block.level.coerceIn(1, 6)}"
                val id = headingId?.invoke(block)
                if (id != null) xml.start(name, "id" to id) else xml.start(name)
                inlines(block.content)
                xml.text("").end()
            }
            is Block.Paragraph -> paragraph(block)
            is Block.ListBlock -> list(block)
            is Block.Quote -> {
                xml.start("blockquote").text("\n")
                blocks(block.blocks)
                xml.end()
            }
            is Block.Code -> xml.start("pre").start("code").text(block.text).end().end()
            is Block.Table -> table(block)
            is Block.Picture -> picture(block)
            Block.Rule -> xml.leaf("hr")
            Block.PageBreak -> xml.start("div", "style" to "page-break-after: always; break-after: page").text("").end()
        }
    }

    private fun paragraph(p: Block.Paragraph) {
        val align = alignCss(p.align)
        if (align != null) xml.start("p", "style" to "text-align: $align") else xml.start("p")
        inlines(p.content)
        xml.text("").end()
    }

    private fun flow(blocks: List<Block>) {
        val first = blocks.firstOrNull()
        if (first is Block.Paragraph && first.align == Align.START) {
            inlines(first.content)
            if (blocks.size > 1) xml.text("\n")
            blocks(blocks.drop(1))
        } else {
            if (blocks.isNotEmpty()) xml.text("\n")
            blocks(blocks)
        }
    }

    private fun list(list: Block.ListBlock) {
        if (list.ordered && list.start != 1) xml.start("ol", "start" to list.start.toString()) else xml.start(if (list.ordered) "ol" else "ul")
        xml.text("\n")
        for (item in list.items) {
            xml.start("li")
            flow(item)
            xml.text("").end().text("\n")
        }
        xml.end()
    }

    private fun table(table: Block.Table) {
        val rows = table.rows.filter { it.isNotEmpty() }
        val header = table.headerRows.coerceIn(0, rows.size)
        xml.start("table").text("\n")
        if (header > 0) section("thead", rows.subList(0, header), "th")
        if (header < rows.size) section("tbody", rows.subList(header, rows.size), "td")
        xml.end()
    }

    private fun section(name: String, rows: List<List<Cell>>, cellName: String) {
        xml.start(name).text("\n")
        for (row in rows) {
            xml.start("tr")
            for (cell in row) {
                if (cell.colSpan > 1) xml.start(cellName, "colspan" to cell.colSpan.toString()) else xml.start(cellName)
                flow(cell.blocks)
                xml.text("").end()
            }
            xml.end().text("\n")
        }
        xml.end().text("\n")
    }

    private fun picture(picture: Block.Picture) {
        val src = image(picture)
        if (src == null) {
            if (picture.alt.isNotBlank()) xml.leaf("p", picture.alt)
            return
        }
        val attributes = ArrayList<Pair<String, String>>()
        attributes.add("src" to src)
        attributes.add("alt" to picture.alt)
        if (picture.widthPt > 0f && picture.heightPt > 0f) {
            attributes.add("width" to maxOf(1, (picture.widthPt / 0.75f).roundToInt()).toString())
            attributes.add("height" to maxOf(1, (picture.heightPt / 0.75f).roundToInt()).toString())
        }
        xml.start("div", "class" to "image").leaf("img", null, *attributes.toTypedArray()).end()
    }

    private fun inlines(content: List<Inline>) {
        val open = ArrayList<Tag>()
        for (item in content) {
            when (item) {
                Inline.LineBreak -> xml.leaf("br")
                is Inline.Text -> {
                    if (item.text.isEmpty()) continue
                    val wanted = tags(item)
                    var common = 0
                    while (common < open.size && common < wanted.size && open[common] == wanted[common]) common++
                    while (open.size > common) {
                        xml.end()
                        open.removeAt(open.lastIndex)
                    }
                    for (tag in wanted.subList(common, wanted.size)) {
                        if (tag.attribute != null) xml.start(tag.name, tag.attribute) else xml.start(tag.name)
                        open.add(tag)
                    }
                    xml.text(item.text)
                }
            }
        }
        repeat(open.size) { xml.end() }
    }

    private fun tags(t: Inline.Text): List<Tag> = buildList {
        t.link?.let(link)?.let { add(Tag("a", "href" to it)) }
        if (t.bold) add(Tag("strong"))
        if (t.italic) add(Tag("em"))
        if (t.underline) add(Tag("u"))
        if (t.strike) add(Tag("s"))
        if (t.code) {
            val spaced = t.text.startsWith(' ') || t.text.endsWith(' ') || "  " in t.text || '\t' in t.text
            add(if (spaced) Tag("code", "style" to "white-space: pre-wrap") else Tag("code"))
        }
        when (t.script) {
            Script.SUPER -> add(Tag("sup"))
            Script.SUB -> add(Tag("sub"))
            Script.NORMAL -> {}
        }
    }

    private fun alignCss(align: Align): String? = when (align) {
        Align.START -> null
        Align.CENTER -> "center"
        Align.END -> "right"
        Align.JUSTIFY -> "justify"
    }
}
