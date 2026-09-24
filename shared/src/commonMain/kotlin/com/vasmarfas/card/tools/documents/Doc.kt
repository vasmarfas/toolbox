package com.vasmarfas.card.tools.documents

class Doc(val blocks: List<Block>, val title: String? = null, val author: String? = null, val language: String? = null)

enum class Align { START, CENTER, END, JUSTIFY }

enum class Script { NORMAL, SUPER, SUB }

sealed interface Inline {
    data class Text(
        val text: String,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val underline: Boolean = false,
        val strike: Boolean = false,
        val code: Boolean = false,
        val script: Script = Script.NORMAL,
        val link: String? = null,
    ) : Inline

    data object LineBreak : Inline
}

class Cell(val blocks: List<Block>, val colSpan: Int = 1)

sealed interface Block {
    data class Heading(val level: Int, val content: List<Inline>) : Block

    data class Paragraph(val content: List<Inline>, val align: Align = Align.START) : Block

    data class ListBlock(val ordered: Boolean, val items: List<List<Block>>, val start: Int = 1) : Block

    data class Quote(val blocks: List<Block>) : Block

    data class Code(val text: String) : Block

    // rows may be shorter than the widest one
    class Table(val rows: List<List<Cell>>, val headerRows: Int = 0) : Block

    // bytes stay in the source format, size in pt or 0 when the source gave none
    class Picture(val bytes: ByteArray, val widthPt: Float = 0f, val heightPt: Float = 0f, val alt: String = "") : Block

    data object Rule : Block

    data object PageBreak : Block
}
