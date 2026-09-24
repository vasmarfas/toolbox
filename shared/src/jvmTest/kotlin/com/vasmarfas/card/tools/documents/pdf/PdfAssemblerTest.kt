package com.vasmarfas.card.tools.documents.pdf

import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.apache.pdfbox.Loader
import org.apache.pdfbox.cos.COSDictionary
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.cos.COSObject
import org.apache.pdfbox.cos.COSStream
import org.apache.pdfbox.pdmodel.PDDestinationNameTreeNode
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary
import org.apache.pdfbox.pdmodel.PDFormContentStream
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.PDResources
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationPopup
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationText
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitDestination
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination
import org.apache.pdfbox.util.Matrix

class PdfAssemblerTest {
    private fun latinDocument(mode: SaveMode) = pdf(mode) { doc ->
        doc.textPage(helvetica(), listOf("Alpha one", "second line of alpha"))
        doc.textPage(times(), listOf("Beta two"), PDRectangle.LETTER, rotation = 90)
        doc.textPage(courier(), listOf("Gamma three"), PDRectangle(400f, 300f), rotation = 270).cropBox = PDRectangle(10f, 10f, 300f, 200f)
        doc.documentInformation.title = "Latin"
    }

    private fun cyrillicDocument(mode: SaveMode) = pdf(mode) { doc ->
        val arial = doc.arial()
        doc.textPage(arial, listOf("Первая страница", "ещё одна строка"))
        doc.textPage(arial, listOf("Вторая страница"), PDRectangle.A5)
    }

    private fun assertSameBox(expected: PDRectangle, actual: PDRectangle) {
        assertEquals(expected.lowerLeftX, actual.lowerLeftX, 1e-3f)
        assertEquals(expected.lowerLeftY, actual.lowerLeftY, 1e-3f)
        assertEquals(expected.upperRightX, actual.upperRightX, 1e-3f)
        assertEquals(expected.upperRightY, actual.upperRightY, 1e-3f)
    }

    private fun assertPagesMatch(output: ByteArray, sources: List<Pair<ByteArray, Int>>) {
        loadCleanly(output) { doc ->
            assertEquals(sources.size, doc.numberOfPages)
            for ((i, source) in sources.withIndex()) {
                Loader.loadPDF(source.first).use { original ->
                    val expected = original.getPage(source.second)
                    val actual = doc.getPage(i)
                    assertSameBox(expected.mediaBox, actual.mediaBox)
                    assertSameBox(expected.cropBox, actual.cropBox)
                    assertEquals(expected.rotation, actual.rotation)
                    assertEquals(pdfboxText(original, source.second), pdfboxText(doc, i))
                }
            }
        }
    }

    @Test
    fun mergeTwoDocuments() {
        for (mode in SaveMode.entries) {
            val latin = latinDocument(mode)
            val cyrillic = cyrillicDocument(SaveMode.COMPRESSED)
            val a = PdfDocument.parse(latin)
            val b = PdfDocument.parse(cyrillic)
            val output = PdfAssembler.assemble((0 until 3).map { PageRef(a, it) } + (0 until 2).map { PageRef(b, it) }, mapOf("Title" to "Сводный файл"))
            assertPagesMatch(output, listOf(latin to 0, latin to 1, latin to 2, cyrillic to 0, cyrillic to 1))
            loadCleanly(output) { doc ->
                assertEquals("Сводный файл", doc.documentInformation.title)
                assertEquals(PdfAssembler.PRODUCER, doc.documentInformation.producer)
            }
            val reread = PdfDocument.parse(output)
            assertEquals(5, reread.pageCount)
            assertEquals("Вторая страница", PdfText.extract(reread, 4))
        }
    }

    @Test
    fun extractReorderAndDuplicate() {
        val source = pdf(SaveMode.COMPRESSED) { doc ->
            val font = helvetica()
            repeat(5) { doc.textPage(font, listOf("Page number $it")) }
        }
        val doc = PdfDocument.parse(source)
        val order = listOf(4, 0, 2, 2)
        val output = PdfAssembler.assemble(order.map { PageRef(doc, it) }, mapOf("Producer" to "Custom producer"))
        assertPagesMatch(output, order.map { source to it })
        loadCleanly(output) { result ->
            assertEquals("Custom producer", result.documentInformation.producer)
            assertNotSame(result.getPage(2).cosObject, result.getPage(3).cosObject)
            assertEquals(fontNumbers(result, 2), fontNumbers(result, 3))
            assertEquals(fontNumbers(result, 0), fontNumbers(result, 1))
            val contents = { i: Int -> (result.getPage(i).cosObject.getItem(COSName.CONTENTS) as COSObject).key }
            assertEquals(contents(2), contents(3))
        }
        val single = PdfAssembler.assemble(listOf(PageRef(doc, 3)))
        assertPagesMatch(single, listOf(source to 3))
        assertFailsWith<IllegalArgumentException> { PdfAssembler.assemble(emptyList()) }
        assertFailsWith<IndexOutOfBoundsException> { PdfAssembler.assemble(listOf(PageRef(doc, 5))) }
    }

    private fun fontNumbers(doc: PDDocument, page: Int): Set<Long> {
        val fonts = doc.getPage(page).resources.cosObject.getCOSDictionary(COSName.FONT)
        return fonts.keySet().map { (fonts.getItem(it) as COSObject).key.number }.toSet()
    }

    @Test
    fun rotateOnTopOfExistingRotation() {
        val source = pdf { doc -> for (r in listOf(0, 90, 180, 270)) doc.textPage(helvetica(), listOf("Rotated $r"), rotation = r) }
        val doc = PdfDocument.parse(source)
        for (extra in listOf(90, -90, 180, 0, 450, -270)) {
            val output = PdfAssembler.assemble((0 until 4).map { PageRef(doc, it, extra) })
            loadCleanly(output) { result ->
                for (i in 0 until 4) {
                    assertEquals(((i * 90 + extra) % 360 + 360) % 360, result.getPage(i).rotation)
                    assertEquals(squeeze(pdfboxText(source, i)), squeeze(pdfboxText(result, i)))
                }
            }
            val reread = PdfDocument.parse(output)
            for (i in 0 until 4) assertEquals(PdfText.extract(doc, i), PdfText.extract(reread, i))
        }
        assertFailsWith<IllegalArgumentException> { PdfAssembler.assemble(listOf(PageRef(doc, 0, 45))) }
    }

    @Test
    fun sharedResourcesAreCopiedOnce() {
        val source = pdf(SaveMode.COMPRESSED) { doc ->
            val arial = doc.arial()
            val plain = helvetica()
            repeat(20) { i -> if (i % 2 == 0) doc.textPage(arial, listOf("Страница $i")) else doc.textPage(plain, listOf("Page $i")) }
        }
        val sourceObjects = Loader.loadPDF(source).use { it.document.xrefTable.size }
        val doc = PdfDocument.parse(source)
        val output = PdfAssembler.assemble((0 until 20).map { PageRef(doc, it) } + (19 downTo 0).map { PageRef(doc, it) })
        loadCleanly(output) { result ->
            val objects = result.document.xrefTable.keys.map { result.document.getObjectFromPool(it).getObject() }
            val fonts = objects.filterIsInstance<COSDictionary>().filter { it.getCOSName(COSName.TYPE) == COSName.FONT }
            assertEquals(3, fonts.size, "Type0, its CIDFont and Helvetica")
            assertEquals(1, objects.filterIsInstance<COSStream>().count { it.containsKey(COSName.LENGTH1) })
            assertTrue(objects.size <= sourceObjects + 20 + 3, "${objects.size} objects from $sourceObjects")
            for (i in 0 until 40) {
                val expected = if (i < 20) i else 39 - i
                assertEquals(if (expected % 2 == 0) "Страница $expected" else "Page $expected", pdfboxText(result, i).trim())
            }
        }
    }

    private fun linkedDocument(): ByteArray = pdf { doc ->
        repeat(4) { doc.textPage(helvetica(), listOf("Linked page $it")) }
        val pages = (0 until 4).map { doc.getPage(it) }
        fun link(y: Float) = PDAnnotationLink().apply { rectangle = PDRectangle(50f, y, 200f, 20f) }
        val toSecond = link(700f).apply { destination = PDPageFitDestination().apply { page = pages[1] } }
        val toThird = link(650f).apply {
            action = PDActionGoTo().apply { destination = PDPageXYZDestination().apply { page = pages[2]; top = 400 } }
        }
        val toFourth = link(600f).apply { destination = PDNamedDestination("chapter4") }
        val external = link(550f).apply { action = PDActionURI().apply { uri = "https://example.com/" } }
        val note = PDAnnotationText().apply {
            rectangle = PDRectangle(300f, 700f, 20f, 20f)
            contents = "Remember this"
        }
        val popup = PDAnnotationPopup().apply {
            rectangle = PDRectangle(320f, 600f, 150f, 100f)
            parent = note
        }
        note.popup = popup
        pages[0].annotations = listOf(toSecond, toThird, toFourth, external, note, popup)
        pages[3].annotations = listOf(link(700f).apply { destination = PDPageFitDestination().apply { page = pages[0] } })
        val tree = PDDestinationNameTreeNode()
        tree.names = mapOf("chapter4" to PDPageFitDestination().apply { page = pages[3] })
        val names = PDDocumentNameDictionary(doc.documentCatalog)
        names.dests = tree
        doc.documentCatalog.names = names
    }

    private fun links(doc: PDDocument, pageIndex: Int): List<Any> = doc.getPage(pageIndex).annotations.filterIsInstance<PDAnnotationLink>().map { link ->
        val destination = link.destination ?: (link.action as? PDActionGoTo)?.destination
        when {
            destination is PDPageDestination -> doc.pages.indexOf(destination.page)
            link.action is PDActionURI -> (link.action as PDActionURI).uri
            else -> "unresolved"
        }
    }

    @Test
    fun linkAnnotationsAreRemappedOrDropped() {
        val source = linkedDocument()
        val doc = PdfDocument.parse(source)
        val firstAndThird = PdfAssembler.assemble(listOf(PageRef(doc, 0), PageRef(doc, 2)))
        loadCleanly(firstAndThird) { result ->
            assertEquals(listOf<Any>(1, "https://example.com/"), links(result, 0))
            val annotations = result.getPage(0).annotations
            assertEquals(4, annotations.size)
            for (annotation in annotations) assertSame(result.getPage(0).cosObject, annotation.cosObject.getCOSDictionary(COSName.P))
            val note = annotations.filterIsInstance<PDAnnotationText>().single()
            val popup = annotations.filterIsInstance<PDAnnotationPopup>().single()
            assertSame(note.cosObject, popup.cosObject.getCOSDictionary(COSName.PARENT))
            assertSame(popup.cosObject, note.cosObject.getCOSDictionary(COSName.POPUP))
            assertEquals("Remember this", note.contents)
        }
        val reordered = PdfAssembler.assemble(listOf(PageRef(doc, 3), PageRef(doc, 0), PageRef(doc, 1)))
        loadCleanly(reordered) { result ->
            assertEquals(listOf<Any>(1), links(result, 0))
            assertEquals(listOf<Any>(2, 0, "https://example.com/"), links(result, 1))
            assertEquals(emptyList(), links(result, 2))
        }
        val duplicated = PdfAssembler.assemble(listOf(PageRef(doc, 0), PageRef(doc, 0), PageRef(doc, 1)))
        loadCleanly(duplicated) { result ->
            assertEquals(listOf<Any>(2, "https://example.com/"), links(result, 0))
            assertEquals(listOf<Any>(2, "https://example.com/"), links(result, 1))
            for (i in 0..1) {
                for (annotation in result.getPage(i).annotations) assertSame(result.getPage(i).cosObject, annotation.cosObject.getCOSDictionary(COSName.P))
            }
            assertNotSame(result.getPage(0).annotations[0].cosObject, result.getPage(1).annotations[0].cosObject)
        }
        assertPagesMatch(duplicated, listOf(source to 0, source to 0, source to 1))
    }

    @Test
    fun imagesAndFormsSurvive() {
        val source = pdf(SaveMode.COMPRESSED) { doc ->
            val picture = BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB).apply {
                val g = createGraphics()
                g.color = Color.RED
                g.fillRect(0, 0, 20, 30)
                g.dispose()
            }
            val lossless = LosslessFactory.createFromImage(doc, picture)
            val jpeg = JPEGFactory.createFromImage(doc, picture)
            val form = PDFormXObject(doc).apply {
                bBox = PDRectangle(0f, 0f, 200f, 50f)
                resources = PDResources()
            }
            PDFormContentStream(form).use {
                it.beginText()
                it.setFont(helvetica(), 10f)
                it.newLineAtOffset(5f, 20f)
                it.showText("Inside a form")
                it.endText()
            }
            repeat(2) { i ->
                val page = PDPage()
                doc.addPage(page)
                PDPageContentStream(doc, page).use { cs ->
                    cs.drawImage(lossless, 50f, 600f)
                    cs.drawImage(jpeg, 150f, 600f)
                    cs.saveGraphicsState()
                    cs.transform(Matrix.getTranslateInstance(100f, 300f))
                    cs.drawForm(form)
                    cs.restoreGraphicsState()
                    cs.beginText()
                    cs.setFont(times(), 14f)
                    cs.newLineAtOffset(50f, 500f)
                    cs.showText("Page with pictures $i")
                    cs.endText()
                }
            }
        }
        val doc = PdfDocument.parse(source)
        val output = PdfAssembler.assemble(listOf(PageRef(doc, 1), PageRef(doc, 0)))
        assertPagesMatch(output, listOf(source to 1, source to 0))
        val originalImages = Loader.loadPDF(source).use { original ->
            val resources = original.getPage(0).resources
            resources.xObjectNames.map { resources.getXObject(it) }
                .filterIsInstance<PDImageXObject>()
                .map { it.cosObject.createRawInputStream().readAllBytes() }
        }
        loadCleanly(output) { result ->
            val resources = result.getPage(0).resources
            val xobjects = resources.xObjectNames.map { resources.getXObject(it) }
            assertEquals(1, xobjects.count { it is PDFormXObject })
            val images = xobjects.filterIsInstance<PDImageXObject>().map { it.cosObject.createRawInputStream().readAllBytes() }
            assertEquals(2, images.size)
            for (i in images.indices) assertContentEquals(originalImages[i], images[i])
            val objects = result.document.xrefTable.keys.map { result.document.getObjectFromPool(it).getObject() }
            assertEquals(2, objects.filterIsInstance<COSStream>().count { it.getCOSName(COSName.SUBTYPE) == COSName.IMAGE })
            assertTrue("Inside a form" in pdfboxText(result, 0))
        }
    }

    @Test
    fun damagedSourcesAssembleIntoCleanFiles() {
        val pdf = RawPdf()
        pdf.obj(1, "<< /Type /Catalog /Pages 2 0 R >>")
        pdf.obj(2, "<< /Type /Pages /Kids [4 0 R 5 0 R] /Count 9 /MediaBox [0 0 500 500] /Rotate 90 /Resources << /Font << /F1 3 0 R >> >> >>")
        pdf.obj(3, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>")
        pdf.obj(4, "<< /Type /Page /Parent 2 0 R /Contents 6 0 R >>")
        pdf.obj(5, "<< /Type /Page /Parent 2 0 R /Contents 7 0 R /Rotate 0 >>")
        pdf.stream(6, "<< /Length 999 >>", latin1("BT /F1 20 Tf 50 400 Td (Damaged one) Tj ET"))
        pdf.stream(7, "<< /Length 3 >>", latin1("BT /F1 20 Tf 50 400 Td (Damaged two) Tj ET"))
        val damaged = pdf.finish("<< /Size 8 /Root 1 0 R >>", startxref = 77)
        val doc = PdfDocument.parse(damaged)
        assertTrue(doc.repaired)
        val output = PdfAssembler.assemble(listOf(PageRef(doc, 1), PageRef(doc, 0)))
        loadCleanly(output) { result ->
            assertEquals(2, result.numberOfPages)
            assertEquals("Damaged two", pdfboxText(result, 0).trim())
            assertEquals("Damagedone", squeeze(pdfboxText(result, 1)))
            assertEquals("Damaged one", PdfText.extract(PdfDocument.parse(output), 1))
            assertEquals(0, result.getPage(0).rotation)
            assertEquals(90, result.getPage(1).rotation)
            assertEquals(500f, result.getPage(1).mediaBox.width)
            assertTrue(result.getPage(1).cosObject.containsKey(COSName.RESOURCES))
        }
    }
}
