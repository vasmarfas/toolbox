package com.vasmarfas.card.tools.documents.pdf

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.apache.pdfbox.Loader
import org.apache.pdfbox.cos.COSArray
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.cos.COSString

class PdfWriterTest {
    private fun smallDocument(compress: Boolean): ByteArray {
        val writer = PdfWriter()
        val catalog = writer.reserve()
        val pages = writer.reserve()
        val font = writer.add(PdfDict("Type" to PdfName("Font"), "Subtype" to PdfName("Type1"), "BaseFont" to PdfName("Helvetica")))
        val content = writer.stream(PdfDict(), latin1("BT /F1 18 Tf 72 700 Td (Written by PdfWriter) Tj ET"), compress)
        writer.reserve()
        val page = writer.add(
            PdfDict(
                "Type" to PdfName("Page"),
                "Parent" to pages,
                "MediaBox" to PdfArray(PdfInt(0), PdfInt(0), PdfReal(595.276), PdfReal(841.89)),
                "Resources" to PdfDict("Font" to PdfDict("F1" to font)),
                "Contents" to content,
            ),
        )
        writer[pages] = PdfDict("Type" to PdfName("Pages"), "Kids" to PdfArray(page), "Count" to PdfInt(1))
        writer[catalog] = PdfDict("Type" to PdfName("Catalog"), "Pages" to pages)
        val info = writer.add(PdfDict("Title" to PdfString.ofText("Заголовок"), "Producer" to PdfString.ofText("Test")))
        return writer.toByteArray(catalog, info)
    }

    @Test
    fun pdfBoxReadsWriterOutput() {
        for (compress in listOf(true, false)) {
            val bytes = smallDocument(compress)
            val text = String(bytes, Charsets.ISO_8859_1)
            assertTrue(text.startsWith("%PDF-1.7\n%âãÏÓ\n"))
            assertEquals(compress, "/FlateDecode" in text)
            loadCleanly(bytes) { doc ->
                assertEquals(1, doc.numberOfPages)
                assertEquals(595.276f, doc.getPage(0).mediaBox.width, 1e-3f)
                assertEquals("Заголовок", doc.documentInformation.title)
                assertEquals("Written by PdfWriter", pdfboxText(doc, 0).trim())
                val id = doc.document.trailer.getDictionaryObject(COSName.ID) as COSArray
                val first = (id.getObject(0) as COSString).bytes
                assertEquals(16, first.size)
                assertContentEquals(first, (id.getObject(1) as COSString).bytes)
            }
            val ours = PdfDocument.parse(bytes)
            assertFalse(ours.repaired)
            assertEquals("Written by PdfWriter", PdfText.extract(ours, 0))
            assertEquals("Заголовок", ours.info["Title"])
        }
    }

    @Test
    fun crossReferenceEntriesAreTwentyBytes() {
        val bytes = smallDocument(compress = true)
        val text = String(bytes, Charsets.ISO_8859_1)
        val start = text.lastIndexOf("\nxref\n") + 1
        val header = Regex("xref\n0 (\\d+)\n").find(text, start)!!
        val count = header.groupValues[1].toInt()
        assertEquals(8, count)
        val entries = text.substring(header.range.last + 1, header.range.last + 1 + count * 20)
        for (i in 0 until count) {
            val entry = entries.substring(i * 20, i * 20 + 20)
            assertTrue(Regex("\\d{10} \\d{5} [nf]\r\n").matches(entry), "entry $i: $entry")
            if (i > 0) {
                val offset = entry.substring(0, 10).toInt()
                assertTrue(text.startsWith("$i 0 obj", offset), "object $i at $offset")
            }
        }
        assertTrue(text.substring(header.range.last + 1 + count * 20).startsWith("trailer\n<</Size 8/Root 1 0 R/Info 7 0 R/ID[<"))
        assertTrue(text.endsWith("startxref\n$start\n%%EOF\n"))
    }

    @Test
    fun checksCatchDamagedFiles() {
        val pdf = RawPdf()
        pdf.obj(1, "<< /Type /Catalog /Pages 2 0 R >>")
        pdf.obj(2, "<< /Type /Pages /Kids [3 0 R] /Count 1 >>")
        pdf.obj(3, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 100 100] /Contents 4 0 R >>")
        pdf.stream(4, "<< /Length 999 >>", latin1("0 0 m"))
        val badLength = pdf.finish("<< /Size 5 /Root 1 0 R >>")
        assertFails { loadCleanly(badLength) { } }
        val (_, warnings) = PdfBoxLog.capture { Loader.loadPDF(badLength).use { it.getPage(0).contentStreams.next().createInputStream().readAllBytes() } }
        assertTrue(warnings.isNotEmpty())
        val empty = { RawPdf().obj(1, "<< /Type /Catalog /Pages 2 0 R >>").obj(2, "<< /Type /Pages /Kids [] /Count 0 >>") }
        assertValidXref(empty().finish("<< /Size 3 /Root 1 0 R >>"))
        assertFails { assertValidXref(empty().finish("<< /Size 3 /Root 1 0 R >>", startxref = 40)) }
        assertFails { assertValidXref(empty().finish("<< /Size 3 /Root 1 0 R >>", eol = "\n")) }
        assertFails { assertValidXref(empty().finish("<< /Size 3 /Root 1 0 R >>", first = 1)) }
    }

    @Test
    fun identifierDependsOnContent() {
        fun id(title: String): String {
            val writer = PdfWriter()
            val root = writer.add(PdfDict("Type" to PdfName("Catalog"), "T" to PdfString.ofText(title)))
            val text = String(writer.toByteArray(root), Charsets.ISO_8859_1)
            return Regex("/ID\\[<([0-9A-F]+)>").find(text)!!.groupValues[1]
        }
        assertEquals(id("a"), id("a"))
        assertTrue(id("a") != id("b"))
    }

    @Test
    fun streamHelperKeepsExistingFilters() {
        val writer = PdfWriter()
        val hex = latin1("48656C6C6F>")
        val ref = writer.stream(PdfDict("Filter" to PdfName("ASCIIHexDecode")), hex)
        val pages = writer.add(PdfDict("Type" to PdfName("Pages"), "Kids" to PdfArray(), "Count" to PdfInt(0)))
        val root = writer.add(PdfDict("Type" to PdfName("Catalog"), "Pages" to pages, "S" to ref))
        val doc = PdfDocument.parse(writer.toByteArray(root))
        val stream = doc.catalog.stream("S", doc)!!
        assertEquals("[/FlateDecode /ASCIIHexDecode]", stream.dict["Filter"].toString())
        assertContentEquals(latin1("Hello"), doc.decodedStream(stream))
    }
}
