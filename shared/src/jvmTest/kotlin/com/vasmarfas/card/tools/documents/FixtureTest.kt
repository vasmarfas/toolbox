package com.vasmarfas.card.tools.documents

import kotlin.test.Test
import kotlin.test.assertEquals

// written by Apache POI 5.5.1 and odfdom 0.13.0, so the readers see files they did not write themselves
class FixtureTest {
    @Test
    fun poiDocx() {
        val bytes = fixture("poi.docx")
        assertEquals(DocFormat.DOCX, Documents.detect(bytes, "document"))
        assertEquals(
            """
            title=Документ из POI author=Apache POI lang=null
            H1 "Заголовок из POI"
            P[center] {b}"жирный" " и " {i}"курсив" ", " {u}"подчёркнутый" ", " {s}"зачёркнутый" ", x" {sup}"2" ", " {c}"моноширинный"
            H2 "Список и таблица"
            OL start=3
              ITEM
                P "третий"
                UL
                  ITEM
                    P "вложенный маркер"
              ITEM
                P "четвёртый"
            P {link=https://poi.apache.org/}"ссылка POI"
            TABLE header=1
              ROW
                CELL
                  P "A"
                CELL
                  P "B"
                CELL
                  P "C"
              ROW
                CELL span=2
                  P "объединено"
                CELL
                  P "x"
              ROW
                CELL
                  P "1"
                CELL
                  P "2"
                CELL
            PICTURE 3de4b59173cb/500 size=600x300 alt="image.png"
            P "До разрыва"
            PAGEBREAK
            P "после разрыва"
            P "Текст со сноской" {sup}"[1]"
            RULE
            OL start=1
              ITEM
                P "Сноска из POI"

            """.trimIndent(),
            dump(Documents.read(bytes, DocFormat.DOCX)),
        )
    }

    @Test
    fun odfToolkitOdt() {
        val bytes = fixture("odfdom.odt")
        assertEquals(DocFormat.ODT, Documents.detect(bytes, "document"))
        val doc = Documents.read(bytes, DocFormat.ODT)
        assertEquals(
            """
            title=Документ из ODF Toolkit author=ODF Toolkit lang=null
            H1 "Заголовок из ODF Toolkit"
            P[center] "Абзац по центру, " {b}"жирный" " и " {i}"курсив" " после двух пробелов"
            H2 "Список"
            UL
              ITEM
                P "пункт один"
                PICTURE 6bd7cb5805dc/448 size=317x159 alt=""
              ITEM
                P "пункт два"
                UL
                  ITEM
                    P "вложенный"
            P {link=https://odftoolkit.org/}"ссылка ODF"
            TABLE header=0
              ROW
                CELL
                  P "a"
                CELL
                  P "b"
              ROW
                CELL
                  P "c"
                CELL
                  P "d"
            P "Текст со сноской" {sup}"[1]"
            RULE
            OL start=1
              ITEM
                P "Сноска из ODF Toolkit"

            """.trimIndent(),
            dump(doc),
        )
        val centered = doc.blocks[1] as Block.Paragraph
        assertEquals("Абзац по центру, жирный и курсив  после двух пробелов", centered.content.plainText())
    }
}
