package com.vasmarfas.card.tools.documents.pdf

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger
import java.util.zip.Deflater
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdfwriter.compress.CompressParameters
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.apache.pdfbox.text.PDFTextStripper

enum class SaveMode { CLASSIC, COMPRESSED }

val arialFile = File("src/commonMain/composeResources/files/fonts/MobitoolSans-Regular.ttf")

fun helvetica() = PDType1Font(Standard14Fonts.FontName.HELVETICA)

fun helveticaBold() = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)

fun times() = PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN)

fun courier() = PDType1Font(Standard14Fonts.FontName.COURIER)

fun PDDocument.arial(): PDFont = PDType0Font.load(this, arialFile)

fun pdf(mode: SaveMode = SaveMode.CLASSIC, build: (PDDocument) -> Unit): ByteArray = PDDocument().use { doc ->
    build(doc)
    save(doc, mode)
}

fun save(doc: PDDocument, mode: SaveMode): ByteArray {
    val out = ByteArrayOutputStream()
    if (mode == SaveMode.CLASSIC) doc.save(out, CompressParameters.NO_COMPRESSION) else doc.save(out)
    return out.toByteArray()
}

fun PDDocument.textPage(
    font: PDFont,
    lines: List<String>,
    box: PDRectangle = PDRectangle.A4,
    rotation: Int = 0,
    fontSize: Float = 12f,
): PDPage {
    val page = PDPage(box)
    page.rotation = rotation
    addPage(page)
    PDPageContentStream(this, page).use { cs ->
        cs.beginText()
        cs.setFont(font, fontSize)
        cs.setLeading(fontSize * 1.5f)
        cs.newLineAtOffset(50f, box.height - 60f)
        for (line in lines) {
            cs.showText(line)
            cs.newLine()
        }
        cs.endText()
    }
    return page
}

fun normalize(text: String) = text.replace(Regex("[\\s\\u00A0]+"), " ").trim()

// PDFBox breaks lines of text not turned with a rotated page, so such pages are compared without
// whitespace
fun squeeze(text: String) = text.filterNot { it.isWhitespace() }

fun pdfboxText(bytes: ByteArray, pageIndex: Int, password: String = ""): String = Loader.loadPDF(bytes, password).use { pdfboxText(it, pageIndex) }

fun pdfboxText(doc: PDDocument, pageIndex: Int): String {
    val stripper = PDFTextStripper()
    stripper.startPage = pageIndex + 1
    stripper.endPage = pageIndex + 1
    return stripper.getText(doc)
}

object PdfBoxLog {
    private val logger: Logger = Logger.getLogger("org.apache.pdfbox")
    private val records = CopyOnWriteArrayList<LogRecord>()

    init {
        logger.level = Level.ALL
        logger.addHandler(
            object : Handler() {
                override fun publish(record: LogRecord) {
                    if (record.level.intValue() >= Level.WARNING.intValue()) records.add(record)
                }

                override fun flush() = Unit

                override fun close() = Unit
            },
        )
    }

    fun <T> capture(block: () -> T): Pair<T, List<String>> {
        records.clear()
        val result = block()
        return result to records.map { "${it.loggerName}: ${it.message}" }
    }
}

fun assertValidXref(bytes: ByteArray) {
    val text = String(bytes, Charsets.ISO_8859_1)
    val start = Regex("startxref\\s+(\\d+)\\s+%%EOF\\s*$").find(text) ?: fail("no startxref at the end")
    val offset = start.groupValues[1].toInt()
    val header = Regex("xref\\r?\\n(\\d+) (\\d+)\\r?\\n").find(text, offset)
    assertTrue(header != null && header.range.first == offset, "startxref $offset does not point at an xref table")
    assertEquals("0", header.groupValues[1])
    val count = header.groupValues[2].toInt()
    var p = header.range.last + 1
    for (i in 0 until count) {
        val entry = text.substring(p, p + 20)
        p += 20
        assertTrue(Regex("\\d{10} \\d{5} [nf](\r\n| \n| \r)").matches(entry), "entry $i: '$entry'")
        if (entry[17] == 'n') {
            val at = entry.substring(0, 10).toInt()
            assertTrue(Regex("$i \\d+ obj").find(text, at)?.range?.first == at, "object $i is not at $at")
        }
    }
    assertTrue(text.startsWith("trailer", p), "no trailer after $count entries")
    assertEquals(count.toString(), Regex("/Size (\\d+)").find(text, p)?.groupValues?.get(1))
}

fun <T> loadCleanly(bytes: ByteArray, block: (PDDocument) -> T): T {
    assertValidXref(bytes)
    val (result, warnings) = PdfBoxLog.capture {
        Loader.loadPDF(bytes).use { doc ->
            for (key in doc.document.xrefTable.keys) doc.document.getObjectFromPool(key).getObject()
            for (page in doc.pages) {
                page.mediaBox
                page.resources
            }
            block(doc)
        }
    }
    assertTrue(warnings.isEmpty(), "PDFBox warnings: $warnings")
    return result
}

fun deflate(data: ByteArray): ByteArray {
    val deflater = Deflater()
    deflater.setInput(data)
    deflater.finish()
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (!deflater.finished()) out.write(buffer, 0, deflater.deflate(buffer))
    return out.toByteArray()
}

fun latin1(text: String) = text.toByteArray(Charsets.ISO_8859_1)

class RawPdf(private val header: String = "%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n") {
    private val body = ByteArrayOutputStream()
    val offsets = sortedMapOf<Int, Int>()

    init {
        body.write(latin1(header))
    }

    val size: Int get() = body.size()

    fun obj(number: Int, text: String): RawPdf {
        offsets[number] = body.size()
        body.write(latin1("$number 0 obj\n$text\nendobj\n"))
        return this
    }

    fun stream(number: Int, dict: String, data: ByteArray): RawPdf {
        offsets[number] = body.size()
        body.write(latin1("$number 0 obj\n$dict\nstream\n"))
        body.write(data)
        body.write(latin1("\nendstream\nendobj\n"))
        return this
    }

    fun raw(bytes: ByteArray): RawPdf {
        body.write(bytes)
        return this
    }

    fun raw(text: String) = raw(latin1(text))

    fun xrefTable(eol: String = "\r\n", first: Int = 0, numbers: Collection<Int> = offsets.keys): String {
        val max = numbers.maxOrNull() ?: 0
        val sb = StringBuilder("xref\n$first ${max + 1}\n")
        sb.append("0000000000 65535 f").append(eol)
        for (n in 1..max) {
            val offset = offsets[n]
            if (offset != null && n in numbers) sb.append("%010d 00000 n".format(offset)).append(eol) else sb.append("0000000000 00000 f").append(eol)
        }
        return sb.toString()
    }

    fun finish(trailer: String, eol: String = "\r\n", first: Int = 0, startxref: Int? = null): ByteArray {
        val xrefAt = body.size()
        raw(xrefTable(eol, first))
        raw("trailer\n$trailer\nstartxref\n${startxref ?: xrefAt}\n%%EOF\n")
        return body.toByteArray()
    }

    fun bytes(): ByteArray = body.toByteArray()
}

fun objectStream(vararg objects: Pair<Int, String>): Pair<ByteArray, Int> {
    val header = StringBuilder()
    val body = StringBuilder()
    for ((number, text) in objects) {
        header.append(number).append(' ').append(body.length).append(' ')
        body.append(text).append('\n')
    }
    return latin1(header.toString() + body) to header.length
}

fun xrefStreamData(rows: List<IntArray>, widths: IntArray): ByteArray {
    val rowLength = widths.sum()
    val out = ByteArrayOutputStream()
    var previous = ByteArray(rowLength)
    for (row in rows) {
        val current = ByteArray(rowLength)
        var p = 0
        for ((i, width) in widths.withIndex()) {
            for (k in width - 1 downTo 0) current[p++] = (row[i] ushr (8 * k)).toByte()
        }
        out.write(2)
        for (i in 0 until rowLength) out.write((current[i] - previous[i]) and 0xFF)
        previous = current
    }
    return deflate(out.toByteArray())
}
