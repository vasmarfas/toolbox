package com.vasmarfas.card.tools.documents

import com.vasmarfas.card.core.XmlBuilder
import com.vasmarfas.card.core.ZipWriter
import kotlin.math.roundToLong

private const val W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
private const val R_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
private const val REL_NS = "http://schemas.openxmlformats.org/package/2006/relationships"
private const val REL_TYPE = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"

// A4 in twips, 2 cm margins
private const val PAGE_WIDTH = 11906
private const val PAGE_HEIGHT = 16838
private const val MARGIN = 1134
private const val TEXT_WIDTH = PAGE_WIDTH - 2 * MARGIN
private const val TEXT_WIDTH_PT = TEXT_WIDTH / 20f
private const val LIST_INDENT = 720
private const val EMU_PER_PT = 12700

private val DOCX_IMAGES = mapOf(
    "image/png" to "png",
    "image/jpeg" to "jpeg",
    "image/gif" to "gif",
    "image/bmp" to "bmp",
    "image/tiff" to "tiff",
    "image/emf" to "emf",
    "image/wmf" to "wmf",
)
private val IMAGE_CONTENT_TYPES = mapOf("emf" to "image/x-emf", "wmf" to "image/x-wmf")
private val HEADING_SIZES = intArrayOf(36, 32, 28, 24, 22, 22)

// every list gets its own w:num, ordered ones restarted with startOverride, so no two lists continue
// each other's numbering
internal class DocxWriter(private val doc: Doc) {
    private class Ctx(val quote: Boolean, val indent: Int?)

    private class Relationship(val id: String, val type: String, val target: String, val external: Boolean)

    private class Num(val abstract: Int, val start: Int?, val level: Int)

    private val xml = XmlBuilder()
    private val rels = ArrayList<Relationship>()
    private val relIds = HashMap<String, String>()
    private val media = ArrayList<Pair<String, ByteArray>>()
    private val nums = ArrayList<Num>()
    private var drawings = 0
    private var lastWasParagraph = false

    fun write(): ByteArray {
        xml.start("w:document", "xmlns:w" to W_NS, "xmlns:r" to R_NS, "xmlns:wp" to "http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing")
        xml.start("w:body")
        blocks(doc.blocks, Ctx(quote = false, indent = null))
        if (!lastWasParagraph) xml.leaf("w:p")
        xml.start("w:sectPr")
        xml.leaf("w:pgSz", null, "w:w" to PAGE_WIDTH.toString(), "w:h" to PAGE_HEIGHT.toString())
        xml.leaf(
            "w:pgMar", null, "w:top" to "$MARGIN", "w:right" to "$MARGIN", "w:bottom" to "$MARGIN", "w:left" to "$MARGIN",
            "w:header" to "709", "w:footer" to "709", "w:gutter" to "0",
        )
        xml.end().end().end()

        val zip = ZipWriter()
        zip.add("[Content_Types].xml", contentTypes().encodeToByteArray())
        zip.add("_rels/.rels", packageRels().encodeToByteArray())
        zip.add("word/document.xml", xml.toString().encodeToByteArray())
        zip.add("word/styles.xml", styles().encodeToByteArray())
        zip.add("word/numbering.xml", numbering().encodeToByteArray())
        zip.add("word/_rels/document.xml.rels", documentRels().encodeToByteArray())
        for ((name, bytes) in media) zip.add("word/$name", bytes, compress = false)
        zip.add("docProps/core.xml", core().encodeToByteArray())
        zip.add("docProps/app.xml", app().encodeToByteArray())
        return zip.toByteArray()
    }

    private fun blocks(blocks: List<Block>, ctx: Ctx) {
        for (block in blocks) block(block, ctx)
    }

    private fun block(block: Block, ctx: Ctx, number: Pair<Int, Int>? = null) {
        when (block) {
            is Block.Heading -> paragraph(block.content, Align.START, "Heading${block.level.coerceIn(1, 6)}", number, ctx.indent)
            is Block.Paragraph -> paragraph(block.content, block.align, if (ctx.quote) "Quote" else null, number, ctx.indent)
            is Block.ListBlock -> list(block, 0, ctx)
            is Block.Quote -> blocks(block.blocks, Ctx(quote = true, indent = ctx.indent))
            is Block.Code -> paragraph(listOf(Inline.Text(block.text)), Align.START, "Code", number, ctx.indent)
            is Block.Table -> table(block, ctx.indent)
            is Block.Picture -> picture(block, ctx)
            Block.Rule -> {
                xml.start("w:p").start("w:pPr")
                if (ctx.quote) xml.leaf("w:pStyle", null, "w:val" to "Quote")
                xml.start("w:pBdr").leaf("w:bottom", null, "w:val" to "single", "w:sz" to "6", "w:space" to "1", "w:color" to "auto").end()
                indent(ctx.indent)
                xml.end().end()
                lastWasParagraph = true
            }
            Block.PageBreak -> {
                xml.start("w:p")
                if (ctx.indent != null) xml.start("w:pPr").also { indent(ctx.indent) }.end()
                xml.start("w:r").leaf("w:br", null, "w:type" to "page").end().end()
                lastWasParagraph = true
            }
        }
    }

    private fun list(list: Block.ListBlock, level: Int, ctx: Ctx) {
        val lvl = level.coerceAtMost(8)
        nums.add(Num(if (list.ordered) 1 else 0, if (list.ordered) list.start else null, lvl))
        val numId = nums.size
        val inner = Ctx(ctx.quote, LIST_INDENT * (lvl + 1))
        for (item in list.items) {
            val first = item.firstOrNull()
            if (first !is Block.Paragraph && first !is Block.Heading) paragraph(emptyList(), Align.START, null, numId to lvl, null)
            item.forEachIndexed { i, b ->
                when {
                    i == 0 && (b is Block.Paragraph || b is Block.Heading) -> block(b, Ctx(ctx.quote, null), numId to lvl)
                    b is Block.ListBlock -> list(b, level + 1, ctx)
                    else -> block(b, inner)
                }
            }
        }
    }

    private fun indent(twips: Int?) {
        if (twips != null) xml.leaf("w:ind", null, "w:left" to twips.toString())
    }

    private fun paragraph(content: List<Inline>, align: Align, style: String?, number: Pair<Int, Int>?, indent: Int?) {
        xml.start("w:p")
        val jc = when (align) {
            Align.START -> null
            Align.CENTER -> "center"
            Align.END -> "right"
            Align.JUSTIFY -> "both"
        }
        if (style != null || number != null || indent != null || jc != null) {
            xml.start("w:pPr")
            if (style != null) xml.leaf("w:pStyle", null, "w:val" to style)
            if (number != null) {
                xml.start("w:numPr").leaf("w:ilvl", null, "w:val" to number.second.toString()).leaf("w:numId", null, "w:val" to number.first.toString()).end()
            }
            indent(indent)
            if (jc != null) xml.leaf("w:jc", null, "w:val" to jc)
            xml.end()
        }
        runs(content)
        xml.end()
        lastWasParagraph = true
    }

    private fun runs(content: List<Inline>) {
        var i = 0
        while (i < content.size) {
            val link = (content[i] as? Inline.Text)?.link
            if (link == null) {
                run(content[i], hyperlink = false)
                i++
                continue
            }
            xml.start("w:hyperlink", "r:id" to rel("hyperlink", uriSafe(link), external = true), "w:history" to "1")
            while (i < content.size && (content[i] as? Inline.Text)?.link == link) {
                run(content[i], hyperlink = true)
                i++
            }
            xml.end()
        }
    }

    private fun run(item: Inline, hyperlink: Boolean) {
        xml.start("w:r")
        if (item !is Inline.Text) {
            xml.leaf("w:br").end()
            return
        }
        val props = hyperlink || item.code || item.bold || item.italic || item.strike || item.underline || item.script != Script.NORMAL
        if (props) {
            xml.start("w:rPr")
            if (hyperlink) xml.leaf("w:rStyle", null, "w:val" to "Hyperlink")
            if (item.code) xml.leaf("w:rFonts", null, "w:ascii" to "Courier New", "w:hAnsi" to "Courier New", "w:cs" to "Courier New")
            if (item.bold) xml.leaf("w:b")
            if (item.italic) xml.leaf("w:i")
            if (item.strike) xml.leaf("w:strike")
            if (item.underline) xml.leaf("w:u", null, "w:val" to "single")
            when (item.script) {
                Script.SUPER -> xml.leaf("w:vertAlign", null, "w:val" to "superscript")
                Script.SUB -> xml.leaf("w:vertAlign", null, "w:val" to "subscript")
                Script.NORMAL -> {}
            }
            xml.end()
        }
        val text = item.text
        var start = 0
        for (k in text.indices) {
            val c = text[k]
            if (c == '\t' || c == '\n') {
                if (k > start) xml.leaf("w:t", text.substring(start, k), "xml:space" to "preserve")
                xml.leaf(if (c == '\t') "w:tab" else "w:br")
                start = k + 1
            }
        }
        if (start < text.length) xml.leaf("w:t", text.substring(start), "xml:space" to "preserve")
        xml.end()
    }

    private fun table(table: Block.Table, indent: Int?) {
        val rows = table.rows.filter { it.isNotEmpty() }
        if (rows.isEmpty()) return
        val columns = rows.maxOf { row -> row.sumOf { it.colSpan } }
        val width = TEXT_WIDTH - (indent ?: 0)
        val column = maxOf(width / columns, 1)
        xml.start("w:tbl").start("w:tblPr")
        xml.leaf("w:tblStyle", null, "w:val" to "TableGrid")
        xml.leaf("w:tblW", null, "w:w" to (column * columns).toString(), "w:type" to "dxa")
        if (indent != null) xml.leaf("w:tblInd", null, "w:w" to indent.toString(), "w:type" to "dxa")
        xml.end()
        xml.start("w:tblGrid")
        repeat(columns) { xml.leaf("w:gridCol", null, "w:w" to column.toString()) }
        xml.end()
        rows.forEachIndexed { r, row ->
            xml.start("w:tr")
            if (r < table.headerRows) xml.start("w:trPr").leaf("w:tblHeader").end()
            var used = 0
            for (cell in row) {
                cell(cell.blocks, cell.colSpan, column)
                used += cell.colSpan
            }
            while (used < columns) {
                cell(emptyList(), 1, column)
                used++
            }
            xml.end()
        }
        xml.end()
        lastWasParagraph = false
    }

    // Word insists on a paragraph at the end of a cell, also after a nested table
    private fun cell(blocks: List<Block>, span: Int, column: Int) {
        xml.start("w:tc").start("w:tcPr")
        xml.leaf("w:tcW", null, "w:w" to (column * span).toString(), "w:type" to "dxa")
        if (span > 1) xml.leaf("w:gridSpan", null, "w:val" to span.toString())
        xml.end()
        lastWasParagraph = false
        blocks(blocks, Ctx(quote = false, indent = null))
        if (!lastWasParagraph) xml.leaf("w:p")
        xml.end()
        lastWasParagraph = false
    }

    private fun picture(picture: Block.Picture, ctx: Ctx) {
        val extension = ImageHeader.mimeType(picture.bytes)?.let(DOCX_IMAGES::get)
        if (extension == null) {
            if (picture.alt.isNotBlank()) paragraph(listOf(Inline.Text(picture.alt)), Align.START, if (ctx.quote) "Quote" else null, null, ctx.indent)
            return
        }
        val (w, h) = displaySize(picture, TEXT_WIDTH_PT - (ctx.indent ?: 0) / 20f)
        val id = rel("image", media(picture.bytes, extension), external = false)
        val cx = (w * EMU_PER_PT).roundToLong().coerceAtLeast(1).toString()
        val cy = (h * EMU_PER_PT).roundToLong().coerceAtLeast(1).toString()
        val n = (++drawings).toString()
        xml.start("w:p")
        if (ctx.indent != null || ctx.quote) {
            xml.start("w:pPr")
            if (ctx.quote) xml.leaf("w:pStyle", null, "w:val" to "Quote")
            indent(ctx.indent)
            xml.end()
        }
        xml.start("w:r").start("w:drawing")
        xml.start("wp:inline", "distT" to "0", "distB" to "0", "distL" to "0", "distR" to "0")
        xml.leaf("wp:extent", null, "cx" to cx, "cy" to cy)
        xml.leaf("wp:effectExtent", null, "l" to "0", "t" to "0", "r" to "0", "b" to "0")
        xml.leaf("wp:docPr", null, "id" to n, "name" to "Picture $n", "descr" to picture.alt)
        xml.start("wp:cNvGraphicFramePr")
        xml.leaf("a:graphicFrameLocks", null, "xmlns:a" to "http://schemas.openxmlformats.org/drawingml/2006/main", "noChangeAspect" to "1")
        xml.end()
        xml.start("a:graphic", "xmlns:a" to "http://schemas.openxmlformats.org/drawingml/2006/main")
        xml.start("a:graphicData", "uri" to "http://schemas.openxmlformats.org/drawingml/2006/picture")
        xml.start("pic:pic", "xmlns:pic" to "http://schemas.openxmlformats.org/drawingml/2006/picture")
        xml.start("pic:nvPicPr").leaf("pic:cNvPr", null, "id" to n, "name" to "image$n.$extension", "descr" to picture.alt).leaf("pic:cNvPicPr").end()
        xml.start("pic:blipFill").leaf("a:blip", null, "r:embed" to id).start("a:stretch").leaf("a:fillRect").end().end()
        xml.start("pic:spPr")
        xml.start("a:xfrm").leaf("a:off", null, "x" to "0", "y" to "0").leaf("a:ext", null, "cx" to cx, "cy" to cy).end()
        xml.start("a:prstGeom", "prst" to "rect").leaf("a:avLst").end()
        xml.end()
        xml.end().end().end()
        xml.end()
        xml.end().end().end()
        lastWasParagraph = true
    }

    private fun media(bytes: ByteArray, extension: String): String {
        media.firstOrNull { it.second.contentEquals(bytes) }?.let { return it.first }
        val name = "media/image${media.size + 1}.$extension"
        media.add(name to bytes)
        return name
    }

    private fun rel(type: String, target: String, external: Boolean): String = relIds.getOrPut("$type $target") {
        val id = "rId${rels.size + 3}"
        rels.add(Relationship(id, "$REL_TYPE/$type", target, external))
        id
    }

    private fun documentRels(): String {
        val xml = XmlBuilder().start("Relationships", "xmlns" to REL_NS)
        xml.leaf("Relationship", null, "Id" to "rId1", "Type" to "$REL_TYPE/styles", "Target" to "styles.xml")
        xml.leaf("Relationship", null, "Id" to "rId2", "Type" to "$REL_TYPE/numbering", "Target" to "numbering.xml")
        for (r in rels) {
            if (r.external) {
                xml.leaf("Relationship", null, "Id" to r.id, "Type" to r.type, "Target" to r.target, "TargetMode" to "External")
            } else {
                xml.leaf("Relationship", null, "Id" to r.id, "Type" to r.type, "Target" to r.target)
            }
        }
        return xml.end().toString()
    }

    private fun packageRels(): String = XmlBuilder().start("Relationships", "xmlns" to REL_NS)
        .leaf("Relationship", null, "Id" to "rId1", "Type" to "$REL_TYPE/officeDocument", "Target" to "word/document.xml")
        .leaf("Relationship", null, "Id" to "rId2", "Type" to "http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties", "Target" to "docProps/core.xml")
        .leaf("Relationship", null, "Id" to "rId3", "Type" to "$REL_TYPE/extended-properties", "Target" to "docProps/app.xml")
        .end()
        .toString()

    private fun contentTypes(): String {
        val xml = XmlBuilder().start("Types", "xmlns" to "http://schemas.openxmlformats.org/package/2006/content-types")
        xml.leaf("Default", null, "Extension" to "rels", "ContentType" to "application/vnd.openxmlformats-package.relationships+xml")
        xml.leaf("Default", null, "Extension" to "xml", "ContentType" to "application/xml")
        for (extension in media.map { it.first.substringAfterLast('.') }.distinct()) {
            xml.leaf("Default", null, "Extension" to extension, "ContentType" to (IMAGE_CONTENT_TYPES[extension] ?: "image/$extension"))
        }
        val main = "application/vnd.openxmlformats-officedocument.wordprocessingml"
        xml.leaf("Override", null, "PartName" to "/word/document.xml", "ContentType" to "$main.document.main+xml")
        xml.leaf("Override", null, "PartName" to "/word/styles.xml", "ContentType" to "$main.styles+xml")
        xml.leaf("Override", null, "PartName" to "/word/numbering.xml", "ContentType" to "$main.numbering+xml")
        xml.leaf("Override", null, "PartName" to "/docProps/core.xml", "ContentType" to "application/vnd.openxmlformats-package.core-properties+xml")
        xml.leaf("Override", null, "PartName" to "/docProps/app.xml", "ContentType" to "application/vnd.openxmlformats-officedocument.extended-properties+xml")
        return xml.end().toString()
    }

    private fun styles(): String {
        val xml = XmlBuilder().start("w:styles", "xmlns:w" to W_NS)
        xml.start("w:docDefaults").start("w:rPrDefault").start("w:rPr")
        xml.leaf("w:rFonts", null, "w:ascii" to "Calibri", "w:eastAsia" to "Calibri", "w:hAnsi" to "Calibri", "w:cs" to "Calibri")
        xml.leaf("w:sz", null, "w:val" to "22").leaf("w:szCs", null, "w:val" to "22")
        doc.language?.takeIf { it.isNotBlank() }?.let { xml.leaf("w:lang", null, "w:val" to it.trim()) }
        xml.end().end()
        xml.start("w:pPrDefault").start("w:pPr").leaf("w:spacing", null, "w:after" to "120", "w:line" to "276", "w:lineRule" to "auto").end().end()
        xml.end()

        xml.start("w:style", "w:type" to "paragraph", "w:default" to "1", "w:styleId" to "Normal").leaf("w:name", null, "w:val" to "Normal").leaf("w:qFormat").end()
        xml.start("w:style", "w:type" to "character", "w:default" to "1", "w:styleId" to "DefaultParagraphFont")
            .leaf("w:name", null, "w:val" to "Default Paragraph Font").leaf("w:uiPriority", null, "w:val" to "1").leaf("w:semiHidden").leaf("w:unhideWhenUsed").end()
        xml.start("w:style", "w:type" to "table", "w:default" to "1", "w:styleId" to "TableNormal")
            .leaf("w:name", null, "w:val" to "Normal Table").leaf("w:uiPriority", null, "w:val" to "99").leaf("w:semiHidden").leaf("w:unhideWhenUsed")
            .start("w:tblPr").leaf("w:tblInd", null, "w:w" to "0", "w:type" to "dxa")
            .start("w:tblCellMar")
            .leaf("w:top", null, "w:w" to "0", "w:type" to "dxa").leaf("w:left", null, "w:w" to "108", "w:type" to "dxa")
            .leaf("w:bottom", null, "w:w" to "0", "w:type" to "dxa").leaf("w:right", null, "w:w" to "108", "w:type" to "dxa")
            .end().end().end()

        paragraphStyle(xml, "Title", "Title") { s ->
            s.start("w:pPr").leaf("w:spacing", null, "w:after" to "240").end()
            s.start("w:rPr").leaf("w:sz", null, "w:val" to "56").leaf("w:szCs", null, "w:val" to "56").end()
        }
        for (level in 1..6) {
            paragraphStyle(xml, "Heading$level", "heading $level") { s ->
                s.start("w:pPr").leaf("w:keepNext").leaf("w:spacing", null, "w:before" to if (level == 1) "360" else "240", "w:after" to "120")
                    .leaf("w:outlineLvl", null, "w:val" to (level - 1).toString()).end()
                s.start("w:rPr").leaf("w:b")
                if (level == 6) s.leaf("w:i")
                val size = HEADING_SIZES[level - 1].toString()
                s.leaf("w:sz", null, "w:val" to size).leaf("w:szCs", null, "w:val" to size).end()
            }
        }
        paragraphStyle(xml, "Quote", "Quote") { s ->
            s.start("w:pPr").leaf("w:ind", null, "w:left" to "567", "w:right" to "567").end()
            s.start("w:rPr").leaf("w:i").leaf("w:color", null, "w:val" to "404040").end()
        }
        paragraphStyle(xml, "Code", "Code") { s ->
            s.start("w:pPr").leaf("w:spacing", null, "w:after" to "0", "w:line" to "240", "w:lineRule" to "auto").end()
            s.start("w:rPr").leaf("w:rFonts", null, "w:ascii" to "Courier New", "w:hAnsi" to "Courier New", "w:cs" to "Courier New")
                .leaf("w:sz", null, "w:val" to "20").leaf("w:szCs", null, "w:val" to "20").end()
        }
        xml.start("w:style", "w:type" to "character", "w:styleId" to "Hyperlink")
            .leaf("w:name", null, "w:val" to "Hyperlink").leaf("w:basedOn", null, "w:val" to "DefaultParagraphFont")
            .leaf("w:uiPriority", null, "w:val" to "99").leaf("w:unhideWhenUsed")
            .start("w:rPr").leaf("w:color", null, "w:val" to "0563C1").leaf("w:u", null, "w:val" to "single").end()
            .end()
        xml.start("w:style", "w:type" to "table", "w:styleId" to "TableGrid")
            .leaf("w:name", null, "w:val" to "Table Grid").leaf("w:basedOn", null, "w:val" to "TableNormal").leaf("w:uiPriority", null, "w:val" to "39")
            .start("w:pPr").leaf("w:spacing", null, "w:after" to "0", "w:line" to "240", "w:lineRule" to "auto").end()
            .start("w:tblPr").start("w:tblBorders")
        for (side in listOf("top", "left", "bottom", "right", "insideH", "insideV")) {
            xml.leaf("w:$side", null, "w:val" to "single", "w:sz" to "4", "w:space" to "0", "w:color" to "auto")
        }
        xml.end().end().end()
        return xml.end().toString()
    }

    private fun paragraphStyle(xml: XmlBuilder, id: String, name: String, body: (XmlBuilder) -> Unit) {
        xml.start("w:style", "w:type" to "paragraph", "w:styleId" to id)
            .leaf("w:name", null, "w:val" to name).leaf("w:basedOn", null, "w:val" to "Normal").leaf("w:next", null, "w:val" to "Normal").leaf("w:qFormat")
        body(xml)
        xml.end()
    }

    private fun numbering(): String {
        val xml = XmlBuilder().start("w:numbering", "xmlns:w" to W_NS)
        for (abstract in 0..1) {
            xml.start("w:abstractNum", "w:abstractNumId" to abstract.toString()).leaf("w:multiLevelType", null, "w:val" to "hybridMultilevel")
            for (lvl in 0..8) {
                xml.start("w:lvl", "w:ilvl" to lvl.toString()).leaf("w:start", null, "w:val" to "1")
                if (abstract == 0) {
                    val (glyph, font) = when (lvl % 3) {
                        0 -> Char(0xF0B7).toString() to "Symbol"
                        1 -> "o" to "Courier New"
                        else -> Char(0xF0A7).toString() to "Wingdings"
                    }
                    xml.leaf("w:numFmt", null, "w:val" to "bullet").leaf("w:lvlText", null, "w:val" to glyph).leaf("w:lvlJc", null, "w:val" to "left")
                    listIndent(xml, lvl)
                    xml.start("w:rPr").leaf("w:rFonts", null, "w:ascii" to font, "w:hAnsi" to font, "w:hint" to "default").end()
                } else {
                    xml.leaf("w:numFmt", null, "w:val" to "decimal").leaf("w:lvlText", null, "w:val" to "%${lvl + 1}.").leaf("w:lvlJc", null, "w:val" to "left")
                    listIndent(xml, lvl)
                }
                xml.end()
            }
            xml.end()
        }
        nums.forEachIndexed { i, num ->
            xml.start("w:num", "w:numId" to (i + 1).toString()).leaf("w:abstractNumId", null, "w:val" to num.abstract.toString())
            if (num.start != null) {
                xml.start("w:lvlOverride", "w:ilvl" to num.level.toString()).leaf("w:startOverride", null, "w:val" to num.start.toString()).end()
            }
            xml.end()
        }
        return xml.end().toString()
    }

    private fun listIndent(xml: XmlBuilder, lvl: Int) {
        xml.start("w:pPr").leaf("w:ind", null, "w:left" to (LIST_INDENT * (lvl + 1)).toString(), "w:hanging" to "360").end()
    }

    private fun core(): String {
        val xml = XmlBuilder().start(
            "cp:coreProperties",
            "xmlns:cp" to "http://schemas.openxmlformats.org/package/2006/metadata/core-properties",
            "xmlns:dc" to "http://purl.org/dc/elements/1.1/",
            "xmlns:dcterms" to "http://purl.org/dc/terms/",
            "xmlns:dcmitype" to "http://purl.org/dc/dcmitype/",
            "xmlns:xsi" to "http://www.w3.org/2001/XMLSchema-instance",
        )
        doc.title?.takeIf { it.isNotBlank() }?.let { xml.leaf("dc:title", it) }
        doc.author?.takeIf { it.isNotBlank() }?.let { xml.leaf("dc:creator", it) }
        doc.language?.takeIf { it.isNotBlank() }?.let { xml.leaf("dc:language", it.trim()) }
        return xml.end().toString()
    }

    private fun app(): String = XmlBuilder()
        .start("Properties", "xmlns" to "http://schemas.openxmlformats.org/officeDocument/2006/extended-properties")
        .leaf("Application", "vasmarfas Mobitool")
        .end()
        .toString()
}

internal fun displaySize(picture: Block.Picture, maxWidth: Float): Pair<Float, Float> {
    var w = picture.widthPt
    var h = picture.heightPt
    if (w <= 0f || h <= 0f) {
        val px = ImageHeader.size(picture.bytes)
        when {
            px != null && w > 0f -> h = w * px.second / px.first
            px != null && h > 0f -> w = h * px.first / px.second
            px != null -> {
                w = px.first * 0.75f
                h = px.second * 0.75f
            }
            else -> {
                w = 240f
                h = 180f
            }
        }
    }
    if (w > maxWidth) {
        h = h * maxWidth / w
        w = maxWidth
    }
    return w to h
}
