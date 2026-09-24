package com.vasmarfas.card.tools.documents

import com.vasmarfas.card.core.ZipArchive
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.TimeSource

class PerformanceTest {
    private val words = (
        "книга текст абзац строка документ глава страница рисунок таблица список пункт ссылка заголовок раздел сноска цитата " +
            "преобразование формат шрифт курсив полужирный подчёркнутый выравнивание отступ интервал колонтитул поле закладка"
        ).split(' ')

    private fun bigDoc(): Doc {
        val random = Random(1)
        fun sentence(n: Int) = (1..n).joinToString(" ") { words[random.nextInt(words.size)] }
        val blocks = ArrayList<Block>()
        for (chapter in 1..30) {
            blocks.add(h(1, "Глава $chapter"))
            repeat(100) { k ->
                blocks.add(
                    when {
                        k % 25 == 0 -> ul(item(p(sentence(8))), item(p(sentence(8))), item(p(sentence(8))))
                        k % 33 == 0 -> table(1, row(cell(p(sentence(2))), cell(p(sentence(2)))), row(cell(p(sentence(3))), cell(p(sentence(3)))))
                        else -> p(t(sentence(14) + " "), b(sentence(3)), t(" " + sentence(10) + " "), i(sentence(2)), t(" " + sentence(6) + "."))
                    },
                )
            }
            blocks.add(Block.PageBreak)
        }
        return Doc(blocks, "Большой документ", "Автор", "ru")
    }

    private inline fun <T> timed(block: () -> T): Pair<T, Duration> {
        val mark = TimeSource.Monotonic.markNow()
        val value = block()
        return value to mark.elapsedNow()
    }

    @Test
    fun docxOfThreeHundredPages() {
        val doc = bigDoc()
        val writes = ArrayList<Duration>()
        val reads = ArrayList<Duration>()
        var bytes = ByteArray(0)
        repeat(4) {
            val (written, w) = timed { Documents.write(doc, DocFormat.DOCX) }
            val (read, r) = timed { Documents.read(written, DocFormat.DOCX) }
            writes.add(w)
            reads.add(r)
            bytes = written
            assertEquals(doc.blocks.size, read.blocks.size)
        }
        val xml = ZipArchive(bytes).read("word/document.xml")!!
        assertTrue(xml.size in 1_800_000..3_500_000, "document.xml is ${xml.size} bytes")
        assertTrue(writes.drop(1).min().inWholeMilliseconds < 1000, "write ${writes.map { it.inWholeMilliseconds }}")
        assertTrue(reads.drop(1).min().inWholeMilliseconds < 1000, "read ${reads.map { it.inWholeMilliseconds }}")
    }

    @Test
    fun otherFormatsOfTheSameSize() {
        val doc = bigDoc()
        for (format in DocFormat.entries.filter { it.writable && it != DocFormat.DOCX }) {
            Documents.read(Documents.write(doc, format), format)
            val (bytes, w) = timed { Documents.write(doc, format) }
            val (_, r) = timed { Documents.read(bytes, format) }
            assertTrue(w.inWholeMilliseconds < 3000 && r.inWholeMilliseconds < 3000, "$format write $w read $r")
        }
    }
}
