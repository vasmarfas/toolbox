package com.vasmarfas.card.tools.documents.pdf

import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDTrueTypeFont
import org.apache.pdfbox.pdmodel.font.encoding.WinAnsiEncoding

class PdfTextTest {
    private fun PDDocument.mixedPage() {
        val page = PDPage(PDRectangle.A4)
        addPage(page)
        PDPageContentStream(this, page).use { cs ->
            cs.beginText()
            cs.setFont(helvetica(), 12f)
            cs.newLineAtOffset(50f, 750f)
            cs.showText("Plain start, ")
            cs.setFont(helveticaBold(), 12f)
            cs.showText("bold middle")
            cs.setFont(times(), 12f)
            cs.showText(" and times end.")
            cs.newLineAtOffset(0f, -20f)
            cs.showTextWithPositioning(arrayOf<Any>("Kerned", -320f, "words", 40f, "stay", -1000f, "apart"))
            cs.newLineAtOffset(0f, -20f)
            cs.setCharacterSpacing(1f)
            cs.setWordSpacing(6f)
            cs.showText("Spaced out words")
            cs.setCharacterSpacing(0f)
            cs.setWordSpacing(0f)
            cs.newLineAtOffset(0f, -20f)
            cs.setHorizontalScaling(70f)
            cs.showText("Condensed line")
            cs.endText()
        }
    }

    private fun comparisonDocument(mode: SaveMode) = pdf(mode) { doc ->
        doc.textPage(helvetica(), listOf("The quick brown fox", "jumps over the lazy dog.", "Price: 100 € — “quoted” text", "Naïve café Ångström"))
        doc.textPage(times(), listOf("Times Roman page", "with two lines"), fontSize = 20f)
        doc.textPage(courier(), listOf("Monospaced   spacing", "x = y + 1;"))
        doc.textPage(doc.arial(), listOf("Съешь же ещё этих мягких французских булок,", "да выпей чаю. Ёё Ъъ Ыы", "Mixed Latin и кириллица 2024"))
        doc.textPage(PDTrueTypeFont.load(doc, arialFile, WinAnsiEncoding.INSTANCE), listOf("TrueType simple font", "Ångström café"))
        doc.mixedPage()
    }

    @Test
    fun matchesPdfBoxTextStripper() {
        for (mode in SaveMode.entries) {
            val bytes = comparisonDocument(mode)
            val doc = PdfDocument.parse(bytes)
            for (i in 0 until doc.pageCount) {
                assertEquals(normalize(pdfboxText(bytes, i)), normalize(PdfText.extract(doc, i)), "page $i")
            }
        }
    }

    @Test
    fun lineAndWordLayout() {
        val doc = PdfDocument.parse(comparisonDocument(SaveMode.CLASSIC))
        assertEquals("The quick brown fox\njumps over the lazy dog.\nPrice: 100 € — “quoted” text\nNaïve café Ångström", PdfText.extract(doc, 0))
        assertEquals("Monospaced   spacing\nx = y + 1;", PdfText.extract(doc, 2))
        assertEquals("Съешь же ещё этих мягких французских булок,\nда выпей чаю. Ёё Ъъ Ыы\nMixed Latin и кириллица 2024", PdfText.extract(doc, 3))
        assertEquals("Plain start, bold middle and times end.\nKerned wordsstay apart\nSpaced out words\nCondensed line", PdfText.extract(doc, 5))
    }

    private fun rawText(content: String, fonts: String = "/F1 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"): String {
        val bytes = rawDocument(latin1(content), fonts)
        return PdfText.extract(PdfDocument.parse(bytes), 0)
    }

    private fun rawDocument(content: ByteArray, fonts: String): ByteArray {
        val pdf = RawPdf()
        pdf.obj(1, "<< /Type /Catalog /Pages 2 0 R >>")
        pdf.obj(2, "<< /Type /Pages /Kids [3 0 R] /Count 1 >>")
        pdf.obj(3, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << $fonts >> /XObject << /X1 5 0 R >> >> >>")
        pdf.stream(4, "<< /Length ${content.size} >>", content)
        val form = latin1("BT /F1 10 Tf 0 0 Td (Form text) Tj ET")
        pdf.stream(5, "<< /Type /XObject /Subtype /Form /BBox [0 0 100 100] /Matrix [1 0 0 1 300 100] /Length ${form.size} >>", form)
        return pdf.finish("<< /Size 6 /Root 1 0 R >>")
    }

    @Test
    fun textOperators() {
        assertEquals("One\nTwo\nThree\nFour", rawText("BT /F1 12 Tf 14 TL 72 700 Td (One) Tj T* (Two) Tj (Three) ' 2 1 (Four) \" ET"))
        assertEquals("Left right", rawText("BT /F1 12 Tf 72 700 Td (Left) Tj 60 0 Td (right) Tj ET"))
        assertEquals("Stuck", rawText("BT /F1 12 Tf 72 700 Td (Stu) Tj 17.34 0 Td (ck) Tj ET"))
        assertEquals("Moved\ndown", rawText("BT /F1 12 Tf 72 700 Td (Moved) Tj 0 -14 TD (down) Tj ET"))
        assertEquals("Matrix placed", rawText("BT /F1 1 Tf 12 0 0 12 72 700 Tm (Matrix) Tj 12 0 0 12 120 700 Tm (placed) Tj ET"))
        assertEquals("Raised2 text", rawText("BT /F1 12 Tf 72 700 Td (Raised) Tj 3 Ts (2) Tj 0 Ts ( text) Tj ET"))
        assertEquals("Scaled by cm", rawText("q 2 0 0 2 0 0 cm BT /F1 6 Tf 36 350 Td (Scaled by cm) Tj ET Q"))
        assertEquals("Vertical run", rawText("BT /F1 12 Tf 0 1 -1 0 300 100 Tm (Vertical run) Tj ET"))
        assertEquals("Page text\nForm text", rawText("BT /F1 12 Tf 72 700 Td (Page text) Tj ET /X1 Do"))
        assertEquals("", rawText("BT /F9 12 Tf 72 700 Td (No such font) Tj ET"))
    }

    @Test
    fun overprintedTextIsNotDoubled() {
        val content = "BT /F1 12 Tf 72 700 Td (Bold title) Tj ET BT /F1 12 Tf 72.4 700.2 Td (Bold title) Tj ET " +
            "BT /F1 12 Tf 72 680 Td (hello, all) Tj ET"
        assertEquals("Bold title\nhello, all", rawText(content))
        val bytes = rawDocument(latin1(content), "/F1 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>")
        assertEquals("Bold title hello, all", normalize(pdfboxText(bytes, 0)))
    }

    @Test
    fun inlineImagesAreSkipped() {
        val font = "/F1 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"
        val text = latin1("BT /F1 12 Tf 72 700 Td (After images) Tj ET")
        val raw = ByteArrayOutputStream()
        raw.write(latin1("q 8 0 0 2 72 600 cm BI /W 4 /H 2 /BPC 8 /CS /G ID "))
        raw.write(byteArrayOf(1, 'E'.code.toByte(), 'I'.code.toByte(), 32, 0x0A, 'E'.code.toByte(), 'I'.code.toByte(), 0x0A))
        raw.write(latin1(" EI Q\n"))
        val sized = rawDocument(raw.toByteArray() + text, font)
        assertEquals("After images", PdfText.extract(PdfDocument.parse(sized), 0))
        assertEquals("After images", pdfboxText(sized, 0).trim())
        raw.write(latin1("BI /W 2 /H 2 /F /DCT ID "))
        raw.write(byteArrayOf(-1, -40, 32, 'E'.code.toByte(), 'I'.code.toByte(), 32, 0, 1, 2, -128, 10, 'E'.code.toByte(), 'I'.code.toByte(), 10, 7, -1))
        raw.write(latin1("\nEI\n"))
        raw.write(latin1("BI /W 2 /H 1 /CS /RGB /F /AHx ID 0a0b0c 45 49 0d0e0f> EI\n"))
        val searched = rawDocument(raw.toByteArray() + text, font)
        assertEquals("After images", PdfText.extract(PdfDocument.parse(searched), 0))
    }

    @Test
    fun simpleFontEncodings() {
        val symbol = "/F1 << /Type /Font /Subtype /Type1 /BaseFont /Symbol >>"
        assertEquals("αβγ ∑", rawText("BT /F1 12 Tf 72 700 Td (abg \\345) Tj ET", symbol))
        val dingbats = "/F1 << /Type /Font /Subtype /Type1 /BaseFont /ZapfDingbats >>"
        assertEquals("✓■①", rawText("BT /F1 12 Tf 72 700 Td (3n\\254) Tj ET", dingbats))
        val cyrillic = "/F1 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding << /Type /Encoding /BaseEncoding /WinAnsiEncoding " +
            "/Differences [192 /afii10017 /afii10018 /afii10019 /afii10023 /uni0416 /u1F600 /f_f_i /A.sc /g37] >> >>"
        assertEquals("АБВЁЖ😀ffiA", rawText("BT /F1 12 Tf 72 700 Td (\\300\\301\\302\\303\\304\\305\\306\\307\\310) Tj ET", cyrillic))
        val mac = "/F1 << /Type /Font /Subtype /Type1 /BaseFont /Times-Roman /Encoding /MacRomanEncoding >>"
        assertEquals("äü•", rawText("BT /F1 12 Tf 72 700 Td (\\212\\237\\245) Tj ET", mac))
        val standard = "/F1 << /Type /Font /Subtype /Type1 /BaseFont /Times-Roman >>"
        assertEquals("’quoted‘ ﬁ", rawText("BT /F1 12 Tf 72 700 Td ('quoted` \\256) Tj ET", standard))
        val type3 = "/F1 << /Type /Font /Subtype /Type3 /FontBBox [0 0 1000 1000] /FontMatrix [0.001 0 0 0.001 0 0] /CharProcs << >> " +
            "/Encoding << /Type /Encoding /Differences [65 /a /b /space] >> /FirstChar 65 /LastChar 67 /Widths [500 600 250] /Resources << >> >>"
        assertEquals("ab ab", rawText("BT /F1 20 Tf 72 700 Td (ABCAB) Tj ET", type3))
        val bytes = rawDocument(latin1("BT /F1 12 Tf 72 700 Td (\\300\\301\\302\\303\\304) Tj ET"), cyrillic)
        assertEquals("АБВЁЖ", pdfboxText(bytes, 0).trim())
    }

    @Test
    fun type0FontsWithToUnicode() {
        val toUnicode = latin1(
            """
            /CIDInit /ProcSet findresource begin 12 dict begin begincmap
            /CMapName /Custom def
            1 begincodespacerange <0000> <FFFF> endcodespacerange
            2 beginbfchar <0001> <0048> <0002> <D83DDE00> endbfchar
            1 beginbfrange <0010> <0012> <0430> endbfrange
            endcmap CMapName currentdict /CMap defineresource pop end end
            """.trimIndent(),
        )
        val pdf = RawPdf()
        pdf.obj(1, "<< /Type /Catalog /Pages 2 0 R >>")
        pdf.obj(2, "<< /Type /Pages /Kids [3 0 R] /Count 1 >>")
        pdf.obj(3, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>")
        val content = latin1("BT /F1 12 Tf 72 700 Td <0001001000110012> Tj [<0002> -3000 <0001>] TJ ET")
        pdf.stream(4, "<< /Length ${content.size} >>", content)
        pdf.obj(5, "<< /Type /Font /Subtype /Type0 /BaseFont /Custom /Encoding /Identity-H /DescendantFonts [6 0 R] /ToUnicode 7 0 R >>")
        pdf.obj(
            6,
            "<< /Type /Font /Subtype /CIDFontType2 /BaseFont /Custom /CIDSystemInfo << /Registry (Adobe) /Ordering (Identity) /Supplement 0 >> " +
                "/DW 500 /W [1 [600] 16 18 400] >>",
        )
        pdf.stream(7, "<< /Length ${toUnicode.size} >>", toUnicode)
        val doc = PdfDocument.parse(pdf.finish("<< /Size 8 /Root 1 0 R >>"))
        assertEquals("Hабв😀 H", PdfText.extract(doc, 0))
    }

    @Test
    fun cmapParsing() {
        val text = """
            /CIDInit /ProcSet findresource begin
            12 dict begin
            begincmap
            /CMapName /Test def
            /WMode 1 def
            2 begincodespacerange
            <00> <7F>
            <8000> <FFFF>
            endcodespacerange
            3 beginbfchar
            <41> <0042>
            <8001> <D83DDE00>
            <8002> <006600660069>
            endbfchar
            3 beginbfrange
            <61> <63> <0430>
            <9000> <9002> [<0041> <0042> <0043>]
            <A0FE> <A101> <00FF>
            endbfrange
            1 beginbfrange
            <B000> <BFFF> <4E00>
            endbfrange
            1 begincidrange
            <8000> <80FF> 100
            endcidrange
            1 begincidchar
            <41> 7
            endcidchar
            endcmap
        """.trimIndent()
        val cmap = CMap.parse(latin1(text), NameCache())
        assertEquals("Test", cmap.name)
        assertEquals(true, cmap.vertical)
        val input = byteArrayOf(0x41, 0x80.toByte(), 0x01, 0x62, 0x90.toByte(), 0x01, 0xB1.toByte(), 0x23, 0xA0.toByte(), 0xFF.toByte(), 0x80.toByte(), 0x02)
        val codes = ArrayList<Pair<Long, Int>>()
        var pos = 0
        while (pos < input.size) {
            val length = cmap.read(input, pos)
            codes.add(cmap.code to length)
            pos += length
        }
        assertEquals(listOf(0x41L to 1, 0x8001L to 2, 0x62L to 1, 0x9001L to 2, 0xB123L to 2, 0xA0FFL to 2, 0x8002L to 2), codes)
        assertEquals(listOf("B", "😀", "б", "B", "伣", "Ā", "ffi"), codes.map { cmap.unicode(it.first, it.second) })
        assertNull(cmap.unicode(0x41, 2))
        assertEquals(116, cmap.cid(0x8010, 2))
        assertEquals(7, cmap.cid(0x41, 1))
        assertEquals(-1, cmap.cid(0x42, 1))
        val derived = CMap.parse(latin1("/Identity-H usecmap 1 begincidchar <0005> 900 endcidchar"), NameCache())
        assertEquals(900, derived.cid(5, 2))
        assertEquals(0x1234, derived.cid(0x1234, 2))
        assertEquals(2, derived.read(byteArrayOf(0, 5), 0))
        val ucs2 = CMap.predefined("UniJIS-UCS2-H")!!
        assertEquals("あ", ucs2.unicode(0x3042, 2))
        assertNull(CMap.predefined("90ms-RKSJ-H"))
    }

    @Test
    fun glyphNames() {
        assertEquals("А", PdfEncodings.glyphToUnicode("afii10017"))
        assertEquals("я", PdfEncodings.glyphToUnicode("afii10097"))
        assertEquals("Ё", PdfEncodings.glyphToUnicode("afii10023"))
        assertEquals("ё", PdfEncodings.glyphToUnicode("afii10071"))
        assertEquals("Ґ", PdfEncodings.glyphToUnicode("afii10050"))
        assertEquals("№", PdfEncodings.glyphToUnicode("afii61352"))
        assertEquals("Ж", PdfEncodings.glyphToUnicode("Zhecyrillic"))
        assertEquals("ж", PdfEncodings.glyphToUnicode("zhecyrillic"))
        assertEquals("Ж", PdfEncodings.glyphToUnicode("uni0416"))
        assertEquals("ЖЗ", PdfEncodings.glyphToUnicode("uni04160417"))
        assertEquals("😀", PdfEncodings.glyphToUnicode("u1F600"))
        assertEquals("ffi", PdfEncodings.glyphToUnicode("f_f_i"))
        assertEquals("A", PdfEncodings.glyphToUnicode("A.sc"))
        assertEquals("€", PdfEncodings.glyphToUnicode("Euro"))
        assertEquals("Δ", PdfEncodings.glyphToUnicode("Delta"))
        assertEquals("ω", PdfEncodings.glyphToUnicode("omega"))
        assertEquals("ł", PdfEncodings.glyphToUnicode("lslash"))
        assertEquals("ž", PdfEncodings.glyphToUnicode("zcaron"))
        assertEquals("ÿ", PdfEncodings.glyphToUnicode("ydieresis"))
        assertEquals("—", PdfEncodings.glyphToUnicode("emdash"))
        assertEquals("ﬁ", PdfEncodings.glyphToUnicode("fi"))
        assertEquals("7", PdfEncodings.glyphToUnicode("seven"))
        assertNull(PdfEncodings.glyphToUnicode("g37"))
        assertNull(PdfEncodings.glyphToUnicode("uniD800"))
        assertNull(PdfEncodings.glyphToUnicode("ubead"))
        assertNull(PdfEncodings.glyphToUnicode(".notdef"))
    }
}
