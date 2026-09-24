package com.vasmarfas.card.tools.documents

private const val PAGE_BREAK = "<div style=\"page-break-after: always;\"></div>"

// every paragraph is parsed back and compared with the source runs. Where the emphasis markers would
// read differently, it is written again with <strong>, <em> and <s>
internal object MarkdownWriter {
    fun write(doc: Doc): String {
        val lines = ArrayList<String>()
        if (!doc.title.isNullOrBlank() || !doc.author.isNullOrBlank() || !doc.language.isNullOrBlank()) {
            lines.add("---")
            doc.title?.takeIf { it.isNotBlank() }?.let { lines.add("title: ${yaml(it)}") }
            doc.author?.takeIf { it.isNotBlank() }?.let { lines.add("author: ${yaml(it)}") }
            doc.language?.takeIf { it.isNotBlank() }?.let { lines.add("lang: ${yaml(it)}") }
            lines.add("---")
            lines.add("")
        }
        lines.addAll(blocks(doc.blocks))
        return lines.joinToString("\n").trimEnd('\n') + "\n"
    }

    private fun yaml(value: String): String = "\"" + value.collapseSpaces().replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private fun blocks(blocks: List<Block>): List<String> {
        val out = ArrayList<String>()
        var previous: Block? = null
        for (block in blocks) {
            val lines = block(block, previous)
            if (lines.isEmpty()) continue
            if (out.isNotEmpty()) out.add("")
            out.addAll(lines)
            previous = block
        }
        return out
    }

    private fun block(block: Block, previous: Block?): List<String> = when (block) {
        is Block.Heading -> {
            var text = inline(block.content.map { if (it == Inline.LineBreak) Inline.Text(" ") else it }, table = false)
            if (text.endsWith('#')) text = text.dropLast(1) + "\\#"
            listOf("#".repeat(block.level.coerceIn(1, 6)) + " " + text)
        }
        is Block.Paragraph -> inline(block.content, table = false).takeIf { it.isNotBlank() }?.split('\n').orEmpty()
        is Block.Code -> {
            val fence = "`".repeat(maxOf(3, longestRun(block.text, '`') + 1))
            listOf(fence) + block.text.split('\n') + fence
        }
        is Block.Quote -> blocks(block.blocks).map { if (it.isEmpty()) ">" else "> $it" }
        is Block.ListBlock -> list(block, previous as? Block.ListBlock)
        is Block.Table -> table(block)
        is Block.Picture -> listOf(picture(block))
        Block.Rule -> listOf("***")
        Block.PageBreak -> listOf(PAGE_BREAK)
    }

    // two lists of the same kind in a row would merge, so the second gets the other bullet or delimiter
    private fun list(list: Block.ListBlock, previous: Block.ListBlock?): List<String> {
        val alternate = previous != null && previous.ordered == list.ordered
        val out = ArrayList<String>()
        val loose = list.items.any { it.size > 1 && !isTight(it) }
        list.items.forEachIndexed { i, item ->
            val marker = if (list.ordered) "${list.start + i}${if (alternate) ")" else "."} " else if (alternate) "* " else "- "
            val content = if (isTight(item)) block(item[0], null) + block(item[1], null) else blocks(item)
            if (loose && i > 0) out.add("")
            if (content.isEmpty()) {
                out.add(marker.trimEnd())
                return@forEachIndexed
            }
            val pad = " ".repeat(marker.length)
            content.forEachIndexed { k, line -> out.add(if (k == 0) marker + line else if (line.isEmpty()) "" else pad + line) }
        }
        return out
    }

    private fun isTight(item: List<Block>): Boolean {
        if (item.size != 2 || item[0] !is Block.Paragraph) return false
        val nested = item[1] as? Block.ListBlock ?: return false
        return nested.items.isNotEmpty() && (!nested.ordered || nested.start == 1) && block(item[0], null).isNotEmpty()
    }

    private fun table(table: Block.Table): List<String> {
        val rows = table.rows.filter { it.isNotEmpty() }
        if (rows.isEmpty()) return emptyList()
        val columns = rows.maxOf { row -> row.sumOf { it.colSpan } }
        val text = rows.map { row ->
            val cells = ArrayList<String>()
            for (cell in row) {
                cells.add(cellText(cell.blocks))
                repeat(cell.colSpan - 1) { cells.add("") }
            }
            while (cells.size < columns) cells.add("")
            cells
        }
        val aligns = ArrayList<Align>()
        for (cell in rows[0]) {
            val align = (cell.blocks.firstOrNull() as? Block.Paragraph)?.align ?: Align.START
            repeat(cell.colSpan) { aligns.add(align) }
        }
        while (aligns.size < columns) aligns.add(Align.START)
        val out = ArrayList<String>()
        out.add(row(text[0]))
        out.add(row(aligns.map { if (it == Align.CENTER) ":-:" else if (it == Align.END) "--:" else "---" }))
        for (r in 1 until text.size) out.add(row(text[r]))
        return out
    }

    private fun row(cells: List<String>): String = cells.joinToString(" | ", prefix = "| ", postfix = " |")

    private fun cellText(blocks: List<Block>): String {
        val parts = ArrayList<String>()
        fun add(list: List<Block>) {
            for (b in list) {
                when (b) {
                    is Block.Paragraph -> parts.add(inline(b.content, table = true))
                    is Block.Heading -> parts.add(inline(b.content, table = true))
                    is Block.Code -> b.text.split('\n').forEach { parts.add(codeSpan(it)) }
                    is Block.Quote -> add(b.blocks)
                    is Block.ListBlock -> b.items.forEachIndexed { i, item ->
                        val marker = if (b.ordered) "${b.start + i}. " else "• "
                        val before = parts.size
                        add(item)
                        if (parts.size > before) parts[before] = marker + parts[before] else parts.add(marker.trim())
                    }
                    is Block.Table -> b.rows.forEach { row -> row.forEach { add(it.blocks) } }
                    is Block.Picture -> parts.add(picture(b))
                    Block.Rule, Block.PageBreak -> {}
                }
            }
        }
        add(blocks)
        return parts.filter { it.isNotBlank() }.joinToString("<br>")
    }

    private fun picture(picture: Block.Picture): String {
        val type = ImageHeader.mimeType(picture.bytes) ?: "application/octet-stream"
        val alt = picture.alt.collapseSpaces().replace("\\", "\\\\").replace("[", "\\[").replace("]", "\\]")
        return "![$alt](data:$type;base64,${encodeBase64(picture.bytes)})"
    }

    private fun inline(content: List<Inline>, table: Boolean): String {
        val expected = normalized(content)
        if (expected.isEmpty()) return ""
        val markdown = serialize(expected, html = false, table = table)
        return if (parsesBack(markdown, expected)) markdown else serialize(expected, html = true, table = table)
    }

    private fun normalized(content: List<Inline>): List<Inline> {
        val builder = InlineBuilder()
        for (item in content) {
            when (item) {
                Inline.LineBreak -> builder.lineBreak()
                is Inline.Text -> {
                    val style = item.copy(text = "", link = externalLink(item.link))
                    if (item.code) builder.preserved(item.text.replace('\n', ' '), style) else builder.collapsed(item.text, style)
                }
            }
        }
        return builder.build()
    }

    private fun parsesBack(markdown: String, expected: List<Inline>): Boolean {
        val segments = Segments()
        MarkdownInline(markdown, emptyMap()).render(segments)
        val actual = (segments.blocks { Block.Paragraph(it) }.singleOrNull() as? Block.Paragraph)?.content ?: return false
        return normalized(actual) == expected
    }

    private data class Mark(val kind: Int, val href: String? = null)

    private fun serialize(runs: List<Inline>, html: Boolean, table: Boolean): String {
        val sb = StringBuilder()
        val open = ArrayList<Mark>()
        var heldSpace = ""
        var lineStart = true

        fun openMark(mark: Mark) {
            when (mark.kind) {
                0 -> sb.append('[')
                1 -> sb.append(if (html) "<strong>" else "**")
                2 -> sb.append(if (html) "<em>" else "*")
                3 -> sb.append(if (html) "<s>" else "~~")
                4 -> sb.append("<u>")
                5 -> sb.append("<sub>")
                6 -> sb.append("<sup>")
            }
        }

        fun closeMark(mark: Mark) {
            when (mark.kind) {
                0 -> sb.append("](").append(destination(mark.href.orEmpty())).append(')')
                1 -> sb.append(if (html) "</strong>" else "**")
                2 -> sb.append(if (html) "</em>" else "*")
                3 -> sb.append(if (html) "</s>" else "~~")
                4 -> sb.append("</u>")
                5 -> sb.append("</sub>")
                6 -> sb.append("</sup>")
            }
        }

        val autolinks = runs.mapIndexed { i, run -> run is Inline.Text && run.link != null && run.text == run.link && !run.code && isPlain(run) && isolatedLink(runs, i) }

        for ((index, run) in runs.withIndex()) {
            if (run == Inline.LineBreak) {
                sb.append(heldSpace)
                heldSpace = ""
                sb.append(if (table) "<br>" else "\\\n")
                lineStart = !table
                continue
            }
            val text = run as Inline.Text
            if (autolinks[index]) {
                while (open.isNotEmpty()) closeMark(open.removeAt(open.lastIndex))
                sb.append(heldSpace)
                heldSpace = ""
                sb.append('<').append(text.link).append('>')
                lineStart = false
                continue
            }
            val wanted = marks(text)
            val core = if (text.code) text.text else text.text.trim()
            val leading = if (text.code) "" else text.text.substring(0, text.text.length - text.text.trimStart().length)
            val trailing = if (text.code) "" else text.text.substring(text.text.trimEnd().length)
            if (core.isEmpty()) {
                heldSpace += text.text
                continue
            }
            var common = 0
            while (common < open.size && common < wanted.size && open[common] == wanted[common]) common++
            while (open.size > common) closeMark(open.removeAt(open.lastIndex))
            sb.append(heldSpace).append(leading)
            if (heldSpace.isNotEmpty() || leading.isNotEmpty()) lineStart = false
            heldSpace = ""
            for (mark in wanted.subList(common, wanted.size)) {
                openMark(mark)
                open.add(mark)
                lineStart = false
            }
            if (text.code) {
                sb.append(codeSpan(core))
            } else {
                escape(core, sb, lineStart, table)
            }
            lineStart = false
            heldSpace = trailing
        }
        while (open.isNotEmpty()) closeMark(open.removeAt(open.lastIndex))
        sb.append(heldSpace)
        return sb.toString().trim { it == ' ' || it == '\t' }
    }

    private fun isPlain(t: Inline.Text) = !t.bold && !t.italic && !t.underline && !t.strike && t.script == Script.NORMAL

    private fun isolatedLink(runs: List<Inline>, i: Int): Boolean {
        val link = (runs[i] as Inline.Text).link
        val before = runs.getOrNull(i - 1) as? Inline.Text
        val after = runs.getOrNull(i + 1) as? Inline.Text
        return before?.link != link && after?.link != link && link!!.none { it <= ' ' || it == '<' || it == '>' } && link.contains(':')
    }

    private fun marks(t: Inline.Text): List<Mark> = buildList {
        t.link?.let { add(Mark(0, it)) }
        if (t.bold) add(Mark(1))
        if (t.italic) add(Mark(2))
        if (t.strike) add(Mark(3))
        if (t.underline) add(Mark(4))
        when (t.script) {
            Script.SUB -> add(Mark(5))
            Script.SUPER -> add(Mark(6))
            Script.NORMAL -> {}
        }
    }

    private fun destination(url: String): String = buildString {
        for (c in uriSafe(url)) {
            if (c == '(' || c == ')' || c == '\\') append('\\')
            append(c)
        }
    }

    private fun codeSpan(text: String): String {
        val fence = "`".repeat(longestRun(text, '`') + 1)
        val pad = text.startsWith('`') || text.endsWith('`') || (text.startsWith(' ') && text.endsWith(' ') && text.isNotBlank())
        return if (pad) "$fence $text $fence" else "$fence$text$fence"
    }

    private fun longestRun(text: String, c: Char): Int {
        var best = 0
        var run = 0
        for (ch in text) {
            run = if (ch == c) run + 1 else 0
            if (run > best) best = run
        }
        return best
    }

    private fun escape(text: String, sb: StringBuilder, lineStart: Boolean, table: Boolean) {
        var start = lineStart
        for (i in text.indices) {
            val c = text[i]
            val prev = if (i > 0) text[i - 1] else ' '
            val next = if (i + 1 < text.length) text[i + 1] else ' '
            when {
                c == '\n' -> sb.append(' ')
                c in "\\`*~[]<" -> sb.append('\\').append(c)
                c == '_' -> if (prev.isLetterOrDigit() && next.isLetterOrDigit()) sb.append(c) else sb.append("\\_")
                c == '|' && table -> sb.append("\\|")
                c == '&' && (next.isLetter() || next == '#') -> sb.append("\\&")
                c == ':' && next == '/' && text.startsWith("//", i + 1) -> sb.append("\\:")
                c == '.' && prev.lowercaseChar() == 'w' && i >= 3 && text.regionMatches(i - 3, "www", 0, 3, ignoreCase = true) -> sb.append("\\.")
                start && (c == '#' || c == '>' || c == '-' || c == '+' || c == '=') -> sb.append('\\').append(c)
                start && c.isDigit() -> {
                    var j = i
                    while (j < text.length && text[j].isDigit()) j++
                    sb.append(c)
                    if (j == i + 1 && j < text.length && (text[j] == '.' || text[j] == ')')) sb.append('\\')
                }
                else -> sb.append(c)
            }
            if (!(start && c.isDigit() && next.isDigit())) start = false
        }
    }
}
