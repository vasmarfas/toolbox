package com.vasmarfas.card.tools.documents

import com.vasmarfas.card.core.ZipWriter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal fun windows1251(text: String): ByteArray = ByteArray(text.length) { i ->
    val c = text[i]
    when {
        c.code < 0x80 -> c.code
        c in 'А'..'я' -> c.code - 0x410 + 0xC0
        c == 'Ё' -> 0xA8
        c == 'ё' -> 0xB8
        c == '«' -> 0xAB
        c == '»' -> 0xBB
        c == '—' -> 0x97
        c == '–' -> 0x96
        c == '…' -> 0x85
        else -> '?'.code
    }.toByte()
}

class Fb2ReaderTest {
    private val cover = TestImages.png(6, 8)
    private val picture = TestImages.png(3, 3, seed = 2)

    private fun book(): String = """
        <?xml version="1.0" encoding="windows-1251"?>
        <fb:FictionBook xmlns:fb="http://www.gribuser.ru/xml/fictionbook/2.0" xmlns:xl="http://www.w3.org/1999/xlink">
        <fb:description>
         <fb:title-info>
          <fb:genre>prose_classic</fb:genre>
          <fb:author><fb:first-name>Лев</fb:first-name><fb:middle-name>Николаевич</fb:middle-name><fb:last-name>Толстой</fb:last-name></fb:author>
          <fb:author><fb:nickname>Соавтор</fb:nickname></fb:author>
          <fb:book-title>Книга</fb:book-title>
          <fb:annotation><fb:p>Аннотация не в тексте</fb:p></fb:annotation>
          <fb:coverpage><fb:image xl:href="#cover.png"/></fb:coverpage>
          <fb:lang>ru</fb:lang>
         </fb:title-info>
        </fb:description>
        <fb:body>
         <fb:title><fb:p>Книга</fb:p><fb:empty-line/><fb:p>Подзаголовок</fb:p></fb:title>
         <fb:epigraph><fb:p>Эпиграф</fb:p><fb:text-author>Автор эпиграфа</fb:text-author></fb:epigraph>
         <fb:section>
          <fb:title><fb:p>Часть первая</fb:p></fb:title>
          <fb:section>
           <fb:title><fb:p>Глава 1</fb:p></fb:title>
           <fb:p>Текст с <fb:strong>жирным</fb:strong>, <fb:emphasis>курсивом</fb:emphasis>, <fb:strikethrough>зачёркнутым</fb:strikethrough>, H<fb:sub>2</fb:sub>O, x<fb:sup>2</fb:sup>, <fb:code>кодом</fb:code>, <fb:style name="x">стилем</fb:style> и <fb:a xl:href="https://example.com">ссылкой</fb:a>.<fb:a xl:href="#n1" type="note">[1]</fb:a></fb:p>
           <fb:empty-line/>
           <fb:subtitle>* * *</fb:subtitle>
           <fb:subtitle>Подраздел</fb:subtitle>
           <fb:poem><fb:title><fb:p>Стих</fb:p></fb:title><fb:stanza><fb:v>Строка один</fb:v><fb:v>Строка два</fb:v></fb:stanza><fb:text-author>Поэт</fb:text-author></fb:poem>
           <fb:cite><fb:p>Цитата</fb:p><fb:text-author>Кто-то</fb:text-author></fb:cite>
           <fb:p><fb:code>код строка 1</fb:code></fb:p><fb:p><fb:code>  отступ</fb:code></fb:p>
           <fb:table><fb:tr><fb:th>A</fb:th><fb:th>B</fb:th></fb:tr><fb:tr><fb:td colspan="2" align="center">широкая</fb:td></fb:tr><fb:tr><fb:td rowspan="2">высокая</fb:td><fb:td>1</fb:td></fb:tr><fb:tr><fb:td>2</fb:td></fb:tr></fb:table>
           <fb:image xl:href="#pic.png" alt="рисунок"/>
           <fb:p>Сноска 2<fb:a xl:href="#n2">2</fb:a> ссылка без type</fb:p>
          </fb:section>
         </fb:section>
        </fb:body>
        <fb:body name="notes">
         <fb:title><fb:p>Примечания</fb:p></fb:title>
         <fb:section id="n1"><fb:title><fb:p>1</fb:p></fb:title><fb:p>Первая сноска</fb:p></fb:section>
         <fb:section id="n2"><fb:title><fb:p>2</fb:p></fb:title><fb:p>Вторая</fb:p></fb:section>
         <fb:section id="n3"><fb:p>Без ссылки</fb:p></fb:section>
        </fb:body>
        <fb:binary id="cover.png" content-type="image/png">${encodeBase64(cover).chunked(60).joinToString("\n")}</fb:binary>
        <fb:binary id="pic.png" content-type="image/png">${encodeBase64(picture)}</fb:binary>
        </fb:FictionBook>
    """.trimIndent()

    private fun sha(bytes: ByteArray): String = dumpBlocks(listOf(Block.Picture(bytes))).removePrefix("PICTURE ").substringBefore(" size")

    private val expected get() = """
        PICTURE ${sha(cover)} size=0x0 alt=""
        H1 "Книга" \n "Подзаголовок"
        QUOTE
          P "Эпиграф"
          P[end] "Автор эпиграфа"
        H1 "Часть первая"
        H2 "Глава 1"
        P "Текст с " {b}"жирным" ", " {i}"курсивом" ", " {s}"зачёркнутым" ", H" {sub}"2" "O, x" {sup}"2" ", " {c}"кодом" ", стилем и " {link=https://example.com}"ссылкой" "." {sup}"[1]"
        RULE
        P[center] {b}"Подраздел"
        P {b}"Стих"
        P "Строка один" \n "Строка два"
        P[end] "Поэт"
        QUOTE
          P "Цитата"
          P[end] "Кто-то"
        CODE "код строка 1\n  отступ"
        TABLE header=1
          ROW
            CELL
              P "A"
            CELL
              P "B"
          ROW
            CELL span=2
              P[center] "широкая"
          ROW
            CELL
              P "высокая"
            CELL
              P "1"
          ROW
            CELL
            CELL
              P "2"
        PICTURE ${sha(picture)} size=0x0 alt="рисунок"
        P "Сноска 2" {sup}"[2]" " ссылка без type"
        RULE
        OL start=1
          ITEM
            P "Первая сноска"
          ITEM
            P "Вторая"
          ITEM
            P "Без ссылки"

    """.trimIndent()

    @Test
    fun windows1251BookWithPrefixes() {
        val doc = Documents.read(windows1251(book()), DocFormat.FB2)
        assertEquals("Книга", doc.title)
        assertEquals("Лев Николаевич Толстой, Соавтор", doc.author)
        assertEquals("ru", doc.language)
        assertEquals(expected, dumpBlocks(doc.blocks))
    }

    @Test
    fun zippedBook() {
        val zip = ZipWriter().add("книга.fb2", windows1251(book())).toByteArray()
        assertEquals(DocFormat.FB2, Documents.detect(zip, "книга.fb2.zip"))
        assertEquals(expected, dumpBlocks(Documents.read(zip, DocFormat.FB2).blocks))
    }

    @Test
    fun rejectsOtherXml() {
        assertFailsWith<DocumentFormatException> { Documents.read("<html><body/></html>".encodeToByteArray(), DocFormat.FB2) }
        assertFailsWith<DocumentFormatException> { Documents.read(ZipWriter().add("a.txt", ByteArray(3)).toByteArray(), DocFormat.FB2) }
    }

    @Test
    fun deeplyNestedSectionsAreRejected() {
        val xml = "<FictionBook><body>" + "<section>".repeat(1000) + "<p>x</p>" + "</section>".repeat(1000) + "</body></FictionBook>"
        assertFailsWith<DocumentFormatException> { Documents.read(xml.encodeToByteArray(), DocFormat.FB2) }
    }
}
