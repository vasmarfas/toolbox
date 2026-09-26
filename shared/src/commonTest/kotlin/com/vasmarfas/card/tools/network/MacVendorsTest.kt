package com.vasmarfas.card.tools.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MacVendorsTest {
    private val registry = MacRegistry.parse(
        """
        #2026-09-25
        4C5E0C	Routerboard.com	LV
        70B3D5	IEEE Registration Authority	US
        70B3D5F2F	TELEPLATFORMS	RU
        C85CE2	IEEE Registration Authority	US
        C85CE27	SYNERGY SYSTEMS AND SOLUTIONS	IN
        741AE09	Private
        """.trimIndent(),
    )

    @Test
    fun theLongestAssignedPrefixNamesTheVendor() {
        assertEquals("2026-09-25", registry.date)
        assertEquals("TELEPLATFORMS", registry.find("70B3D5F2F123")?.vendor)
        assertEquals("MA-S", registry.find("70B3D5F2F123")?.type)
        assertEquals("IEEE Registration Authority", registry.find("70B3D5000000")?.vendor)
        assertEquals("SYNERGY SYSTEMS AND SOLUTIONS", registry.find("C85CE27ABCDE")?.vendor)
        assertEquals("MA-M", registry.find("C85CE27")?.type)
        assertEquals("LV", registry.find("4C5E0C")?.country)
        assertNull(registry.find("741AE09")?.country)
        assertNull(registry.find("001122334455"))
    }

    @Test
    fun aBlockSpansItsFreeBits() {
        val block = registry.find("C85CE27ABCDE")!!
        assertEquals("C85CE2700000", block.first)
        assertEquals("C85CE27FFFFF", block.last)
    }

    @Test
    fun servicesSpellingTheSameVendorAgree() {
        assertTrue(MacVendors.sameVendor("Apple, Inc.", "Apple Inc"))
        assertTrue(MacVendors.sameVendor("HUAWEI TECHNOLOGIES CO.,LTD", "Huawei Technologies Co., Ltd."))
        assertFalse(MacVendors.sameVendor("Cisco Systems, Inc", "Apple, Inc."))
        assertFalse(MacVendors.sameVendor("Inc.", "Ltd"))
        assertFalse(MacVendors.sameVendor("Intel Corporate", "Intelbras"))
        assertTrue(MacVendors.sameVendor("TP-LINK TECHNOLOGIES CO.,LTD.", "TP-Link Corporation Limited"))
    }

    @Test
    fun aVendorNeedsAtLeastTheOui() {
        assertEquals("4C5E0C", MacAddress.prefix("4c-5e-0c"))
        assertEquals("4C5E0C123456", MacAddress.prefix("4C:5E:0C:12:34:56"))
        assertNull(MacAddress.prefix("4C:5E"))
    }

    @Test
    fun groupsWithoutLeadingZerosArePadded() {
        assertEquals("3C0754123456", MacAddress.normalize("3c:7:54:12:34:56"))
        assertEquals("001A2B3", MacAddress.prefix("00:1a:2b:3"))
        assertEquals("001A2B", MacAddress.prefix("00:1A:2B:"))
        assertNull(MacAddress.prefix("4C:5E:0C:12:34:5G"))
    }

    @Test
    fun aGroupAddressBelongsToTheOwnerOfItsOui() {
        assertEquals("00005E000001", MacAddress.individual("01005E000001"))
        assertEquals("ICANN", MacRegistry.parse("#2026-09-25\n00005E\tICANN\tUS").find(MacAddress.individual("01005E"))?.vendor)
    }
}
