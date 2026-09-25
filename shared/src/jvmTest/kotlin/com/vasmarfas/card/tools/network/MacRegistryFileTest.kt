package com.vasmarfas.card.tools.network

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MacRegistryFileTest {
    private val registry = MacRegistry.parse(File("src/commonMain/composeResources/files/mac-vendors.tsv").readText())

    @Test
    fun theBundledRegistryKnowsWellKnownBlocks() {
        assertEquals("XEROX CORPORATION", registry.find("000000")?.vendor)
        assertEquals("Routerboard.com", registry.find("4C5E0C")?.vendor)
        assertEquals("LV", registry.find("4C5E0C")?.country)
        assertEquals("TELEPLATFORMS", registry.find("70B3D5F2F")?.vendor)
        assertTrue(registry.date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }
}
