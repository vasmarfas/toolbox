package com.vasmarfas.card.tools.documents

import com.vasmarfas.card.core.XmlBuilder
import com.vasmarfas.card.core.ZipWriter
import com.vasmarfas.card.core.fmt

private const val ODF_VERSION = "1.3"
private const val ODT_TEXT_WIDTH_PT = 17f / 2.54f * 72f

private val ODF_NAMESPACES = listOf(
    "xmlns:office" to "urn:oasis:names:tc:opendocument:xmlns:office:1.0",
    "xmlns:style" to "urn:oasis:names:tc:opendocument:xmlns:style:1.0",
    "xmlns:text" to "urn:oasis:names:tc:opendocument:xmlns:text:1.0",
    "xmlns:table" to "urn:oasis:names:tc:opendocument:xmlns:table:1.0",
    "xmlns:draw" to "urn:oasis:names:tc:opendocument:xmlns:drawing:1.0",
    "xmlns:fo" to "urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0",
    "xmlns:xlink" to "http://www.w3.org/1999/xlink",
    "xmlns:dc" to "http://purl.org/dc/elements/1.1/",
    "xmlns:meta" to "urn:oasis:names:tc:opendocument:xmlns:meta:1.0",
    "xmlns:svg" to "urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0",
)

private val ODT_IMAGES = mapOf(
    "image/png" to "png", "image/jpeg" to "jpg", "image/gif" to "gif", "image/bmp" to "bmp", "image/tiff" to "tif",
    "image/svg+xml" to "svg", "image/emf" to "emf", "image/wmf" to "wmf", "image/webp" to "webp",
)
private val ODT_HEADING_SIZES = listOf("130%", "115%", "101%", "95%", "85%", "85%")

// ODF allows no table inside a list item, such tables go right after the outermost list
internal class OdtWriter(private val doc: Doc) {
    private val body = XmlBuilder(declaration = false)
    private val paragraphStyles = LinkedHashMap<String, String>()
    private val textStyles = LinkedHashMap<String, String>()
    private val pictures = ArrayList<Pair<String, ByteArray>>()
    private val deferredTables = ArrayList<Block.Table>()
    private var pendingBreak = false
    private var listDepth = 0
    private var tables = 0
    private var frames = 0
    private var spaceBefore = true

    fun write(): ByteArray {
        body.start("office:body").start("office:text")
        blocks(doc.blocks, quote = false)
        body.end().end()
        val content = XmlBuilder().start("office:document-content", *ODF_NAMESPACES.toTypedArray(), "office:version" to ODF_VERSION).toString()
        val prefix = content.removeSuffix("/>") + ">"
        val contentXml = prefix + automaticStyles() + body.toString() + "</office:document-content>"
        val zip = ZipWriter()
        zip.add("mimetype", "application/vnd.oasis.opendocument.text".encodeToByteArray(), compress = false)
        zip.add("META-INF/manifest.xml", manifest().encodeToByteArray())
        zip.add("content.xml", contentXml.encodeToByteArray())
        zip.add("styles.xml", styles().encodeToByteArray())
        zip.add("meta.xml", meta().encodeToByteArray())
        for ((name, bytes) in pictures) zip.add(name, bytes, compress = false)
        return zip.toByteArray()
    }

    private fun blocks(blocks: List<Block>, quote: Boolean) {
        for (block in blocks) block(block, quote)
    }

    private fun block(block: Block, quote: Boolean) {
        when (block) {
            is Block.Heading -> {
                val level = block.level.coerceIn(1, 6)
                body.start("text:h", "text:style-name" to paragraphStyle("Heading_20_$level", Align.START), "text:outline-level" to level.toString())
                inlines(block.content)
                body.end()
            }
            is Block.Paragraph -> {
                body.start("text:p", "text:style-name" to paragraphStyle(if (quote) "Quotations" else "Text_20_body", block.align))
                inlines(block.content)
                body.end()
            }
            is Block.ListBlock -> list(block, 1)
            is Block.Quote -> blocks(block.blocks, quote = true)
            is Block.Code -> {
                body.start("text:p", "text:style-name" to paragraphStyle("Preformatted_20_Text", Align.START))
                spaceBefore = true
                text(block.text)
                body.end()
            }
            is Block.Table -> if (listDepth > 0) deferredTables.add(block) else table(block)
            is Block.Picture -> picture(block, quote)
            Block.Rule -> body.start("text:p", "text:style-name" to paragraphStyle("Horizontal_20_Line", Align.START)).end()
            Block.PageBreak -> pendingBreak = true
        }
    }

    private fun list(list: Block.ListBlock, level: Int) {
        body.start("text:list", "text:style-name" to if (list.ordered) "L2" else "L1")
        listDepth++
        list.items.forEachIndexed { i, item ->
            if (i == 0 && list.ordered && list.start != 1) body.start("text:list-item", "text:start-value" to list.start.toString()) else body.start("text:list-item")
            if (item.isEmpty()) body.start("text:p", "text:style-name" to paragraphStyle("Text_20_body", Align.START)).end()
            for (b in item) if (b is Block.ListBlock) list(b, level + 1) else block(b, quote = false)
            body.end()
        }
        listDepth--
        body.end()
        if (listDepth == 0) {
            val tables = deferredTables.toList()
            deferredTables.clear()
            tables.forEach(::table)
        }
    }

    private fun table(table: Block.Table) {
        val rows = table.rows.filter { it.isNotEmpty() }
        if (rows.isEmpty()) return
        val columns = rows.maxOf { row -> row.sumOf { it.colSpan } }
        val name = "Table${++tables}"
        body.start("table:table", "table:name" to name, "table:style-name" to if (takeBreak()) "TableBreak" else "Table")
        body.leaf("table:table-column", null, "table:number-columns-repeated" to columns.toString())
        val header = table.headerRows.coerceIn(0, rows.size)
        if (header > 0) {
            body.start("table:table-header-rows")
            rows.subList(0, header).forEach { row(it, columns, "Table_20_Heading") }
            body.end()
        }
        rows.subList(header, rows.size).forEach { row(it, columns, "Table_20_Contents") }
        body.end()
    }

    private fun row(row: List<Cell>, columns: Int, paragraphStyle: String) {
        body.start("table:table-row")
        var used = 0
        for (cell in row) {
            if (cell.colSpan > 1) {
                body.start("table:table-cell", "table:style-name" to "TableCell", "office:value-type" to "string", "table:number-columns-spanned" to cell.colSpan.toString())
            } else {
                body.start("table:table-cell", "table:style-name" to "TableCell", "office:value-type" to "string")
            }
            for (b in cell.blocks) {
                if (b is Block.Paragraph) {
                    body.start("text:p", "text:style-name" to paragraphStyle(paragraphStyle, b.align))
                    inlines(b.content)
                    body.end()
                } else {
                    block(b, quote = false)
                }
            }
            if (cell.blocks.isEmpty()) body.leaf("text:p", null, "text:style-name" to paragraphStyle)
            body.end()
            repeat(cell.colSpan - 1) { body.leaf("table:covered-table-cell") }
            used += cell.colSpan
        }
        while (used++ < columns) body.start("table:table-cell", "table:style-name" to "TableCell").leaf("text:p", null, "text:style-name" to paragraphStyle).end()
        body.end()
    }

    private fun picture(picture: Block.Picture, quote: Boolean) {
        val type = ImageHeader.mimeType(picture.bytes)
        val extension = type?.let(ODT_IMAGES::get)
        if (extension == null) {
            if (picture.alt.isNotBlank()) block(Block.Paragraph(listOf(Inline.Text(picture.alt))), quote)
            return
        }
        val path = pictures.firstOrNull { it.second.contentEquals(picture.bytes) }?.first
            ?: "Pictures/image${pictures.size + 1}.$extension".also { pictures.add(it to picture.bytes) }
        val (w, h) = displaySize(picture, ODT_TEXT_WIDTH_PT)
        body.start("text:p", "text:style-name" to paragraphStyle(if (quote) "Quotations" else "Picture", Align.CENTER))
        body.start(
            "draw:frame",
            "draw:style-name" to "Frame",
            "draw:name" to "Image${++frames}",
            "text:anchor-type" to "as-char",
            "svg:width" to "${w.toDouble().fmt(2)}pt",
            "svg:height" to "${h.toDouble().fmt(2)}pt",
        )
        body.leaf("draw:image", null, "xlink:href" to path, "xlink:type" to "simple", "xlink:show" to "embed", "xlink:actuate" to "onLoad", "draw:mime-type" to type)
        if (picture.alt.isNotBlank()) body.leaf("svg:title", picture.alt)
        body.end().end()
    }

    private fun takeBreak(): Boolean = pendingBreak.also { pendingBreak = false }

    private fun paragraphStyle(parent: String, align: Align): String {
        val breakBefore = takeBreak()
        if (align == Align.START && !breakBefore) return parent
        return paragraphStyles.getOrPut("$parent|$align|$breakBefore") { "P${paragraphStyles.size + 1}" }
    }

    private fun inlines(content: List<Inline>) {
        spaceBefore = true
        var i = 0
        while (i < content.size) {
            val link = (content[i] as? Inline.Text)?.link
            if (link == null) {
                inline(content[i])
                i++
                continue
            }
            body.start(
                "text:a",
                "xlink:type" to "simple",
                "xlink:href" to uriSafe(link),
                "text:style-name" to "Internet_20_link",
                "text:visited-style-name" to "Visited_20_Internet_20_Link",
            )
            while (i < content.size && (content[i] as? Inline.Text)?.link == link) {
                inline(content[i])
                i++
            }
            body.end()
        }
    }

    private fun inline(item: Inline) {
        if (item !is Inline.Text) {
            body.leaf("text:line-break")
            spaceBefore = true
            return
        }
        val key = textStyleKey(item)
        if (key.isEmpty()) {
            text(item.text)
        } else {
            body.start("text:span", "text:style-name" to textStyles.getOrPut(key) { "T${textStyles.size + 1}" })
            text(item.text)
            body.end()
        }
    }

    private fun textStyleKey(t: Inline.Text): String = buildString {
        if (t.bold) append('b')
        if (t.italic) append('i')
        if (t.underline) append('u')
        if (t.strike) append('s')
        if (t.code) append('c')
        when (t.script) {
            Script.SUPER -> append('^')
            Script.SUB -> append('_')
            Script.NORMAL -> {}
        }
    }

    // ODF collapses whitespace, so a leading space and every further one of a run go into text:s
    private fun text(value: String) {
        val sb = StringBuilder()
        fun flush() {
            if (sb.isNotEmpty()) body.text(sb.toString())
            sb.clear()
        }
        var i = 0
        while (i < value.length) {
            when (val c = value[i]) {
                ' ' -> {
                    var n = 0
                    while (i < value.length && value[i] == ' ') {
                        n++
                        i++
                    }
                    if (!spaceBefore) {
                        sb.append(' ')
                        n--
                    }
                    if (n > 0) {
                        flush()
                        if (n == 1) body.leaf("text:s") else body.leaf("text:s", null, "text:c" to n.toString())
                    }
                    spaceBefore = true
                    continue
                }
                '\t' -> {
                    flush()
                    body.leaf("text:tab")
                    spaceBefore = false
                }
                '\n' -> {
                    flush()
                    body.leaf("text:line-break")
                    spaceBefore = true
                }
                else -> {
                    sb.append(c)
                    spaceBefore = false
                }
            }
            i++
        }
        flush()
    }

    private fun automaticStyles(): String {
        val xml = XmlBuilder(declaration = false).start("office:automatic-styles")
        for ((key, name) in paragraphStyles) {
            val (parent, align, breakBefore) = key.split('|')
            xml.start("style:style", "style:name" to name, "style:family" to "paragraph", "style:parent-style-name" to parent)
            val attributes = ArrayList<Pair<String, String>>()
            when (Align.valueOf(align)) {
                Align.START -> {}
                Align.CENTER -> attributes.add("fo:text-align" to "center")
                Align.END -> attributes.add("fo:text-align" to "end")
                Align.JUSTIFY -> attributes.add("fo:text-align" to "justify")
            }
            if (breakBefore == "true") attributes.add("fo:break-before" to "page")
            xml.leaf("style:paragraph-properties", null, *attributes.toTypedArray())
            xml.end()
        }
        for ((key, name) in textStyles) {
            val attributes = ArrayList<Pair<String, String>>()
            if ('b' in key) attributes.add("fo:font-weight" to "bold")
            if ('i' in key) attributes.add("fo:font-style" to "italic")
            if ('u' in key) {
                attributes.add("style:text-underline-style" to "solid")
                attributes.add("style:text-underline-width" to "auto")
                attributes.add("style:text-underline-color" to "font-color")
            }
            if ('s' in key) attributes.add("style:text-line-through-style" to "solid")
            if ('c' in key) attributes.add("style:font-name" to "Liberation Mono")
            if ('^' in key) attributes.add("style:text-position" to "super 58%")
            if ('_' in key) attributes.add("style:text-position" to "sub 58%")
            xml.start("style:style", "style:name" to name, "style:family" to "text").leaf("style:text-properties", null, *attributes.toTypedArray()).end()
        }
        xml.start("style:style", "style:name" to "Table", "style:family" to "table")
            .leaf("style:table-properties", null, "style:width" to "17cm", "table:align" to "margins").end()
        xml.start("style:style", "style:name" to "TableBreak", "style:family" to "table")
            .leaf("style:table-properties", null, "style:width" to "17cm", "table:align" to "margins", "fo:break-before" to "page").end()
        xml.start("style:style", "style:name" to "TableCell", "style:family" to "table-cell")
            .leaf("style:table-cell-properties", null, "fo:padding" to "0.1cm", "fo:border" to "0.5pt solid #000000").end()
        xml.start("style:style", "style:name" to "Frame", "style:family" to "graphic", "style:parent-style-name" to "Graphics")
            .leaf("style:graphic-properties", null, "style:vertical-pos" to "top", "style:vertical-rel" to "baseline").end()
        listStyle(xml, "L1", ordered = false)
        listStyle(xml, "L2", ordered = true)
        return xml.end().toString()
    }

    private fun listStyle(xml: XmlBuilder, name: String, ordered: Boolean) {
        xml.start("text:list-style", "style:name" to name)
        for (level in 1..10) {
            if (ordered) {
                xml.start("text:list-level-style-number", "text:level" to level.toString(), "style:num-suffix" to ".", "style:num-format" to "1")
            } else {
                val bullet = when (level % 3) {
                    1 -> "•"
                    2 -> "◦"
                    else -> "▪"
                }
                xml.start("text:list-level-style-bullet", "text:level" to level.toString(), "text:bullet-char" to bullet)
            }
            val margin = "${(0.635 * level).fmt(3)}cm"
            xml.start("style:list-level-properties", "text:list-level-position-and-space-mode" to "label-alignment")
                .leaf(
                    "style:list-level-label-alignment", null,
                    "text:label-followed-by" to "listtab", "text:list-tab-stop-position" to margin, "fo:text-indent" to "-0.635cm", "fo:margin-left" to margin,
                )
                .end()
            xml.end()
        }
        xml.end()
    }

    private fun styles(): String {
        val xml = XmlBuilder().start("office:document-styles", *ODF_NAMESPACES.toTypedArray(), "office:version" to ODF_VERSION)
        xml.start("office:font-face-decls")
        xml.leaf("style:font-face", null, "style:name" to "Liberation Serif", "svg:font-family" to "'Liberation Serif'", "style:font-family-generic" to "roman", "style:font-pitch" to "variable")
        xml.leaf("style:font-face", null, "style:name" to "Liberation Sans", "svg:font-family" to "'Liberation Sans'", "style:font-family-generic" to "swiss", "style:font-pitch" to "variable")
        xml.leaf("style:font-face", null, "style:name" to "Liberation Mono", "svg:font-family" to "'Liberation Mono'", "style:font-family-generic" to "modern", "style:font-pitch" to "fixed")
        xml.end()
        xml.start("office:styles")
        val language = doc.language?.trim()?.takeIf { it.isNotEmpty() }
        val textDefaults = ArrayList<Pair<String, String>>()
        textDefaults.add("style:font-name" to "Liberation Serif")
        textDefaults.add("fo:font-size" to "12pt")
        if (language != null) {
            textDefaults.add("fo:language" to language.substringBefore('-'))
            language.substringAfter('-', "").takeIf { it.length == 2 }?.let { textDefaults.add("fo:country" to it.uppercase()) }
        }
        xml.start("style:default-style", "style:family" to "paragraph").leaf("style:text-properties", null, *textDefaults.toTypedArray()).end()
        paragraph(xml, "Standard", null, null)
        paragraph(xml, "Text_20_body", "Text body", "Standard") { it.leaf("style:paragraph-properties", null, "fo:margin-top" to "0cm", "fo:margin-bottom" to "0.247cm", "fo:line-height" to "115%") }
        paragraph(xml, "Heading", null, "Standard", next = "Text_20_body") {
            it.leaf("style:paragraph-properties", null, "fo:margin-top" to "0.423cm", "fo:margin-bottom" to "0.212cm", "fo:keep-with-next" to "always")
            it.leaf("style:text-properties", null, "style:font-name" to "Liberation Sans", "fo:font-size" to "14pt")
        }
        for (level in 1..6) {
            xml.start(
                "style:style",
                "style:name" to "Heading_20_$level",
                "style:display-name" to "Heading $level",
                "style:family" to "paragraph",
                "style:parent-style-name" to "Heading",
                "style:next-style-name" to "Text_20_body",
                "style:default-outline-level" to level.toString(),
                "style:class" to "text",
            )
            xml.leaf("style:text-properties", null, "fo:font-size" to ODT_HEADING_SIZES[level - 1], "fo:font-weight" to "bold")
            xml.end()
        }
        paragraph(xml, "Quotations", null, "Standard") {
            it.leaf("style:paragraph-properties", null, "fo:margin-left" to "1cm", "fo:margin-right" to "1cm", "fo:margin-top" to "0cm", "fo:margin-bottom" to "0.247cm")
            it.leaf("style:text-properties", null, "fo:font-style" to "italic")
        }
        paragraph(xml, "Preformatted_20_Text", "Preformatted Text", "Standard") {
            it.leaf("style:paragraph-properties", null, "fo:margin-top" to "0cm", "fo:margin-bottom" to "0cm")
            it.leaf("style:text-properties", null, "style:font-name" to "Liberation Mono", "fo:font-size" to "10pt")
        }
        paragraph(xml, "Table_20_Contents", "Table Contents", "Standard")
        paragraph(xml, "Table_20_Heading", "Table Heading", "Table_20_Contents") { it.leaf("style:text-properties", null, "fo:font-weight" to "bold") }
        paragraph(xml, "Picture", null, "Standard")
        paragraph(xml, "Horizontal_20_Line", "Horizontal Line", "Standard") {
            it.leaf("style:paragraph-properties", null, "fo:margin-top" to "0cm", "fo:margin-bottom" to "0.5cm", "fo:border-bottom" to "0.06pt solid #808080", "fo:padding" to "0cm")
        }
        xml.start("style:style", "style:name" to "Internet_20_link", "style:display-name" to "Internet link", "style:family" to "text")
            .leaf("style:text-properties", null, "fo:color" to "#000080", "style:text-underline-style" to "solid", "style:text-underline-width" to "auto", "style:text-underline-color" to "font-color")
            .end()
        xml.start("style:style", "style:name" to "Visited_20_Internet_20_Link", "style:display-name" to "Visited Internet Link", "style:family" to "text")
            .leaf("style:text-properties", null, "fo:color" to "#800000", "style:text-underline-style" to "solid", "style:text-underline-width" to "auto", "style:text-underline-color" to "font-color")
            .end()
        xml.start("style:style", "style:name" to "Graphics", "style:family" to "graphic")
            .leaf("style:graphic-properties", null, "text:anchor-type" to "as-char", "svg:y" to "0cm", "style:vertical-pos" to "top", "style:vertical-rel" to "baseline")
            .end()
        xml.end()
        xml.start("office:automatic-styles")
        xml.start("style:page-layout", "style:name" to "pm1")
            .leaf(
                "style:page-layout-properties", null,
                "fo:page-width" to "21cm", "fo:page-height" to "29.7cm", "style:print-orientation" to "portrait",
                "fo:margin-top" to "2cm", "fo:margin-bottom" to "2cm", "fo:margin-left" to "2cm", "fo:margin-right" to "2cm",
            )
            .end()
        xml.end()
        xml.start("office:master-styles").leaf("style:master-page", null, "style:name" to "Standard", "style:page-layout-name" to "pm1").end()
        return xml.end().toString()
    }

    private fun paragraph(xml: XmlBuilder, name: String, display: String?, parent: String?, next: String? = null, body: (XmlBuilder) -> Unit = {}) {
        val attributes = ArrayList<Pair<String, String>>()
        attributes.add("style:name" to name)
        if (display != null) attributes.add("style:display-name" to display)
        attributes.add("style:family" to "paragraph")
        if (parent != null) attributes.add("style:parent-style-name" to parent)
        if (next != null) attributes.add("style:next-style-name" to next)
        xml.start("style:style", *attributes.toTypedArray())
        body(xml)
        xml.end()
    }

    private fun meta(): String {
        val xml = XmlBuilder().start("office:document-meta", *ODF_NAMESPACES.toTypedArray(), "office:version" to ODF_VERSION).start("office:meta")
        xml.leaf("meta:generator", "vasmarfas Toolbox")
        doc.title?.takeIf { it.isNotBlank() }?.let { xml.leaf("dc:title", it) }
        doc.author?.takeIf { it.isNotBlank() }?.let {
            xml.leaf("meta:initial-creator", it)
            xml.leaf("dc:creator", it)
        }
        doc.language?.takeIf { it.isNotBlank() }?.let { xml.leaf("dc:language", it.trim()) }
        return xml.end().end().toString()
    }

    private fun manifest(): String {
        val xml = XmlBuilder().start("manifest:manifest", "xmlns:manifest" to "urn:oasis:names:tc:opendocument:xmlns:manifest:1.0", "manifest:version" to ODF_VERSION)
        xml.leaf("manifest:file-entry", null, "manifest:full-path" to "/", "manifest:version" to ODF_VERSION, "manifest:media-type" to "application/vnd.oasis.opendocument.text")
        for (part in listOf("content.xml", "styles.xml", "meta.xml")) xml.leaf("manifest:file-entry", null, "manifest:full-path" to part, "manifest:media-type" to "text/xml")
        for ((path, bytes) in pictures) xml.leaf("manifest:file-entry", null, "manifest:full-path" to path, "manifest:media-type" to ImageHeader.mimeType(bytes).orEmpty())
        return xml.end().toString()
    }
}
