package com.vasmarfas.card.tools.text

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MojibakeTest {
    private fun garble(text: String, actual: Codepage, shownAs: Codepage) = shownAs.decode(actual.encode(text)!!)

    @Test
    fun singleBytePagesRoundTrip() {
        val all = ByteArray(256) { it.toByte() }
        for (page in Codepage.entries - Codepage.UTF8) {
            assertContentEquals(all, page.encode(page.decode(all)), page.label)
        }
        assertEquals("Привет", Codepage.CP1251.decode(byteArrayOf(0xCF.toByte(), 0xF0.toByte(), 0xE8.toByte(), 0xE2.toByte(), 0xE5.toByte(), 0xF2.toByte())))
        assertNull(Codepage.CP1252.encode("Привет"))
    }

    @Test
    fun undoesCommonMisreadings() {
        val text = "Привет, мир! Это проверка текста."
        val cases = listOf(
            Codepage.UTF8 to Codepage.CP1251,
            Codepage.UTF8 to Codepage.CP1252,
            Codepage.UTF8 to Codepage.CP866,
            Codepage.CP1251 to Codepage.CP1252,
            Codepage.CP1251 to Codepage.KOI8R,
            Codepage.KOI8R to Codepage.CP1251,
            Codepage.CP866 to Codepage.CP1251,
            Codepage.CP1251 to Codepage.CP866,
        )
        for ((actual, shown) in cases) {
            val fix = Mojibake.repairs(garble(text, actual, shown)).first()
            assertEquals(text, fix.text, "${actual.label} shown as ${shown.label}")
            assertEquals(listOf(MojibakeStep(shown, actual)), fix.steps)
        }
        assertEquals("ПРИВЕТ МИР", Mojibake.repairs(garble("ПРИВЕТ МИР", Codepage.CP1251, Codepage.KOI8R)).first().text)
    }

    @Test
    fun undoesDoubleMisreading() {
        val once = garble("Привет, мир", Codepage.UTF8, Codepage.CP1251)
        val twice = garble(once, Codepage.UTF8, Codepage.CP1251)
        val fix = Mojibake.repairs(twice).first()
        assertEquals("Привет, мир", fix.text)
        assertEquals(2, fix.steps.size)
    }

    @Test
    fun otherScriptsAndEmoji() {
        assertEquals("Café déjà vu", Mojibake.repairs(garble("Café déjà vu", Codepage.UTF8, Codepage.CP1252)).first().text)
        assertEquals("It’s a “test” — really…", Mojibake.repairs(garble("It’s a “test” — really…", Codepage.UTF8, Codepage.CP1252)).first().text)
        assertEquals("Привет 👋 как дела?", Mojibake.repairs(garble("Привет 👋 как дела?", Codepage.UTF8, Codepage.CP1251)).first().text)
        val file = "Отчёт за 2024 год.xlsx"
        assertEquals(file, Mojibake.repairs(garble(file, Codepage.UTF8, Codepage.CP866)).first().text)
    }

    @Test
    fun leavesReadableTextAlone() {
        for (text in listOf("Hello world", "Привет, мир! Всё в порядке.", "Проверка 123")) {
            val fix = Mojibake.repairs(text).first()
            assertEquals(text, fix.text)
            assertTrue(fix.steps.isEmpty())
        }
    }

    @Test
    fun appliesTheWholeTextNotJustTheSample() {
        val long = "Проверка длинного текста. ".repeat(200)
        val fix = Mojibake.repairs(garble(long, Codepage.UTF8, Codepage.CP1251)).first()
        assertEquals(long, fix.text)
    }

    @Test
    fun lostCharacters() {
        assertTrue(Mojibake.hasLostCharacters("?????? ???"))
        assertTrue(Mojibake.hasLostCharacters("Пр�вет"))
        assertFalse(Mojibake.hasLostCharacters("Что? Где? Когда?"))
    }
}
