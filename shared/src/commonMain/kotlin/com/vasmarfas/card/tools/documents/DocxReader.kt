package com.vasmarfas.card.tools.documents

import com.vasmarfas.card.core.XmlElement

private val HEADING_NAME = Regex("(?:heading|заголовок|überschrift|titre|título|titolo|nagłówek|kop|rubrik|overskrift|otsikko)\\s*([1-9])")
private val TITLE_NAMES = setOf("title", "название")
private val SUBTITLE_NAMES = setOf("subtitle", "подзаголовок")
private val QUOTE_NAMES = setOf("quote", "intense quote", "block text", "quotations", "цитата", "выделенная цитата", "цитата 2")
private val CODE_NAME = Regex(".*(?:code|preformatted|verbatim|listing|macro text|plain text|исходный текст|код).*")

internal fun onOff(el: XmlElement?): Boolean {
    val v = el?.attrNotRel("val") ?: return el != null
    return v != "0" && v != "false" && v != "off" && v != "none"
}

// w: attributes only, r:id and friends from the relationships namespace share local names
internal fun XmlElement.attrNotRel(name: String): String? {
    for (a in attributes) if (a.localName == name && !a.namespace.endsWith("relationships")) return a.value
    return null
}

internal fun XmlElement.attrRel(name: String): String? {
    for (a in attributes) if (a.localName == name && a.namespace.endsWith("relationships")) return a.value
    return null
}

internal fun alternate(el: XmlElement): XmlElement? = el.child("Choice") ?: el.child("Fallback")

// does not descend into matches or stop elements, text boxes inside a drawing have their own reader
internal fun XmlElement.find(localName: String, stop: String? = null): List<XmlElement> {
    val result = ArrayList<XmlElement>()
    val stack = ArrayList<XmlElement>()
    for (i in elements.lastIndex downTo 0) stack.add(elements[i])
    while (stack.isNotEmpty()) {
        val el = stack.removeAt(stack.lastIndex)
        if (el.localName == localName) {
            result.add(el)
            continue
        }
        if (el.localName == stop) continue
        for (i in el.elements.lastIndex downTo 0) stack.add(el.elements[i])
    }
    return result
}

private class Rel(val type: String, val target: String, val external: Boolean)

private fun RunProps.read(rPr: XmlElement?): RunProps {
    if (rPr == null) return this
    for (el in rPr.elements) {
        when (el.localName) {
            "b" -> bold = onOff(el)
            "i" -> italic = onOff(el)
            "u" -> underline = el.attrNotRel("val").let { it == null || (it != "none" && it != "0" && it != "false") }
            "strike", "dstrike" -> strike = onOff(el)
            "vertAlign" -> script = when (el.attrNotRel("val")) {
                "superscript" -> Script.SUPER
                "subscript" -> Script.SUB
                else -> Script.NORMAL
            }
            "rFonts" -> {
                val theme = el.attrNotRel("asciiTheme") ?: el.attrNotRel("hAnsiTheme")
                val name = el.attrNotRel("ascii") ?: el.attrNotRel("hAnsi")
                if (theme != null) {
                    mono = false
                    font = null
                } else if (name != null) {
                    mono = isMonospaceFont(name)
                    font = name
                }
            }
            "vanish", "specVanish" -> hidden = onOff(el)
        }
    }
    return this
}

private class Style(val name: String, val basedOn: String?, val pPr: XmlElement?, val rPr: XmlElement?)

private enum class Kind { NORMAL, HEADING, CODE, QUOTE }

private class ParagraphStyle(
    val heading: Int?,
    val kind: Kind,
    val align: Align?,
    val numId: String?,
    val level: Int?,
    val indent: Int?,
    val pageBreakBefore: Boolean,
    val run: RunProps,
)

private class Level(val ordered: Boolean, val none: Boolean, val start: Int, val indent: Int?)

private class Field {
    val instruction = StringBuilder()
    var inResult = false
    var link: String? = null
}

private class RunContext(val paragraphRun: RunProps?, val code: Boolean, val link: String?) {
    fun withLink(link: String?): RunContext = if (link == null) this else RunContext(paragraphRun, code, link)
}

// headings by w:name, which Word keeps in English while style IDs are localized, or by outline level.
// Numbering continues across interruptions per abstract numbering as Word does, startOverride restarts it
internal class DocxReader(private val pkg: Package) {
    private var main = "word/document.xml"
    private var rels: Map<String, Rel> = emptyMap()
    private val styles = HashMap<String, Style>()
    private var defaultStyle: String? = null
    private val defaults = RunProps()
    private var language: String? = null
    private val paragraphStyles = HashMap<String, ParagraphStyle>()
    private val characterStyles = HashMap<String, RunProps>()

    private val abstractLevels = HashMap<String, Map<Int, Level>>()
    private val abstractLinks = HashMap<String, String>()
    private val numAbstract = HashMap<String, String>()
    private val numOverrides = HashMap<String, Map<Int, Int>>()
    private val counters = HashMap<String, IntArray>()
    private val overridden = HashSet<String>()

    private val fields = ArrayList<Field>()
    private val nesting = Nesting()
    private val notes = Notes()
    private val noteQueue = ArrayList<Triple<Boolean, String, Int>>()
    private var footnotes: Map<String, XmlElement> = emptyMap()
    private var endnotes: Map<String, XmlElement> = emptyMap()
    private var readingNotes = false

    fun read(): Doc {
        val packageRels = rels("_rels/.rels", "")
        packageRels.values.firstOrNull { it.type.endsWith("/officeDocument") && !it.external }?.let { main = it.target }
        val document = pkg.xml(main) ?: throw DocumentFormatException("Not a DOCX: $main is missing")
        val body = document.child("body") ?: throw DocumentFormatException("Not a DOCX: $main has no body")
        rels = rels(parentDir(main) + "_rels/" + main.substringAfterLast('/') + ".rels", parentDir(main))
        loadStyles(pkg.optionalXml(part("/styles", "styles.xml")))
        loadNumbering(pkg.optionalXml(part("/numbering", "numbering.xml")))
        footnotes = notesById(pkg.optionalXml(part("/footnotes", "footnotes.xml")), "footnote")
        endnotes = notesById(pkg.optionalXml(part("/endnotes", "endnotes.xml")), "endnote")

        val blocks = ArrayList<Block>()
        BodyReader(blocks).apply { read(body) }.finish()
        readNotes()
        notes.appendTo(blocks)

        val core = pkg.optionalXml(packageRels.values.firstOrNull { it.type.endsWith("/core-properties") }?.target ?: "docProps/core.xml")
        return Doc(
            blocks = mergeRuns(blocks),
            title = core?.child("title")?.text()?.collapseSpaces()?.takeIf { it.isNotEmpty() },
            author = core?.child("creator")?.text()?.collapseSpaces()?.takeIf { it.isNotEmpty() },
            language = core?.child("language")?.text()?.trim()?.takeIf { it.isNotEmpty() } ?: language,
        )
    }

    private fun part(typeSuffix: String, fallback: String): String =
        rels.values.firstOrNull { it.type.endsWith(typeSuffix) && !it.external }?.target ?: (parentDir(main) + fallback)

    private fun rels(path: String, base: String): Map<String, Rel> {
        val root = pkg.optionalXml(path) ?: return emptyMap()
        val result = HashMap<String, Rel>()
        for (r in root.children("Relationship")) {
            val id = r.attr("Id") ?: continue
            val target = r.attr("Target") ?: continue
            val external = r.attr("TargetMode").equals("External", ignoreCase = true)
            result[id] = Rel(r.attr("Type").orEmpty(), if (external) target else resolvePath(base, target), external)
        }
        return result
    }

    private fun notesById(root: XmlElement?, name: String): Map<String, XmlElement> {
        if (root == null) return emptyMap()
        val result = HashMap<String, XmlElement>()
        for (n in root.children(name)) {
            val type = n.attrNotRel("type")
            if (type == "separator" || type == "continuationSeparator" || type == "continuationNotice") continue
            n.attrNotRel("id")?.let { result[it] = n }
        }
        return result
    }

    private fun readNotes() {
        readingNotes = true
        for ((footnote, id, index) in noteQueue) {
            val note = (if (footnote) footnotes else endnotes)[id] ?: continue
            val blocks = ArrayList<Block>()
            BodyReader(blocks).apply { read(note) }.finish()
            notes.fill(index, trimLeadingSpace(mergeRuns(blocks)))
        }
    }

    // the note number Word puts first is dropped and leaves its space behind
    private fun trimLeadingSpace(blocks: List<Block>): List<Block> {
        val first = blocks.firstOrNull() as? Block.Paragraph ?: return blocks
        val text = first.content.firstOrNull() as? Inline.Text ?: return blocks
        val trimmed = text.text.trimStart()
        val content = if (trimmed.isEmpty()) first.content.drop(1) else listOf(text.copy(text = trimmed)) + first.content.drop(1)
        return listOf(Block.Paragraph(content, first.align)) + blocks.drop(1)
    }

    private fun loadStyles(root: XmlElement?) {
        if (root == null) return
        val rPrDefault = root.child("docDefaults")?.child("rPrDefault")?.child("rPr")
        defaults.read(rPrDefault)
        language = rPrDefault?.child("lang")?.attrNotRel("val")
        for (s in root.children("style")) {
            val id = s.attrNotRel("styleId") ?: continue
            styles[id] = Style(
                name = (s.child("name")?.attrNotRel("val") ?: id).lowercase().trim(),
                basedOn = s.child("basedOn")?.attrNotRel("val"),
                pPr = s.child("pPr"),
                rPr = s.child("rPr"),
            )
            val isDefault = s.attrNotRel("default").let { it == "1" || it == "true" || it == "on" }
            if (defaultStyle == null && isDefault && s.attrNotRel("type") == "paragraph") defaultStyle = id
        }
    }

    private fun chain(id: String?): List<Style> {
        val result = ArrayList<Style>()
        var current = id
        val seen = HashSet<String>()
        while (current != null && seen.add(current) && result.size < 32) {
            val style = styles[current] ?: break
            result.add(style)
            current = style.basedOn
        }
        return result
    }

    private fun paragraphStyle(id: String?): ParagraphStyle = paragraphStyles.getOrPut(id.orEmpty()) {
        val chain = chain(id)
        var heading: Int? = null
        for (s in chain) {
            heading = headingLevel(s.name) ?: s.pPr?.child("outlineLvl")?.attrNotRel("val")?.toIntOrNull()?.let { if (it in 0..8) it + 1 else 0 }
            if (heading != null) break
        }
        val run = RunProps()
        for (s in chain.asReversed()) run.read(s.rPr)
        val kind = when {
            heading != null && heading > 0 -> Kind.HEADING
            chain.any { it.name in QUOTE_NAMES } -> Kind.QUOTE
            id != defaultStyle && (chain.any { CODE_NAME.matches(it.name) } || run.mono == true) -> Kind.CODE
            else -> Kind.NORMAL
        }
        val numPr = chain.firstNotNullOfOrNull { it.pPr?.child("numPr") }
        ParagraphStyle(
            heading = heading?.takeIf { it > 0 },
            kind = kind,
            align = chain.firstNotNullOfOrNull { s -> s.pPr?.child("jc")?.let(::alignOf) },
            numId = numPr?.child("numId")?.attrNotRel("val"),
            level = numPr?.child("ilvl")?.attrNotRel("val")?.toIntOrNull(),
            indent = chain.firstNotNullOfOrNull { s -> s.pPr?.child("ind")?.let(::leftIndent) },
            pageBreakBefore = chain.firstNotNullOfOrNull { s -> s.pPr?.child("pageBreakBefore")?.let(::onOff) } ?: false,
            run = run,
        )
    }

    private fun headingLevel(name: String): Int? = when (name) {
        in TITLE_NAMES -> 1
        in SUBTITLE_NAMES -> 2
        else -> HEADING_NAME.matchEntire(name)?.groupValues?.get(1)?.toInt()
    }

    private fun characterStyle(id: String): RunProps = characterStyles.getOrPut(id) {
        val run = RunProps()
        for (s in chain(id).asReversed()) run.read(s.rPr)
        run
    }

    private fun alignOf(jc: XmlElement): Align? = when (jc.attrNotRel("val")) {
        "left", "start" -> Align.START
        "center" -> Align.CENTER
        "right", "end" -> Align.END
        "both", "distribute", "lowKashida", "mediumKashida", "highKashida", "thaiDistribute" -> Align.JUSTIFY
        else -> null
    }

    private fun leftIndent(ind: XmlElement): Int? = (ind.attrNotRel("left") ?: ind.attrNotRel("start"))?.toIntOrNull()

    private fun loadNumbering(root: XmlElement?) {
        if (root == null) return
        for (a in root.children("abstractNum")) {
            val id = a.attrNotRel("abstractNumId") ?: continue
            a.child("numStyleLink")?.attrNotRel("val")?.let { abstractLinks[id] = it }
            val levels = HashMap<Int, Level>()
            for (lvl in a.children("lvl")) {
                val ilvl = lvl.attrNotRel("ilvl")?.toIntOrNull() ?: continue
                levels[ilvl] = level(lvl)
            }
            abstractLevels[id] = levels
        }
        for (num in root.children("num")) {
            val id = num.attrNotRel("numId") ?: continue
            val abstract = num.child("abstractNumId")?.attrNotRel("val") ?: continue
            numAbstract[id] = abstract
            val overrides = HashMap<Int, Int>()
            for (o in num.children("lvlOverride")) {
                val ilvl = o.attrNotRel("ilvl")?.toIntOrNull() ?: continue
                (o.child("startOverride")?.attrNotRel("val")?.toIntOrNull() ?: o.child("lvl")?.child("start")?.attrNotRel("val")?.toIntOrNull())
                    ?.let { overrides[ilvl] = it }
            }
            if (overrides.isNotEmpty()) numOverrides[id] = overrides
        }
    }

    private fun level(lvl: XmlElement): Level {
        val format = lvl.child("numFmt")?.attrNotRel("val") ?: "decimal"
        return Level(
            ordered = format != "bullet" && format != "none",
            none = format == "none",
            start = lvl.child("start")?.attrNotRel("val")?.toIntOrNull() ?: 1,
            indent = lvl.child("pPr")?.child("ind")?.let(::leftIndent),
        )
    }

    // a numbering style link points at a style whose numbering has the actual levels
    private fun abstractOf(numId: String): String? {
        var abstract = numAbstract[numId] ?: return null
        repeat(4) {
            val link = abstractLinks[abstract] ?: return abstract
            val linkedNum = chain(link).firstNotNullOfOrNull { it.pPr?.child("numPr")?.child("numId")?.attrNotRel("val") } ?: return abstract
            abstract = numAbstract[linkedNum] ?: return abstract
        }
        return abstract
    }

    private fun levelOf(numId: String, ilvl: Int): Level? = abstractOf(numId)?.let { abstractLevels[it]?.get(ilvl) }

    private fun listIndent(numId: String, ilvl: Int): Int = levelOf(numId, ilvl)?.indent ?: (720 * (ilvl + 1))

    private fun nextNumber(numId: String, ilvl: Int, level: Level): Int {
        val counter = counters.getOrPut(abstractOf(numId) ?: numId) { IntArray(9) { Int.MIN_VALUE } }
        val i = ilvl.coerceIn(0, 8)
        numOverrides[numId]?.get(i)?.let { if (overridden.add("$numId:$i")) counter[i] = it - 1 }
        val n = if (counter[i] == Int.MIN_VALUE) level.start else counter[i] + 1
        counter[i] = n
        for (k in i + 1 until 9) counter[k] = Int.MIN_VALUE
        return n
    }

    private inner class BodyReader(private val out: MutableList<Block>) {
        private val lists = ListCollector(out)

        fun read(container: XmlElement) {
            nesting.within {
                for (el in container.elements) {
                    when (el.localName) {
                        "p" -> paragraph(el)
                        "tbl" -> {
                            val table = table(el) ?: continue
                            val indent = el.child("tblPr")?.child("tblInd")?.attrNotRel("w")?.toIntOrNull() ?: 0
                            val level = if (indent > 0 && lists.isOpen) lists.levelFor(indent, ::listIndent) else null
                            if (level != null) {
                                lists.continuation(level, listOf(table))
                            } else {
                                lists.flush()
                                out.add(table)
                            }
                        }
                        "sdt" -> el.child("sdtContent")?.let(::read)
                        "customXml", "ins", "moveTo", "smartTag" -> read(el)
                        "AlternateContent" -> alternate(el)?.let(::read)
                    }
                }
            }
        }

        fun finish() {
            lists.flush()
        }

        private fun paragraph(p: XmlElement) {
            val pPr = p.child("pPr")
            val styleId = pPr?.child("pStyle")?.attrNotRel("val") ?: defaultStyle
            val info = paragraphStyle(styleId)
            val outline = pPr?.child("outlineLvl")?.attrNotRel("val")?.toIntOrNull()
            val headingLevel = if (outline != null) (if (outline in 0..8) outline + 1 else null) else info.heading
            val kind = if (headingLevel != null) Kind.HEADING else if (info.kind == Kind.HEADING) Kind.NORMAL else info.kind
            val align = pPr?.child("jc")?.let(::alignOf) ?: info.align ?: Align.START
            val numPr = pPr?.child("numPr")
            val numId = numPr?.child("numId")?.attrNotRel("val") ?: info.numId
            val ilvl = numPr?.child("ilvl")?.attrNotRel("val")?.toIntOrNull() ?: info.level ?: 0
            val indent = pPr?.child("ind")?.let(::leftIndent) ?: info.indent ?: 0
            val breakBefore = pPr?.child("pageBreakBefore")?.let(::onOff) ?: info.pageBreakBefore
            val border = pPr?.child("pBdr")?.child("bottom")?.attrNotRel("val")?.let { it != "none" && it != "nil" } ?: false
            val section = pPr?.child("sectPr")?.let { it.child("type")?.attrNotRel("val") != "continuous" } ?: false

            val segments = Segments()
            val boxes = ArrayList<Block>()
            inlines(p, RunContext(if (kind == Kind.NORMAL) info.run else null, kind == Kind.CODE, null), segments, boxes)
            fields.removeAll { !it.inResult }

            var blocks = when (kind) {
                Kind.HEADING -> segments.heading(headingLevel ?: 1)
                Kind.CODE -> segments.code()
                Kind.QUOTE -> segments.blocks { Block.Paragraph(it, align) }.let { if (it.isEmpty()) it else listOf(Block.Quote(it)) }
                Kind.NORMAL -> segments.blocks { Block.Paragraph(it, align) }
            }
            if (blocks.isEmpty() && border) blocks = listOf(Block.Rule)
            if (breakBefore) blocks = listOf(Block.PageBreak) + blocks
            blocks = blocks + boxes
            if (section) blocks = blocks + Block.PageBreak
            val level = if (numId != null && numId != "0" && kind != Kind.HEADING) levelOf(numId, ilvl) else null
            if (blocks.isEmpty() && (level == null || level.none)) return

            val continuation = if (level == null && kind != Kind.HEADING && indent > 0 && lists.isOpen) lists.levelFor(indent, ::listIndent) else null
            when {
                level != null && !level.none -> {
                    val number = nextNumber(numId!!, ilvl, level)
                    lists.item(numId, ilvl.coerceIn(0, 8), level.ordered, if (level.ordered) number else 1, blocks)
                }
                continuation != null -> lists.continuation(continuation, blocks)
                else -> {
                    lists.flush()
                    out.addAll(blocks)
                }
            }
        }

        private fun table(tbl: XmlElement): Block.Table? {
            val rows = ArrayList<List<Cell>>()
            var header = 0
            var leading = true
            for (tr in tbl.flatten("tr")) {
                val trPr = tr.child("trPr")
                val cells = ArrayList<Cell>()
                trPr?.child("gridBefore")?.attrNotRel("val")?.toIntOrNull()?.takeIf { it > 0 }?.let { cells.add(Cell(emptyList(), it.coerceAtMost(MAX_COL_SPAN))) }
                for (tc in tr.flatten("tc")) {
                    val tcPr = tc.child("tcPr")
                    val span = tcPr?.child("gridSpan")?.attrNotRel("val")?.toIntOrNull()?.coerceIn(1, MAX_COL_SPAN) ?: 1
                    val hMerge = tcPr?.child("hMerge")
                    if (hMerge != null && hMerge.attrNotRel("val") != "restart" && cells.isNotEmpty()) {
                        val last = cells.removeAt(cells.lastIndex)
                        cells.add(Cell(last.blocks, (last.colSpan + span).coerceAtMost(MAX_COL_SPAN)))
                        continue
                    }
                    val vMerge = tcPr?.child("vMerge")
                    val blocks = if (vMerge != null && vMerge.attrNotRel("val") != "restart") {
                        emptyList()
                    } else {
                        ArrayList<Block>().also { BodyReader(it).apply { read(tc) }.finish() }
                    }
                    cells.add(Cell(blocks, span))
                }
                if (cells.isEmpty()) continue
                if (leading && onOff(trPr?.child("tblHeader"))) header++ else leading = false
                rows.add(cells)
            }
            return if (rows.isEmpty()) null else Block.Table(rows, header)
        }

        private fun inlines(container: XmlElement, ctx: RunContext, segments: Segments, boxes: MutableList<Block>) {
            nesting.within {
                for (el in container.elements) {
                    when (el.localName) {
                        "r" -> run(el, ctx, segments, boxes)
                        "hyperlink" -> inlines(el, ctx.withLink(hyperlink(el)), segments, boxes)
                        "fldSimple" -> inlines(el, ctx.withLink(fieldLink(el.attrNotRel("instr").orEmpty())), segments, boxes)
                        "ins", "moveTo", "smartTag", "customXml", "dir", "bdo" -> inlines(el, ctx, segments, boxes)
                        "sdt" -> el.child("sdtContent")?.let { inlines(it, ctx, segments, boxes) }
                        "AlternateContent" -> alternate(el)?.let { inlines(it, ctx, segments, boxes) }
                        "oMath", "oMathPara" -> {
                            val text = el.find("t").joinToString("") { it.text() }
                            if (text.isNotEmpty()) segments.inline.preserved(text, runProps(ctx, null).style(currentLink(ctx)))
                        }
                    }
                }
            }
        }

        private fun runProps(ctx: RunContext, rPr: XmlElement?): RunProps {
            val props = defaults.copy()
            ctx.paragraphRun?.let(props::merge)
            rPr?.child("rStyle")?.attrNotRel("val")?.let { props.merge(characterStyle(it)) }
            return props.read(rPr)
        }

        private fun run(r: XmlElement, ctx: RunContext, segments: Segments, boxes: MutableList<Block>) {
            val props = runProps(ctx, r.child("rPr"))
            runContent(r, props, ctx, segments, boxes)
        }

        private fun runContent(r: XmlElement, props: RunProps, ctx: RunContext, segments: Segments, boxes: MutableList<Block>) {
            val inline = segments.inline
            for (el in r.elements) {
                val visible = props.hidden != true && fields.all { it.inResult }
                val style = props.style(currentLink(ctx))
                when (el.localName) {
                    "t" -> if (visible) inline.preserved(symbolText(props.font, el.text()), style)
                    "instrText" -> fields.lastOrNull()?.takeIf { !it.inResult }?.instruction?.append(el.text())
                    "fldChar" -> fieldChar(el)
                    "tab", "ptab" -> if (visible) inline.preserved(if (ctx.code) "\t" else " ", style)
                    "br" -> if (visible) {
                        if (el.attrNotRel("type") == "page") segments.block(Block.PageBreak) else inline.lineBreak()
                    }
                    "cr" -> if (visible) inline.lineBreak()
                    "noBreakHyphen" -> if (visible) inline.preserved("-", style)
                    "sym" -> if (visible) {
                        val code = el.attrNotRel("char")?.toIntOrNull(16)
                        if (code != null) SymbolFonts.map(el.attrNotRel("font"), code)?.let { inline.preserved(it, style) }
                    }
                    "drawing" -> if (visible) drawing(el, segments, boxes)
                    "pict", "object" -> if (visible) vml(el, segments, boxes)
                    "footnoteReference", "endnoteReference" -> if (visible && !readingNotes) {
                        el.attrNotRel("id")?.let { id ->
                            val index = notes.reserve()
                            noteQueue.add(Triple(el.localName == "footnoteReference", id, index))
                            inline.inline(notes.marker(index))
                        }
                    }
                    "AlternateContent" -> alternate(el)?.let { runContent(it, props, ctx, segments, boxes) }
                    "ruby" -> el.child("rubyBase")?.let { inlines(it, ctx, segments, boxes) }
                }
            }
        }

        private fun symbolText(font: String?, text: String): String = if (font != null && SymbolFonts.isSymbolFont(font)) SymbolFonts.text(font, text) else text

        private fun currentLink(ctx: RunContext): String? = fields.lastOrNull { it.link != null }?.link ?: ctx.link

        private fun fieldChar(el: XmlElement) {
            when (el.attrNotRel("fldCharType")) {
                "begin" -> fields.add(Field())
                "separate" -> fields.lastOrNull()?.let {
                    it.inResult = true
                    it.link = fieldLink(it.instruction.toString())
                }
                "end" -> if (fields.isNotEmpty()) fields.removeAt(fields.lastIndex)
            }
        }

        private fun hyperlink(el: XmlElement): String? {
            val target = el.attrRel("id")?.let { rels[it] }?.takeIf { it.external }?.target ?: return null
            val anchor = el.attrNotRel("anchor")
            return externalLink(if (anchor != null && '#' !in target) "$target#$anchor" else target)
        }

        private fun drawing(el: XmlElement, segments: Segments, boxes: MutableList<Block>) {
            val frame = el.child("inline") ?: el.child("anchor")
            val docPr = frame?.child("docPr")
            val alt = (docPr?.attr("descr")?.takeIf { it.isNotBlank() } ?: docPr?.attr("title")).orEmpty().collapseSpaces()
            val extent = frame?.child("extent")
            val blips = el.find("blip", stop = "txbxContent")
            for (blip in blips) {
                val bytes = image(blip.attrRel("embed")) ?: continue
                val w = extent?.attr("cx")?.toLongOrNull()?.takeIf { blips.size == 1 }?.let { it / 12700f }
                val h = extent?.attr("cy")?.toLongOrNull()?.takeIf { blips.size == 1 }?.let { it / 12700f }
                segments.block(Block.Picture(bytes, w ?: 0f, h ?: 0f, alt))
            }
            textBoxes(el, boxes)
        }

        private fun vml(el: XmlElement, segments: Segments, boxes: MutableList<Block>) {
            if (el.find("rect").any { it.attr("hr") == "t" }) segments.block(Block.Rule)
            val shapes = ArrayList<XmlElement>()
            if (el.child("imagedata") != null) shapes.add(el)
            val stack = ArrayList(el.elements)
            while (stack.isNotEmpty()) {
                val e = stack.removeAt(stack.lastIndex)
                if (e.localName == "txbxContent") continue
                if (e.child("imagedata") != null) shapes.add(e)
                stack.addAll(e.elements)
            }
            for (shape in shapes) {
                val data = shape.child("imagedata") ?: continue
                val bytes = image(data.attrRel("id") ?: data.attr("relid")) ?: continue
                val css = shape.attr("style").orEmpty().split(';').associate { it.substringBefore(':').trim().lowercase() to it.substringAfter(':', "").trim() }
                val (w, h) = pictureSize(bytes, lengthPt(css["width"]), lengthPt(css["height"]))
                segments.block(Block.Picture(bytes, w, h, (data.attr("title") ?: shape.attr("alt")).orEmpty().collapseSpaces()))
            }
            textBoxes(el, boxes)
        }

        private fun textBoxes(el: XmlElement, boxes: MutableList<Block>) {
            for (box in el.find("txbxContent")) BodyReader(boxes).apply { read(box) }.finish()
        }

        private fun image(id: String?): ByteArray? {
            val rel = rels[id ?: return null] ?: return null
            if (rel.external) return null
            return pkg.optionalBytes(rel.target) ?: pkg.optionalBytes(percentDecode(rel.target))
        }
    }

    // HYPERLINK "url" \l "anchor" \o "tip", a link to a bookmark only has no place in the model
    private fun fieldLink(instruction: String): String? {
        val tokens = ArrayList<String>()
        var i = 0
        while (i < instruction.length) {
            val c = instruction[i]
            when {
                c.isWhitespace() -> i++
                c == '"' -> {
                    val end = instruction.indexOf('"', i + 1).let { if (it < 0) instruction.length else it }
                    tokens.add(instruction.substring(i + 1, end))
                    i = end + 1
                }
                else -> {
                    var end = i
                    while (end < instruction.length && !instruction[end].isWhitespace()) end++
                    tokens.add(instruction.substring(i, end))
                    i = end
                }
            }
        }
        if (!tokens.firstOrNull().equals("HYPERLINK", ignoreCase = true)) return null
        var url: String? = null
        var anchor: String? = null
        var k = 1
        while (k < tokens.size) {
            val t = tokens[k]
            when {
                t == "\\l" -> {
                    anchor = tokens.getOrNull(k + 1)
                    k++
                }
                t == "\\o" || t == "\\t" -> k++
                t.startsWith("\\") -> {}
                url == null -> url = t
            }
            k++
        }
        val link = url ?: return null
        return externalLink(if (anchor != null && '#' !in link) "$link#$anchor" else link)
    }

    private fun XmlElement.flatten(name: String): List<XmlElement> {
        val result = ArrayList<XmlElement>()
        for (el in elements) {
            when (el.localName) {
                name -> result.add(el)
                "sdt" -> el.child("sdtContent")?.let { result.addAll(it.flatten(name)) }
                "customXml", "ins", "moveTo" -> result.addAll(el.flatten(name))
            }
        }
        return result
    }
}
