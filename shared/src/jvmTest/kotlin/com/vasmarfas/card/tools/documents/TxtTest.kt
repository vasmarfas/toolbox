package com.vasmarfas.card.tools.documents

import com.vasmarfas.card.tools.developer.hexToBytes
import kotlin.test.Test
import kotlin.test.assertEquals

class TxtTest {
    private fun read(bytes: ByteArray) = dumpBlocks(Documents.read(bytes, DocFormat.TXT).blocks)

    @Test
    fun paragraphsLineBreaksAndPages() {
        val text = "Первый абзац\r\nвторая строка   \r\n\r\n   \r\nВторой абзац\u000CПосле разрыва\n\n\n"
        assertEquals("P \"Первый абзац\" \\n \"вторая строка\"\nP \"Второй абзац\"\nPAGEBREAK\nP \"После разрыва\"\n", read(text.encodeToByteArray()))
    }

    @Test
    fun encodings() {
        assertEquals("P \"Привет, мир\"\n", read(windows1251("Привет, мир")))
        assertEquals("P \"Привет\"\n", read(hexToBytes("F0D2C9D7C5D4")!!))
        assertEquals("P \"Привет\"\n", read(hexToBytes("FFFE1F0440043804320435044204")!!))
        assertEquals("P \"Привет\"\n", read(hexToBytes("EFBBBF")!! + "Привет".encodeToByteArray()))
    }

    @Test
    fun writerLayout() {
        val doc = Doc(
            listOf(
                h(1, "Заголовок"),
                p(t("строка"), br, t("вторая")),
                ul(item(p("один"), ol(2, item(p("два")), item(p("три")))), item(p("четыре"), p("продолжение"))),
                Block.Quote(listOf(p("цитата"))),
                table(1, row(cell(p("a")), cell(p("b"), span = 2)), row(cell(p("c")), cell(p("d")), cell(p("e")))),
                Block.Rule,
                Block.PageBreak,
                Block.Picture(Samples.png, alt = "подпись"),
            ),
        )
        assertEquals(
            "Заголовок\n\nстрока\nвторая\n\n• один\n  2. два\n  3. три\n• четыре\n\n  продолжение\n\n    цитата\n\na\tb\t\nc\td\te\n\n* * *\n\n\u000C\n\n[подпись]\n",
            Documents.write(doc, DocFormat.TXT).decodeToString(),
        )
    }
}
