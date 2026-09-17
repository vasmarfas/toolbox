package com.vasmarfas.card.tools.developer

sealed class MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock()
    data class Paragraph(val text: String) : MdBlock()
    data class ListItem(val text: String, val marker: String, val indent: Int) : MdBlock()
    data class Quote(val text: String) : MdBlock()
    data class Code(val language: String?, val code: String) : MdBlock()
    data object Rule : MdBlock()
}

sealed class MdSpan {
    data class Text(val text: String) : MdSpan()
    data class Bold(val text: String) : MdSpan()
    data class Italic(val text: String) : MdSpan()
    data class BoldItalic(val text: String) : MdSpan()
    data class Code(val text: String) : MdSpan()
    data class Strike(val text: String) : MdSpan()
    data class Link(val text: String, val url: String) : MdSpan()
}

object Markdown {
    private val headingRegex = Regex("^(#{1,6})\\s+(.*)$")
    private val ruleRegex = Regex("^\\s*([-*_])(\\s*\\1){2,}\\s*$")
    private val bulletRegex = Regex("^(\\s*)[-*+]\\s+(.*)$")
    private val orderedRegex = Regex("^(\\s*)(\\d+)[.)]\\s+(.*)$")
    private val fenceRegex = Regex("^\\s*(`{3,}|~{3,})\\s*(\\w+)?\\s*$")
    private val linkRegex = Regex("\\[([^\\]]*)\\]\\(([^)\\s]+)(?:\\s+\"[^\"]*\")?\\)|<(https?://[^>\\s]+)>")

    fun blocks(text: String): List<MdBlock> {
        val out = mutableListOf<MdBlock>()
        val lines = text.lines()
        val paragraph = StringBuilder()
        fun flush() {
            if (paragraph.isNotEmpty()) {
                out += MdBlock.Paragraph(paragraph.toString().trim())
                paragraph.clear()
            }
        }
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val fence = fenceRegex.find(line)
            if (fence != null) {
                flush()
                val marker = fence.groupValues[1]
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith(marker)) {
                    code.appendLine(lines[i])
                    i++
                }
                i++
                out += MdBlock.Code(fence.groupValues[2].takeIf { it.isNotEmpty() }, code.toString().trimEnd('\n'))
                continue
            }
            when {
                line.isBlank() -> flush()
                ruleRegex.matches(line) -> {
                    flush()
                    out += MdBlock.Rule
                }
                headingRegex.matches(line) -> {
                    flush()
                    val m = headingRegex.find(line)!!
                    out += MdBlock.Heading(m.groupValues[1].length, m.groupValues[2].trim().trimEnd('#').trim())
                }
                line.trimStart().startsWith(">") -> {
                    flush()
                    out += MdBlock.Quote(line.trimStart().removePrefix(">").trim())
                }
                bulletRegex.matches(line) -> {
                    flush()
                    val m = bulletRegex.find(line)!!
                    out += MdBlock.ListItem(m.groupValues[2], "•", m.groupValues[1].length / 2)
                }
                orderedRegex.matches(line) -> {
                    flush()
                    val m = orderedRegex.find(line)!!
                    out += MdBlock.ListItem(m.groupValues[3], m.groupValues[2] + ".", m.groupValues[1].length / 2)
                }
                else -> {
                    if (paragraph.isNotEmpty()) paragraph.append(' ')
                    paragraph.append(line.trim())
                }
            }
            i++
        }
        flush()
        return out
    }

    fun spans(text: String): List<MdSpan> {
        val out = mutableListOf<MdSpan>()
        val plain = StringBuilder()
        fun flush() {
            if (plain.isNotEmpty()) {
                out += MdSpan.Text(plain.toString())
                plain.clear()
            }
        }
        var i = 0
        while (i < text.length) {
            val rest = text.substring(i)
            val link = linkRegex.find(rest)
            if (link != null && link.range.first == 0) {
                flush()
                val bare = link.groupValues[3]
                if (bare.isNotEmpty()) {
                    out += MdSpan.Link(bare, bare)
                } else {
                    out += MdSpan.Link(link.groupValues[1], link.groupValues[2])
                }
                i += link.value.length
                continue
            }
            val marker = listOf("***", "___", "**", "__", "~~", "*", "_", "`").firstOrNull { rest.startsWith(it) }
            if (marker != null) {
                val end = rest.indexOf(marker, marker.length)
                if (end > marker.length) {
                    val inner = rest.substring(marker.length, end)
                    flush()
                    out += when (marker) {
                        "***", "___" -> MdSpan.BoldItalic(inner)
                        "**", "__" -> MdSpan.Bold(inner)
                        "~~" -> MdSpan.Strike(inner)
                        "`" -> MdSpan.Code(inner)
                        else -> MdSpan.Italic(inner)
                    }
                    i += end + marker.length
                    continue
                }
            }
            plain.append(text[i])
            i++
        }
        flush()
        return out
    }
}
