package com.vasmarfas.card.tools.documents

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImagePdfTest {
    private fun jpeg(width: Int, height: Int): PreparedImage {
        val out = ByteArrayOutputStream()
        ImageIO.write(BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "jpg", out)
        return PreparedImage(out.toByteArray(), width, height)
    }

    @Test
    fun picturesFillTheirPagesAndTurnThemLandscape() {
        val pdf = ImagePdf.build(listOf(jpeg(300, 400), jpeg(800, 400)), PageFormat.A4, margin = 28.35f, title = "Сканы")
        Loader.loadPDF(pdf).use { doc ->
            assertEquals(2, doc.numberOfPages)
            assertEquals("Сканы", doc.documentInformation.title)
            val portrait = doc.getPage(0).mediaBox
            val landscape = doc.getPage(1).mediaBox
            assertTrue(portrait.height > portrait.width && landscape.width > landscape.height)
            val image = doc.getPage(1).resources.xObjectNames.map { doc.getPage(1).resources.getXObject(it) }.single() as PDImageXObject
            assertEquals(800, image.width)
        }
    }

    @Test
    fun pageLikeThePictureIsThePictureAt150Dpi() {
        Loader.loadPDF(ImagePdf.build(listOf(jpeg(1500, 750)), null, margin = 0f)).use { doc ->
            val box = doc.getPage(0).mediaBox
            assertTrue(abs(box.width - 720f) < 0.5f && abs(box.height - 360f) < 0.5f, "${box.width} × ${box.height}")
        }
    }
}
