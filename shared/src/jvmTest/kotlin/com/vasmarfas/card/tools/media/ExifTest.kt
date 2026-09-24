package com.vasmarfas.card.tools.media

import org.apache.commons.imaging.common.RationalNumber
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants
import org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants
import java.awt.image.BufferedImage
import java.nio.ByteOrder
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class ExifTest {
    private fun ExifData.value(group: String, name: String): String? = tags.firstOrNull { it.group == group && it.name == name }?.value

    private fun assertCamera(exif: ExifData) {
        assertEquals("Google", exif.make)
        assertEquals("Pixel 8", exif.model)
        assertEquals("HDR+ 1.0", exif.software)
        assertEquals("Pixel 8 back camera 6.9mm f/1.68", exif.lens)
        assertEquals("2024:05:01 12:34:56", exif.dateTimeOriginal)
        assertEquals("1/250 s", exif.exposureTime)
        assertEquals("f/1.8", exif.fNumber)
        assertEquals(200, exif.iso)
        assertEquals("4.3 mm", exif.focalLength)
        assertEquals(4000, exif.pixelWidth)
        assertEquals(3000, exif.pixelHeight)
    }

    @Test
    fun cameraFieldsInBothByteOrders() {
        for (order in listOf(ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN)) {
            val exif = assertNotNull(Exif.read(exifJpeg(order = order)), "$order")
            assertCamera(exif)
            assertEquals("Fired, auto mode", exif.value("Exif", "Flash"))
            assertEquals("Multi-segment", exif.value("Exif", "MeteringMode"))
            assertEquals("1/250 s", exif.value("Exif", "ExposureTime"))
            assertEquals("Horizontal (normal)", exif.value("Image", "Orientation"))
            assertEquals("North", exif.value("GPS", "GPSLatitudeRef"))
            assertEquals("55° 45' 20.88\"", exif.value("GPS", "GPSLatitude"))
            assertEquals("156.5 m", exif.value("GPS", "GPSAltitude"))
            assertTrue(exif.tags.none { it.name.endsWith("IFDPointer") || it.id == 0x8769 || it.id == 0x8825 })
        }
    }

    @Test
    fun everyOrientation() {
        for (orientation in 1..8) {
            val jpeg = exifJpeg(orientation = orientation)
            assertEquals(orientation, Exif.orientation(jpeg))
            assertEquals(orientation, Exif.read(jpeg)!!.orientation)
        }
        assertEquals(1, Exif.orientation(baseJpeg))
        assertEquals(1, Exif.orientation(ascii("not an image")))
    }

    @Test
    fun gpsHemispheresAndAltitude() {
        val fixes = listOf(
            GpsFix(55.7558, 37.6173, 156.5),
            GpsFix(40.7128, -74.006, 10.0),
            GpsFix(-33.8688, 151.2093, null),
            GpsFix(-22.9068, -43.1729, -28.25),
        )
        for (fix in fixes) {
            val gps = assertNotNull(Exif.read(exifJpeg(gps = fix))!!.gps)
            assertEquals(fix.latitude, gps.latitude, 1e-5)
            assertEquals(fix.longitude, gps.longitude, 1e-5)
            if (fix.altitude == null) assertNull(gps.altitudeMeters) else assertEquals(fix.altitude, gps.altitudeMeters!!, 1e-3)
        }
        assertNull(Exif.read(exifJpeg(gps = null))!!.gps)
    }

    @Test
    fun readableValues() {
        val jpeg = exifJpeg { set ->
            val root = set.orCreateRootDirectory
            root.add(TiffTagConstants.TIFF_TAG_XRESOLUTION, RationalNumber(72, 1))
            root.add(TiffTagConstants.TIFF_TAG_RESOLUTION_UNIT, 2.toShort())
            val exif = set.orCreateExifDirectory
            exif.add(ExifTagConstants.EXIF_TAG_EXPOSURE_PROGRAM, 2.toShort())
            exif.add(ExifTagConstants.EXIF_TAG_WHITE_BALANCE_1, 0.toShort())
            exif.add(ExifTagConstants.EXIF_TAG_EXPOSURE_MODE, 1.toShort())
            exif.add(ExifTagConstants.EXIF_TAG_EXPOSURE_COMPENSATION, RationalNumber(-2, 3))
            exif.add(ExifTagConstants.EXIF_TAG_EXIF_VERSION, *ascii("0232"))
            exif.add(ExifTagConstants.EXIF_TAG_USER_COMMENT, "Summer trip")
            exif.add(ExifTagConstants.EXIF_TAG_LENS_SPECIFICATION, RationalNumber(24, 1), RationalNumber(70, 1), RationalNumber(28, 10), RationalNumber(28, 10))
            val gps = set.orCreateGpsDirectory
            gps.add(GpsTagConstants.GPS_TAG_GPS_TIME_STAMP, RationalNumber(9, 1), RationalNumber(4, 1), RationalNumber(5, 1))
            gps.add(GpsTagConstants.GPS_TAG_GPS_SPEED_REF, "K")
            gps.add(GpsTagConstants.GPS_TAG_GPS_IMG_DIRECTION, RationalNumber(12345, 100))
            gps.add(GpsTagConstants.GPS_TAG_GPS_DATE_STAMP, "2024:05:01")
        }
        val exif = Exif.read(jpeg)!!
        assertEquals("72", exif.value("Image", "XResolution"))
        assertEquals("inches", exif.value("Image", "ResolutionUnit"))
        assertEquals("Normal program", exif.value("Exif", "ExposureProgram"))
        assertEquals("Auto", exif.value("Exif", "WhiteBalance"))
        assertEquals("Manual", exif.value("Exif", "ExposureMode"))
        assertEquals("-0.67 EV", exif.value("Exif", "ExposureBiasValue"))
        assertEquals("2.32", exif.value("Exif", "ExifVersion"))
        assertEquals("Summer trip", exif.value("Exif", "UserComment"))
        assertEquals("24-70 mm f/2.8", exif.value("Exif", "LensSpecification"))
        assertEquals("09:04:05", exif.value("GPS", "GPSTimeStamp"))
        assertEquals("km/h", exif.value("GPS", "GPSSpeedRef"))
        assertEquals("123.45°", exif.value("GPS", "GPSImgDirection"))
        assertEquals("2024:05:01", exif.value("GPS", "GPSDateStamp"))
    }

    @Test
    fun pngWebpHeifAndTiffContainers() {
        val tiff = exifBlock(exifJpeg(orientation = 6))
        val png = Png.encode(photo(8, 8), 8, 8)
        val chunks = pngChunks(png).map { it.type to it.data }
        val withExif = pngOf(chunks.take(1) + listOf("eXIf" to tiff) + chunks.drop(1))
        val webpPlain = webp(vp8x(0x08), vp8lChunk, webpChunk("EXIF", tiff))
        val webpPrefixed = webp(vp8x(0x08), vp8lChunk, webpChunk("EXIF", ascii("Exif\u0000\u0000") + tiff))
        val files = listOf(
            "png" to withExif,
            "webp" to webpPlain,
            "webp with Exif header" to webpPrefixed,
            "heif" to heif(tiff, false),
            "heif idat" to heif(tiff, true),
        )
        for ((name, file) in files) {
            val exif = assertNotNull(Exif.read(file), name)
            assertCamera(exif)
            assertEquals(6, exif.orientation, name)
            assertEquals(6, Exif.orientation(file), name)
            assertEquals(55.7558, exif.gps!!.latitude, 1e-5, name)
        }
        assertEquals(ImageContainer.HEIF, ImageMetadata.detect(heif(tiff, false)))

        val imageIoTiff = encodeWith(image(photo(21, 13), 21, 13, BufferedImage.TYPE_INT_RGB), "tif")
        val fromTiff = assertNotNull(Exif.read(imageIoTiff))
        assertEquals(21, fromTiff.pixelWidth)
        assertEquals(13, fromTiff.pixelHeight)
        assertEquals("21", fromTiff.value("Image", "ImageWidth"))
    }

    @Test
    fun noExifMeansNull() {
        assertNull(Exif.read(baseJpeg))
        assertNull(Exif.read(Png.encode(photo(4, 4), 4, 4)))
        assertNull(Exif.read(webp(vp8lChunk)))
        assertNull(Exif.read(ByteArray(0)))
        assertNull(Exif.read(ascii("GIF89a")))
    }

    @Test
    fun truncatedSegmentsKeepWhatCanBeRead() {
        val jpeg = exifJpeg()
        val app1 = jpegSegments(jpeg).first { it.marker == 0xE1 }
        var partial = 0
        for (cut in 10 until app1.end - app1.start) {
            val length = cut - 2
            val shortened = jpeg.copyOf(app1.start + 2) + byteArrayOf((length shr 8).toByte(), length.toByte()) +
                jpeg.copyOfRange(app1.start + 4, app1.start + cut) + jpeg.copyOfRange(app1.end, jpeg.size)
            val exif = try {
                Exif.read(shortened)
            } catch (e: Throwable) {
                fail("cut at $cut: ${e::class.simpleName} ${e.message}")
            }
            if (exif != null && exif.make == "Google") partial++
            Exif.orientation(shortened)
        }
        assertTrue(partial > 0, "a cut after IFD0 should still give the make")
    }

    @Test
    fun corruptBlocksNeverThrow() {
        val tiff = exifBlock(exifJpeg(orientation = 3))
        val rnd = Random(5)
        val containers = listOf<(ByteArray) -> ByteArray>(
            { withSegments(baseJpeg, segment(0xE1, ascii("Exif\u0000\u0000") + it)) },
            { heif(it, rnd.nextBoolean()) },
            { webp(vp8x(0x08), vp8lChunk, webpChunk("EXIF", it)) },
        )
        repeat(4000) { round ->
            val broken = tiff.copyOf()
            repeat(1 + rnd.nextInt(6)) { broken[rnd.nextInt(broken.size)] = rnd.nextInt(256).toByte() }
            val file = containers[round % containers.size](if (round % 7 == 0) broken.copyOf(rnd.nextInt(broken.size)) else broken)
            try {
                Exif.read(file)?.tags?.forEach { it.value.length }
                Exif.orientation(file)
            } catch (e: Throwable) {
                fail("round $round: ${e::class.simpleName} ${e.message}")
            }
        }
    }

    @Test
    fun directoryLoopsTerminate() {
        val loop = TiffSink(bigEndian = false).run {
            bytes(ascii("II*\u0000"))
            u32(8)
            u16(2)
            u16(0x0112)
            u16(3)
            u32(1)
            u16(8)
            u16(0)
            u16(0x8769)
            u16(4)
            u32(1)
            u32(8)
            u32(8)
            toByteArray()
        }
        val exif = assertNotNull(Exif.read(withSegments(baseJpeg, segment(0xE1, ascii("Exif\u0000\u0000") + loop))))
        assertEquals(8, exif.orientation)
        assertEquals(1, exif.tags.size)
    }
}
