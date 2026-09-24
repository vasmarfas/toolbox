package com.vasmarfas.card.tools.documents

private sealed interface MdBlock

private class MdParagraph(val text: String) : MdBlock

private class MdHeading(val level: Int, val text: String) : MdBlock

private class MdCode(val text: String) : MdBlock

private class MdQuote(val children: List<MdBlock>) : MdBlock

private class MdList(val ordered: Boolean, val start: Int, val items: List<List<MdBlock>>) : MdBlock

private class MdTable(val rows: List<List<String>>, val aligns: List<Align>) : MdBlock

private data object MdRule : MdBlock

private data object MdPageBreak : MdBlock

private class Marker(val indent: Int, val content: Int, val ordered: Boolean, val char: Char, val number: Int, val empty: Boolean, val first: String)

private val ATX = Regex("#{1,6}(?:[ \\t].*)?")
private val REFERENCE = Regex("""\[([^\]]{1,999})\]:[ \t]*(<[^>\n]*>|\S+)(?:[ \t]+(?:"[^"]*"|'[^']*'|\([^)]*\)))?[ \t]*""")
private val DELIMITER_CELL = Regex(":?-+:?")
private val FRONT_MATTER_LINE = Regex("[A-Za-z_][A-Za-z0-9_-]*:.*|#.*|\\s*")
private val PAGE_BREAK_HTML = Regex("""<(div|p|hr|br)\b[^>]*(page-break-(before|after)\s*:\s*always|break-(before|after)\s*:\s*page)[^>]*>(\s*</\1>)?""", RegexOption.IGNORE_CASE)
private val RAW_BLOCK = Regex("""<(script|style|textarea|pre)(\s|>|$).*""", RegexOption.IGNORE_CASE)

internal object MarkdownReader {
    fun read(text: String): Doc {
        val lines = text.trimStart(BOM).replace("\r\n", "\n").replace('\r', '\n').split('\n')
        var title: String? = null
        var author: String? = null
        var language: String? = null
        var start = 0
        if (lines.firstOrNull()?.trimEnd() == "---") {
            val end = (1 until minOf(lines.size, 100)).firstOrNull { lines[it].trimEnd() == "---" || lines[it].trimEnd() == "..." }
            if (end != null && (1 until end).all { FRONT_MATTER_LINE.matches(lines[it]) }) {
                for (i in 1 until end) {
                    val key = lines[i].substringBefore(':').trim().lowercase()
                    val value = yamlScalar(lines[i].substringAfter(':', "").trim())
                    when (key) {
                        "title" -> title = value
                        "author" -> author = value
                        "lang", "language" -> language = value
                    }
                }
                start = end + 1
            }
        }
        val refs = HashMap<String, LinkReference>()
        val blocks = MdBlockParser(refs).parse(lines.subList(start, lines.size), 0)
        return Doc(MdConverter(refs).convert(blocks), title, author, language)
    }
}

private fun yamlScalar(raw: String): String? {
    val value = when {
        raw.length >= 2 && raw.startsWith('"') && raw.endsWith('"') -> raw.substring(1, raw.length - 1).replace("\\\"", "\"").replace("\\\\", "\\")
        raw.length >= 2 && raw.startsWith('\'') && raw.endsWith('\'') -> raw.substring(1, raw.length - 1).replace("''", "'")
        else -> raw
    }
    return value.trim().ifEmpty { null }
}

private class MdBlockParser(private val refs: MutableMap<String, LinkReference>) {
    fun parse(lines: List<String>, depth: Int): List<MdBlock> {
        val out = ArrayList<MdBlock>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (line.isBlank()) {
                i++
                continue
            }
            val indent = indentOf(line)
            if (indent >= 4) {
                i = indentedCode(lines, i, out)
                continue
            }
            val t = content(line)
            i = when {
                fence(t) != null -> fencedCode(lines, i, indent, out)
                ATX.matches(t) -> {
                    out.add(atx(t))
                    i + 1
                }
                isRule(t) -> {
                    out.add(MdRule)
                    i + 1
                }
                t.startsWith("<!--") -> skipComment(lines, i)
                PAGE_BREAK_HTML.matches(t.trim()) -> {
                    out.add(MdPageBreak)
                    i + 1
                }
                RAW_BLOCK.matches(t) -> rawBlock(lines, i, out)
                t.startsWith(">") && depth < MAX_NESTING -> quote(lines, i, depth, out)
                marker(line) != null && depth < MAX_NESTING -> list(lines, i, depth, out)
                tableAt(lines, i) -> table(lines, i, out)
                else -> paragraph(lines, i, out)
            }
        }
        return out
    }

    private fun indentOf(line: String): Int {
        var column = 0
        for (c in line) {
            when (c) {
                ' ' -> column++
                '\t' -> column += 4 - column % 4
                else -> return column
            }
        }
        return column
    }

    private fun content(line: String): String = line.trimStart(' ', '\t')

    private fun stripColumns(line: String, columns: Int): String {
        var column = 0
        var i = 0
        while (i < line.length && column < columns) {
            when (line[i]) {
                ' ' -> column++
                '\t' -> {
                    val width = 4 - column % 4
                    if (column + width > columns) return " ".repeat(column + width - columns) + line.substring(i + 1)
                    column += width
                }
                else -> break
            }
            i++
        }
        return line.substring(i)
    }

    private fun fence(t: String): Pair<Char, Int>? {
        val c = t.firstOrNull() ?: return null
        if (c != '`' && c != '~') return null
        var n = 0
        while (n < t.length && t[n] == c) n++
        if (n < 3) return null
        if (c == '`' && t.indexOf('`', n) >= 0) return null
        return c to n
    }

    private fun fencedCode(lines: List<String>, start: Int, indent: Int, out: MutableList<MdBlock>): Int {
        val (char, length) = fence(content(lines[start]))!!
        val code = ArrayList<String>()
        var i = start + 1
        while (i < lines.size) {
            val line = lines[i]
            val ind = indentOf(line)
            if (ind < 4) {
                val t = content(line)
                var n = 0
                while (n < t.length && t[n] == char) n++
                if (n >= length && t.substring(n).isBlank()) {
                    i++
                    break
                }
            }
            code.add(stripColumns(line, minOf(indent, ind)))
            i++
        }
        out.add(MdCode(code.joinToString("\n")))
        return i
    }

    private fun indentedCode(lines: List<String>, start: Int, out: MutableList<MdBlock>): Int {
        val code = ArrayList<String>()
        var i = start
        while (i < lines.size && (lines[i].isBlank() || indentOf(lines[i]) >= 4)) {
            code.add(if (lines[i].isBlank()) "" else stripColumns(lines[i], 4))
            i++
        }
        while (code.isNotEmpty() && code.last().isBlank()) code.removeAt(code.lastIndex)
        out.add(MdCode(code.joinToString("\n")))
        return i
    }

    private fun atx(t: String): MdHeading {
        var level = 0
        while (level < t.length && t[level] == '#') level++
        var text = t.substring(level).trim()
        val closing = text.trimEnd('#')
        if (closing.isEmpty() || closing.endsWith(' ') || closing.endsWith('\t')) text = closing.trim()
        return MdHeading(level, text)
    }

    private fun isRule(t: String): Boolean {
        val c = t.firstOrNull() ?: return false
        if (c != '-' && c != '*' && c != '_') return false
        var count = 0
        for (ch in t) {
            when (ch) {
                c -> count++
                ' ', '\t' -> {}
                else -> return false
            }
        }
        return count >= 3
    }

    private fun skipComment(lines: List<String>, start: Int): Int {
        var i = start
        while (i < lines.size) {
            if (lines[i].contains("-->") && (i > start || lines[i].indexOf("-->") > lines[i].indexOf("<!--") + 3)) return i + 1
            i++
        }
        return i
    }

    private fun rawBlock(lines: List<String>, start: Int, out: MutableList<MdBlock>): Int {
        val name = RAW_BLOCK.find(content(lines[start]))!!.groupValues[1].lowercase()
        val raw = ArrayList<String>()
        var i = start
        while (i < lines.size) {
            raw.add(lines[i])
            if (lines[i].contains("</$name", ignoreCase = true)) {
                i++
                break
            }
            i++
        }
        if (name == "pre") {
            val text = raw.joinToString("\n").replace(Regex("<[^>]*>"), "")
            out.add(MdCode(HtmlEntities.decode(text).trim('\n')))
        }
        return i
    }

    private fun quote(lines: List<String>, start: Int, depth: Int, out: MutableList<MdBlock>): Int {
        val inner = ArrayList<String>()
        var i = start
        var fenced = false
        while (i < lines.size) {
            val line = lines[i]
            val t = content(line)
            if (indentOf(line) < 4 && t.startsWith(">")) {
                val rest = stripColumns(t.substring(1), 1)
                inner.add(rest)
                if (fence(content(rest)) != null) fenced = !fenced
                i++
                continue
            }
            if (line.isBlank() || fenced || inner.isEmpty() || inner.last().isBlank() || startsBlock(line)) break
            inner.add(line)
            i++
        }
        out.add(MdQuote(parse(inner, depth + 1)))
        return i
    }

    private fun marker(line: String): Marker? {
        val indent = indentOf(line)
        if (indent >= 4) return null
        val t = content(line)
        if (t.isEmpty()) return null
        val c = t[0]
        var end: Int
        var ordered = false
        var number = 0
        if (c == '-' || c == '+' || c == '*') {
            end = 1
        } else if (c.isDigit()) {
            end = 0
            while (end < t.length && t[end].isDigit() && end < 9) end++
            if (end >= t.length || (t[end] != '.' && t[end] != ')')) return null
            number = t.substring(0, end).toInt()
            ordered = true
            end++
        } else {
            return null
        }
        if (end < t.length && t[end] != ' ' && t[end] != '\t') return null
        val rest = t.substring(end)
        val empty = rest.isBlank()
        val spaces = indentOf(rest)
        val gap = if (empty || spaces > 4) 1 else spaces
        return Marker(indent, indent + end + gap, ordered, if (ordered) t[end - 1] else c, number, empty, if (empty) "" else stripColumns(rest, gap))
    }

    private fun startsBlock(line: String): Boolean {
        if (indentOf(line) >= 4) return false
        val t = content(line)
        return t.startsWith(">") || fence(t) != null || ATX.matches(t) || isRule(t) || marker(line) != null || t.startsWith("<!--")
    }

    private fun list(lines: List<String>, start: Int, depth: Int, out: MutableList<MdBlock>): Int {
        val first = marker(lines[start])!!
        val items = ArrayList<List<MdBlock>>()
        var i = start
        while (i < lines.size) {
            val m = marker(lines[i]) ?: break
            if (m.ordered != first.ordered || m.char != first.char || isRule(content(lines[i]))) break
            val itemLines = ArrayList<String>()
            itemLines.add(m.first)
            i++
            var blank = m.empty
            var fenced = fence(content(m.first)) != null
            var blanks = if (m.empty) 1 else 0
            while (i < lines.size) {
                val l = lines[i]
                if (l.isBlank()) {
                    if (m.empty && itemLines.all { it.isBlank() } && blanks >= 1) break
                    itemLines.add("")
                    blank = true
                    blanks++
                    i++
                    continue
                }
                if (indentOf(l) >= m.content) {
                    val inner = stripColumns(l, m.content)
                    if (fence(content(inner)) != null) fenced = !fenced
                    itemLines.add(inner)
                    blank = false
                    i++
                    continue
                }
                if (blank || fenced || startsBlock(l)) break
                val last = itemLines.lastOrNull { it.isNotBlank() }
                if (last == null || indentOf(last) >= 4 || ATX.matches(content(last)) || isRule(content(last))) break
                itemLines.add(content(l))
                i++
            }
            while (itemLines.isNotEmpty() && itemLines.last().isBlank()) itemLines.removeAt(itemLines.lastIndex)
            items.add(parse(itemLines, depth + 1))
            var j = i
            while (j < lines.size && lines[j].isBlank()) j++
            val next = if (j < lines.size) marker(lines[j]) else null
            if (next == null || next.ordered != first.ordered || next.char != first.char || next.indent >= first.content) break
            i = j
        }
        out.add(MdList(first.ordered, if (first.ordered) first.number else 1, items))
        return i
    }

    private fun splitRow(line: String): List<String> {
        var t = line.trim()
        if (t.startsWith('|')) t = t.substring(1)
        if (t.endsWith('|') && !t.endsWith("\\|")) t = t.substring(0, t.length - 1)
        val cells = ArrayList<String>()
        val cell = StringBuilder()
        var i = 0
        while (i < t.length) {
            val c = t[i]
            if (c == '\\' && i + 1 < t.length && t[i + 1] == '|') {
                cell.append("\\|")
                i += 2
                continue
            }
            if (c == '|') {
                cells.add(cell.toString().trim())
                cell.clear()
            } else {
                cell.append(c)
            }
            i++
        }
        cells.add(cell.toString().trim())
        return cells
    }

    private fun delimiterRow(line: String): List<Align>? {
        if ('-' !in line || !(line.contains('|') || line.trim().startsWith(':'))) return null
        val cells = splitRow(line)
        if (cells.any { !DELIMITER_CELL.matches(it) }) return null
        return cells.map {
            when {
                it.startsWith(':') && it.endsWith(':') -> Align.CENTER
                it.endsWith(':') -> Align.END
                else -> Align.START
            }
        }
    }

    private fun tableAt(lines: List<String>, i: Int): Boolean {
        if (i + 1 >= lines.size || '|' !in lines[i] || indentOf(lines[i + 1]) >= 4) return false
        val aligns = delimiterRow(lines[i + 1]) ?: return false
        return splitRow(lines[i]).size == aligns.size
    }

    private fun table(lines: List<String>, start: Int, out: MutableList<MdBlock>): Int {
        val header = splitRow(lines[start])
        val aligns = delimiterRow(lines[start + 1])!!
        val rows = ArrayList<List<String>>()
        rows.add(header)
        var i = start + 2
        while (i < lines.size && lines[i].isNotBlank() && !startsBlock(lines[i])) {
            val cells = splitRow(lines[i])
            rows.add(List(header.size) { cells.getOrElse(it) { "" } })
            i++
        }
        out.add(MdTable(rows, aligns))
        return i
    }

    private fun paragraph(lines: List<String>, start: Int, out: MutableList<MdBlock>): Int {
        val buffer = ArrayList<String>()
        buffer.add(content(lines[start]))
        var i = start + 1
        while (i < lines.size) {
            val line = lines[i]
            if (line.isBlank()) break
            if (indentOf(line) < 4) {
                val t = content(line).trimEnd()
                if (t.isNotEmpty() && (t.all { it == '=' } || t.all { it == '-' }) && takeReferences(buffer)) {
                    if (buffer.isEmpty()) {
                        buffer.add(content(line))
                        i++
                        continue
                    }
                    out.add(MdHeading(if (t[0] == '=') 1 else 2, buffer.joinToString("\n").trim()))
                    return i + 1
                }
                if (interrupts(line, t) || tableAt(lines, i)) break
            }
            buffer.add(content(line))
            i++
        }
        takeReferences(buffer)
        if (buffer.isNotEmpty()) out.add(MdParagraph(buffer.joinToString("\n").trimEnd()))
        return i
    }

    private fun interrupts(line: String, t: String): Boolean {
        if (t.startsWith(">") || fence(t) != null || ATX.matches(t) || isRule(t) || t.startsWith("<!--") || PAGE_BREAK_HTML.matches(t)) return true
        val m = marker(line) ?: return false
        return !m.empty && (!m.ordered || m.number == 1)
    }

    // always true, so it can sit in a condition
    private fun takeReferences(buffer: MutableList<String>): Boolean {
        while (buffer.isNotEmpty()) {
            val match = REFERENCE.matchEntire(buffer[0]) ?: break
            val label = normalizeLabel(match.groupValues[1])
            val url = match.groupValues[2].removeSurrounding("<", ">")
            if (label.isNotEmpty() && label !in refs) refs[label] = LinkReference(HtmlEntities.decode(url))
            buffer.removeAt(0)
        }
        return true
    }
}

private class MdConverter(private val refs: Map<String, LinkReference>) {
    fun convert(blocks: List<MdBlock>): List<Block> {
        val out = ArrayList<Block>()
        for (block in blocks) {
            when (block) {
                is MdParagraph -> out.addAll(inline(block.text) { Block.Paragraph(it) })
                is MdHeading -> {
                    val segments = Segments()
                    MarkdownInline(block.text, refs).render(segments)
                    out.addAll(segments.heading(block.level))
                }
                is MdCode -> if (block.text.isNotBlank()) out.add(Block.Code(block.text.trimEnd('\n')))
                is MdQuote -> out.add(Block.Quote(convert(block.children)))
                is MdList -> out.add(Block.ListBlock(block.ordered, block.items.map(::convert), block.start))
                is MdTable -> out.add(table(block))
                MdRule -> out.add(Block.Rule)
                MdPageBreak -> out.add(Block.PageBreak)
            }
        }
        return out
    }

    private fun inline(text: String, wrap: (List<Inline>) -> Block): List<Block> {
        val segments = Segments()
        MarkdownInline(text, refs).render(segments)
        return segments.blocks(wrap)
    }

    private fun table(table: MdTable): Block.Table {
        val rows = table.rows.map { row ->
            row.mapIndexed { i, cell ->
                val align = table.aligns.getOrElse(i) { Align.START }
                Cell(inline(cell) { Block.Paragraph(it, align) })
            }
        }
        return Block.Table(rows, headerRows = 1)
    }
}
