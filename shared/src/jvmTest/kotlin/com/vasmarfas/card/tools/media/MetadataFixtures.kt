package com.vasmarfas.card.tools.media

import org.apache.commons.imaging.common.RationalNumber
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants
import org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import kotlin.math.abs

val baseJpeg: ByteArray by lazy { encodeWith(image(photo(64, 48), 64, 48, BufferedImage.TYPE_INT_RGB), "jpg") }

class GpsFix(val latitude: Double, val longitude: Double, val altitude: Double?)

fun exifJpeg(
    orientation: Int = 1,
    gps: GpsFix? = GpsFix(55.7558, 37.6173, 156.5),
    order: ByteOrder = ByteOrder.BIG_ENDIAN,
    extra: (TiffOutputSet) -> Unit = {},
): ByteArray {
    val set = TiffOutputSet(order)
    val root = set.orCreateRootDirectory
    root.add(TiffTagConstants.TIFF_TAG_MAKE, "Google")
    root.add(TiffTagConstants.TIFF_TAG_MODEL, "Pixel 8")
    root.add(TiffTagConstants.TIFF_TAG_SOFTWARE, "HDR+ 1.0")
    root.add(TiffTagConstants.TIFF_TAG_ORIENTATION, orientation.toShort())
    val exif = set.orCreateExifDirectory
    exif.add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, "2024:05:01 12:34:56")
    exif.add(ExifTagConstants.EXIF_TAG_EXPOSURE_TIME, RationalNumber(1, 250))
    exif.add(ExifTagConstants.EXIF_TAG_FNUMBER, RationalNumber(18, 10))
    exif.add(ExifTagConstants.EXIF_TAG_ISO, 200.toShort())
    exif.add(ExifTagConstants.EXIF_TAG_FOCAL_LENGTH, RationalNumber(43, 10))
    exif.add(ExifTagConstants.EXIF_TAG_LENS_MODEL, "Pixel 8 back camera 6.9mm f/1.68")
    exif.add(ExifTagConstants.EXIF_TAG_FLASH, 0x19.toShort())
    exif.add(ExifTagConstants.EXIF_TAG_METERING_MODE, 5.toShort())
    exif.add(ExifTagConstants.EXIF_TAG_EXIF_IMAGE_WIDTH, 4000.toShort())
    exif.add(ExifTagConstants.EXIF_TAG_EXIF_IMAGE_LENGTH, 3000.toShort())
    if (gps != null) {
        set.setGpsInDegrees(gps.longitude, gps.latitude)
        if (gps.altitude != null) {
            val dir = set.orCreateGpsDirectory
            dir.add(GpsTagConstants.GPS_TAG_GPS_ALTITUDE_REF, (if (gps.altitude < 0) 1 else 0).toByte())
            dir.add(GpsTagConstants.GPS_TAG_GPS_ALTITUDE, RationalNumber.valueOf(abs(gps.altitude)))
        }
    }
    extra(set)
    val out = ByteArrayOutputStream()
    ExifRewriter().updateExifMetadataLossless(baseJpeg, out, set)
    return out.toByteArray()
}

class JpegSegment(val marker: Int, val start: Int, val end: Int)

fun jpegSegments(jpeg: ByteArray): List<JpegSegment> {
    val list = ArrayList<JpegSegment>()
    var pos = 2
    while (pos + 4 <= jpeg.size) {
        val marker = jpeg[pos + 1].toInt() and 0xFF
        val end = pos + 2 + (((jpeg[pos + 2].toInt() and 0xFF) shl 8) or (jpeg[pos + 3].toInt() and 0xFF))
        list += JpegSegment(marker, pos, end)
        if (marker == 0xDA) break
        pos = end
    }
    return list
}

fun exifBlock(jpeg: ByteArray): ByteArray {
    val app1 = jpegSegments(jpeg).first { it.marker == 0xE1 && jpeg.copyOfRange(it.start + 4, it.start + 10).contentEquals(ascii("Exif\u0000\u0000")) }
    return jpeg.copyOfRange(app1.start + 10, app1.end)
}

fun segment(marker: Int, payload: ByteArray): ByteArray =
    byteArrayOf(-1, marker.toByte(), ((payload.size + 2) shr 8).toByte(), (payload.size + 2).toByte()) + payload

fun withSegments(jpeg: ByteArray, vararg segments: ByteArray): ByteArray {
    val first = jpegSegments(jpeg).first()
    val at = if (first.marker == 0xE0) first.end else 2
    return jpeg.copyOf(at) + segments.fold(ByteArray(0)) { a, s -> a + s } + jpeg.copyOfRange(at, jpeg.size)
}

fun scanData(jpeg: ByteArray): ByteArray {
    val sos = jpegSegments(jpeg).last()
    check(sos.marker == 0xDA)
    var end = jpeg.size
    while (end >= 2 && !(jpeg[end - 2] == (-1).toByte() && jpeg[end - 1] == 0xD9.toByte())) end--
    return jpeg.copyOfRange(sos.start, end)
}

fun le32(v: Int): ByteArray = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array()

fun be32(v: Int): ByteArray = ByteBuffer.allocate(4).putInt(v).array()

fun be16(v: Int): ByteArray = byteArrayOf((v shr 8).toByte(), v.toByte())

val vp8lChunk: ByteArray by lazy { Base64.getDecoder().decode("UklGRhoAAABXRUJQVlA4TA0AAAAvAAAAEAcQERGIiP4HAA==").copyOfRange(12, 12 + 8 + 14) }

fun webpChunk(fourcc: String, data: ByteArray): ByteArray = ascii(fourcc) + le32(data.size) + data + (if (data.size % 2 == 1) byteArrayOf(0) else ByteArray(0))

fun vp8x(flags: Int): ByteArray = webpChunk("VP8X", byteArrayOf(flags.toByte(), 0, 0, 0, 0, 0, 0, 0, 0, 0))

fun webp(vararg chunks: ByteArray): ByteArray {
    val body = ascii("WEBP") + chunks.fold(ByteArray(0)) { a, c -> a + c }
    return ascii("RIFF") + le32(body.size) + body
}

private fun box(type: String, vararg parts: ByteArray): ByteArray {
    val body = parts.fold(ByteArray(0)) { a, p -> a + p }
    return be32(8 + body.size) + ascii(type) + body
}

private fun fullBox(type: String, version: Int, vararg parts: ByteArray): ByteArray = box(type, byteArrayOf(version.toByte(), 0, 0, 0), *parts)

fun heif(tiff: ByteArray, inIdat: Boolean): ByteArray {
    val payload = be32(6) + ascii("Exif\u0000\u0000") + tiff
    val image = ByteArray(32) { 7 }
    val ftyp = box("ftyp", ascii("heic"), be32(0), ascii("mif1"), ascii("heic"))
    fun meta(imageOffset: Int, exifOffset: Int): ByteArray {
        val iinf = fullBox(
            "iinf",
            0,
            be16(2),
            fullBox("infe", 2, be16(1), be16(0), ascii("hvc1"), byteArrayOf(0)),
            fullBox("infe", 2, be16(2), be16(0), ascii("Exif"), byteArrayOf(0)),
        )
        val iloc = if (inIdat) {
            fullBox(
                "iloc",
                1,
                byteArrayOf(0x44, 0),
                be16(2),
                be16(1), be16(0), be16(0), be16(1), be32(imageOffset), be32(image.size),
                be16(2), be16(1), be16(0), be16(1), be32(0), be32(payload.size),
            )
        } else {
            fullBox(
                "iloc",
                0,
                byteArrayOf(0x44, 0),
                be16(2),
                be16(1), be16(0), be16(1), be32(imageOffset), be32(image.size),
                be16(2), be16(0), be16(1), be32(exifOffset), be32(payload.size),
            )
        }
        val parts = mutableListOf(fullBox("hdlr", 0, be32(0), ascii("pict"), ByteArray(12), byteArrayOf(0)), fullBox("pitm", 0, be16(1)), iinf, iloc)
        if (inIdat) parts += box("idat", payload)
        return fullBox("meta", 0, *parts.toTypedArray())
    }
    val metaSize = meta(0, 0).size
    val mdatData = ftyp.size + metaSize + 8
    val mdat = if (inIdat) box("mdat", image) else box("mdat", image, payload)
    return ftyp + meta(mdatData, mdatData + image.size) + mdat
}
