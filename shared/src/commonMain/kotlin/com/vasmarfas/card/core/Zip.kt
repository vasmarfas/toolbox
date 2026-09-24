package com.vasmarfas.card.core

import com.vasmarfas.card.tools.developer.Crc32

class ZipException(message: String) : Exception(message)

// DOS timestamp read as UTC, 2 s resolution
data class ZipEntryInfo(
    val name: String,
    val size: Long,
    val compressedSize: Long,
    val method: Int,
    val crc: Int,
    val isDirectory: Boolean,
    val modifiedEpochMillis: Long,
)

private const val LOCAL_HEADER = 0x04034b50
private const val CENTRAL_HEADER = 0x02014b50
private const val END_OF_DIRECTORY = 0x06054b50
private const val ZIP64_END_OF_DIRECTORY = 0x06064b50
private const val ZIP64_LOCATOR = 0x07064b50
private const val ZIP64_EXTRA = 0x0001
private const val UNICODE_PATH_EXTRA = 0x7075
private const val UTF8_FLAG = 0x800
private const val MAX_U16 = 0xFFFF
private const val MAX_U32 = 0xFFFFFFFFL
private const val MAX_ENTRY_SIZE = Int.MAX_VALUE - 8
private const val SECONDS_PER_DAY = 86_400L
private const val DOS_EPOCH_SECONDS = 315_532_800L

// names without the UTF-8 flag are CP437 unless an Info-ZIP Unicode path field with a matching CRC
// is there
class ZipArchive(private val bytes: ByteArray) {
    val entries: List<ZipEntryInfo>

    // equal central directory records still have to read from their own local headers
    private val offsets = ArrayList<Long>()
    private val byName = HashMap<String, Int>()

    init {
        entries = readCentralDirectory()
        entries.forEachIndexed { i, entry -> byName.getOrPut(entry.name) { i } }
    }

    fun read(name: String): ByteArray? = byName[name]?.let { read(entries[it], offsets[it]) }

    fun read(entry: ZipEntryInfo): ByteArray {
        val index = entries.indexOfFirst { it === entry }
        if (index < 0) throw ZipException("Entry ${entry.name} does not belong to this archive")
        return read(entry, offsets[index])
    }

    private fun read(entry: ZipEntryInfo, offset: Long): ByteArray {
        if (offset < 0 || offset > bytes.size - 30L || int32(offset.toInt()) != LOCAL_HEADER) throw ZipException("Invalid local header for ${entry.name}")
        val header = offset.toInt()
        if (uint16(header + 6) and 1 != 0) throw ZipException("Entry ${entry.name} is encrypted")
        val start = header + 30 + uint16(header + 26) + uint16(header + 28)
        if (entry.compressedSize < 0 || entry.compressedSize > bytes.size.toLong() - start) throw ZipException("Data of ${entry.name} is truncated")
        if (entry.size < 0 || entry.size > MAX_ENTRY_SIZE) throw ZipException("Entry ${entry.name} is too large")
        val length = entry.compressedSize.toInt()
        val size = entry.size.toInt()
        val data = when (entry.method) {
            0 -> bytes.copyOfRange(start, start + length)
            8 -> try {
                Inflate.inflateUpTo(bytes, start, length, size)
            } catch (e: DeflateException) {
                throw ZipException("Corrupt data in ${entry.name}: ${e.message}")
            }
            else -> throw ZipException("Unsupported compression method ${entry.method}${methodName(entry.method)} in ${entry.name}")
        }
        if (data.size != size) throw ZipException("Size mismatch in ${entry.name}: expected $size bytes, got ${data.size}")
        if (Crc32.compute(data) != entry.crc) throw ZipException("CRC mismatch in ${entry.name}")
        return data
    }

    private fun readCentralDirectory(): List<ZipEntryInfo> {
        val lowest = maxOf(0, bytes.size - 22 - MAX_U16)
        for (i in bytes.size - 22 downTo lowest) {
            if (bytes[i].toInt() == 0x50 && int32(i) == END_OF_DIRECTORY) return readDirectory(i) ?: continue
        }
        throw ZipException("Not a ZIP archive: end of central directory not found")
    }

    private fun readDirectory(end: Int): List<ZipEntryInfo>? {
        if (end + 22 + uint16(end + 20) > bytes.size) return null
        var count = uint16(end + 10).toLong()
        var size = uint32(end + 12)
        var offset = uint32(end + 16)
        var recordStart = end.toLong()
        if (count == MAX_U16.toLong() || size == MAX_U32 || offset == MAX_U32) {
            val zip64 = findZip64End(end)
            if (zip64 >= 0) {
                count = int64(zip64 + 32)
                size = int64(zip64 + 40)
                offset = int64(zip64 + 48)
                recordStart = zip64.toLong()
            }
        }
        if (size < 0 || offset < 0 || size > recordStart) return null
        var start = offset
        if (start + size > recordStart || (size > 0 && int32(start.toInt()) != CENTRAL_HEADER)) {
            start = recordStart - size
            if (size > 0 && int32(start.toInt()) != CENTRAL_HEADER) return null
        }
        return readEntries(start.toInt(), (start + size).toInt(), start - offset, count)
    }

    private fun findZip64End(end: Int): Int {
        val locator = end - 20
        if (locator < 0 || int32(locator) != ZIP64_LOCATOR) return -1
        val recorded = int64(locator + 8)
        if (recorded in 0..locator - 56L && int32(recorded.toInt()) == ZIP64_END_OF_DIRECTORY) return recorded.toInt()
        val adjacent = locator - 56
        return if (adjacent >= 0 && int32(adjacent) == ZIP64_END_OF_DIRECTORY) adjacent else -1
    }

    private fun readEntries(start: Int, end: Int, base: Long, count: Long): List<ZipEntryInfo> {
        val list = ArrayList<ZipEntryInfo>(count.coerceIn(0L, (end - start) / 46L).toInt())
        var p = start
        while (p < end) {
            if (p + 46 > end || int32(p) != CENTRAL_HEADER) throw ZipException("Corrupt central directory at offset $p")
            val flags = uint16(p + 8)
            var size = uint32(p + 24)
            var compressedSize = uint32(p + 20)
            var localOffset = uint32(p + 42)
            val nameStart = p + 46
            val extraStart = nameStart + uint16(p + 28)
            val extraLength = uint16(p + 30)
            val next = extraStart + extraLength + uint16(p + 32)
            if (next > end) throw ZipException("Corrupt central directory at offset $p")
            if (size == MAX_U32 || compressedSize == MAX_U32 || localOffset == MAX_U32) {
                val extra = findExtra(extraStart, extraLength, ZIP64_EXTRA)
                if (extra >= 0) {
                    var q = extra + 4
                    val limit = q + uint16(extra + 2)
                    if (size == MAX_U32 && q + 8 <= limit) {
                        size = int64(q)
                        q += 8
                    }
                    if (compressedSize == MAX_U32 && q + 8 <= limit) {
                        compressedSize = int64(q)
                        q += 8
                    }
                    if (localOffset == MAX_U32 && q + 8 <= limit) localOffset = int64(q)
                }
            }
            val name = entryName(flags, nameStart, extraStart, extraLength)
            val entry = ZipEntryInfo(
                name = name,
                size = size,
                compressedSize = compressedSize,
                method = uint16(p + 10),
                crc = int32(p + 16),
                isDirectory = name.endsWith('/'),
                modifiedEpochMillis = dosToEpochMillis(uint16(p + 14), uint16(p + 12)),
            )
            list.add(entry)
            offsets.add(localOffset + base)
            p = next
        }
        return list
    }

    private fun entryName(flags: Int, start: Int, end: Int, extraLength: Int): String {
        if (flags and UTF8_FLAG != 0) return bytes.decodeToString(start, end)
        val raw = bytes.copyOfRange(start, end)
        val unicode = findExtra(end, extraLength, UNICODE_PATH_EXTRA)
        if (unicode >= 0) {
            val length = uint16(unicode + 2)
            if (length >= 5 && bytes[unicode + 4].toInt() == 1 && int32(unicode + 5) == Crc32.compute(raw)) {
                return bytes.decodeToString(unicode + 9, unicode + 4 + length)
            }
        }
        return TextDecoding.decode(raw, "ibm437")
    }

    private fun findExtra(start: Int, length: Int, id: Int): Int {
        var p = start
        val end = start + length
        while (p + 4 <= end) {
            val size = uint16(p + 2)
            if (p + 4 + size > end) return -1
            if (uint16(p) == id) return p
            p += 4 + size
        }
        return -1
    }

    private fun uint16(i: Int): Int = (bytes[i].toInt() and 0xFF) or ((bytes[i + 1].toInt() and 0xFF) shl 8)

    private fun int32(i: Int): Int = uint16(i) or (uint16(i + 2) shl 16)

    private fun uint32(i: Int): Long = int32(i).toLong() and MAX_U32

    private fun int64(i: Int): Long = uint32(i) or (uint32(i + 4) shl 32)
}

class ZipWriter {
    private class Entry(
        val name: ByteArray,
        val flags: Int,
        val method: Int,
        val crc: Int,
        val size: Int,
        val data: ByteArray,
        val dosTime: Int,
        val directory: Boolean,
    )

    private val entries = ArrayList<Entry>()

    // insertion order is kept, EPUB and ODF need the stored mimetype first
    fun add(name: String, data: ByteArray, compress: Boolean = true, modifiedEpochMillis: Long = 0L): ZipWriter {
        val nameBytes = name.encodeToByteArray()
        if (nameBytes.size > MAX_U16) throw ZipException("Entry name is longer than $MAX_U16 bytes")
        val deflated = if (compress && data.isNotEmpty()) Deflate.deflate(data) else null
        val store = deflated == null || deflated.size >= data.size
        entries.add(
            Entry(
                name = nameBytes,
                flags = if (nameBytes.size != name.length) UTF8_FLAG else 0,
                method = if (store) 0 else 8,
                crc = Crc32.compute(data),
                size = data.size,
                data = if (store) data else deflated,
                dosTime = epochMillisToDos(modifiedEpochMillis),
                directory = name.endsWith('/'),
            ),
        )
        return this
    }

    fun toByteArray(): ByteArray {
        if (entries.size > MAX_U16) throw ZipException("More than $MAX_U16 entries need ZIP64")
        var total = 22L
        for (e in entries) total += 76L + 2 * e.name.size + e.data.size
        if (total > MAX_ENTRY_SIZE) throw ZipException("Archive of $total bytes does not fit into a byte array")
        val out = ByteArray(total.toInt())
        val offsets = IntArray(entries.size)
        var p = 0
        for ((i, e) in entries.withIndex()) {
            offsets[i] = p
            p = putInt(out, p, LOCAL_HEADER)
            p = putShort(out, p, if (e.method == 8) 20 else 10)
            p = putCommonFields(out, p, e)
            p = putShort(out, p, 0)
            e.name.copyInto(out, p)
            p += e.name.size
            e.data.copyInto(out, p)
            p += e.data.size
        }
        val directoryStart = p
        for ((i, e) in entries.withIndex()) {
            p = putInt(out, p, CENTRAL_HEADER)
            p = putShort(out, p, 20)
            p = putShort(out, p, if (e.method == 8) 20 else 10)
            p = putCommonFields(out, p, e)
            p = putInt(out, p, 0)
            p = putInt(out, p, 0)
            p = putInt(out, p, if (e.directory) 0x10 else 0)
            p = putInt(out, p, offsets[i])
            e.name.copyInto(out, p)
            p += e.name.size
        }
        val directorySize = p - directoryStart
        p = putInt(out, p, END_OF_DIRECTORY)
        p = putInt(out, p, 0)
        p = putShort(out, p, entries.size)
        p = putShort(out, p, entries.size)
        p = putInt(out, p, directorySize)
        p = putInt(out, p, directoryStart)
        putShort(out, p, 0)
        return out
    }

    private fun putCommonFields(out: ByteArray, at: Int, e: Entry): Int {
        var p = putShort(out, at, e.flags)
        p = putShort(out, p, e.method)
        p = putInt(out, p, e.dosTime)
        p = putInt(out, p, e.crc)
        p = putInt(out, p, e.data.size)
        p = putInt(out, p, e.size)
        return putShort(out, p, e.name.size)
    }

    private fun putShort(out: ByteArray, at: Int, value: Int): Int {
        out[at] = value.toByte()
        out[at + 1] = (value ushr 8).toByte()
        return at + 2
    }

    private fun putInt(out: ByteArray, at: Int, value: Int): Int {
        putShort(out, at, value)
        return putShort(out, at + 2, value ushr 16)
    }
}

private fun methodName(method: Int): String = when (method) {
    1 -> " (Shrink)"
    6 -> " (Implode)"
    9 -> " (Deflate64)"
    12 -> " (BZIP2)"
    14 -> " (LZMA)"
    93 -> " (Zstandard)"
    95 -> " (XZ)"
    98 -> " (PPMd)"
    99 -> " (AES)"
    else -> ""
}

private fun dosToEpochMillis(date: Int, time: Int): Long {
    val months = (1980 + (date ushr 9)) * 12 + ((date ushr 5) and 0xF) - 1
    val days = daysFromCivil(months / 12, months % 12 + 1, 1) + (date and 0x1F) - 1
    val seconds = (time ushr 11) * 3600 + ((time ushr 5) and 0x3F) * 60 + (time and 0x1F) * 2
    return (days * SECONDS_PER_DAY + seconds) * 1000
}

private fun epochMillisToDos(epochMillis: Long): Int {
    val seconds = maxOf(epochMillis.floorDiv(1000L), DOS_EPOCH_SECONDS)
    val days = seconds.floorDiv(SECONDS_PER_DAY)
    val secondOfDay = (seconds - days * SECONDS_PER_DAY).toInt()
    val z = days + 719_468
    val era = z.floorDiv(146_097L)
    val dayOfEra = (z - era * 146_097).toInt()
    val yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36_524 - dayOfEra / 146_096) / 365
    val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
    val mp = (5 * dayOfYear + 2) / 153
    val day = dayOfYear - (153 * mp + 2) / 5 + 1
    val month = if (mp < 10) mp + 3 else mp - 9
    val year = yearOfEra + era * 400 + if (month <= 2) 1 else 0
    if (year > 2107) return (((127 shl 9) or (12 shl 5) or 31) shl 16) or ((23 shl 11) or (59 shl 5) or 29)
    val date = (((year - 1980).toInt()) shl 9) or (month shl 5) or day
    val time = ((secondOfDay / 3600) shl 11) or (((secondOfDay / 60) % 60) shl 5) or ((secondOfDay % 60) / 2)
    return (date shl 16) or time
}

private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
    val y = if (month <= 2) year - 1 else year
    val era = (if (y >= 0) y else y - 399) / 400
    val yearOfEra = y - era * 400
    val dayOfYear = (153 * ((month + 9) % 12) + 2) / 5 + day - 1
    val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
    return era * 146_097L + dayOfEra - 719_468
}
