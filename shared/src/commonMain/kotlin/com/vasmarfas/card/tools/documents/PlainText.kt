package com.vasmarfas.card.tools.documents

private const val FORM_FEED = '\u000C'

internal object TxtReader {
    fun read(text: String): Doc {
        val blocks = ArrayList<Block>()
        val pages = text.trimStart(BOM).replace("\r\n", "\n").replace('\r', '\n').split(FORM_FEED)
        pages.forEachIndexed { i, page ->
            if (i > 0) blocks.add(Block.PageBreak)
            val builder = InlineBuilder()
            var lines = 0
            for (line in page.split('\n')) {
                if (line.isBlank()) {
                    builder.build().takeIf { it.isNotEmpty() }?.let { blocks.add(Block.Paragraph(it)) }
                    lines = 0
                    continue
                }
                if (lines > 0) builder.lineBreak()
                builder.preserved(line, PLAIN)
                lines++
            }
            builder.build().takeIf { it.isNotEmpty() }?.let { blocks.add(Block.Paragraph(it)) }
        }
        return Doc(blocks)
    }
}

internal object TxtWriter {
    fun write(doc: Doc): String {
        val chunks = ArrayList<List<String>>()
        blocks(doc.blocks, chunks)
        return chunks.joinToString("\n\n") { it.joinToString("\n") }.trimEnd('\n') + "\n"
    }

    private fun blocks(blocks: List<Block>, chunks: MutableList<List<String>>) {
        for (block in blocks) {
            val lines = lines(block)
            if (lines.isNotEmpty()) chunks.add(lines)
        }
    }

    private fun lines(block: Block): List<String> = when (block) {
        is Block.Heading -> text(block.content).split('\n')
        is Block.Paragraph -> text(block.content).takeIf { it.isNotBlank() }?.split('\n').orEmpty()
        is Block.Code -> block.text.split('\n')
        is Block.Quote -> nested(block.blocks).map { if (it.isEmpty()) it else "    $it" }
        is Block.ListBlock -> list(block)
        is Block.Table -> block.rows.map { row ->
            row.joinToString("\t") { cell -> cellText(cell.blocks) + "\t".repeat(cell.colSpan - 1) }
        }
        is Block.Picture -> if (block.alt.isBlank()) emptyList() else listOf("[${block.alt.collapseSpaces()}]")
        Block.Rule -> listOf("* * *")
        Block.PageBreak -> listOf(FORM_FEED.toString())
    }

    private fun nested(blocks: List<Block>): List<String> {
        val out = ArrayList<String>()
        for (block in blocks) {
            val lines = lines(block)
            if (lines.isEmpty()) continue
            if (out.isNotEmpty() && block !is Block.ListBlock) out.add("")
            out.addAll(lines)
        }
        return out
    }

    private fun list(list: Block.ListBlock): List<String> {
        val out = ArrayList<String>()
        list.items.forEachIndexed { i, item ->
            val marker = if (list.ordered) "${list.start + i}. " else "• "
            val content = nested(item)
            if (content.isEmpty()) {
                out.add(marker.trimEnd())
                return@forEachIndexed
            }
            val pad = " ".repeat(marker.length)
            content.forEachIndexed { k, line -> out.add(if (k == 0) marker + line else if (line.isEmpty()) "" else pad + line) }
        }
        return out
    }

    private fun cellText(blocks: List<Block>): String = blocksText(blocks).collapseSpaces()

    private fun text(content: List<Inline>): String = content.plainText()
}
