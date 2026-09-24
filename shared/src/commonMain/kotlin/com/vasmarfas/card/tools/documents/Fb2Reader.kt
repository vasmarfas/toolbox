package com.vasmarfas.card.tools.documents

import com.vasmarfas.card.core.TextDecoding
import com.vasmarfas.card.core.XmlElement
import com.vasmarfas.card.core.XmlText
import com.vasmarfas.card.core.parseXml

private val NOTE_BODIES = setOf("notes", "comments", "footnotes")
private val DINKUS = Regex("[\\s*\\-_~#=·•—–]+")

// elements are matched by local name, files use whatever prefixes they like
internal object Fb2Reader {
    fun read(bytes: ByteArray): Doc {
        val xml = if (bytes.size > 4 && bytes[0].toInt() == 'P'.code && bytes[1].toInt() == 'K'.code) {
            val pkg = Package(bytes)
            val name = pkg.names.firstOrNull { it.lowercase().endsWith(".fb2") } ?: throw DocumentFormatException("The archive holds no .fb2 file")
            pkg.read(name) ?: throw DocumentFormatException("The archive holds no .fb2 file")
        } else {
            bytes
        }
        val root = parseXml(TextDecoding.decode(xml))
        if (root.localName != "FictionBook") throw DocumentFormatException("Not a FictionBook document")
        return Fb2Parser(root).read()
    }
}

private class Fb2Parser(private val root: XmlElement) {
    private val binaries = HashMap<String, XmlElement>()
    private val images = HashMap<String, ByteArray?>()
    private val noteSections = LinkedHashMap<String, XmlElement>()
    private val noteIndex = HashMap<String, Int>()
    private val notes = Notes()
    private val nesting = Nesting()

    fun read(): Doc {
        for (b in root.children("binary")) b.attr("id")?.let { binaries[it] = b }
        val bodies = root.children("body")
        val main = bodies.firstOrNull { it.attr("name")?.lowercase() !in NOTE_BODIES } ?: bodies.firstOrNull()
        for (body in bodies) {
            if (body === main) continue
            for (section in body.descendants("section")) section.attr("id")?.let { noteSections.getOrPut(it) { section } }
        }
        val info = root.child("description")?.child("title-info")
        val blocks = ArrayList<Block>()
        info?.child("coverpage")?.children("image")?.firstOrNull()?.let { image(it)?.let(blocks::add) }
        if (main != null) body(main, blocks)
        for (id in noteSections.keys) if (id !in noteIndex) note(id)
        notes.appendTo(blocks)
        val authors = info?.children("author").orEmpty().map { a ->
            val parts = listOf("first-name", "middle-name", "last-name").mapNotNull { a.child(it)?.text()?.collapseSpaces()?.takeIf(String::isNotEmpty) }
            if (parts.isEmpty()) a.child("nickname")?.text()?.collapseSpaces().orEmpty() else parts.joinToString(" ")
        }.filter { it.isNotEmpty() }
        return Doc(
            blocks = blocks,
            title = info?.child("book-title")?.text()?.collapseSpaces()?.takeIf { it.isNotEmpty() },
            author = authors.takeIf { it.isNotEmpty() }?.joinToString(", "),
            language = info?.child("lang")?.text()?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    private fun body(body: XmlElement, out: MutableList<Block>) {
        for (child in body.elements) {
            when (child.localName) {
                "image" -> image(child)?.let(out::add)
                "title" -> title(child, 1, out)
                "epigraph" -> out.add(Block.Quote(container(child, 1)))
                "section" -> section(child, 1, out)
            }
        }
    }

    private fun section(section: XmlElement, depth: Int, out: MutableList<Block>) {
        nesting.within {
            for (child in section.elements) {
                when (child.localName) {
                    "title" -> title(child, depth, out)
                    "section" -> section(child, depth + 1, out)
                    else -> content(child, depth, out)
                }
            }
        }
    }

    private fun container(el: XmlElement, depth: Int): List<Block> {
        val out = ArrayList<Block>()
        nesting.within {
            for (child in el.elements) content(child, depth, out)
        }
        return out
    }

    private fun content(el: XmlElement, depth: Int, out: MutableList<Block>) {
        when (el.localName) {
            "p" -> paragraph(el, Align.START, out)
            "subtitle" -> {
                val text = el.text()
                if (text.isNotBlank() && DINKUS.matches(text)) {
                    out.add(Block.Rule)
                } else {
                    val segments = Segments()
                    inlines(el, PLAIN.copy(bold = true), segments)
                    out.addAll(segments.blocks { Block.Paragraph(it, Align.CENTER) })
                }
            }
            "poem" -> poem(el, depth, out)
            "cite", "epigraph" -> out.add(Block.Quote(container(el, depth)))
            "table" -> table(el)?.let(out::add)
            "image" -> image(el)?.let(out::add)
            "text-author", "date" -> paragraph(el, Align.END, out)
            "annotation" -> out.addAll(container(el, depth))
            "section" -> section(el, depth + 1, out)
            "title" -> title(el, depth, out)
        }
    }

    private fun title(title: XmlElement, depth: Int, out: MutableList<Block>) {
        val segments = Segments()
        var first = true
        for (p in title.elements) {
            if (p.localName != "p") continue
            if (!first) segments.inline.lineBreak()
            inlines(p, PLAIN, segments)
            first = false
        }
        out.addAll(segments.heading(depth))
    }

    private fun paragraph(p: XmlElement, align: Align, out: MutableList<Block>) {
        val code = p.elements.singleOrNull()?.takeIf { it.localName == "code" && p.children.all { n -> n !is XmlText || n.text.isBlank() } }
        if (code != null && align == Align.START) {
            val line = code.text().replace(Char(0xA0), ' ')
            val last = out.lastOrNull()
            if (last is Block.Code) out[out.lastIndex] = Block.Code(last.text + "\n" + line) else out.add(Block.Code(line))
            return
        }
        val segments = Segments()
        inlines(p, PLAIN, segments)
        out.addAll(segments.blocks { Block.Paragraph(it, align) })
    }

    private fun poem(poem: XmlElement, depth: Int, out: MutableList<Block>) {
        for (child in poem.elements) {
            when (child.localName) {
                "title" -> for (p in child.children("p")) {
                    val segments = Segments()
                    inlines(p, PLAIN.copy(bold = true), segments)
                    out.addAll(segments.blocks { Block.Paragraph(it) })
                }
                "epigraph" -> out.add(Block.Quote(container(child, depth)))
                "stanza" -> {
                    val segments = Segments()
                    var first = true
                    for (line in child.elements) {
                        when (line.localName) {
                            "v" -> {
                                if (!first) segments.inline.lineBreak()
                                inlines(line, PLAIN, segments)
                                first = false
                            }
                            "title", "subtitle" -> content(line, depth, out)
                        }
                    }
                    out.addAll(segments.blocks { Block.Paragraph(it) })
                }
                "text-author", "date" -> paragraph(child, Align.END, out)
            }
        }
    }

    private fun inlines(el: XmlElement, style: Inline.Text, segments: Segments) {
        nesting.within {
            for (node in el.children) {
                if (node is XmlText) {
                    segments.inline.collapsed(node.text, style)
                    continue
                }
                val child = node as XmlElement
                when (child.localName) {
                    "strong" -> inlines(child, style.copy(bold = true), segments)
                    "emphasis" -> inlines(child, style.copy(italic = true), segments)
                    "strikethrough" -> inlines(child, style.copy(strike = true), segments)
                    "sub" -> inlines(child, style.copy(script = Script.SUB), segments)
                    "sup" -> inlines(child, style.copy(script = Script.SUPER), segments)
                    "code" -> inlines(child, style.copy(code = true), segments)
                    "image" -> image(child)?.let(segments::block)
                    "a" -> link(child, style, segments)
                    else -> inlines(child, style, segments)
                }
            }
        }
    }

    private fun link(a: XmlElement, style: Inline.Text, segments: Segments) {
        val href = a.attr("href")?.trim().orEmpty()
        val id = href.removePrefix("#").takeIf { href.startsWith("#") }
        if (id != null && (a.attr("type") == "note" || id in noteSections)) {
            segments.inline.inline(notes.marker(note(id)))
            return
        }
        inlines(a, externalLink(href)?.let { style.copy(link = it) } ?: style, segments)
    }

    // the index is taken before the text is read, so a note that refers to itself gets a number, not a loop
    private fun note(id: String): Int {
        noteIndex[id]?.let { return it }
        val index = notes.reserve()
        noteIndex[id] = index
        val section = noteSections[id] ?: return index
        val blocks = ArrayList<Block>()
        nesting.within {
            for (child in section.elements) if (child.localName != "title") content(child, 1, blocks)
        }
        notes.fill(index, blocks)
        return index
    }

    private fun table(table: XmlElement): Block.Table? {
        val grid = TableGrid()
        var header = 0
        var leading = true
        for (tr in table.children("tr")) {
            val cells = ArrayList<Cell>()
            val spans = ArrayList<Int>()
            var allTh = true
            for (cell in tr.elements) {
                if (cell.localName != "td" && cell.localName != "th") continue
                if (cell.localName != "th") allTh = false
                val align = when (cell.attr("align")) {
                    "center" -> Align.CENTER
                    "right" -> Align.END
                    else -> Align.START
                }
                val segments = Segments()
                inlines(cell, PLAIN, segments)
                cells.add(Cell(segments.blocks { Block.Paragraph(it, align) }, (cell.attr("colspan")?.toIntOrNull() ?: 1).coerceIn(1, MAX_COL_SPAN)))
                spans.add(cell.attr("rowspan")?.toIntOrNull() ?: 1)
            }
            if (cells.isEmpty()) continue
            if (leading && allTh) header++ else leading = false
            grid.row(cells, spans)
        }
        return if (grid.rowCount == 0) null else grid.build(header)
    }

    private fun image(el: XmlElement): Block.Picture? {
        val href = el.attr("href")?.trim() ?: return null
        if (!href.startsWith("#")) return null
        val id = href.substring(1)
        val bytes = images.getOrPut(id) { binaries[id]?.let { decodeBase64(it.text()) } } ?: return null
        return Block.Picture(bytes, alt = (el.attr("alt") ?: el.attr("title")).orEmpty().collapseSpaces())
    }
}
