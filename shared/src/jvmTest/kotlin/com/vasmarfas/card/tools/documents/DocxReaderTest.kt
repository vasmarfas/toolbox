package com.vasmarfas.card.tools.documents

import com.vasmarfas.card.core.ZipWriter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val NS = "xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" " +
    "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" " +
    "xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\" " +
    "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" " +
    "xmlns:pic=\"http://schemas.openxmlformats.org/drawingml/2006/picture\" " +
    "xmlns:mc=\"http://schemas.openxmlformats.org/markup-compatibility/2006\" " +
    "xmlns:wps=\"http://schemas.microsoft.com/office/word/2010/wordprocessingShape\" " +
    "xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:o=\"urn:schemas-microsoft-com:office:office\" " +
    "xmlns:m=\"http://schemas.openxmlformats.org/officeDocument/2006/math\""

class DocxReaderTest {
    private val styles = """
        <w:styles $NS>
          <w:docDefaults><w:rPrDefault><w:rPr><w:rFonts w:asciiTheme="minorHAnsi" w:hAnsiTheme="minorHAnsi"/><w:lang w:val="ru-RU"/></w:rPr></w:rPrDefault></w:docDefaults>
          <w:style w:type="paragraph" w:default="1" w:styleId="a"><w:name w:val="Normal"/></w:style>
          <w:style w:type="paragraph" w:styleId="1"><w:name w:val="heading 1"/><w:basedOn w:val="a"/><w:pPr><w:outlineLvl w:val="0"/></w:pPr><w:rPr><w:b/><w:sz w:val="32"/></w:rPr></w:style>
          <w:style w:type="paragraph" w:styleId="Заголовок2"><w:name w:val="heading 2"/><w:basedOn w:val="a"/><w:rPr><w:b/><w:i/></w:rPr></w:style>
          <w:style w:type="paragraph" w:styleId="MyHeading"><w:name w:val="My Custom Heading"/><w:basedOn w:val="a"/><w:pPr><w:outlineLvl w:val="2"/></w:pPr></w:style>
          <w:style w:type="paragraph" w:styleId="Derived"><w:name w:val="Derived From Custom"/><w:basedOn w:val="MyHeading"/></w:style>
          <w:style w:type="paragraph" w:styleId="Emph"><w:name w:val="Emphasis Para"/><w:basedOn w:val="a"/><w:pPr><w:jc w:val="center"/></w:pPr><w:rPr><w:i/></w:rPr></w:style>
          <w:style w:type="character" w:styleId="Strong"><w:name w:val="Strong"/><w:rPr><w:b/></w:rPr></w:style>
          <w:style w:type="character" w:styleId="Mono"><w:name w:val="Mono"/><w:rPr><w:rFonts w:ascii="Consolas" w:hAnsi="Consolas"/></w:rPr></w:style>
          <w:style w:type="character" w:styleId="Hyperlink"><w:name w:val="Hyperlink"/><w:rPr><w:u w:val="single"/></w:rPr></w:style>
          <w:style w:type="paragraph" w:styleId="HTML"><w:name w:val="HTML Preformatted"/><w:basedOn w:val="a"/><w:rPr><w:rFonts w:ascii="Courier New" w:hAnsi="Courier New"/></w:rPr></w:style>
          <w:style w:type="paragraph" w:styleId="Quote"><w:name w:val="Quote"/><w:basedOn w:val="a"/><w:rPr><w:i/></w:rPr></w:style>
          <w:style w:type="paragraph" w:styleId="ListBullet"><w:name w:val="List Bullet"/><w:basedOn w:val="a"/><w:pPr><w:numPr><w:numId w:val="3"/></w:numPr></w:pPr></w:style>
          <w:style w:type="numbering" w:styleId="OutlineList"><w:name w:val="Outline List"/><w:pPr><w:numPr><w:numId w:val="1"/></w:numPr></w:pPr></w:style>
        </w:styles>
    """.trimIndent()

    private val numbering = """
        <w:numbering $NS>
          <w:abstractNum w:abstractNumId="10">
            <w:lvl w:ilvl="0"><w:start w:val="1"/><w:numFmt w:val="decimal"/><w:lvlText w:val="%1."/><w:pPr><w:ind w:left="720" w:hanging="360"/></w:pPr></w:lvl>
            <w:lvl w:ilvl="1"><w:start w:val="1"/><w:numFmt w:val="lowerLetter"/><w:lvlText w:val="%2)"/><w:pPr><w:ind w:left="1440" w:hanging="360"/></w:pPr></w:lvl>
          </w:abstractNum>
          <w:abstractNum w:abstractNumId="20"><w:lvl w:ilvl="0"><w:numFmt w:val="bullet"/><w:lvlText w:val="•"/><w:pPr><w:ind w:left="720" w:hanging="360"/></w:pPr></w:lvl></w:abstractNum>
          <w:abstractNum w:abstractNumId="30"><w:numStyleLink w:val="OutlineList"/></w:abstractNum>
          <w:abstractNum w:abstractNumId="40"><w:lvl w:ilvl="0"><w:numFmt w:val="none"/></w:lvl></w:abstractNum>
          <w:num w:numId="1"><w:abstractNumId w:val="10"/></w:num>
          <w:num w:numId="2"><w:abstractNumId w:val="10"/><w:lvlOverride w:ilvl="0"><w:startOverride w:val="7"/></w:lvlOverride></w:num>
          <w:num w:numId="3"><w:abstractNumId w:val="20"/></w:num>
          <w:num w:numId="4"><w:abstractNumId w:val="30"/></w:num>
          <w:num w:numId="5"><w:abstractNumId w:val="40"/></w:num>
        </w:numbering>
    """.trimIndent()

    private val png = TestImages.png(10, 5)

    private fun docx(body: String, footnotes: String? = null, rels: String = "", core: String? = null): ByteArray {
        val zip = ZipWriter()
        zip.add(
            "[Content_Types].xml",
            ("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>").encodeToByteArray(),
        )
        zip.add(
            "_rels/.rels",
            ("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>" +
                "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties\" Target=\"docProps/core.xml\"/>" +
                "</Relationships>").encodeToByteArray(),
        )
        zip.add("word/document.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><w:document $NS><w:body>$body<w:sectPr/></w:body></w:document>".encodeToByteArray())
        zip.add("word/styles.xml", styles.encodeToByteArray())
        zip.add("word/numbering.xml", numbering.encodeToByteArray())
        if (footnotes != null) zip.add("word/footnotes.xml", footnotes.encodeToByteArray())
        zip.add("word/media/image1.png", png)
        zip.add(
            "word/_rels/document.xml.rels",
            ("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rIdStyles\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>" +
                "<Relationship Id=\"rIdNum\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/numbering\" Target=\"numbering.xml\"/>" +
                "<Relationship Id=\"rIdNotes\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/footnotes\" Target=\"footnotes.xml\"/>" +
                "<Relationship Id=\"rIdImg\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"media/image1.png\"/>" +
                "<Relationship Id=\"rIdAbs\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"/word/media/image1.png\"/>" +
                "<Relationship Id=\"rIdLink\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink\" Target=\"https://example.com/путь\" TargetMode=\"External\"/>" +
                rels + "</Relationships>").encodeToByteArray(),
        )
        if (core != null) zip.add("docProps/core.xml", core.encodeToByteArray())
        return zip.toByteArray()
    }

    private fun read(body: String, footnotes: String? = null): Doc = Documents.read(docx(body, footnotes), DocFormat.DOCX)

    private fun p(runs: String, pPr: String = "") = "<w:p>${if (pPr.isEmpty()) "" else "<w:pPr>$pPr</w:pPr>"}$runs</w:p>"

    private fun r(text: String, rPr: String = "") = "<w:r>${if (rPr.isEmpty()) "" else "<w:rPr>$rPr</w:rPr>"}<w:t xml:space=\"preserve\">$text</w:t></w:r>"

    @Test
    fun headingsFromStyleNamesAndOutlineLevels() {
        val doc = read(
            p(r("Глава"), "<w:pStyle w:val=\"1\"/>") +
                p(r("Раздел"), "<w:pStyle w:val=\"Заголовок2\"/>") +
                p(r("Через basedOn"), "<w:pStyle w:val=\"Derived\"/>") +
                p(r("Прямой outlineLvl"), "<w:outlineLvl w:val=\"3\"/>") +
                p(r("Уровень 9 значит текст"), "<w:pStyle w:val=\"1\"/><w:outlineLvl w:val=\"9\"/>") +
                p(r("частично ") + r("жирный", "<w:b/>"), "<w:pStyle w:val=\"1\"/>"),
        )
        assertEquals(
            "H1 \"Глава\"\nH2 \"Раздел\"\nH3 \"Через basedOn\"\nH4 \"Прямой outlineLvl\"\nP {b}\"Уровень 9 значит текст\"\nH1 \"частично \" {b}\"жирный\"\n",
            dumpBlocks(doc.blocks),
        )
    }

    @Test
    fun runProperties() {
        val runs = r("жирный", "<w:b/>") + r("не", "<w:b w:val=\"0\"/>") + r("курсив", "<w:i w:val=\"true\"/>") +
            r("подчёркнут", "<w:u w:val=\"single\"/>") + r("нет", "<w:u w:val=\"none\"/>") + r("зачёркнут", "<w:strike/>") +
            r("двойной", "<w:dstrike/>") + r("верх", "<w:vertAlign w:val=\"superscript\"/>") + r("низ", "<w:vertAlign w:val=\"subscript\"/>") +
            r("стиль", "<w:rStyle w:val=\"Strong\"/>") + r("моно", "<w:rStyle w:val=\"Mono\"/>") + r("курьер", "<w:rFonts w:ascii=\"Courier New\" w:hAnsi=\"Courier New\"/>") +
            r("скрыт", "<w:vanish/>") + r("тема", "<w:rStyle w:val=\"Mono\"/><w:rFonts w:asciiTheme=\"minorHAnsi\"/>")
        assertEquals(
            "P {b}\"жирный\" \"не\" {i}\"курсив\" {u}\"подчёркнут\" \"нет\" {s}\"зачёркнутдвойной\" {sup}\"верх\" {sub}\"низ\" {b}\"стиль\" {c}\"монокурьер\" \"тема\"\n",
            dumpBlocks(read(p(runs)).blocks),
        )
        val special = "<w:r><w:t>a</w:t><w:tab/><w:t>b</w:t><w:br/><w:t>c</w:t><w:cr/><w:t>d</w:t><w:noBreakHyphen/><w:t>e</w:t><w:softHyphen/><w:t>f</w:t>" +
            "<w:sym w:font=\"Symbol\" w:char=\"F0B7\"/><w:sym w:font=\"Wingdings\" w:char=\"F0FC\"/><w:sym w:font=\"Symbol\" w:char=\"F061\"/></w:r>" +
            "<w:r><w:rPr><w:rFonts w:ascii=\"Symbol\" w:hAnsi=\"Symbol\"/></w:rPr><w:t>abg</w:t></w:r>"
        assertEquals("P \"a b\" \\n \"c\" \\n \"d-ef•✓ααβγ\"\n", dumpBlocks(read(p(special)).blocks))
    }

    @Test
    fun paragraphStyleFormattingAndAlignment() {
        val doc = read(
            p(r("из стиля ") + r("выключено", "<w:i w:val=\"0\"/>"), "<w:pStyle w:val=\"Emph\"/>") +
                p(r("справа"), "<w:jc w:val=\"right\"/>") + p(r("по ширине"), "<w:jc w:val=\"both\"/>") + p(r("start"), "<w:jc w:val=\"start\"/>") +
                p(r("цитата"), "<w:pStyle w:val=\"Quote\"/>") + p(r("вторая"), "<w:pStyle w:val=\"Quote\"/>") +
                p(r("код  с пробелами"), "<w:pStyle w:val=\"HTML\"/>") + p("", "<w:pStyle w:val=\"HTML\"/>") + p(r("после пустой"), "<w:pStyle w:val=\"HTML\"/>") +
                p("", "<w:pBdr><w:bottom w:val=\"single\" w:sz=\"6\" w:space=\"1\" w:color=\"auto\"/></w:pBdr>"),
        )
        assertEquals(
            "P[center] {i}\"из стиля \" \"выключено\"\nP[end] \"справа\"\nP[justify] \"по ширине\"\nP \"start\"\nQUOTE\n  P \"цитата\"\n  P \"вторая\"\n" +
                "CODE \"код  с пробелами\\n\\nпосле пустой\"\nRULE\n",
            dumpBlocks(doc.blocks),
        )
        assertEquals("ru-RU", doc.language)
    }

    @Test
    fun hyperlinksAndFields() {
        val body = p(
            "<w:hyperlink r:id=\"rIdLink\"><w:r><w:rPr><w:rStyle w:val=\"Hyperlink\"/></w:rPr><w:t>ссылка</w:t></w:r></w:hyperlink>" +
                "<w:hyperlink w:anchor=\"_Toc1\"><w:r><w:t> якорь</w:t></w:r></w:hyperlink>" +
                "<w:fldSimple w:instr=\" HYPERLINK &quot;https://simple.example/&quot; \"><w:r><w:t> простое поле</w:t></w:r></w:fldSimple>" +
                "<w:r><w:fldChar w:fldCharType=\"begin\"/></w:r><w:r><w:instrText xml:space=\"preserve\"> HYPERLINK \"https://complex</w:instrText></w:r>" +
                "<w:r><w:instrText>.example/\" \\o \"подсказка\" </w:instrText></w:r><w:r><w:fldChar w:fldCharType=\"separate\"/></w:r>" +
                "<w:r><w:t> сложное </w:t></w:r><w:r><w:fldChar w:fldCharType=\"begin\"/></w:r><w:r><w:instrText> PAGE </w:instrText></w:r>" +
                "<w:r><w:fldChar w:fldCharType=\"separate\"/></w:r><w:r><w:t>7</w:t></w:r><w:r><w:fldChar w:fldCharType=\"end\"/></w:r>" +
                "<w:r><w:fldChar w:fldCharType=\"end\"/></w:r>" +
                "<w:r><w:fldChar w:fldCharType=\"begin\"/></w:r><w:r><w:instrText> HYPERLINK \\l \"закладка\" </w:instrText></w:r>" +
                "<w:r><w:fldChar w:fldCharType=\"separate\"/></w:r><w:r><w:t> на закладку</w:t></w:r><w:r><w:fldChar w:fldCharType=\"end\"/></w:r>",
        ) + p(
            "<w:r><w:fldChar w:fldCharType=\"begin\"/></w:r><w:r><w:instrText> TOC \\o \"1-3\" </w:instrText></w:r><w:r><w:fldChar w:fldCharType=\"separate\"/></w:r><w:r><w:t>Оглавление 1</w:t></w:r>",
        ) + p(r("Оглавление 2") + "<w:r><w:fldChar w:fldCharType=\"end\"/></w:r>") + p(r("после поля"))
        assertEquals(
            "P {link=https://example.com/путь}\"ссылка\" \" якорь\" {link=https://simple.example/}\" простое поле\" {link=https://complex.example/}\" сложное 7\" \" на закладку\"\n" +
                "P \"Оглавление 1\"\nP \"Оглавление 2\"\nP \"после поля\"\n",
            dumpBlocks(read(body).blocks),
        )
    }

    @Test
    fun listsAndNumbering() {
        fun item(text: String, numId: Int, level: Int = 0) = p(r(text), "<w:numPr><w:ilvl w:val=\"$level\"/><w:numId w:val=\"$numId\"/></w:numPr>")
        val body = item("один", 1) + item("два", 1) + p("") + item("вложенный а", 1, 1) +
            p(r("продолжение пункта"), "<w:ind w:left=\"1440\"/>") +
            p(r("обычный абзац прерывает список")) +
            item("три, нумерация продолжается", 1) +
            item("семь из startOverride", 2) + item("восемь", 2) +
            p(r("маркер из стиля"), "<w:pStyle w:val=\"ListBullet\"/>") + p(r("и ещё"), "<w:pStyle w:val=\"ListBullet\"/>") +
            p(r("numId 0 отменяет стиль"), "<w:pStyle w:val=\"ListBullet\"/><w:numPr><w:numId w:val=\"0\"/></w:numPr>") +
            item("через numStyleLink", 4) +
            item("формат none", 5)
        assertEquals(
            """
            OL start=1
              ITEM
                P "один"
              ITEM
                P "два"
                OL start=1
                  ITEM
                    P "вложенный а"
                    P "продолжение пункта"
            P "обычный абзац прерывает список"
            OL start=3
              ITEM
                P "три, нумерация продолжается"
            OL start=7
              ITEM
                P "семь из startOverride"
              ITEM
                P "восемь"
            UL
              ITEM
                P "маркер из стиля"
              ITEM
                P "и ещё"
            P "numId 0 отменяет стиль"
            OL start=9
              ITEM
                P "через numStyleLink"
            P "формат none"

            """.trimIndent(),
            dumpBlocks(read(body).blocks),
        )
    }

    @Test
    fun tables() {
        fun tc(text: String, tcPr: String = "") = "<w:tc>${if (tcPr.isEmpty()) "" else "<w:tcPr>$tcPr</w:tcPr>"}${p(r(text))}</w:tc>"
        val nested = "<w:tbl><w:tr>${tc("вложенная")}</w:tr></w:tbl><w:p/>"
        val body = "<w:tbl><w:tblPr/><w:tblGrid><w:gridCol/><w:gridCol/><w:gridCol/></w:tblGrid>" +
            "<w:tr><w:trPr><w:tblHeader/></w:trPr>${tc("A")}${tc("B")}${tc("C")}</w:tr>" +
            "<w:tr>${tc("шире", "<w:gridSpan w:val=\"2\"/>")}${tc("сверху", "<w:vMerge w:val=\"restart\"/>")}</w:tr>" +
            "<w:tr><w:trPr><w:gridBefore w:val=\"1\"/></w:trPr>${tc("x")}<w:tc><w:tcPr><w:vMerge/></w:tcPr><w:p/></w:tc></w:tr>" +
            "<w:sdt><w:sdtContent><w:tr><w:tc>$nested</w:tc>${tc("sdt строка")}</w:tr></w:sdtContent></w:sdt>" +
            "</w:tbl>"
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
                  P "шире"
                CELL
                  P "сверху"
              ROW
                CELL
                CELL
                  P "x"
                CELL
              ROW
                CELL
                  TABLE header=0
                    ROW
                      CELL
                        P "вложенная"
                CELL
                  P "sdt строка"

            """.trimIndent(),
            dumpBlocks(read(body).blocks),
        )
    }

    @Test
    fun drawingsTextBoxesAndBreaks() {
        val inline = "<w:r><w:drawing><wp:inline><wp:extent cx=\"1270000\" cy=\"635000\"/><wp:docPr id=\"1\" name=\"Рисунок 1\" descr=\"описание\"/>" +
            "<a:graphic><a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/picture\"><pic:pic><pic:blipFill><a:blip r:embed=\"rIdImg\"/></pic:blipFill></pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r>"
        val textBox = "<w:r><mc:AlternateContent><mc:Choice Requires=\"wps\"><w:drawing><wp:anchor><wp:extent cx=\"100\" cy=\"100\"/><wp:docPr id=\"2\" name=\"Надпись\"/>" +
            "<a:graphic><a:graphicData uri=\"http://schemas.microsoft.com/office/word/2010/wordprocessingShape\"><wps:wsp><wps:txbx><w:txbxContent>${p(r("текст надписи"))}</w:txbxContent></wps:txbx></wps:wsp></a:graphicData></a:graphic>" +
            "</wp:anchor></w:drawing></mc:Choice><mc:Fallback><w:pict><v:shape><v:textbox><w:txbxContent>${p(r("текст надписи"))}</w:txbxContent></v:textbox></v:shape></w:pict></mc:Fallback></mc:AlternateContent></w:r>"
        val vml = "<w:r><w:pict><v:shape style=\"width:72pt;height:36pt\"><v:imagedata r:id=\"rIdAbs\" o:title=\"vml\"/></v:shape></w:pict></w:r>"
        val body = p(r("до") + inline + r("после")) + p(r("хозяин надписи") + textBox) + p(vml) +
            p(r("перед разрывом") + "<w:r><w:br w:type=\"page\"/></w:r>" + r("после разрыва")) +
            p(r("с новой страницы"), "<w:pageBreakBefore/>") +
            p(r("конец раздела"), "<w:sectPr><w:type w:val=\"nextPage\"/></w:sectPr>") +
            p(r("непрерывный раздел"), "<w:sectPr><w:type w:val=\"continuous\"/></w:sectPr>") +
            p("<w:r><w:pict><v:rect o:hr=\"t\" style=\"width:0;height:1.5pt\"/></w:pict></w:r>") +
            p("<m:oMathPara><m:oMath><m:r><m:t>E=mc</m:t></m:r><m:sSup><m:e><m:r><m:t>2</m:t></m:r></m:e></m:sSup></m:oMath></m:oMathPara>")
        val doc = read(body)
        assertEquals(
            """
            P "до"
            PICTURE ${sha(png)} size=1000x500 alt="описание"
            P "после"
            P "хозяин надписи"
            P "текст надписи"
            PICTURE ${sha(png)} size=720x360 alt="vml"
            P "перед разрывом"
            PAGEBREAK
            P "после разрыва"
            PAGEBREAK
            P "с новой страницы"
            P "конец раздела"
            PAGEBREAK
            P "непрерывный раздел"
            RULE
            P "E=mc2"

            """.trimIndent(),
            dumpBlocks(doc.blocks),
        )
    }

    @Test
    fun revisionsAndWrappers() {
        val body = p(
            r("было ") + "<w:del><w:r><w:delText>удалено</w:delText></w:r></w:del>" + "<w:ins><w:r><w:t>вставлено</w:t></w:r></w:ins>" +
                "<w:moveFrom><w:r><w:t>ушло</w:t></w:r></w:moveFrom>" + "<w:moveTo><w:r><w:t> пришло</w:t></w:r></w:moveTo>" +
                "<w:smartTag><w:r><w:t> умный</w:t></w:r></w:smartTag>" + "<w:sdt><w:sdtContent><w:r><w:t> элемент</w:t></w:r></w:sdtContent></w:sdt>" +
                "<w:customXml><w:r><w:t> свой</w:t></w:r></w:customXml>" + "<w:proofErr w:type=\"spellStart\"/><w:bookmarkStart w:id=\"0\" w:name=\"x\"/>",
        ) + "<w:sdt><w:sdtPr/><w:sdtContent>${p(r("абзац в sdt"))}</w:sdtContent></w:sdt>"
        assertEquals("P \"было вставлено пришло умный элемент свой\"\nP \"абзац в sdt\"\n", dumpBlocks(read(body).blocks))
    }

    @Test
    fun footnotesAndEndnotes() {
        val footnotes = """
            <w:footnotes $NS>
              <w:footnote w:type="separator" w:id="-1"><w:p><w:r><w:separator/></w:r></w:p></w:footnote>
              <w:footnote w:id="1"><w:p><w:r><w:rPr><w:vertAlign w:val="superscript"/></w:rPr><w:footnoteRef/></w:r><w:r><w:t xml:space="preserve"> Текст сноски</w:t></w:r></w:p></w:footnote>
              <w:footnote w:id="2"><w:p><w:r><w:footnoteRef/></w:r><w:r><w:t xml:space="preserve"> Вторая </w:t></w:r><w:r><w:rPr><w:b/></w:rPr><w:t>жирная</w:t></w:r></w:p></w:footnote>
            </w:footnotes>
        """.trimIndent()
        val body = p(r("Текст") + "<w:r><w:rPr><w:vertAlign w:val=\"superscript\"/></w:rPr><w:footnoteReference w:id=\"1\"/></w:r>" + r(" и ещё") + "<w:r><w:footnoteReference w:id=\"2\"/></w:r>")
        assertEquals(
            "P \"Текст\" {sup}\"[1]\" \" и ещё\" {sup}\"[2]\"\nRULE\nOL start=1\n  ITEM\n    P \"Текст сноски\"\n  ITEM\n    P \"Вторая \" {b}\"жирная\"\n",
            dumpBlocks(read(body, footnotes).blocks),
        )
    }

    @Test
    fun coreProperties() {
        val core = "<cp:coreProperties xmlns:cp=\"http://schemas.openxmlformats.org/package/2006/metadata/core-properties\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">" +
            "<dc:title>Название</dc:title><dc:creator>Автор Авторович</dc:creator><dc:language>ru</dc:language></cp:coreProperties>"
        val doc = Documents.read(docx(p(r("x")), core = core), DocFormat.DOCX)
        assertEquals("Название", doc.title)
        assertEquals("Автор Авторович", doc.author)
        assertEquals("ru", doc.language)
    }

    @Test
    fun missingOptionalPartsAreTolerated() {
        val zip = ZipWriter()
        zip.add("word/document.xml", "<w:document $NS><w:body>${p(r("только документ"))}</w:body></w:document>".encodeToByteArray())
        zip.add("word/styles.xml", "<w:styles broken".encodeToByteArray())
        val doc = Documents.read(zip.toByteArray(), DocFormat.DOCX)
        assertEquals("P \"только документ\"\n", dumpBlocks(doc.blocks))
        assertTrue(Documents.detect(zip.toByteArray(), "x.bin") == DocFormat.DOCX)
    }

    private fun sha(bytes: ByteArray): String = dumpBlocks(listOf(Block.Picture(bytes))).removePrefix("PICTURE ").substringBefore(" size")
}
