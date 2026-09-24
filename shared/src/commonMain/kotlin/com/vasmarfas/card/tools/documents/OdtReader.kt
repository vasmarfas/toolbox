package com.vasmarfas.card.tools.documents

import com.vasmarfas.card.core.XmlElement
import com.vasmarfas.card.core.XmlText

private const val MAX_REPEAT = 1000
private val ODF_QUOTE_NAMES = setOf("quotations", "quote", "цитата", "цитаты")
private val ODF_CODE_NAME = Regex(".*(?:preformatted|code|source|verbatim|listing|исходный текст|код).*")

private class OdfStyle(val display: String, val parent: String?, val text: XmlElement?, val paragraph: XmlElement?)

private class ListLevel(val ordered: Boolean, val start: Int)

private class OdfParagraph(val heading: Int?, val kind: OdfKind, val align: Align?, val breakBefore: Boolean, val breakAfter: Boolean, val rule: Boolean, val run: RunProps)

private enum class OdfKind { NORMAL, CODE, QUOTE }

// _20_ and the other _xx_ escapes ODF uses in style names
private fun decodeStyleName(name: String): String = Regex("_([0-9a-fA-F]{2})_").replace(name) { it.groupValues[1].toInt(16).toChar().toString() }

internal class OdtReader(private val pkg: Package) {
    private val paragraphStyles = HashMap<String, OdfStyle>()
    private val textStyles = HashMap<String, OdfStyle>()
    private val listStyles = HashMap<String, Map<Int, ListLevel>>()
    private val monoFonts = HashSet<String>()
    private var defaultText: XmlElement? = null
    private val resolvedParagraphs = HashMap<String, OdfParagraph>()
    private val resolvedText = HashMap<String, RunProps>()
    private val lastNumber = HashMap<String, Int>()
    private val nesting = Nesting()
    private val notes = Notes()
    private var headerRows = 0

    fun read(): Doc {
        val content = pkg.xml("content.xml") ?: throw DocumentFormatException("Not an ODT: content.xml is missing")
        val styles = pkg.optionalXml("styles.xml")
        fonts(styles?.child("font-face-decls"))
        fonts(content.child("font-face-decls"))
        styles?.child("styles")?.let(::loadStyles)
        content.child("automatic-styles")?.let(::loadStyles)
        val text = content.child("body")?.child("text") ?: throw DocumentFormatException("Not an ODF text document")
        val blocks = ArrayList<Block>()
        blocks(text, blocks, 0, null)
        notes.appendTo(blocks)
        val meta = pkg.optionalXml("meta.xml")?.child("meta")
        val defaultLanguage = defaultText?.attr("language")?.takeIf { it != "none" }?.let { lang ->
            defaultText?.attr("country")?.takeIf { it != "none" }?.let { "$lang-$it" } ?: lang
        }
        return Doc(
            blocks = mergeRuns(blocks),
            title = meta?.child("title")?.text()?.collapseSpaces()?.takeIf { it.isNotEmpty() },
            author = (meta?.child("initial-creator") ?: meta?.child("creator"))?.text()?.collapseSpaces()?.takeIf { it.isNotEmpty() },
            language = meta?.child("language")?.text()?.trim()?.takeIf { it.isNotEmpty() } ?: defaultLanguage,
        )
    }

    private fun fonts(decls: XmlElement?) {
        for (face in decls?.children("font-face").orEmpty()) {
            val name = face.attr("name") ?: continue
            if (face.attr("font-pitch") == "fixed" || face.attr("font-family-generic") == "modern" || isMonospaceFont(face.attr("font-family") ?: name)) {
                monoFonts.add(name)
            }
        }
    }

    private fun loadStyles(container: XmlElement) {
        for (el in container.elements) {
            when (el.localName) {
                "style" -> {
                    val name = el.attr("name") ?: continue
                    val style = OdfStyle(
                        display = (el.attr("display-name") ?: decodeStyleName(name)).lowercase().trim(),
                        parent = el.attr("parent-style-name"),
                        text = el.child("text-properties"),
                        paragraph = el.child("paragraph-properties"),
                    )
                    when (el.attr("family")) {
                        "paragraph" -> paragraphStyles[name] = style
                        "text" -> textStyles[name] = style
                    }
                }
                "default-style" -> if (el.attr("family") == "paragraph") defaultText = el.child("text-properties")
                "list-style" -> {
                    val name = el.attr("name") ?: continue
                    val levels = HashMap<Int, ListLevel>()
                    for (level in el.elements) {
                        val n = level.attr("level")?.toIntOrNull() ?: continue
                        levels[n] = when (level.localName) {
                            "list-level-style-number" -> ListLevel(
                                ordered = !level.attr("num-format").isNullOrEmpty(),
                                start = level.attr("start-value")?.toIntOrNull() ?: 1,
                            )
                            else -> ListLevel(ordered = false, start = 1)
                        }
                    }
                    listStyles[name] = levels
                }
            }
        }
    }

    private fun chain(map: Map<String, OdfStyle>, name: String?): List<OdfStyle> {
        val result = ArrayList<OdfStyle>()
        var current = name
        val seen = HashSet<String>()
        while (current != null && seen.add(current) && result.size < 32) {
            val style = map[current] ?: break
            result.add(style)
            current = style.parent
        }
        return result
    }

    private fun RunProps.read(props: XmlElement?): RunProps {
        if (props == null) return this
        props.attr("font-weight")?.let { w -> bold = w == "bold" || (w.toIntOrNull() ?: 0) >= 600 }
        props.attr("font-style")?.let { italic = it == "italic" || it == "oblique" }
        (props.attr("text-underline-style") ?: props.attr("text-underline-type"))?.let { underline = it != "none" }
        (props.attr("text-line-through-style") ?: props.attr("text-line-through-type"))?.let { strike = it != "none" }
        props.attr("text-position")?.let { p ->
            val first = p.trim().substringBefore(' ')
            script = when {
                first == "super" -> Script.SUPER
                first == "sub" -> Script.SUB
                (first.removeSuffix("%").toFloatOrNull() ?: 0f) > 0f -> Script.SUPER
                (first.removeSuffix("%").toFloatOrNull() ?: 0f) < 0f -> Script.SUB
                else -> Script.NORMAL
            }
        }
        val font = props.attr("font-name") ?: props.attr("font-family")
        if (font != null) mono = font in monoFonts || isMonospaceFont(font)
        props.attr("display")?.let { hidden = it == "none" }
        return this
    }

    private fun paragraphInfo(name: String?): OdfParagraph = resolvedParagraphs.getOrPut(name.orEmpty()) {
        val chain = chain(paragraphStyles, name)
        val run = RunProps()
        for (s in chain.asReversed()) run.read(s.text)
        fun prop(key: String): String? = chain.firstNotNullOfOrNull { it.paragraph?.attr(key) }
        val heading = chain.firstNotNullOfOrNull {
            when (it.display) {
                "title" -> 1
                "subtitle" -> 2
                else -> null
            }
        }
        val border = (prop("border-bottom") ?: prop("border"))?.let { it != "none" } ?: false
        OdfParagraph(
            heading = heading,
            kind = when {
                chain.any { it.display in ODF_QUOTE_NAMES } -> OdfKind.QUOTE
                chain.isNotEmpty() && (chain.any { ODF_CODE_NAME.matches(it.display) } || run.mono == true) -> OdfKind.CODE
                else -> OdfKind.NORMAL
            },
            align = when (prop("text-align")) {
                "start", "left" -> Align.START
                "center" -> Align.CENTER
                "end", "right" -> Align.END
                "justify" -> Align.JUSTIFY
                else -> null
            },
            breakBefore = prop("break-before") == "page",
            breakAfter = prop("break-after") == "page",
            rule = border || chain.any { it.display == "horizontal line" },
            run = run,
        )
    }

    private fun textProps(name: String?): RunProps = resolvedText.getOrPut(name.orEmpty()) {
        val run = RunProps()
        for (s in chain(textStyles, name).asReversed()) run.read(s.text)
        run
    }

    private fun blocks(container: XmlElement, out: MutableList<Block>, listLevel: Int, listStyle: String?) {
        nesting.within {
            for (el in container.elements) {
                when (el.localName) {
                    "h" -> paragraph(el, out, el.attr("outline-level")?.toIntOrNull() ?: 1)
                    "p" -> paragraph(el, out, null)
                    "list" -> list(el, listLevel + 1, listStyle, out)
                    "numbered-paragraph" -> blocks(el, out, listLevel, listStyle)
                    "table" -> table(el)?.let(out::add)
                    "section", "index-body", "index-title" -> blocks(el, out, listLevel, listStyle)
                    "table-of-content", "alphabetical-index", "illustration-index", "table-index", "object-index", "user-index", "bibliography" ->
                        el.child("index-body")?.let { blocks(it, out, listLevel, listStyle) }
                    "frame", "a" -> {
                        val segments = Segments()
                        val boxes = ArrayList<Block>()
                        drawing(el, segments, boxes)
                        out.addAll(segments.blocks { Block.Paragraph(it) })
                        out.addAll(boxes)
                    }
                }
            }
        }
    }

    private fun paragraph(el: XmlElement, out: MutableList<Block>, outline: Int?) {
        val info = paragraphInfo(el.attr("style-name"))
        val level = outline ?: info.heading
        val base = RunProps().read(defaultText)
        if (level == null && info.kind == OdfKind.NORMAL && headerRows == 0) base.merge(info.run)
        val segments = Segments()
        val boxes = ArrayList<Block>()
        inlines(el, base, null, info.kind == OdfKind.CODE, segments, boxes)
        var blocks = when {
            level != null -> segments.heading(level)
            info.kind == OdfKind.CODE -> segments.code()
            info.kind == OdfKind.QUOTE -> segments.blocks { Block.Paragraph(it, info.align ?: Align.START) }.let { if (it.isEmpty()) it else listOf(Block.Quote(it)) }
            else -> segments.blocks { Block.Paragraph(it, info.align ?: Align.START) }
        }
        if (blocks.isEmpty() && info.rule && level == null) blocks = listOf(Block.Rule)
        if (info.breakBefore) out.add(Block.PageBreak)
        out.addAll(blocks)
        out.addAll(boxes)
        if (info.breakAfter) out.add(Block.PageBreak)
    }

    private fun inlines(el: XmlElement, props: RunProps, link: String?, code: Boolean, segments: Segments, boxes: MutableList<Block>) {
        nesting.within {
            val inline = segments.inline
            for (node in el.children) {
                if (node is XmlText) {
                    if (props.hidden != true) inline.collapsed(node.text, props.style(link))
                    continue
                }
                val child = node as XmlElement
                when (child.localName) {
                    "span" -> inlines(child, props.copy().merge(textProps(child.attr("style-name"))), link, code, segments, boxes)
                    "a" -> if (child.namespace.endsWith(":drawing:1.0")) {
                        drawing(child, segments, boxes)
                    } else {
                        inlines(child, props, externalLink(child.attr("href")) ?: link, code, segments, boxes)
                    }
                    "s" -> if (props.hidden != true) inline.preserved(" ".repeat((child.attr("c")?.toIntOrNull() ?: 1).coerceIn(1, MAX_REPEAT)), props.style(link))
                    "tab" -> if (props.hidden != true) inline.preserved(if (code) "\t" else " ", props.style(link))
                    "line-break" -> inline.lineBreak()
                    "note" -> child.child("note-body")?.let { body ->
                        val blocks = ArrayList<Block>()
                        blocks(body, blocks, 0, null)
                        inline.inline(notes.add(mergeRuns(blocks)))
                    }
                    "frame" -> drawing(child, segments, boxes)
                    "ruby" -> child.child("ruby-base")?.let { inlines(it, props, link, code, segments, boxes) }
                    "hidden-text" -> if (child.attr("is-hidden") != "true") inlines(child, props, link, code, segments, boxes)
                    "soft-page-break", "bookmark", "bookmark-start", "bookmark-end", "reference-mark", "reference-mark-start", "reference-mark-end",
                    "toc-mark", "toc-mark-start", "toc-mark-end", "alphabetical-index-mark", "alphabetical-index-mark-start",
                    "alphabetical-index-mark-end", "annotation", "annotation-end", "change", "change-start", "change-end", "note-citation",
                    -> {}
                    "custom-shape", "rect", "ellipse", "polygon", "path", "g", "text-box" -> blocks(child, boxes, 0, null)
                    else -> inlines(child, props, link, code, segments, boxes)
                }
            }
        }
    }

    private fun drawing(el: XmlElement, segments: Segments, boxes: MutableList<Block>) {
        if (el.localName == "a") {
            for (frame in el.children("frame")) drawing(frame, segments, boxes)
            return
        }
        val alt = (el.child("title")?.text()?.takeIf { it.isNotBlank() } ?: el.child("desc")?.text()).orEmpty().collapseSpaces()
        for (image in el.children("image")) {
            val bytes = image.attr("href")?.takeIf { externalLink(it) == null }?.let { pkg.optionalBytes(resolvePath("", percentDecode(it))) }
                ?: image.child("binary-data")?.text()?.let(::decodeBase64)
                ?: continue
            val (w, h) = pictureSize(bytes, lengthPt(el.attr("width"), "pt"), lengthPt(el.attr("height"), "pt"))
            segments.block(Block.Picture(bytes, w, h, alt))
            break
        }
        el.child("text-box")?.let { blocks(it, boxes, 0, null) }
    }

    private fun list(el: XmlElement, level: Int, inherited: String?, out: MutableList<Block>) {
        val style = el.attr("style-name") ?: inherited
        val info = style?.let { listStyles[it] }?.get(level)
        val ordered = info?.ordered ?: false
        val continues = el.attr("continue-numbering") == "true" || el.attr("continue-list") != null
        var start = info?.start ?: 1
        if (continues && style != null) lastNumber[style + ":" + level]?.let { start = it + 1 }
        val items = ArrayList<List<Block>>()
        for (item in el.elements) {
            if (item.localName != "list-item" && item.localName != "list-header") continue
            if (items.isEmpty()) item.attr("start-value")?.toIntOrNull()?.let { start = it }
            val blocks = ArrayList<Block>()
            blocks(item, blocks, level, style)
            items.add(blocks)
        }
        if (style != null && ordered) lastNumber[style + ":" + level] = start + items.size - 1
        if (items.any { it.firstOrNull() is Block.Heading } && items.all { it.isEmpty() || it[0] is Block.Heading }) {
            items.forEach(out::addAll)
            return
        }
        out.add(Block.ListBlock(ordered, items, start))
    }

    // spreadsheet-style padding, a repeated empty cell or row far past the declared columns, is cut off
    private fun table(el: XmlElement): Block.Table? {
        val rows = ArrayList<List<Cell>>()
        var header = 0
        val columns = el.elements.flatMap {
            when (it.localName) {
                "table-column" -> listOf(it)
                "table-columns", "table-header-columns", "table-column-group" -> it.children("table-column")
                else -> emptyList()
            }
        }
        val declared = columns.sumOf { (it.attr("number-columns-repeated")?.toIntOrNull() ?: 1).coerceIn(1, MAX_REPEAT) }

        fun row(tr: XmlElement, head: Boolean) {
            val cells = ArrayList<Cell>()
            var covered = 0
            for (c in tr.elements) {
                val repeat = (c.attr("number-columns-repeated")?.toIntOrNull() ?: 1).coerceIn(1, MAX_REPEAT)
                when (c.localName) {
                    "table-cell" -> {
                        val span = (c.attr("number-columns-spanned")?.toIntOrNull() ?: 1).coerceIn(1, MAX_COL_SPAN)
                        if (head) headerRows++
                        val blocks = ArrayList<Block>()
                        blocks(c, blocks, 0, null)
                        if (head) headerRows--
                        repeat(repeat) { cells.add(Cell(blocks, span)) }
                        covered = span - 1
                    }
                    "covered-table-cell" -> repeat(repeat) {
                        if (covered > 0) covered-- else cells.add(Cell(emptyList()))
                    }
                }
                if (cells.size > MAX_REPEAT) break
            }
            if (declared > 0) {
                var width = cells.sumOf { it.colSpan }
                while (width > declared && cells.isNotEmpty() && cells.last().blocks.isEmpty()) width -= cells.removeAt(cells.lastIndex).colSpan
            }
            val empty = cells.all { it.blocks.isEmpty() }
            val times = if (empty) 1 else (tr.attr("number-rows-repeated")?.toIntOrNull() ?: 1).coerceIn(1, MAX_REPEAT)
            repeat(times) {
                if (rows.size < 10 * MAX_REPEAT) {
                    rows.add(cells)
                    if (head) header++
                }
            }
        }

        fun rows(container: XmlElement, head: Boolean) {
            nesting.within {
                for (child in container.elements) {
                    when (child.localName) {
                        "table-header-rows" -> rows(child, true)
                        "table-rows", "table-row-group" -> rows(child, head)
                        "table-row" -> row(child, head)
                    }
                }
            }
        }

        rows(el, false)
        while (rows.isNotEmpty() && rows.last().isEmpty()) rows.removeAt(rows.lastIndex)
        if (rows.isEmpty()) return null
        return Block.Table(rows.map { if (it.isEmpty()) listOf(Cell(emptyList())) else it }, header.coerceAtMost(rows.size))
    }
}
