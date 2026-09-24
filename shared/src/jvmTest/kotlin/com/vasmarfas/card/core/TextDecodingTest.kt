package com.vasmarfas.card.core

import java.nio.charset.Charset
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TextDecodingTest {
    private val allBytes = ByteArray(256) { it.toByte() }
    private val cp1251 = Charset.forName("windows-1251")
    private val koi8 = Charset.forName("KOI8-R")
    private val mixed = "Mixed текст with emoji \uD83D\uDE00, CJK 漢字, accents é and ё"

    private val sentences = listOf(
        "В чащах юга жил бы цитрус? Да, но фальшивый экземпляр!",
        "Широкая электрификация южных губерний даст мощный толчок подъёму сельского хозяйства.",
        "Съешь же ещё этих мягких французских булок, да выпей чаю.",
        "Глава первая. Утро выдалось холодным, и Анна долго не решалась выйти из дома.",
        "Москва, 1998 г. Всего 42 страницы, тираж 5000 экз.",
        "ГЛАВА II\nВ которой герой получает письмо и уезжает из города.",
    )

    @Test
    fun singleByteTablesMatchJdk() {
        val aliases = mapOf(
            "windows-1251" to listOf("windows-1251", "cp1251", "WINDOWS-1251", "win-1251", "x-cp1251"),
            "KOI8-R" to listOf("koi8-r", "KOI8R", "koi8"),
            "windows-1252" to listOf("windows-1252", "cp1252"),
            "ISO-8859-1" to listOf("iso-8859-1", "ISO8859_1", "latin1", "Latin-1", "l1"),
            "IBM866" to listOf("cp866", "IBM866"),
            "IBM437" to listOf("ibm437", "cp437", "IBM437"),
        )
        val sample = Random(1).nextBytes(10_000)
        for ((jdkName, names) in aliases) {
            val charset = Charset.forName(jdkName)
            for (name in names) {
                assertEquals(String(allBytes, charset), TextDecoding.decode(allBytes, name), name)
                assertEquals(String(sample, charset), TextDecoding.decode(sample, name), name)
            }
        }
    }

    @Test
    fun unicodeEncodingsMatchJdk() {
        assertEquals(mixed, TextDecoding.decode(mixed.toByteArray(Charsets.UTF_8), "utf-8"))
        assertEquals(mixed, TextDecoding.decode(mixed.toByteArray(Charsets.UTF_8), "UTF8"))
        assertEquals(mixed, TextDecoding.decode(mixed.toByteArray(Charsets.UTF_16LE), "utf-16le"))
        assertEquals(mixed, TextDecoding.decode(mixed.toByteArray(Charsets.UTF_16BE), "UTF-16BE"))
        assertEquals(mixed, TextDecoding.decode(mixed.toByteArray(Charsets.UTF_16), "utf-16"))
        assertEquals(mixed, TextDecoding.decode(byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + mixed.toByteArray(Charsets.UTF_16LE), "utf-16"))
        assertEquals(mixed, TextDecoding.decode(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + mixed.toByteArray(Charsets.UTF_8), "utf-8"))
        assertEquals("a\uFFFD", TextDecoding.decode(byteArrayOf(0x61, 0x00, 0x62), "utf-16le"))
        assertFailsWith<IllegalArgumentException> { TextDecoding.decode(allBytes, "gb2312") }
    }

    @Test
    fun detectsByteOrderMarks() {
        assertEquals(mixed, TextDecoding.decode(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + mixed.toByteArray(Charsets.UTF_8)))
        assertEquals(mixed, TextDecoding.decode(byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + mixed.toByteArray(Charsets.UTF_16LE)))
        assertEquals(mixed, TextDecoding.decode(byteArrayOf(0xFE.toByte(), 0xFF.toByte()) + mixed.toByteArray(Charsets.UTF_16BE)))
        val xml = "<?xml version=\"1.0\" encoding=\"UTF-16\"?><a>$mixed</a>"
        assertEquals(xml, TextDecoding.decode(xml.toByteArray(Charsets.UTF_16LE)))
        assertEquals(xml, TextDecoding.decode(xml.toByteArray(Charsets.UTF_16BE)))
        assertEquals("", TextDecoding.decode(ByteArray(0)))
        assertEquals("", TextDecoding.decode(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())))
    }

    @Test
    fun honoursXmlDeclaration() {
        val body = "<FictionBook><body><p>${sentences.joinToString(" ")}</p></body></FictionBook>"
        val windows = "<?xml version=\"1.0\" encoding=\"windows-1251\"?>\n$body"
        assertEquals(windows, TextDecoding.decode(windows.toByteArray(cp1251)))
        val koi = "<?xml version='1.0' encoding = 'KOI8-R' ?>\n$body"
        assertEquals(koi, TextDecoding.decode(koi.toByteArray(koi8)))
        val latin = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><p>café, naïve, Ærø</p>"
        assertEquals(latin, TextDecoding.decode(latin.toByteArray(Charsets.ISO_8859_1)))
        val utf8 = "<?xml version=\"1.0\" encoding=\"utf-8\"?>$body"
        assertEquals(utf8, TextDecoding.decode(utf8.encodeToByteArray()))
        val unknown = "<?xml version=\"1.0\" encoding=\"x-mac-cyrillic\"?>$body"
        assertEquals(unknown, TextDecoding.decode(unknown.encodeToByteArray()))
        val mislabelled = "<?xml version=\"1.0\" encoding=\"UTF-16\"?>$body"
        assertEquals(mislabelled, TextDecoding.decode(mislabelled.toByteArray(cp1251)))
    }

    @Test
    fun guessesCyrillicSingleByteEncoding() {
        for (sentence in sentences) {
            assertEquals(sentence, TextDecoding.decode(sentence.toByteArray(cp1251)), "windows-1251")
            assertEquals(sentence, TextDecoding.decode(sentence.toByteArray(koi8)), "KOI8-R")
            assertEquals(sentence, TextDecoding.decode(sentence.encodeToByteArray()), "UTF-8")
        }
        val book = Samples.get(SampleKind.RUSSIAN, 200_000).decodeToString().replace("\uFFFD", "")
        assertEquals(book, TextDecoding.decode(book.toByteArray(cp1251)))
        assertEquals(book, TextDecoding.decode(book.toByteArray(koi8)))
        val englishWithSign = "Copyright © 2004, all rights reserved."
        assertEquals(englishWithSign, TextDecoding.decode(englishWithSign.toByteArray(cp1251)))
    }

    @Test
    fun keepsTruncatedUtf8AsUtf8() {
        val bytes = "Привет, мир".encodeToByteArray()
        assertEquals("Привет, ми\uFFFD", TextDecoding.decode(bytes.copyOf(bytes.size - 1)))
        val emoji = "ok \uD83D\uDE00".encodeToByteArray()
        assertEquals("ok \uFFFD", TextDecoding.decode(emoji.copyOf(emoji.size - 2)))
    }
}
