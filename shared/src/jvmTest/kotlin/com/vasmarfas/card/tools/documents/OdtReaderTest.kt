package com.vasmarfas.card.tools.documents

import com.vasmarfas.card.core.ZipWriter
import kotlin.test.Test
import kotlin.test.assertEquals

private const val ODF = "xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" " +
    "xmlns:style=\"urn:oasis:names:tc:opendocument:xmlns:style:1.0\" xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\" " +
    "xmlns:table=\"urn:oasis:names:tc:opendocument:xmlns:table:1.0\" xmlns:draw=\"urn:oasis:names:tc:opendocument:xmlns:drawing:1.0\" " +
    "xmlns:fo=\"urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
    "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:meta=\"urn:oasis:names:tc:opendocument:xmlns:meta:1.0\" " +
    "xmlns:svg=\"urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0\" office:version=\"1.3\""

class OdtReaderTest {
    private val png = TestImages.png(12, 6)

    private val styles = """
        <office:document-styles $ODF>
          <office:font-face-decls>
            <style:font-face style:name="Liberation Mono" svg:font-family="'Liberation Mono'" style:font-pitch="fixed"/>
            <style:font-face style:name="MyFixed" svg:font-family="Unknown" style:font-pitch="fixed"/>
          </office:font-face-decls>
          <office:styles>
            <style:default-style style:family="paragraph"><style:text-properties fo:language="ru" fo:country="RU"/></style:default-style>
            <style:style style:name="Standard" style:family="paragraph"/>
            <style:style style:name="Heading" style:family="paragraph" style:parent-style-name="Standard"><style:text-properties fo:font-weight="bold"/></style:style>
            <style:style style:name="Heading_20_1" style:display-name="Heading 1" style:family="paragraph" style:parent-style-name="Heading"/>
            <style:style style:name="Title" style:family="paragraph" style:parent-style-name="Heading"/>
            <style:style style:name="Quotations" style:family="paragraph" style:parent-style-name="Standard"><style:text-properties fo:font-style="italic"/></style:style>
            <style:style style:name="Preformatted_20_Text" style:display-name="Preformatted Text" style:family="paragraph"><style:text-properties style:font-name="Liberation Mono"/></style:style>
            <style:style style:name="Horizontal_20_Line" style:display-name="Horizontal Line" style:family="paragraph"/>
            <style:style style:name="Strong_20_Emphasis" style:display-name="Strong Emphasis" style:family="text"><style:text-properties fo:font-weight="bold"/></style:style>
            <style:style style:name="Internet_20_link" style:display-name="Internet link" style:family="text"><style:text-properties style:text-underline-style="solid"/></style:style>
            <style:style style:name="Table_20_Heading" style:display-name="Table Heading" style:family="paragraph"><style:text-properties fo:font-weight="bold"/></style:style>
            <text:list-style style:name="Numbering_20_123" style:display-name="Numbering 123">
              <text:list-level-style-number text:level="1" style:num-format="1" text:start-value="4"/>
              <text:list-level-style-number text:level="2" style:num-format="a"/>
            </text:list-style>
          </office:styles>
        </office:document-styles>
    """.trimIndent()

    private fun odt(body: String, automatic: String = "", meta: String? = null): ByteArray {
        val content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><office:document-content $ODF><office:automatic-styles>$automatic</office:automatic-styles>" +
            "<office:body><office:text><text:sequence-decls/>$body</office:text></office:body></office:document-content>"
        val zip = ZipWriter()
        zip.add("mimetype", "application/vnd.oasis.opendocument.text".encodeToByteArray(), compress = false)
        zip.add("content.xml", content.encodeToByteArray())
        zip.add("styles.xml", styles.encodeToByteArray())
        zip.add("Pictures/10000000.png", png)
        if (meta != null) zip.add("meta.xml", "<office:document-meta $ODF><office:meta>$meta</office:meta></office:document-meta>".encodeToByteArray())
        return zip.toByteArray()
    }

    private fun read(body: String, automatic: String = ""): String = dumpBlocks(Documents.read(odt(body, automatic), DocFormat.ODT).blocks)

    private val automatic = """
        <style:style style:name="P1" style:family="paragraph" style:parent-style-name="Standard"><style:paragraph-properties fo:text-align="center"/></style:style>
        <style:style style:name="P2" style:family="paragraph" style:parent-style-name="P1"><style:paragraph-properties fo:break-before="page"/><style:text-properties fo:font-style="italic"/></style:style>
        <style:style style:name="P3" style:family="paragraph" style:parent-style-name="Standard"><style:paragraph-properties fo:text-align="end" fo:break-after="page"/></style:style>
        <style:style style:name="P4" style:family="paragraph" style:parent-style-name="Standard"><style:paragraph-properties fo:border-bottom="0.06pt solid #000000"/></style:style>
        <style:style style:name="T1" style:family="text"><style:text-properties fo:font-weight="bold"/></style:style>
        <style:style style:name="T2" style:family="text"><style:text-properties fo:font-style="italic" style:text-underline-style="solid"/></style:style>
        <style:style style:name="T3" style:family="text"><style:text-properties style:text-position="super 58%"/></style:style>
        <style:style style:name="T4" style:family="text"><style:text-properties style:text-position="-33% 58%"/></style:style>
        <style:style style:name="T5" style:family="text"><style:text-properties style:font-name="MyFixed"/></style:style>
        <style:style style:name="T6" style:family="text"><style:text-properties style:text-line-through-style="solid"/></style:style>
        <style:style style:name="T7" style:family="text" style:parent-style-name="Strong_20_Emphasis"><style:text-properties fo:font-weight="normal"/></style:style>
        <style:style style:name="T8" style:family="text"><style:text-properties text:display="none"/></style:style>
        <text:list-style style:name="L1"><text:list-level-style-bullet text:level="1" text:bullet-char="•"/><text:list-level-style-number text:level="2" style:num-format="1"/></text:list-style>
    """.trimIndent()

    @Test
    fun paragraphsHeadingsAndSpans() {
        val body = """
            <text:h text:style-name="Heading_20_1" text:outline-level="1">Глава</text:h>
            <text:h text:outline-level="3">Третий <text:span text:style-name="T1">уровень</text:span></text:h>
            <text:p text:style-name="Title">Титул</text:p>
            <text:p text:style-name="P1">По центру <text:span text:style-name="T1">жирный</text:span> <text:span text:style-name="T2">курсив подчёркнут</text:span></text:p>
            <text:p text:style-name="P2">Наследует центр, курсив и разрыв</text:p>
            <text:p text:style-name="P3">Справа, разрыв после</text:p>
            <text:p>H<text:span text:style-name="T4">2</text:span>O и x<text:span text:style-name="T3">2</text:span> <text:span text:style-name="T5">моно</text:span> <text:span text:style-name="T6">зачёркнут</text:span> <text:span text:style-name="T7">не жирный</text:span><text:span text:style-name="T8">скрыто</text:span></text:p>
            <text:p>пробелы:<text:s text:c="3"/>три<text:tab/>таб<text:line-break/>перенос   схлопнут
            ся</text:p>
            <text:p text:style-name="P4"/>
            <text:p text:style-name="Horizontal_20_Line"></text:p>
            <text:p text:style-name="Quotations">цитата</text:p><text:p text:style-name="Quotations">и ещё</text:p>
            <text:p text:style-name="Preformatted_20_Text">код<text:s text:c="2"/>с отступом</text:p><text:p text:style-name="Preformatted_20_Text"/><text:p text:style-name="Preformatted_20_Text">вторая строка</text:p>
            <text:p><text:a xlink:type="simple" xlink:href="https://example.com/?a=1&amp;b=2" text:style-name="Internet_20_link">ссылка</text:a> <text:a xlink:href="#bookmark">якорь</text:a> <text:a xlink:href="../other.odt">файл</text:a></text:p>
            <text:section text:name="Раздел"><text:p>в разделе<text:soft-page-break/> продолжение</text:p></text:section>
            <text:table-of-content><text:table-of-content-source/><text:index-body><text:p>пункт оглавления</text:p></text:index-body></text:table-of-content>
            <text:p><text:bookmark text:name="b"/><text:span><text:span text:style-name="T1">вложенные</text:span> спаны</text:span><office:annotation><text:p>комментарий</text:p></office:annotation></text:p>
        """.trimIndent()
        assertEquals(
            """
            H1 "Глава"
            H3 "Третий " {b}"уровень"
            H1 "Титул"
            P[center] "По центру " {b}"жирный" " " {i,u}"курсив подчёркнут"
            PAGEBREAK
            P[center] {i}"Наследует центр, курсив и разрыв"
            P[end] "Справа, разрыв после"
            PAGEBREAK
            P "H" {sub}"2" "O и x" {sup}"2" " " {c}"моно" " " {s}"зачёркнут" " не жирный"
            P "пробелы: три таб" \n "перенос схлопнут ся"
            RULE
            RULE
            QUOTE
              P "цитата"
              P "и ещё"
            CODE "код  с отступом\n\nвторая строка"
            P {link=https://example.com/?a=1&b=2}"ссылка" " якорь файл"
            P "в разделе продолжение"
            P "пункт оглавления"
            P {b}"вложенные" " спаны"

            """.trimIndent(),
            read(body, automatic),
        )
        val spaces = Documents.read(odt(body, automatic), DocFormat.ODT).blocks.filterIsInstance<Block.Paragraph>().first { it.content.plainText().startsWith("пробелы") }
        assertEquals("пробелы:   три таб\nперенос схлопнут ся", spaces.content.plainText())
    }

    @Test
    fun lists() {
        val body = """
            <text:list text:style-name="L1">
              <text:list-item><text:p>маркер</text:p>
                <text:list><text:list-item><text:p>вложенный номер</text:p></text:list-item><text:list-item><text:p>второй</text:p></text:list-item></text:list>
              </text:list-item>
              <text:list-item><text:p>ещё маркер</text:p><text:p>второй абзац пункта</text:p></text:list-item>
            </text:list>
            <text:list text:style-name="Numbering_20_123"><text:list-item><text:p>четыре из стиля</text:p></text:list-item><text:list-item><text:p>пять</text:p></text:list-item></text:list>
            <text:p>между</text:p>
            <text:list text:style-name="Numbering_20_123" text:continue-numbering="true"><text:list-item><text:p>шесть, продолжение</text:p></text:list-item></text:list>
            <text:list text:style-name="Numbering_20_123"><text:list-item text:start-value="10"><text:p>десять</text:p></text:list-item><text:list-header><text:p>заголовок списка</text:p></text:list-header></text:list>
            <text:list text:style-name="Numbering_20_123"><text:list-item><text:h text:outline-level="2">Нумерованный заголовок</text:h></text:list-item></text:list>
        """.trimIndent()
        assertEquals(
            """
            UL
              ITEM
                P "маркер"
                OL start=1
                  ITEM
                    P "вложенный номер"
                  ITEM
                    P "второй"
              ITEM
                P "ещё маркер"
                P "второй абзац пункта"
            OL start=4
              ITEM
                P "четыре из стиля"
              ITEM
                P "пять"
            P "между"
            OL start=6
              ITEM
                P "шесть, продолжение"
            OL start=10
              ITEM
                P "десять"
              ITEM
                P "заголовок списка"
            H2 "Нумерованный заголовок"

            """.trimIndent(),
            read(body, automatic),
        )
    }

    @Test
    fun tables() {
        val body = """
            <table:table table:name="T">
              <table:table-column table:number-columns-repeated="3"/>
              <table:table-header-rows><table:table-row><table:table-cell><text:p text:style-name="Table_20_Heading">A</text:p></table:table-cell><table:table-cell><text:p>B</text:p></table:table-cell><table:table-cell><text:p>C</text:p></table:table-cell></table:table-row></table:table-header-rows>
              <table:table-row><table:table-cell table:number-columns-spanned="2"><text:p>широкая</text:p></table:table-cell><table:covered-table-cell/><table:table-cell table:number-rows-spanned="2"><text:p>высокая</text:p></table:table-cell></table:table-row>
              <table:table-row><table:table-cell table:number-columns-repeated="2"><text:p>повтор</text:p></table:table-cell><table:covered-table-cell/></table:table-row>
              <table:table-row><table:table-cell/><table:table-cell><table:table table:name="N"><table:table-row><table:table-cell><text:p>вложенная</text:p></table:table-cell></table:table-row></table:table></table:table-cell><table:table-cell table:number-columns-repeated="1000"/></table:table-row>
            </table:table>
        """.trimIndent()
        assertEquals(
            """
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
                  P "широкая"
                CELL
                  P "высокая"
              ROW
                CELL
                  P "повтор"
                CELL
                  P "повтор"
                CELL
              ROW
                CELL
                CELL
                  TABLE header=0
                    ROW
                      CELL
                        P "вложенная"
                CELL

            """.trimIndent(),
            read(body, automatic),
        )
    }

    @Test
    fun framesNotesAndMeta() {
        val body = """
            <text:p>до<draw:frame draw:name="img" text:anchor-type="as-char" svg:width="2.54cm" svg:height="0.5in"><draw:image xlink:href="Pictures/10000000.png"/><svg:title>подпись</svg:title></draw:frame>после</text:p>
            <text:p><draw:a xlink:href="https://example.com"><draw:frame svg:width="10pt" svg:height="5pt"><draw:image><office:binary-data>${encodeBase64(png)}</office:binary-data></draw:image></draw:frame></draw:a></text:p>
            <text:p>надпись<draw:frame><draw:text-box><text:p>текст во врезке</text:p></draw:text-box></draw:frame></text:p>
            <text:p>Текст<text:note text:note-class="footnote"><text:note-citation>1</text:note-citation><text:note-body><text:p>Сноска <text:span text:style-name="T1">жирная</text:span></text:p></text:note-body></text:note> дальше</text:p>
        """.trimIndent()
        val doc = Documents.read(odt(body, automatic, meta = "<dc:title>Название</dc:title><meta:initial-creator>Автор</meta:initial-creator><dc:creator>Правщик</dc:creator>"), DocFormat.ODT)
        val sha = dumpBlocks(listOf(Block.Picture(png))).removePrefix("PICTURE ").substringBefore(" size")
        assertEquals(
            """
            P "до"
            PICTURE $sha size=720x360 alt="подпись"
            P "после"
            PICTURE $sha size=100x50 alt=""
            P "надпись"
            P "текст во врезке"
            P "Текст" {sup}"[1]" " дальше"
            RULE
            OL start=1
              ITEM
                P "Сноска " {b}"жирная"

            """.trimIndent(),
            dumpBlocks(doc.blocks),
        )
        assertEquals("Название", doc.title)
        assertEquals("Автор", doc.author)
        assertEquals("ru-RU", doc.language)
    }
}
