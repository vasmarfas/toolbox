package com.vasmarfas.card.core

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppleDisplaysTest {
    @Test
    fun densityMatchesTheAdvertisedDiagonal() {
        appleDisplays.forEach {
            val diagonal = hypot(it.widthPx.toDouble(), it.heightPx.toDouble()) / it.ppi
            assertTrue(abs(diagonal - it.diagonalInches) < 0.15, "${it.name}: $diagonal")
        }
    }

    @Test
    fun identifiersBelongToOneModel() {
        val owners = appleDisplays.flatMap { display -> display.identifiers.map { it to display.name } }.groupBy({ it.first }, { it.second })
        owners.forEach { (identifier, names) -> assertEquals(1, names.distinct().size, identifier) }
        assertTrue(owners.keys.all { Regex("^(iPhone|iPad)\\d+,\\d+$").matches(it) })
    }

    @Test
    fun identifierNamesTheModel() {
        val found = appleCandidates(AppleScreen("iPhone16,1", 3.0), 1179 to 2556, 3f)
        assertEquals(listOf("iPhone 15 Pro"), found.map { it.name })
        assertEquals(460.0, found.single().pxPerInch(3f, 3.0), 1e-9)
    }

    @Test
    fun panelSizePicksTheScreenOfAFoldable() {
        val inner = appleCandidates(AppleScreen("iPhone19,4", 3.0), 2670 to 1878, 3f).single()
        val outer = appleCandidates(AppleScreen("iPhone19,4", 3.0), 1398 to 2034, 3f).single()
        assertEquals(430, inner.ppi)
        assertEquals(460, outer.ppi)
    }

    @Test
    fun displayZoomChangesPixelsPerInch() {
        val display = appleDisplays.first { it.name == "iPhone 15 Pro" }
        assertEquals(460.0 * 3 / 3.144, display.pxPerInch(3f, 3.144), 1e-9)
    }

    @Test
    fun safariMatchesBySizeInPoints() {
        val safari = AppleScreen(null, null)
        val pro = appleCandidates(safari, 1179 to 2556, 3f)
        assertTrue(pro.isNotEmpty())
        assertEquals(1, pro.map { it.pxPerInch(3f) }.distinct().size)
        val mini = appleCandidates(safari, 1125 to 2436, 3f).map { it.name }
        assertTrue("iPhone 12 mini" in mini && "iPhone XS" in mini, mini.toString())
        assertTrue(appleCandidates(safari, 1920 to 1080, 1f).isEmpty())
    }
}

class EdidTest {
    private fun edid(widthMm: Int = 597, heightMm: Int = 336, name: String? = "TEST 27QHD"): ByteArray {
        val e = ByteArray(128)
        byteArrayOf(0, -1, -1, -1, -1, -1, -1, 0).copyInto(e)
        val maker = ((('D' - 'A' + 1) shl 10) or (('E' - 'A' + 1) shl 5) or ('L' - 'A' + 1))
        e[8] = (maker shr 8).toByte()
        e[9] = maker.toByte()
        e[54] = 0x56
        e[55] = 0x5E
        e[56] = (2560 and 0xFF).toByte()
        e[58] = ((2560 shr 8) shl 4).toByte()
        e[59] = (1440 and 0xFF).toByte()
        e[61] = ((1440 shr 8) shl 4).toByte()
        e[66] = (widthMm and 0xFF).toByte()
        e[67] = (heightMm and 0xFF).toByte()
        e[68] = (((widthMm shr 8) shl 4) or (heightMm shr 8)).toByte()
        if (name != null) {
            e[75] = 0xFC.toByte()
            (name + "\n").padEnd(13, ' ').encodeToByteArray().copyInto(e, 77)
        }
        return e
    }

    @Test
    fun readsModeSizeAndName() {
        val panel = assertNotNull(parseEdid(edid()))
        assertEquals("TEST 27QHD", panel.name)
        assertEquals(2560 to 1440, panel.widthPx to panel.heightPx)
        assertEquals(597 to 336, panel.widthMm to panel.heightMm)
        assertEquals(27.0, panel.diagonalInches, 0.05)
    }

    @Test
    fun makerCodeStandsInForAMissingName() {
        assertEquals("DEL", parseEdid(edid(name = null))?.name)
    }

    @Test
    fun rejectsSizesThatAreOnlyAnAspectRatio() {
        assertNull(parseEdid(edid(widthMm = 16, heightMm = 9)))
        assertNull(parseEdid(edid(widthMm = 0, heightMm = 0)))
        assertNull(parseEdid(ByteArray(128)))
    }
}
