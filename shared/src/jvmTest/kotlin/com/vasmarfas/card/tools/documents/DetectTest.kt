package com.vasmarfas.card.tools.documents

import com.vasmarfas.card.core.ZipWriter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DetectTest {
    @Test
    fun ownOutputIsRecognisedWhateverTheName() {
        val doc = Samples.rich()
        for (format in DocFormat.entries.filter { it.writable && it != DocFormat.TXT }) {
            assertEquals(format, Documents.detect(Documents.write(doc, format), "file.bin"), format.name)
        }
        assertEquals(DocFormat.TXT, Documents.detect(Documents.write(doc, DocFormat.TXT), "file.txt"))
    }

    @Test
    fun textFormats() {
        assertEquals(DocFormat.MARKDOWN, Documents.detect("# Заголовок\n\nтекст".encodeToByteArray(), "notes"))
        assertEquals(DocFormat.MARKDOWN, Documents.detect("see [link](https://x.y) here".encodeToByteArray(), ""))
        assertEquals(DocFormat.TXT, Documents.detect("# Заголовок\n\nтекст".encodeToByteArray(), "notes.txt"))
        assertEquals(DocFormat.MARKDOWN, Documents.detect("просто текст".encodeToByteArray(), "readme.md"))
        assertEquals(DocFormat.MARKDOWN, Documents.detect("<p align=\"center\"><img src=\"logo.png\"></p>\n\n# Project".encodeToByteArray(), "README.md"))
        assertEquals(DocFormat.TXT, Documents.detect("Просто текст без разметки.\nВторая строка.".encodeToByteArray(), "письмо"))
        assertEquals(DocFormat.TXT, Documents.detect(windows1251("Письмо в windows-1251"), "letter"))
        assertEquals(DocFormat.HTML, Documents.detect("<!DOCTYPE html><title>x</title>".encodeToByteArray(), "page.txt"))
        assertEquals(DocFormat.HTML, Documents.detect("<div><p>фрагмент</p><br></div>".encodeToByteArray(), "fragment"))
        assertEquals(DocFormat.HTML, Documents.detect("plain text".encodeToByteArray(), "page.htm"))
        assertEquals(DocFormat.FB2, Documents.detect(windows1251("<?xml version=\"1.0\" encoding=\"windows-1251\"?><FictionBook><body/></FictionBook>"), "x.xml"))
        assertEquals(DocFormat.RTF, Documents.detect("{\\rtf1\\ansi}".encodeToByteArray(), "x.doc"))
        assertEquals(DocFormat.TXT, Documents.detect(ByteArray(0), "empty.txt"))
    }

    @Test
    fun notDocuments() {
        assertNull(Documents.detect("<?xml version=\"1.0\"?><config><a/></config>".encodeToByteArray(), "config.xml"))
        assertNull(Documents.detect(TestImages.png(2, 2), "image.png"))
        assertNull(Documents.detect("%PDF-1.7\n%âãÏÓ".encodeToByteArray(), "doc.pdf"))
        assertNull(Documents.detect(byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(), 0xA1.toByte(), 0xB1.toByte()), "old.doc"))
        assertNull(Documents.detect(byteArrayOf(0, 1, 2, 3, 0, 0, 0, 7), "data"))
        assertNull(Documents.detect(ZipWriter().add("xl/workbook.xml", "<workbook/>".encodeToByteArray()).toByteArray(), "sheet.xlsx"))
        assertNull(Documents.detect(ZipWriter().add("mimetype", "application/vnd.oasis.opendocument.spreadsheet".encodeToByteArray(), compress = false).toByteArray(), "x.ods"))
        assertNull(Documents.detect("PK\u0003\u0004 broken".encodeToByteArray(), "broken.docx"))
        assertNull(Documents.detect(ByteArray(0), "empty.bin"))
    }

    @Test
    fun rtfCannotBeWritten() {
        assertFailsWith<IllegalArgumentException> { Documents.write(Doc(emptyList()), DocFormat.RTF) }
    }
}
