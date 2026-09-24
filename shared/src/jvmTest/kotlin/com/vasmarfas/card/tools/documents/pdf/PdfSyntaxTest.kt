package com.vasmarfas.card.tools.documents.pdf

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PdfSyntaxTest {
    private fun parse(text: String): PdfObject = PdfParser(latin1(text), NameCache()).parseObject()

    private fun write(obj: PdfObject): String = String(PdfSyntax.serialize(obj), Charsets.ISO_8859_1)

    @Test
    fun numbers() {
        assertEquals(PdfInt(42), parse("42"))
        assertEquals(PdfInt(-17), parse("-17"))
        assertEquals(PdfInt(9_007_199_254_740_993L), parse("9007199254740993"))
        assertEquals(PdfReal(0.5), parse(".5"))
        assertEquals(PdfReal(-12.25), parse("-12.25"))
        assertEquals(PdfReal(4.0), parse("4."))
        assertEquals(PdfReal(-0.002), parse("-.002"))
        assertEquals(PdfReal(0.0), parse("0.00-1"))
        assertEquals(PdfReal(-3.0), parse("--3.0"))
        val big = parse("123456789012345678901234") as PdfReal
        assertEquals(1.2345678901234568E23, big.value, 1e9)
    }

    @Test
    fun namesStringsAndKeywords() {
        assertEquals(PdfName("A B#"), parse("/A#20B#23"))
        assertEquals(PdfName("Name#zz"), parse("/Name#zz"))
        assertEquals(PdfName("é"), parse("/#E9"))
        assertEquals(PdfName(""), parse("/ "))
        val escaped = parse("(a(b)c\\\\d\\n\\r\\t\\b\\f\\123\\1x\\\ny)") as PdfString
        assertContentEquals(latin1("a(b)c\\d\n\r\t\u0008\u000C" + "S" + "\u0001" + "xy"), escaped.bytes)
        assertContentEquals(latin1("line\nnext\nlast"), (parse("(line\r\nnext\rlast)") as PdfString).bytes)
        val hex = parse("<48 65 6c6C 6>") as PdfString
        assertTrue(hex.hex)
        assertContentEquals(byteArrayOf(0x48, 0x65, 0x6C, 0x6C, 0x60), hex.bytes)
        assertEquals(PdfBoolean.TRUE, parse("true"))
        assertEquals(PdfNull, parse("null"))
    }

    @Test
    fun containersAndReferences() {
        val dict = parse("<</Type/Page/Kids[1 0 R 2 0 R 3]/Count 3/Nested<</A null/B(x)>>/Empty/>>") as PdfDict
        assertEquals("Page", dict.name("Type"))
        val kids = dict.array("Kids")!!
        assertEquals(listOf(PdfRef(1, 0), PdfRef(2, 0), PdfInt(3)), kids.items)
        assertEquals(3, dict.int("Count"))
        val nested = dict.dict("Nested")!!
        assertEquals(false, "A" in nested)
        assertEquals("x", nested.text("B"))
        assertEquals(PdfName(""), dict["Empty"])
        assertEquals(listOf("Type", "Kids", "Count", "Nested", "Empty"), dict.keys.toList())
    }

    @Test
    fun brokenContainersStillParse() {
        val dict = parse("<< /A 1 /B [1 2 /C (x) >> ") as PdfDict
        assertEquals(1, dict.int("A"))
        assertEquals(4, dict.array("B")!!.size)
        val garbage = parse("<< /A 1 junk /B 2 >>") as PdfDict
        assertEquals(2, garbage.int("B"))
        val deep = "[".repeat(150) + "]".repeat(150)
        val failure = runCatching { parse(deep) }.exceptionOrNull()
        assertIs<PdfException>(failure)
    }

    @Test
    fun realFormatting() {
        assertEquals("0.5", formatReal(0.5))
        assertEquals("-12.25", formatReal(-12.25))
        assertEquals("612", formatReal(612.0))
        assertEquals("0", formatReal(-0.0))
        assertEquals("0", formatReal(1e-12))
        assertEquals("0.0001", formatReal(1e-4))
        assertEquals("0.3", formatReal(0.1 + 0.2))
        assertEquals("123456789012.5", formatReal(123456789012.5))
        assertEquals("100000000000000000000", formatReal(1e20))
        assertEquals("595.276", formatReal(595.276))
        assertEquals("0.0000000001", formatReal(1e-10))
    }

    @Test
    fun objectSerialization() {
        assertEquals("/A#20B#23#28#2F", write(PdfName("A B#(/")))
        assertEquals("/#C3#A9", write(PdfName("Ã©")))
        assertEquals("/#D0#96", write(PdfName("Ж")))
        assertEquals("(a\\(b\\)\\\\\\n\\r\\001ÿ)", write(PdfString(latin1("a(b)\\\n\r\u0001ÿ"))))
        assertEquals("<00FF10>", write(PdfString(byteArrayOf(0, -1, 16), hex = true)))
        val dict = PdfDict(
            "Type" to PdfName("Page"),
            "Box" to PdfArray(PdfInt(0), PdfReal(0.5), PdfInt(-3), PdfRef(4, 0)),
            "N" to PdfInt(1),
            "Flag" to PdfBoolean.TRUE,
            "Sub" to PdfDict("X" to PdfNull),
        )
        assertEquals("<</Type/Page/Box[0 0.5 -3 4 0 R]/N 1/Flag true/Sub<</X null>>>>", write(dict))
        val roundTrip = parse(write(dict)) as PdfDict
        assertEquals(PdfArray(PdfInt(0), PdfReal(0.5), PdfInt(-3), PdfRef(4, 0)).items, roundTrip.array("Box")!!.items)
    }

    @Test
    fun textStrings() {
        assertEquals("Hello", PdfString(latin1("Hello")).text())
        assertEquals("•—€", PdfString(byteArrayOf(0x80.toByte(), 0x84.toByte(), 0xA0.toByte())).text())
        assertEquals("Привет", PdfString(byteArrayOf(-2, -1, 0x04, 0x1F, 0x04, 0x40, 0x04, 0x38, 0x04, 0x32, 0x04, 0x35, 0x04, 0x42)).text())
        assertEquals("Grüße", PdfString(byteArrayOf(-17, -69, -65) + "Grüße".encodeToByteArray()).text())
        assertEquals("😀", PdfString(byteArrayOf(-2, -1, 0xD8.toByte(), 0x3D, 0xDE.toByte(), 0x00)).text())
        assertContentEquals(latin1("Café \u0080"), PdfString.ofText("Café •").bytes)
        val cyrillic = PdfString.ofText("Отчёт")
        assertEquals("Отчёт", cyrillic.text())
        assertEquals(0xFE, cyrillic.bytes[0].toInt() and 0xFF)
    }

    @Test
    fun indirectObjectWithWrongLength() {
        val text = "7 0 obj\n<< /Length 999 >>\nstream\r\nABCDEF\nendstream\nendobj\n"
        val obj = PdfParser(latin1(text), NameCache()).readIndirect(0)!!
        assertEquals(7, obj.number)
        assertContentEquals(latin1("ABCDEF"), (obj.value as PdfStream).data)
        val short = "8 0 obj\n<< /Length 2 >>\nstream\nABCDEF\r\nendstream\nendobj\n"
        assertContentEquals(latin1("ABCDEF"), (PdfParser(latin1(short), NameCache()).readIndirect(0)!!.value as PdfStream).data)
        val tricky = "9 0 obj\n<< /Length 13 >>\nstream\nX\nendstream\nY\nendstream\nendobj\n"
        assertContentEquals(latin1("X\nendstream\nY"), (PdfParser(latin1(tricky), NameCache()).readIndirect(0)!!.value as PdfStream).data)
    }
}
