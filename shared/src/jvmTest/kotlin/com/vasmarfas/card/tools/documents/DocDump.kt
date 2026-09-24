package com.vasmarfas.card.tools.documents

import com.vasmarfas.card.tools.developer.Sha256
import com.vasmarfas.card.tools.developer.toHex
import kotlin.math.roundToInt
import kotlin.test.assertEquals

internal fun dump(doc: Doc, sizes: Boolean = true): String = buildString {
    append("title=").append(doc.title).append(" author=").append(doc.author).append(" lang=").append(doc.language).append('\n')
    dumpBlocks(doc.blocks, "", sizes, this)
}

internal fun dumpBlocks(blocks: List<Block>, sizes: Boolean = true): String = buildString { dumpBlocks(blocks, "", sizes, this) }

private fun dumpBlocks(blocks: List<Block>, indent: String, sizes: Boolean, sb: StringBuilder) {
    for (b in blocks) {
        sb.append(indent)
        when (b) {
            is Block.Heading -> sb.append("H").append(b.level).append(' ').append(dumpInlines(b.content)).append('\n')
            is Block.Paragraph -> {
                sb.append("P")
                if (b.align != Align.START) sb.append('[').append(b.align.name.lowercase()).append(']')
                sb.append(' ').append(dumpInlines(b.content)).append('\n')
            }
            is Block.ListBlock -> {
                sb.append(if (b.ordered) "OL start=${b.start}" else "UL").append('\n')
                for (item in b.items) {
                    sb.append(indent).append("  ITEM\n")
                    dumpBlocks(item, "$indent    ", sizes, sb)
                }
            }
            is Block.Quote -> {
                sb.append("QUOTE\n")
                dumpBlocks(b.blocks, "$indent  ", sizes, sb)
            }
            is Block.Code -> sb.append("CODE ").append(quote(b.text)).append('\n')
            is Block.Table -> {
                sb.append("TABLE header=").append(b.headerRows).append('\n')
                for (row in b.rows) {
                    sb.append(indent).append("  ROW\n")
                    for (cell in row) {
                        sb.append(indent).append("    CELL")
                        if (cell.colSpan != 1) sb.append(" span=").append(cell.colSpan)
                        sb.append('\n')
                        dumpBlocks(cell.blocks, "$indent      ", sizes, sb)
                    }
                }
            }
            is Block.Picture -> {
                sb.append("PICTURE ").append(Sha256.digest(b.bytes).toHex().take(12)).append('/').append(b.bytes.size)
                if (sizes) sb.append(" size=").append((b.widthPt * 10).roundToInt()).append('x').append((b.heightPt * 10).roundToInt())
                sb.append(" alt=").append(quote(b.alt)).append('\n')
            }
            Block.Rule -> sb.append("RULE\n")
            Block.PageBreak -> sb.append("PAGEBREAK\n")
        }
    }
}

internal fun dumpInlines(content: List<Inline>): String = normalizeInlines(content).joinToString(" ") { item ->
    when (item) {
        Inline.LineBreak -> "\\n"
        is Inline.Text -> {
            val flags = buildList {
                if (item.bold) add("b")
                if (item.italic) add("i")
                if (item.underline) add("u")
                if (item.strike) add("s")
                if (item.code) add("c")
                if (item.script == Script.SUPER) add("sup")
                if (item.script == Script.SUB) add("sub")
                item.link?.let { add("link=$it") }
            }
            (if (flags.isEmpty()) "" else flags.joinToString(",", "{", "}")) + quote(item.text)
        }
    }
}

internal fun normalizeInlines(content: List<Inline>): List<Inline> {
    val builder = InlineBuilder()
    for (item in content) {
        when (item) {
            Inline.LineBreak -> builder.lineBreak()
            is Inline.Text -> if (item.code) builder.preserved(item.text, item) else builder.collapsed(item.text, item)
        }
    }
    return builder.build()
}

private fun quote(text: String): String = "\"" + text.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t") + "\""

internal fun assertSameDoc(expected: Doc, actual: Doc, message: String? = null, sizes: Boolean = true) {
    assertEquals(dump(expected, sizes), dump(actual, sizes), message)
}

internal fun assertSameBlocks(expected: List<Block>, actual: List<Block>, message: String? = null) {
    assertEquals(dumpBlocks(expected), dumpBlocks(actual), message)
}
