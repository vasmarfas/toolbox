package com.vasmarfas.card.tools.time

import com.vasmarfas.card.core.Lang
import java.io.File
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CitiesTest {
    private val cities = Cities.parse(File("src/commonMain/composeResources/files/cities.json").readText())

    @Test
    fun everyCityHasAKnownZoneAndOneName() {
        assertTrue(cities.size > 200, "${cities.size} cities")
        cities.forEach { ZoneId.of(it.zone) }
        assertEquals(cities.size, cities.map { it.en }.toSet().size)
    }

    @Test
    fun searchReadsBothLanguages() {
        assertEquals("Moscow", Cities.search(cities, "моск").first().en)
        assertEquals("Санкт-Петербург", Cities.search(cities, "petersburg").single().name(Lang.RU))
        assertEquals("Oryol", Cities.search(cities, "орел").single().en)
        assertEquals("Rostov-on-Don", Cities.search(cities, "ростов на дону").single().en)
        assertTrue(Cities.search(cities, "м").isEmpty())
    }
}
