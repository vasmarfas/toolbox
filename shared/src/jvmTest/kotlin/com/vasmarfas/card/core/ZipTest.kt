package com.vasmarfas.card.core

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ZipTest {
    private val text = Samples.get(SampleKind.ENGLISH, 40_000)
    private val russian = Samples.get(SampleKind.RUSSIAN, 30_000)
    private val random = Samples.get(SampleKind.RANDOM, 5000)
    private val ibm437 = Charset.forName("IBM437")

    private fun jdkZip(charset: Charset = Charsets.UTF_8, comment: String? = null, build: ZipOutputStream.() -> Unit): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out, charset).use { zip ->
            if (comment != null) zip.setComment(comment)
            zip.build()
        }
        return out.toByteArray()
    }

    private fun ZipOutputStream.deflated(name: String, data: ByteArray, time: LocalDateTime? = null, extra: ByteArray? = null) {
        val entry = ZipEntry(name)
        if (time != null) entry.timeLocal = time
        if (extra != null) entry.extra = extra
        putNextEntry(entry)
        write(data)
        closeEntry()
    }

    private fun ZipOutputStream.stored(name: String, data: ByteArray) {
        val entry = ZipEntry(name)
        entry.method = ZipEntry.STORED
        entry.size = data.size.toLong()
        entry.compressedSize = data.size.toLong()
        entry.crc = crc(data).toLong() and 0xFFFFFFFFL
        putNextEntry(entry)
        write(data)
        closeEntry()
    }

    private fun crc(data: ByteArray): Int = CRC32().apply { update(data) }.value.toInt()

    private fun <T> withFile(bytes: ByteArray, block: (ZipFile) -> T): T {
        val dir = File("build/tmp/zip-test").apply { mkdirs() }
        val file = File.createTempFile("archive", ".zip", dir)
        try {
            file.writeBytes(bytes)
            return ZipFile(file).use(block)
        } finally {
            file.delete()
        }
    }

    private fun uint16(b: ByteArray, at: Int) = (b[at].toInt() and 0xFF) or ((b[at + 1].toInt() and 0xFF) shl 8)

    private fun localHeaderOffsets(b: ByteArray): List<Int> =
        (0..b.size - 4).filter { b[it] == 0x50.toByte() && b[it + 1] == 0x4B.toByte() && b[it + 2] == 3.toByte() && b[it + 3] == 4.toByte() }

    private fun centralHeaderOffsets(b: ByteArray): List<Int> =
        (0..b.size - 4).filter { b[it] == 0x50.toByte() && b[it + 1] == 0x4B.toByte() && b[it + 2] == 1.toByte() && b[it + 3] == 2.toByte() }

    @Test
    fun readsJdkArchiveWithDescriptorsCommentAndUtf8Names() {
        val time = LocalDateTime.of(2023, 7, 15, 13, 45, 30)
        val bytes = jdkZip(comment = "Архив с комментарием PK\u0005\u0006 внутри комментария, чтобы сбить поиск конца каталога") {
            deflated("readme.txt", text, time)
            stored("data.bin", random)
            deflated("empty.txt", ByteArray(0))
            putNextEntry(ZipEntry("dir/"))
            closeEntry()
            deflated("dir/Текст по-русски.txt", russian)
            deflated("emoji \uD83D\uDE00.txt", "smile".encodeToByteArray())
        }
        val archive = ZipArchive(bytes)
        assertEquals(
            listOf("readme.txt", "data.bin", "empty.txt", "dir/", "dir/Текст по-русски.txt", "emoji \uD83D\uDE00.txt"),
            archive.entries.map { it.name },
        )
        val readme = archive.entries[0]
        assertEquals(8, readme.method)
        assertEquals(text.size.toLong(), readme.size)
        assertEquals(crc(text), readme.crc)
        assertEquals(time.toEpochSecond(ZoneOffset.UTC) * 1000, readme.modifiedEpochMillis)
        assertTrue(uint16(bytes, 6) and 0x08 != 0, "JDK should have written a data descriptor")
        assertEquals(0, archive.entries[1].method)
        assertTrue(archive.entries[3].isDirectory)
        assertFalse(archive.entries[4].isDirectory)
        assertContentEquals(text, archive.read("readme.txt"))
        assertContentEquals(random, archive.read("data.bin"))
        assertContentEquals(ByteArray(0), archive.read("empty.txt"))
        assertContentEquals(ByteArray(0), archive.read(archive.entries[3]))
        assertContentEquals(russian, archive.read("dir/Текст по-русски.txt"))
        assertEquals("smile", archive.read("emoji \uD83D\uDE00.txt")?.decodeToString())
        assertNull(archive.read("missing.txt"))
        withFile(bytes) { zip ->
            for (entry in archive.entries) {
                val jdk = zip.getEntry(entry.name)
                assertEquals(jdk.size, entry.size, entry.name)
                assertEquals(jdk.compressedSize, entry.compressedSize, entry.name)
                assertEquals(jdk.crc.toInt(), entry.crc, entry.name)
                assertEquals(jdk.method, entry.method, entry.name)
            }
        }
    }

    @Test
    fun decodesLegacyNamesAsCp437() {
        val names = listOf("café/naïve résumé.txt", "Ñandú ½ ░▒▓.txt", "plain.txt")
        val bytes = jdkZip(ibm437) { for (name in names) deflated(name, name.encodeToByteArray()) }
        for (offset in localHeaderOffsets(bytes)) assertEquals(0, uint16(bytes, offset + 6) and 0x800)
        val archive = ZipArchive(bytes)
        assertEquals(names, archive.entries.map { it.name })
        for (name in names) assertEquals(name, archive.read(name)?.decodeToString())
    }

    @Test
    fun prefersUnicodePathExtraFieldWhenItsCrcMatches() {
        fun unicodePath(crcOf: String, name: String): ByteArray {
            val utf8 = name.encodeToByteArray()
            val size = 5 + utf8.size
            val crc = crc(crcOf.toByteArray(ibm437))
            val out = ByteArrayOutputStream()
            out.write(byteArrayOf(0x75, 0x70, size.toByte(), (size ushr 8).toByte(), 1))
            out.write(byteArrayOf(crc.toByte(), (crc ushr 8).toByte(), (crc ushr 16).toByte(), (crc ushr 24).toByte()))
            out.write(utf8)
            return out.toByteArray()
        }
        val bytes = jdkZip(ibm437) {
            deflated("Dokument.txt", text, extra = unicodePath("Dokument.txt", "Документ.txt"))
            deflated("Stale.txt", text, extra = unicodePath("Renamed.txt", "Устаревшее.txt"))
        }
        assertEquals(listOf("Документ.txt", "Stale.txt"), ZipArchive(bytes).entries.map { it.name })
    }

    @Test
    fun convertsDosTimestampsAsUtc() {
        val times = listOf(
            LocalDateTime.of(1980, 1, 1, 0, 0, 0),
            LocalDateTime.of(1999, 12, 31, 23, 59, 58),
            LocalDateTime.of(2024, 2, 29, 12, 30, 44),
            LocalDateTime.of(2107, 12, 31, 23, 59, 58),
        )
        val bytes = jdkZip { times.forEachIndexed { i, time -> deflated("f$i", ByteArray(i), time) } }
        val archive = ZipArchive(bytes)
        times.forEachIndexed { i, time -> assertEquals(time.toEpochSecond(ZoneOffset.UTC) * 1000, archive.entries[i].modifiedEpochMillis, "$time") }
    }

    @Test
    fun readsZip64EndOfCentralDirectory() {
        val count = 70_000
        val bytes = jdkZip {
            for (i in 0 until count) stored("f/$i", byteArrayOf(i.toByte()))
        }
        val eocd = bytes.size - 22
        assertEquals(0xFFFF, uint16(bytes, eocd + 10), "JDK should have fallen back to ZIP64")
        val archive = ZipArchive(bytes)
        assertEquals(count, archive.entries.size)
        for (i in intArrayOf(0, 1, 65_534, 65_535, 65_536, count - 1)) {
            assertEquals("f/$i", archive.entries[i].name)
            assertContentEquals(byteArrayOf(i.toByte()), archive.read(archive.entries[i]))
        }
    }

    @Test
    fun readsZip64ExtraFieldsInCentralDirectory() {
        val first = "first entry".encodeToByteArray()
        val second = Samples.get(SampleKind.SOURCE, 20_000)
        val secondDeflated = Deflate.deflate(second)
        val out = ByteArrayOutputStream()
        fun short(v: Int) = out.write(byteArrayOf(v.toByte(), (v ushr 8).toByte()))
        fun int(v: Int) = out.write(byteArrayOf(v.toByte(), (v ushr 8).toByte(), (v ushr 16).toByte(), (v ushr 24).toByte()))
        fun long(v: Long) {
            int(v.toInt())
            int((v ushr 32).toInt())
        }
        val entries = listOf(Triple("a.txt", first, first), Triple("b.kt", second, secondDeflated))
        val offsets = ArrayList<Int>()
        for ((name, data, payload) in entries) {
            offsets += out.size()
            int(0x04034b50)
            short(45)
            short(0)
            short(if (payload === data) 0 else 8)
            int(0x00210000)
            int(crc(data))
            int(-1)
            int(-1)
            short(name.length)
            short(20)
            out.write(name.encodeToByteArray())
            short(1)
            short(16)
            long(data.size.toLong())
            long(payload.size.toLong())
            out.write(payload)
        }
        val directoryStart = out.size()
        entries.forEachIndexed { i, (name, data, payload) ->
            int(0x02014b50)
            short(45)
            short(45)
            short(0)
            short(if (payload === data) 0 else 8)
            int(0x00210000)
            int(crc(data))
            int(-1)
            int(-1)
            short(name.length)
            short(28)
            short(0)
            short(0)
            short(0)
            int(0)
            int(-1)
            out.write(name.encodeToByteArray())
            short(1)
            short(24)
            long(data.size.toLong())
            long(payload.size.toLong())
            long(offsets[i].toLong())
        }
        val directorySize = out.size() - directoryStart
        int(0x06054b50)
        short(0)
        short(0)
        short(entries.size)
        short(entries.size)
        int(directorySize)
        int(directoryStart)
        short(0)
        val bytes = out.toByteArray()

        withFile(bytes) { zip -> assertContentEquals(second, zip.getInputStream(zip.getEntry("b.kt")).readBytes()) }
        val archive = ZipArchive(bytes)
        assertEquals(listOf(first.size.toLong(), second.size.toLong()), archive.entries.map { it.size })
        assertEquals(secondDeflated.size.toLong(), archive.entries[1].compressedSize)
        assertContentEquals(first, archive.read("a.txt"))
        assertContentEquals(second, archive.read("b.kt"))
        assertEquals(LocalDateTime.of(1980, 1, 1, 0, 0).toEpochSecond(ZoneOffset.UTC) * 1000, archive.entries[0].modifiedEpochMillis)
    }

    @Test
    fun readsArchiveWithPrependedStub() {
        val zip = jdkZip {
            deflated("a.txt", text)
            stored("b.bin", random)
        }
        val archive = ZipArchive("#!/bin/sh\nexec unzip \"$0\"\n".encodeToByteArray() + Random(3).nextBytes(1000) + zip)
        assertContentEquals(text, archive.read("a.txt"))
        assertContentEquals(random, archive.read("b.bin"))
    }

    @Test
    fun writerOutputIsReadByJdk() {
        val time = LocalDateTime.of(2022, 11, 3, 8, 15, 42)
        val millis = time.toEpochSecond(ZoneOffset.UTC) * 1000 + 1999
        val bytes = ZipWriter()
            .add("text.txt", text, modifiedEpochMillis = millis)
            .add("random.bin", random)
            .add("stored.txt", russian, compress = false)
            .add("empty.txt", ByteArray(0))
            .add("folder/", ByteArray(0))
            .add("folder/Файл с пробелами.txt", russian)
            .toByteArray()
        val expected = linkedMapOf(
            "text.txt" to text,
            "random.bin" to random,
            "stored.txt" to russian,
            "empty.txt" to ByteArray(0),
            "folder/" to ByteArray(0),
            "folder/Файл с пробелами.txt" to russian,
        )
        withFile(bytes) { zip ->
            assertEquals(expected.keys.toList(), zip.entries().toList().map { it.name })
            for ((name, data) in expected) {
                val entry = zip.getEntry(name)
                assertContentEquals(data, zip.getInputStream(entry).readBytes(), name)
                assertEquals(crc(data).toLong() and 0xFFFFFFFFL, entry.crc, name)
                assertEquals(data.size.toLong(), entry.size, name)
            }
            assertEquals(ZipEntry.DEFLATED, zip.getEntry("text.txt").method)
            assertEquals(ZipEntry.STORED, zip.getEntry("random.bin").method)
            assertEquals(ZipEntry.STORED, zip.getEntry("stored.txt").method)
            assertTrue(zip.getEntry("folder/").isDirectory)
            assertEquals(time, zip.getEntry("text.txt").timeLocal)
            assertEquals(LocalDateTime.of(1980, 1, 1, 0, 0), zip.getEntry("random.bin").timeLocal)
        }
        ZipInputStream(ByteArrayInputStream(bytes)).use { stream ->
            val seen = ArrayList<String>()
            while (true) {
                val entry = stream.nextEntry ?: break
                seen += entry.name
                assertContentEquals(expected.getValue(entry.name), stream.readBytes(), entry.name)
            }
            assertEquals(expected.keys.toList(), seen)
        }
        val archive = ZipArchive(bytes)
        for ((name, data) in expected) assertContentEquals(data, archive.read(name), name)
        assertEquals(time.toEpochSecond(ZoneOffset.UTC) * 1000, archive.entries[0].modifiedEpochMillis)
    }

    @Test
    fun writesEpubLayoutWithStoredMimetypeFirst() {
        val container = """<?xml version="1.0"?><container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
            |<rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles></container>""".trimMargin()
        val bytes = ZipWriter()
            .add("mimetype", "application/epub+zip".encodeToByteArray(), compress = false)
            .add("META-INF/container.xml", container.encodeToByteArray())
            .add("OEBPS/content.opf", text)
            .add("OEBPS/chapter1.xhtml", russian)
            .toByteArray()
        assertEquals("mimetype", bytes.decodeToString(30, 38))
        assertEquals("application/epub+zip", bytes.decodeToString(38, 58))
        assertEquals(0, uint16(bytes, 8), "mimetype must be stored")
        assertEquals(0, uint16(bytes, 28), "mimetype must have no extra field")
        withFile(bytes) { zip ->
            val names = zip.entries().toList().map { it.name }
            assertEquals(listOf("mimetype", "META-INF/container.xml", "OEBPS/content.opf", "OEBPS/chapter1.xhtml"), names)
            assertEquals(container, zip.getInputStream(zip.getEntry("META-INF/container.xml")).readBytes().decodeToString())
        }
    }

    @Test
    fun setsUtf8FlagOnlyForNonAsciiNames() {
        val bytes = ZipWriter().add("ascii.txt", text).add("кириллица.txt", text).toByteArray()
        val local = localHeaderOffsets(bytes)
        val central = centralHeaderOffsets(bytes)
        assertEquals(listOf(0, 0x800), local.map { uint16(bytes, it + 6) })
        assertEquals(listOf(0, 0x800), central.map { uint16(bytes, it + 8) })
    }

    @Test
    fun clampsTimestampsToDosRange() {
        val archive = ZipArchive(
            ZipWriter()
                .add("zero", ByteArray(0))
                .add("before", ByteArray(0), modifiedEpochMillis = -86_400_000L * 365)
                .add("after", ByteArray(0), modifiedEpochMillis = LocalDateTime.of(2200, 1, 1, 0, 0).toEpochSecond(ZoneOffset.UTC) * 1000)
                .add("odd", ByteArray(0), modifiedEpochMillis = LocalDateTime.of(2001, 9, 9, 1, 46, 41).toEpochSecond(ZoneOffset.UTC) * 1000)
                .toByteArray(),
        )
        val expected = listOf(
            LocalDateTime.of(1980, 1, 1, 0, 0, 0),
            LocalDateTime.of(1980, 1, 1, 0, 0, 0),
            LocalDateTime.of(2107, 12, 31, 23, 59, 58),
            LocalDateTime.of(2001, 9, 9, 1, 46, 40),
        )
        assertEquals(expected.map { it.toEpochSecond(ZoneOffset.UTC) * 1000 }, archive.entries.map { it.modifiedEpochMillis })
    }

    @Test
    fun detectsCrcMismatch() {
        val bytes = ZipWriter().add("a.bin", random).add("b.txt", text).toByteArray()
        val storedCopy = bytes.copyOf().also { it[30 + 5 + 100] = (it[30 + 5 + 100].toInt() xor 1).toByte() }
        val error = assertFailsWith<ZipException> { ZipArchive(storedCopy).read("a.bin") }
        assertTrue("CRC" in error.message.orEmpty(), error.message)
        val deflatedStart = localHeaderOffsets(bytes)[1] + 30 + 5
        for (delta in intArrayOf(10, 500, 3000)) {
            val corrupt = bytes.copyOf().also { it[deflatedStart + delta] = (it[deflatedStart + delta].toInt() xor 0x10).toByte() }
            assertFailsWith<ZipException> { ZipArchive(corrupt).read("b.txt") }
        }
    }

    @Test
    fun rejectsEntriesThatInflateToAnotherSize() {
        val bytes = ZipWriter().add("a.txt", text).toByteArray()
        val central = centralHeaderOffsets(bytes).single()
        fun declaring(size: Int) = bytes.copyOf().also { b -> for (k in 0 until 4) b[central + 24 + k] = (size ushr (8 * k)).toByte() }
        val shorter = assertFailsWith<ZipException> { ZipArchive(declaring(text.size - 1)).read("a.txt") }
        assertTrue("exceeds ${text.size - 1} bytes" in shorter.message.orEmpty(), shorter.message)
        val longer = assertFailsWith<ZipException> { ZipArchive(declaring(text.size + 1)).read("a.txt") }
        assertTrue("Size mismatch" in longer.message.orEmpty(), longer.message)
    }

    @Test
    fun rejectsUnsupportedMethodEncryptionAndGarbage() {
        val bytes = ZipWriter().add("a.txt", text).toByteArray()
        val central = centralHeaderOffsets(bytes).single()
        val bzip2 = bytes.copyOf().also {
            it[8] = 12
            it[central + 10] = 12
        }
        val error = assertFailsWith<ZipException> { ZipArchive(bzip2).read("a.txt") }
        assertTrue("12" in error.message.orEmpty() && "BZIP2" in error.message.orEmpty(), error.message)
        val encrypted = bytes.copyOf().also { it[6] = (it[6].toInt() or 1).toByte() }
        assertFailsWith<ZipException> { ZipArchive(encrypted).read("a.txt") }
        assertFailsWith<ZipException> { ZipArchive(Random(1).nextBytes(10_000)) }
        assertFailsWith<ZipException> { ZipArchive(ByteArray(0)) }
        assertFailsWith<ZipException> { ZipArchive(bytes.copyOf(bytes.size - 30)) }
        val otherEntry = ZipArchive(ZipWriter().add("other.txt", random).toByteArray()).entries.single()
        assertFailsWith<ZipException> { ZipArchive(bytes).read(otherEntry) }
    }

    @Test
    fun equalRecordsKeepTheirOwnData() {
        val data = "same".encodeToByteArray()
        val bytes = ZipWriter().add("a.txt", data, compress = false).add("a.txt", data, compress = false).toByteArray()
        bytes[localHeaderOffsets(bytes).last() + 30 + "a.txt".length] = 'S'.code.toByte()
        val archive = ZipArchive(bytes)
        assertEquals(archive.entries[0], archive.entries[1])
        assertContentEquals(data, archive.read(archive.entries[0]))
        assertContentEquals(data, archive.read("a.txt"))
        assertFailsWith<ZipException> { archive.read(archive.entries[1]) }
    }

    @Test
    fun handlesEmptyArchive() {
        val bytes = ZipWriter().toByteArray()
        assertEquals(22, bytes.size)
        assertEquals(emptyList(), ZipArchive(bytes).entries)
        assertEquals(emptyList(), ZipArchive(jdkZip {}).entries)
        withFile(bytes) { zip -> assertEquals(0, zip.size()) }
        ZipInputStream(ByteArrayInputStream(bytes)).use { assertNull(it.nextEntry) }
    }

    @Test
    fun roundTripsManyEntries() {
        val writer = ZipWriter()
        val expected = LinkedHashMap<String, ByteArray>()
        val rnd = Random(11)
        for (i in 0 until 300) {
            val kind = SampleKind.entries[i % SampleKind.entries.size]
            val data = Samples.get(kind, rnd.nextInt(0, 70_000))
            val name = "dir${i % 7}/file-$i-${kind.name.lowercase()}.dat"
            writer.add(name, data)
            expected[name] = data
        }
        val big = Samples.get(SampleKind.ENGLISH, 3 shl 20)
        writer.add("big.txt", big)
        expected["big.txt"] = big
        val archive = ZipArchive(writer.toByteArray())
        assertEquals(expected.keys.toList(), archive.entries.map { it.name })
        for ((name, data) in expected) assertContentEquals(data, archive.read(name), name)
    }
}
