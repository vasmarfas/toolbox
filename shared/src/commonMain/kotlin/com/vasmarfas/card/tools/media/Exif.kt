package com.vasmarfas.card.tools.media

import com.vasmarfas.card.core.fmt
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

class ExifTag(val group: String, val id: Int, val name: String, val value: String)

class GpsPosition(val latitude: Double, val longitude: Double, val altitudeMeters: Double?)

class ExifData(
    val tags: List<ExifTag>,
    val orientation: Int,
    val gps: GpsPosition?,
    val make: String?,
    val model: String?,
    val lens: String?,
    val dateTimeOriginal: String?,
    val software: String?,
    val exposureTime: String?,
    val fNumber: String?,
    val iso: Int?,
    val focalLength: String?,
    val pixelWidth: Int?,
    val pixelHeight: Int?,
)

object Exif {
    internal const val ORIENTATION = 0x0112

    fun read(bytes: ByteArray): ExifData? = locate(bytes)?.let { ExifParser(it).parse() }

    fun orientation(bytes: ByteArray): Int = locate(bytes)?.let { orientationOf(it) } ?: 1

    private fun orientationOf(tiff: TiffReader): Int {
        val entry = tiff.ifd(tiff.firstIfd)?.entries?.firstOrNull { it.tag == ORIENTATION } ?: return 1
        val v = tiff.int(entry)
        return if (v in 1L..8L) v.toInt() else 1
    }

    private fun locate(bytes: ByteArray): TiffReader? = when (ImageMetadata.detect(bytes)) {
        ImageContainer.JPEG -> jpeg(bytes)
        ImageContainer.PNG -> png(bytes)
        ImageContainer.WEBP -> webp(bytes)
        ImageContainer.TIFF -> TiffReader.of(bytes, 0, bytes.size)
        ImageContainer.HEIF -> try {
            heif(bytes)
        } catch (e: ImageFormatException) {
            null
        }
        else -> null
    }

    private fun jpeg(bytes: ByteArray): TiffReader? {
        var pos = 2
        while (pos + 4 <= bytes.size) {
            if (bytes.u8(pos) != 0xFF) return null
            val marker = bytes.u8(pos + 1)
            if (marker == 0xFF) {
                pos++
                continue
            }
            if (marker == 0x01 || marker in 0xD0..0xD8) {
                pos += 2
                continue
            }
            if (marker == 0xDA || marker == 0xD9) return null
            val end = pos + 2 + bytes.u16be(pos + 2)
            if (marker == 0xE1 && bytes.matches(pos + 4, "Exif\u0000")) return TiffReader.of(bytes, pos + 10, min(end, bytes.size))
            pos = end
        }
        return null
    }

    private fun png(bytes: ByteArray): TiffReader? {
        var pos = 8
        while (pos + 12 <= bytes.size) {
            val length = bytes.u32be(pos)
            if (length > bytes.size - pos - 12) return null
            val data = pos + 8
            if (bytes.matches(pos + 4, "eXIf")) return TiffReader.of(bytes, data, data + length.toInt())
            if (bytes.matches(pos + 4, "IEND")) return null
            pos = data + length.toInt() + 4
        }
        return null
    }

    private fun webp(bytes: ByteArray): TiffReader? {
        var pos = 12
        while (pos + 8 <= bytes.size) {
            val size = bytes.u32le(pos + 4)
            if (size > bytes.size - pos - 8) return null
            val data = pos + 8
            if (bytes.matches(pos, "EXIF")) {
                val start = if (bytes.matches(data, "Exif\u0000\u0000")) data + 6 else data
                return TiffReader.of(bytes, start, data + size.toInt())
            }
            pos = data + size.toInt() + (size.toInt() and 1)
        }
        return null
    }

    private fun heif(bytes: ByteArray): TiffReader? {
        val meta = box(bytes, 0, bytes.size, "meta") ?: return null
        val iinf = box(bytes, meta.start + 4, meta.end, "iinf") ?: return null
        val iloc = box(bytes, meta.start + 4, meta.end, "iloc") ?: return null
        val item = exifItem(bytes, iinf) ?: return null
        val data = itemData(bytes, iloc, item, box(bytes, meta.start + 4, meta.end, "idat")) ?: return null
        if (data.end - data.start < 4) return null
        val start = data.start + 4 + bytes.u32be(data.start)
        if (start < data.end) TiffReader.of(bytes, start.toInt(), data.end)?.let { return it }
        for (p in data.start until min(data.end - 8, data.start + 64)) {
            if (bytes.matches(p, "Exif\u0000\u0000")) return TiffReader.of(bytes, p + 6, data.end)
        }
        return null
    }

    private class Span(val start: Int, val end: Int)

    private fun box(bytes: ByteArray, from: Int, to: Int, type: String): Span? {
        var pos = from
        while (pos + 8 <= to) {
            var size = bytes.u32be(pos)
            var header = 8
            if (size == 1L) {
                if (pos + 16 > to) return null
                size = (bytes.u32be(pos + 8) shl 32) or bytes.u32be(pos + 12)
                header = 16
            } else if (size == 0L) {
                size = (to - pos).toLong()
            }
            if (size < header || size > to - pos) return null
            if (bytes.matches(pos + 4, type)) return Span(pos + header, pos + size.toInt())
            pos += size.toInt()
        }
        return null
    }

    private fun exifItem(bytes: ByteArray, iinf: Span): Long? {
        val c = Cursor(bytes, iinf.start, iinf.end)
        val version = c.u8()
        c.skip(3)
        c.skip(if (version == 0) 2 else 4)
        while (c.pos + 8 <= iinf.end) {
            val start = c.pos
            val size = c.u32()
            if (size < 8 || size > iinf.end - start) return null
            if (bytes.matches(start + 4, "infe")) {
                c.skip(4)
                val v = c.u8()
                c.skip(3)
                if (v >= 2) {
                    val id = if (v == 2) c.u16().toLong() else c.u32()
                    c.skip(2)
                    if (c.pos + 4 <= start + size && bytes.matches(c.pos, "Exif")) return id
                }
            }
            c.pos = start + size.toInt()
        }
        return null
    }

    private fun itemData(bytes: ByteArray, iloc: Span, item: Long, idat: Span?): Span? {
        val c = Cursor(bytes, iloc.start, iloc.end)
        val version = c.u8()
        c.skip(3)
        val sizes = c.u16()
        val offsetSize = sizes shr 12
        val lengthSize = (sizes shr 8) and 15
        val baseSize = (sizes shr 4) and 15
        val indexSize = if (version == 1 || version == 2) sizes and 15 else 0
        val count = if (version < 2) c.u16().toLong() else c.u32()
        for (n in 0 until count) {
            val id = if (version < 2) c.u16().toLong() else c.u32()
            val method = if (version == 1 || version == 2) c.u16() and 15 else 0
            c.skip(2)
            val base = c.sized(baseSize)
            val extents = c.u16()
            var offset = -1L
            var length = 0L
            for (e in 0 until extents) {
                c.sized(indexSize)
                val o = c.sized(offsetSize)
                val l = c.sized(lengthSize)
                if (e == 0) {
                    offset = o
                    length = l
                }
            }
            if (id != item || offset < 0) continue
            val origin = when (method) {
                0 -> 0L
                1 -> idat?.start?.toLong() ?: return null
                else -> return null
            }
            val start = origin + base + offset
            val end = if (length == 0L) (idat?.end ?: bytes.size).toLong() else start + length
            if (start < 0 || end > bytes.size || start >= end) return null
            return Span(start.toInt(), end.toInt())
        }
        return null
    }

    private class Cursor(private val bytes: ByteArray, var pos: Int, private val end: Int) {
        private fun need(n: Int) {
            if (pos + n > end) throw ImageFormatException("HEIF box ends early")
        }

        fun skip(n: Int) {
            need(n)
            pos += n
        }

        fun u8(): Int {
            need(1)
            return bytes.u8(pos++)
        }

        fun u16(): Int {
            need(2)
            pos += 2
            return bytes.u16be(pos - 2)
        }

        fun u32(): Long {
            need(4)
            pos += 4
            return bytes.u32be(pos - 4)
        }

        fun sized(size: Int): Long = when (size) {
            0 -> 0L
            4 -> u32()
            8 -> (u32() shl 32) or u32()
            else -> throw ImageFormatException("HEIF field size $size is not valid")
        }
    }
}

private class ExifParser(private val tiff: TiffReader) {
    private val bytes = tiff.bytes
    private val tags = ArrayList<ExifTag>()
    private val seen = HashSet<Long>()

    fun parse(): ExifData? {
        val root = directory(tiff.firstIfd, IMAGE)
        val image = root?.entries.orEmpty().associateBy { it.tag }
        val exif = directory(pointer(image, EXIF_POINTER), EXIF)?.entries.orEmpty().associateBy { it.tag }
        val gps = directory(pointer(image, GPS_POINTER), GPS)?.entries.orEmpty().associateBy { it.tag }
        directory(pointer(exif, INTEROP_POINTER), INTEROP)
        if (root != null) directory(root.next, THUMBNAIL)
        if (tags.isEmpty()) return null
        return ExifData(
            tags = tags,
            orientation = image[Exif.ORIENTATION]?.let { tiff.int(it) }?.takeIf { it in 1L..8L }?.toInt() ?: 1,
            gps = position(gps),
            make = text(image[0x010F]),
            model = text(image[0x0110]),
            lens = text(exif[0xA434]),
            dateTimeOriginal = text(exif[0x9003]) ?: text(exif[0x9004]),
            software = text(image[0x0131]),
            exposureTime = exif[0x829A]?.let { exposure(it) } ?: exif[0x9201]?.let { apexSeconds(it) },
            fNumber = exif[0x829D]?.let { fNumber(it) } ?: exif[0x9202]?.let { apexAperture(it) },
            iso = (exif[0x8827] ?: exif[0x8833])?.let { tiff.int(it).toInt() }?.takeIf { it > 0 },
            focalLength = exif[0x920A]?.let { focal(it) },
            pixelWidth = (exif[0xA002] ?: image[0x0100])?.let { tiff.int(it).toInt() },
            pixelHeight = (exif[0xA003] ?: image[0x0101])?.let { tiff.int(it).toInt() },
        )
    }

    private fun pointer(dir: Map<Int, TiffEntry>, tag: Int): Long = dir[tag]?.let { tiff.int(it) } ?: 0L

    private fun directory(offset: Long, group: String): TiffIfd? {
        if (offset <= 0L || !seen.add(offset)) return null
        val ifd = tiff.ifd(offset) ?: return null
        val names = when (group) {
            GPS -> GPS_NAMES
            INTEROP -> INTEROP_NAMES
            else -> NAMES
        }
        for (e in ifd.entries) {
            if (e.tag == EXIF_POINTER || e.tag == GPS_POINTER || e.tag == INTEROP_POINTER) continue
            val name = names[e.tag] ?: "Tag 0x" + e.tag.toString(16).uppercase().padStart(4, '0')
            tags += ExifTag(group, e.tag, name, format(group, e))
        }
        return ifd
    }

    private fun text(e: TiffEntry?): String? = e?.takeIf { it.type == TiffReader.ASCII }?.let { tiff.text(it) }?.ifEmpty { null }

    private fun format(group: String, e: TiffEntry): String = when (group) {
        GPS -> gpsValue(e)
        INTEROP -> if (e.tag == 2) version(e) else plain(e)
        else -> imageValue(e)
    } ?: plain(e)

    private fun imageValue(e: TiffEntry): String? = when (e.tag) {
        0x829A -> exposure(e)
        0x829D -> fNumber(e)
        0x9201 -> apexSeconds(e)
        0x9202, 0x9205 -> apexAperture(e)
        0x9203 -> rational(e)?.let { it.fmt(2) + " EV" }
        0x9204 -> rational(e)?.let { (if (it > 0) "+" else "") + it.fmt(2) + " EV" }
        0x920A -> focal(e)
        0xA405 -> "${tiff.int(e)} mm"
        0x9206 -> subjectDistance(e)
        0xA404 -> rational(e)?.let { if (it == 0.0) "Not used" else it.fmt(2) }
        0x9000, 0xA000 -> version(e)
        0x9101 -> (0 until min(e.count, 4)).joinToString(", ") { COMPONENTS.getOrElse(tiff.int(e, it).toInt()) { "?" } }
        0x9286 -> encodedText(e)
        0x927C -> "(${e.count} bytes)"
        0xA432 -> lensSpecification(e)
        0x9209 -> flash(tiff.int(e).toInt())
        in 0x9C9B..0x9C9F -> utf16(e.offset, e.offset + e.count, littleEndian = true)
        else -> ENUMS[e.tag]?.get(tiff.int(e).toInt())
    }

    private fun gpsValue(e: TiffEntry): String? = when (e.tag) {
        0x00 -> (0 until e.count).joinToString(".") { tiff.int(e, it).toString() }
        0x01, 0x13 -> reference(e, "N" to "North", "S" to "South")
        0x03, 0x15 -> reference(e, "E" to "East", "W" to "West")
        0x02, 0x04, 0x14, 0x16 -> degrees(e)?.let { dms(it) }
        0x05 -> when (tiff.int(e)) {
            0L -> "Above sea level"
            1L -> "Below sea level"
            else -> null
        }
        0x06, 0x1F -> rational(e)?.let { it.fmt(1) + " m" }
        0x07 -> time(e)
        0x09 -> reference(e, "A" to "Measurement active", "V" to "Measurement void")
        0x0A -> reference(e, "2" to "2-dimensional", "3" to "3-dimensional")
        0x0C -> reference(e, "K" to "km/h", "M" to "mph", "N" to "knots")
        0x0E, 0x10, 0x17 -> reference(e, "T" to "True north", "M" to "Magnetic north")
        0x0F, 0x11, 0x18 -> rational(e)?.let { it.fmt(2) + "°" }
        0x19 -> reference(e, "K" to "km", "M" to "miles", "N" to "nautical miles")
        0x1B, 0x1C -> encodedText(e)
        0x1E -> when (tiff.int(e)) {
            0L -> "No correction"
            1L -> "Differential correction"
            else -> null
        }
        else -> null
    }

    private fun plain(e: TiffEntry): String {
        return when (e.type) {
            TiffReader.ASCII -> tiff.text(e)
            TiffReader.UNDEFINED -> if (e.count <= 64 && printable(e)) bytes.decodeToString(e.offset, e.offset + e.count).trim() else "(${e.count} bytes)"
            TiffReader.BYTE, TiffReader.SBYTE -> if (e.count <= 16) numbers(e) else "(${e.count} bytes)"
            else -> numbers(e)
        }
    }

    private fun numbers(e: TiffEntry): String {
        val shown = min(e.count, 16)
        val sb = StringBuilder()
        for (i in 0 until shown) {
            if (i > 0) sb.append(", ")
            when (e.type) {
                TiffReader.RATIONAL, TiffReader.SRATIONAL -> {
                    val d = tiff.denominator(e, i)
                    if (d == 0L) sb.append(tiff.numerator(e, i)).append("/0") else sb.append((tiff.numerator(e, i).toDouble() / d).fmt(4))
                }
                TiffReader.FLOAT, TiffReader.DOUBLE -> sb.append(tiff.double(e, i).fmt(4))
                else -> sb.append(tiff.int(e, i))
            }
        }
        if (e.count > shown) sb.append(", …")
        return sb.toString()
    }

    private fun printable(e: TiffEntry): Boolean {
        var end = e.offset + e.count
        while (end > e.offset && bytes[end - 1] == 0.toByte()) end--
        if (end == e.offset) return false
        for (i in e.offset until end) if (bytes.u8(i) !in 0x20..0x7E) return false
        return true
    }

    private fun rational(e: TiffEntry): Double? {
        if (e.type !in setOf(TiffReader.RATIONAL, TiffReader.SRATIONAL, TiffReader.FLOAT, TiffReader.DOUBLE)) return null
        return tiff.double(e).takeIf { !it.isNaN() }
    }

    private fun exposure(e: TiffEntry): String? {
        if (e.type != TiffReader.RATIONAL) return rational(e)?.takeIf { it > 0 }?.let(::seconds)
        val n = tiff.numerator(e, 0)
        val d = tiff.denominator(e, 0)
        if (d == 0L) return null
        return if (n == 1L && d > 1L) "1/$d s" else seconds(n.toDouble() / d)
    }

    private fun apexSeconds(e: TiffEntry): String? = rational(e)?.let { seconds(2.0.pow(-it)) }

    private fun apexAperture(e: TiffEntry): String? = rational(e)?.let { "f/" + 2.0.pow(it / 2).fmt(1) }

    private fun fNumber(e: TiffEntry): String? = rational(e)?.takeIf { it > 0 }?.let { "f/" + it.fmt(1) }

    private fun focal(e: TiffEntry): String? = rational(e)?.let { it.fmt(2) + " mm" }

    private fun seconds(s: Double): String = when {
        s <= 0.0 -> "0 s"
        s < 0.25001 -> "1/" + (1 / s).roundToLong() + " s"
        else -> s.fmt(1) + " s"
    }

    private fun subjectDistance(e: TiffEntry): String? {
        if (e.type != TiffReader.RATIONAL) return null
        return when {
            tiff.numerator(e, 0) == 0xFFFFFFFFL -> "Infinity"
            tiff.numerator(e, 0) == 0L -> "Unknown"
            else -> rational(e)?.let { it.fmt(2) + " m" }
        }
    }

    private fun version(e: TiffEntry): String? {
        if (e.count != 4) return null
        val s = CharArray(4) { tiff.int(e, it).toInt().toChar() }.concatToString()
        if (!s.all { it in '0'..'9' }) return null
        val minor = if (s[3] == '0') s.substring(2, 3) else s.substring(2, 4)
        return "${s.substring(0, 2).toInt()}.$minor"
    }

    private fun flash(v: Int): String {
        if (v and 0x20 != 0) return "No flash function"
        val parts = ArrayList<String>()
        parts += if (v and 1 != 0) "Fired" else "Did not fire"
        when ((v shr 3) and 3) {
            1 -> parts += "forced on"
            2 -> parts += "forced off"
            3 -> parts += "auto mode"
        }
        when ((v shr 1) and 3) {
            2 -> parts += "return not detected"
            3 -> parts += "return detected"
        }
        if (v and 0x40 != 0) parts += "red-eye reduction"
        return parts.joinToString(", ")
    }

    private fun lensSpecification(e: TiffEntry): String? {
        if (e.count < 4 || rational(e) == null) return null
        val v = DoubleArray(4) { tiff.double(e, it) }
        if (v[0].isNaN() || v[0] <= 0.0) return null
        val focal = if (v[1].isNaN() || v[1] == v[0]) v[0].fmt(1) else v[0].fmt(1) + "-" + v[1].fmt(1)
        val aperture = when {
            v[2].isNaN() || v[2] <= 0.0 -> return "$focal mm"
            v[3].isNaN() || v[3] <= 0.0 || v[3] == v[2] -> v[2].fmt(1)
            else -> v[2].fmt(1) + "-" + v[3].fmt(1)
        }
        return "$focal mm f/$aperture"
    }

    // UserComment and the GPS text fields start with an 8-byte character code
    private fun encodedText(e: TiffEntry): String {
        if (e.count <= 8) return ""
        val body = e.offset + 8
        val end = e.offset + e.count
        val text = if (bytes.matches(e.offset, "UNICODE\u0000")) utf16(body, end, tiff.littleEndian) else bytes.decodeToString(body, end)
        return text.trim { it == '\u0000' || it.isWhitespace() }
    }

    private fun utf16(from: Int, to: Int, littleEndian: Boolean): String {
        val chars = CharArray((to - from) / 2) {
            val p = from + it * 2
            (if (littleEndian) bytes.u16le(p) else bytes.u16be(p)).toChar()
        }
        return chars.concatToString().trim { it == '\u0000' || it.isWhitespace() }
    }

    private fun reference(e: TiffEntry, vararg names: Pair<String, String>): String? {
        val v = if (e.type == TiffReader.ASCII) tiff.text(e) else return null
        return names.firstOrNull { it.first.equals(v, ignoreCase = true) }?.second
    }

    private fun degrees(e: TiffEntry?): Double? {
        if (e == null || (e.type != TiffReader.RATIONAL && e.type != TiffReader.SRATIONAL)) return null
        var value = 0.0
        var scale = 1.0
        for (i in 0 until min(e.count, 3)) {
            val v = tiff.double(e, i)
            if (v.isNaN()) {
                if (i == 0) return null
            } else {
                value += v / scale
            }
            scale *= 60.0
        }
        return value
    }

    private fun dms(value: Double): String {
        val hundredths = (abs(value) * 360_000).roundToLong()
        val d = hundredths / 360_000
        val m = hundredths % 360_000 / 6_000
        val s = hundredths % 6_000 / 100.0
        return "$d° $m' ${s.fmt(2)}\""
    }

    private fun time(e: TiffEntry): String? {
        if (e.count < 3 || rational(e) == null) return null
        val parts = DoubleArray(3) { tiff.double(e, it) }
        if (parts.any { it.isNaN() }) return null
        val sec = parts[2]
        val secText = if (sec == sec.toLong().toDouble()) sec.toLong().toString().padStart(2, '0') else sec.fmt(2).padStart(5, '0')
        return parts[0].toLong().toString().padStart(2, '0') + ":" + parts[1].toLong().toString().padStart(2, '0') + ":" + secText
    }

    private fun position(gps: Map<Int, TiffEntry>): GpsPosition? {
        val lat = degrees(gps[0x02]) ?: return null
        val lon = degrees(gps[0x04]) ?: return null
        val latitude = if (gps[0x01]?.let { tiff.text(it) }.equals("S", ignoreCase = true)) -lat else lat
        val longitude = if (gps[0x03]?.let { tiff.text(it) }.equals("W", ignoreCase = true)) -lon else lon
        if (abs(latitude) > 90.0 || abs(longitude) > 180.0) return null
        val below = gps[0x05]?.let { tiff.int(it) } == 1L
        val altitude = gps[0x06]?.let { rational(it) }?.let { if (below) -it else it }
        return GpsPosition(latitude, longitude, altitude)
    }

    private companion object {
        const val IMAGE = "Image"
        const val EXIF = "Exif"
        const val GPS = "GPS"
        const val INTEROP = "Interop"
        const val THUMBNAIL = "Thumbnail"
        const val EXIF_POINTER = 0x8769
        const val GPS_POINTER = 0x8825
        const val INTEROP_POINTER = 0xA005

        val COMPONENTS = listOf("-", "Y", "Cb", "Cr", "R", "G", "B")

        val NAMES = mapOf(
            0x00FE to "NewSubfileType",
            0x0100 to "ImageWidth",
            0x0101 to "ImageLength",
            0x0102 to "BitsPerSample",
            0x0103 to "Compression",
            0x0106 to "PhotometricInterpretation",
            0x010D to "DocumentName",
            0x010E to "ImageDescription",
            0x010F to "Make",
            0x0110 to "Model",
            0x0111 to "StripOffsets",
            0x0112 to "Orientation",
            0x0115 to "SamplesPerPixel",
            0x0116 to "RowsPerStrip",
            0x0117 to "StripByteCounts",
            0x011A to "XResolution",
            0x011B to "YResolution",
            0x011C to "PlanarConfiguration",
            0x011D to "PageName",
            0x0128 to "ResolutionUnit",
            0x0129 to "PageNumber",
            0x012D to "TransferFunction",
            0x0131 to "Software",
            0x0132 to "DateTime",
            0x013B to "Artist",
            0x013C to "HostComputer",
            0x013D to "Predictor",
            0x013E to "WhitePoint",
            0x013F to "PrimaryChromaticities",
            0x0140 to "ColorMap",
            0x0142 to "TileWidth",
            0x0143 to "TileLength",
            0x0144 to "TileOffsets",
            0x0145 to "TileByteCounts",
            0x0152 to "ExtraSamples",
            0x0153 to "SampleFormat",
            0x0201 to "JPEGInterchangeFormat",
            0x0202 to "JPEGInterchangeFormatLength",
            0x0211 to "YCbCrCoefficients",
            0x0212 to "YCbCrSubSampling",
            0x0213 to "YCbCrPositioning",
            0x0214 to "ReferenceBlackWhite",
            0x02BC to "XMLPacket",
            0x4746 to "Rating",
            0x4749 to "RatingPercent",
            0x8298 to "Copyright",
            0x829A to "ExposureTime",
            0x829D to "FNumber",
            0x83BB to "IPTC-NAA",
            0x8773 to "InterColorProfile",
            0x8822 to "ExposureProgram",
            0x8824 to "SpectralSensitivity",
            0x8827 to "PhotographicSensitivity",
            0x8828 to "OECF",
            0x8830 to "SensitivityType",
            0x8831 to "StandardOutputSensitivity",
            0x8832 to "RecommendedExposureIndex",
            0x8833 to "ISOSpeed",
            0x9000 to "ExifVersion",
            0x9003 to "DateTimeOriginal",
            0x9004 to "DateTimeDigitized",
            0x9010 to "OffsetTime",
            0x9011 to "OffsetTimeOriginal",
            0x9012 to "OffsetTimeDigitized",
            0x9101 to "ComponentsConfiguration",
            0x9102 to "CompressedBitsPerPixel",
            0x9201 to "ShutterSpeedValue",
            0x9202 to "ApertureValue",
            0x9203 to "BrightnessValue",
            0x9204 to "ExposureBiasValue",
            0x9205 to "MaxApertureValue",
            0x9206 to "SubjectDistance",
            0x9207 to "MeteringMode",
            0x9208 to "LightSource",
            0x9209 to "Flash",
            0x920A to "FocalLength",
            0x9214 to "SubjectArea",
            0x927C to "MakerNote",
            0x9286 to "UserComment",
            0x9290 to "SubSecTime",
            0x9291 to "SubSecTimeOriginal",
            0x9292 to "SubSecTimeDigitized",
            0x9400 to "Temperature",
            0x9401 to "Humidity",
            0x9402 to "Pressure",
            0x9403 to "WaterDepth",
            0x9404 to "Acceleration",
            0x9405 to "CameraElevationAngle",
            0x9C9B to "XPTitle",
            0x9C9C to "XPComment",
            0x9C9D to "XPAuthor",
            0x9C9E to "XPKeywords",
            0x9C9F to "XPSubject",
            0xA000 to "FlashpixVersion",
            0xA001 to "ColorSpace",
            0xA002 to "PixelXDimension",
            0xA003 to "PixelYDimension",
            0xA004 to "RelatedSoundFile",
            0xA20B to "FlashEnergy",
            0xA20C to "SpatialFrequencyResponse",
            0xA20E to "FocalPlaneXResolution",
            0xA20F to "FocalPlaneYResolution",
            0xA210 to "FocalPlaneResolutionUnit",
            0xA214 to "SubjectLocation",
            0xA215 to "ExposureIndex",
            0xA217 to "SensingMethod",
            0xA300 to "FileSource",
            0xA301 to "SceneType",
            0xA302 to "CFAPattern",
            0xA401 to "CustomRendered",
            0xA402 to "ExposureMode",
            0xA403 to "WhiteBalance",
            0xA404 to "DigitalZoomRatio",
            0xA405 to "FocalLengthIn35mmFilm",
            0xA406 to "SceneCaptureType",
            0xA407 to "GainControl",
            0xA408 to "Contrast",
            0xA409 to "Saturation",
            0xA40A to "Sharpness",
            0xA40B to "DeviceSettingDescription",
            0xA40C to "SubjectDistanceRange",
            0xA420 to "ImageUniqueID",
            0xA430 to "CameraOwnerName",
            0xA431 to "BodySerialNumber",
            0xA432 to "LensSpecification",
            0xA433 to "LensMake",
            0xA434 to "LensModel",
            0xA435 to "LensSerialNumber",
            0xA460 to "CompositeImage",
            0xA461 to "SourceImageNumberOfCompositeImage",
            0xA462 to "SourceExposureTimesOfCompositeImage",
            0xA500 to "Gamma",
            0xC4A5 to "PrintImageMatching",
        )

        val GPS_NAMES = mapOf(
            0x00 to "GPSVersionID",
            0x01 to "GPSLatitudeRef",
            0x02 to "GPSLatitude",
            0x03 to "GPSLongitudeRef",
            0x04 to "GPSLongitude",
            0x05 to "GPSAltitudeRef",
            0x06 to "GPSAltitude",
            0x07 to "GPSTimeStamp",
            0x08 to "GPSSatellites",
            0x09 to "GPSStatus",
            0x0A to "GPSMeasureMode",
            0x0B to "GPSDOP",
            0x0C to "GPSSpeedRef",
            0x0D to "GPSSpeed",
            0x0E to "GPSTrackRef",
            0x0F to "GPSTrack",
            0x10 to "GPSImgDirectionRef",
            0x11 to "GPSImgDirection",
            0x12 to "GPSMapDatum",
            0x13 to "GPSDestLatitudeRef",
            0x14 to "GPSDestLatitude",
            0x15 to "GPSDestLongitudeRef",
            0x16 to "GPSDestLongitude",
            0x17 to "GPSDestBearingRef",
            0x18 to "GPSDestBearing",
            0x19 to "GPSDestDistanceRef",
            0x1A to "GPSDestDistance",
            0x1B to "GPSProcessingMethod",
            0x1C to "GPSAreaInformation",
            0x1D to "GPSDateStamp",
            0x1E to "GPSDifferential",
            0x1F to "GPSHPositioningError",
        )

        val INTEROP_NAMES = mapOf(
            0x0001 to "InteroperabilityIndex",
            0x0002 to "InteroperabilityVersion",
            0x1000 to "RelatedImageFileFormat",
            0x1001 to "RelatedImageWidth",
            0x1002 to "RelatedImageLength",
        )

        val RESOLUTION_UNITS = mapOf(1 to "None", 2 to "inches", 3 to "cm")

        val LIGHT_SOURCES = mapOf(
            0 to "Unknown",
            1 to "Daylight",
            2 to "Fluorescent",
            3 to "Tungsten",
            4 to "Flash",
            9 to "Fine weather",
            10 to "Cloudy",
            11 to "Shade",
            12 to "Daylight fluorescent",
            13 to "Day white fluorescent",
            14 to "Cool white fluorescent",
            15 to "White fluorescent",
            16 to "Warm white fluorescent",
            17 to "Standard light A",
            18 to "Standard light B",
            19 to "Standard light C",
            20 to "D55",
            21 to "D65",
            22 to "D75",
            23 to "D50",
            24 to "ISO studio tungsten",
            255 to "Other",
        )

        val ENUMS = mapOf(
            0x0103 to mapOf(1 to "Uncompressed", 5 to "LZW", 6 to "JPEG (old-style)", 7 to "JPEG", 8 to "Deflate", 32773 to "PackBits"),
            0x0106 to mapOf(0 to "WhiteIsZero", 1 to "BlackIsZero", 2 to "RGB", 3 to "Palette", 5 to "CMYK", 6 to "YCbCr"),
            0x0112 to mapOf(
                1 to "Horizontal (normal)",
                2 to "Mirror horizontal",
                3 to "Rotate 180",
                4 to "Mirror vertical",
                5 to "Mirror horizontal and rotate 270 CW",
                6 to "Rotate 90 CW",
                7 to "Mirror horizontal and rotate 90 CW",
                8 to "Rotate 270 CW",
            ),
            0x011C to mapOf(1 to "Chunky", 2 to "Planar"),
            0x0128 to RESOLUTION_UNITS,
            0xA210 to RESOLUTION_UNITS,
            0x0213 to mapOf(1 to "Centered", 2 to "Co-sited"),
            0x8822 to mapOf(
                0 to "Not defined",
                1 to "Manual",
                2 to "Normal program",
                3 to "Aperture priority",
                4 to "Shutter priority",
                5 to "Creative program",
                6 to "Action program",
                7 to "Portrait mode",
                8 to "Landscape mode",
            ),
            0x8830 to mapOf(
                0 to "Unknown",
                1 to "Standard output sensitivity",
                2 to "Recommended exposure index",
                3 to "ISO speed",
                4 to "SOS and REI",
                5 to "SOS and ISO speed",
                6 to "REI and ISO speed",
                7 to "SOS, REI and ISO speed",
            ),
            0x9207 to mapOf(
                0 to "Unknown",
                1 to "Average",
                2 to "Center-weighted average",
                3 to "Spot",
                4 to "Multi-spot",
                5 to "Multi-segment",
                6 to "Partial",
                255 to "Other",
            ),
            0x9208 to LIGHT_SOURCES,
            0xA001 to mapOf(1 to "sRGB", 2 to "Adobe RGB", 0xFFFF to "Uncalibrated"),
            0xA217 to mapOf(
                1 to "Not defined",
                2 to "One-chip color area",
                3 to "Two-chip color area",
                4 to "Three-chip color area",
                5 to "Color sequential area",
                7 to "Trilinear",
                8 to "Color sequential linear",
            ),
            0xA300 to mapOf(1 to "Film scanner", 2 to "Reflection print scanner", 3 to "Digital camera"),
            0xA301 to mapOf(1 to "Directly photographed"),
            0xA401 to mapOf(0 to "Normal", 1 to "Custom"),
            0xA402 to mapOf(0 to "Auto", 1 to "Manual", 2 to "Auto bracket"),
            0xA403 to mapOf(0 to "Auto", 1 to "Manual"),
            0xA406 to mapOf(0 to "Standard", 1 to "Landscape", 2 to "Portrait", 3 to "Night"),
            0xA407 to mapOf(0 to "None", 1 to "Low gain up", 2 to "High gain up", 3 to "Low gain down", 4 to "High gain down"),
            0xA408 to mapOf(0 to "Normal", 1 to "Low", 2 to "High"),
            0xA409 to mapOf(0 to "Normal", 1 to "Low", 2 to "High"),
            0xA40A to mapOf(0 to "Normal", 1 to "Soft", 2 to "Hard"),
            0xA40C to mapOf(0 to "Unknown", 1 to "Macro", 2 to "Close", 3 to "Distant"),
            0xA460 to mapOf(0 to "Unknown", 1 to "Not a composite", 2 to "General composite", 3 to "Composite captured while shooting"),
        )
    }
}
