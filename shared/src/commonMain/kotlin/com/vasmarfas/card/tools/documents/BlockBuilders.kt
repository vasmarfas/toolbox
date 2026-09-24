package com.vasmarfas.card.tools.documents

internal const val MAX_NESTING = 200
internal const val MAX_COL_SPAN = 1000
internal const val MAX_ROW_SPAN = 65534

// the readers are recursive, a document nested deeper than MAX_NESTING is rejected
internal class Nesting {
    private var depth = 0

    fun enter() {
        if (++depth > MAX_NESTING) throw DocumentFormatException("Nesting is deeper than $MAX_NESTING levels")
    }

    fun exit() {
        depth--
    }
}

internal inline fun <T> Nesting.within(block: () -> T): T {
    enter()
    try {
        return block()
    } finally {
        exit()
    }
}

// bold over the whole heading comes from the heading style, not from the author
internal fun heading(level: Int, content: List<Inline>): Block.Heading {
    val texts = content.filterIsInstance<Inline.Text>()
    if (texts.isEmpty() || texts.any { !it.bold && it.text.isNotBlank() }) return Block.Heading(level.coerceIn(1, 6), content)
    val builder = InlineBuilder()
    for (item in content) builder.inline(if (item is Inline.Text) item.copy(bold = false) else item)
    return Block.Heading(level.coerceIn(1, 6), builder.build())
}

internal class Segments {
    val inline = InlineBuilder()
    private val parts = ArrayList<Block>()

    val isEmpty: Boolean get() = inline.isEmpty && parts.isEmpty()

    fun block(block: Block) {
        cut()
        parts.add(block)
    }

    fun blocks(wrap: (List<Inline>) -> Block): List<Block> {
        cut()
        val result = parts.map { if (it is Block.Paragraph) wrap(it.content) else it }
        parts.clear()
        return result
    }

    fun heading(level: Int): List<Block> {
        cut()
        val before = ArrayList<Block>()
        val after = ArrayList<Block>()
        val content = InlineBuilder()
        var seen = false
        for (part in parts) {
            when {
                part is Block.Paragraph -> {
                    if (seen) content.preserved(" ", PLAIN)
                    part.content.forEach(content::inline)
                    seen = true
                }
                seen -> after.add(part)
                else -> before.add(part)
            }
        }
        parts.clear()
        return if (seen) before + heading(level, content.build()) + after else before
    }

    // always yields a Code block first, even an empty one: an empty line inside a code run belongs to it
    fun code(): List<Block> {
        cut()
        val text = parts.filterIsInstance<Block.Paragraph>().joinToString("\n") { it.content.plainText() }
        val rest = parts.filter { it !is Block.Paragraph }
        parts.clear()
        return listOf(Block.Code(text)) + rest
    }

    private fun cut() {
        val content = inline.build()
        if (content.isNotEmpty()) parts.add(Block.Paragraph(content))
    }
}

internal class ListCollector(private val out: MutableList<Block>) {
    private class Open(val key: String, val level: Int, val ordered: Boolean, val start: Int) {
        val items = ArrayList<MutableList<Block>>()
    }

    private val stack = ArrayList<Open>()

    val isOpen: Boolean get() = stack.isNotEmpty()

    fun item(key: String, level: Int, ordered: Boolean, start: Int, blocks: List<Block>) {
        while (stack.isNotEmpty() && stack[stack.lastIndex].level > level) close()
        val top = stack.lastOrNull()
        val list = if (top != null && top.level == level && top.key == key && top.ordered == ordered) {
            top
        } else {
            if (top != null && top.level == level) close()
            Open(key, level, ordered, start).also { stack.add(it) }
        }
        list.items.add(blocks.toMutableList())
    }

    fun continuation(level: Int, blocks: List<Block>): Boolean {
        if (stack.isEmpty()) return false
        while (stack.size > 1 && stack[stack.lastIndex].level > level) close()
        stack[stack.lastIndex].items.last().addAll(blocks)
        return true
    }

    fun levelFor(indent: Int, indentOf: (key: String, level: Int) -> Int): Int? {
        for (i in stack.lastIndex downTo 0) {
            if (indentOf(stack[i].key, stack[i].level) <= indent) return stack[i].level
        }
        return null
    }

    fun flush() {
        while (stack.isNotEmpty()) close()
    }

    private fun close() {
        val list = stack.removeAt(stack.lastIndex)
        val block = Block.ListBlock(list.ordered, list.items, list.start)
        if (stack.isEmpty()) out.add(block) else stack[stack.lastIndex].items.last().add(block)
    }
}

internal class TableGrid {
    private val rows = ArrayList<List<Cell>>()
    private var covered = IntArray(8)
    private var width = IntArray(8)

    val rowCount: Int get() = rows.size

    fun row(cells: List<Cell>, rowSpans: List<Int>) {
        val out = ArrayList<Cell>(cells.size)
        var col = 0
        var k = 0
        while (k < cells.size) {
            if (col < covered.size && covered[col] > 0) {
                val w = maxOf(1, width[col])
                out.add(Cell(emptyList(), w))
                col += w
                continue
            }
            val cell = cells[k]
            val span = rowSpans.getOrElse(k) { 1 }.coerceIn(1, MAX_ROW_SPAN)
            k++
            out.add(cell)
            if (span > 1) {
                ensure(col + cell.colSpan)
                for (c in col until col + cell.colSpan) {
                    covered[c] = span
                    width[c] = 0
                }
                width[col] = cell.colSpan
            }
            col += cell.colSpan
        }
        for (c in covered.indices) if (covered[c] > 0) covered[c]--
        if (out.isNotEmpty()) rows.add(out)
    }

    fun build(headerRows: Int): Block.Table = Block.Table(rows.toList(), headerRows.coerceIn(0, rows.size))

    private fun ensure(size: Int) {
        if (size <= covered.size) return
        val n = maxOf(size, covered.size * 2)
        covered = covered.copyOf(n)
        width = width.copyOf(n)
    }
}

internal class Notes {
    private val items = ArrayList<List<Block>>()

    fun reserve(): Int {
        items.add(emptyList())
        return items.lastIndex
    }

    fun fill(index: Int, blocks: List<Block>) {
        items[index] = blocks
    }

    fun add(blocks: List<Block>): Inline.Text = marker(reserve().also { fill(it, blocks) })

    fun marker(index: Int): Inline.Text = Inline.Text("[${index + 1}]", script = Script.SUPER)

    fun appendTo(blocks: MutableList<Block>) {
        if (items.isEmpty()) return
        blocks.add(Block.Rule)
        blocks.add(Block.ListBlock(ordered = true, items = items.toList()))
    }
}

// word processors store a listing or a quote as a run of same-style paragraphs. Readers emit one block
// per paragraph, this joins them, also inside lists, quotes and cells
internal fun mergeRuns(blocks: List<Block>): List<Block> {
    val out = ArrayList<Block>(blocks.size)
    for (block in blocks) {
        val b = when (block) {
            is Block.ListBlock -> block.copy(items = block.items.map(::mergeRuns))
            is Block.Quote -> Block.Quote(mergeRuns(block.blocks))
            is Block.Table -> Block.Table(block.rows.map { row -> row.map { Cell(mergeRuns(it.blocks), it.colSpan) } }, block.headerRows)
            else -> block
        }
        val last = out.lastOrNull()
        when {
            b is Block.Code && last is Block.Code -> out[out.lastIndex] = Block.Code(last.text + "\n" + b.text)
            b is Block.Quote && last is Block.Quote -> out[out.lastIndex] = Block.Quote(last.blocks + b.blocks)
            else -> out.add(b)
        }
    }
    return out.mapNotNull { b ->
        if (b is Block.Code) b.text.trim('\n').takeIf { it.isNotBlank() }?.let { Block.Code(it) } else b
    }
}
