package com.vasmarfas.card.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnalyticsEventsTest {
    @Test
    fun namesFollowFirebaseRules() {
        val pattern = Regex("^[a-z][a-z0-9]*(_[a-z0-9]+)*$")
        val names = AnalyticsEvent.entries.map { it.eventName }
        assertTrue(names.all { pattern.matches(it) && it.length <= 40 }, names.toString())
        assertTrue(names.none { it.startsWith("firebase_") || it.startsWith("google_") || it.startsWith("ga_") })
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun userPropertiesFitFirebaseLimits() {
        val keys = UserProperty.entries.map { it.key }
        assertTrue(keys.all { Regex("^[a-z][a-z_]*$").matches(it) && it.length <= 24 }, keys.toString())
        assertTrue(keys.size <= 25)
    }

    @Test
    fun parameterKeysAreSnakeCase() {
        val keys = listOf(
            AnalyticsParam.SCREEN_NAME, AnalyticsParam.SCREEN_CLASS, AnalyticsParam.TOOL, AnalyticsParam.CATEGORY,
            AnalyticsParam.SOURCE, AnalyticsParam.STEP, AnalyticsParam.ROLE, AnalyticsParam.INTEREST, AnalyticsParam.STATE,
            AnalyticsParam.SETTING, AnalyticsParam.VALUE, AnalyticsParam.LINK, AnalyticsParam.ENTRY, AnalyticsParam.SCREEN,
            AnalyticsParam.DEPTH,
        )
        assertEquals(keys, Analytics.safeParams(keys.associateWith { it }).keys.toList())
    }
}

class AnalyticsParamsTest {
    @Test
    fun keepsRegistryIdentifiers() {
        val params = mapOf(AnalyticsParam.TOOL to "dns-lookup", AnalyticsParam.SOURCE to "home")
        assertEquals(params, Analytics.safeParams(params))
    }

    @Test
    fun dropsUserInput() {
        assertEquals(emptyMap(), Analytics.safeParams(mapOf("query" to "vasmarfas@mail.russian()")))
        assertEquals(emptyMap(), Analytics.safeParams(mapOf(AnalyticsParam.VALUE to "192.168.1.1")))
        assertEquals(emptyMap(), Analytics.safeParams(mapOf(AnalyticsParam.VALUE to "Привет")))
        assertEquals(emptyMap(), Analytics.safeParams(mapOf(AnalyticsParam.VALUE to "two words")))
        assertEquals(emptyMap(), Analytics.safeParams(mapOf(AnalyticsParam.TOOL to "a".repeat(41))))
        assertEquals(emptyMap(), Analytics.safeParams(mapOf(AnalyticsParam.TOOL to "")))
        assertEquals(emptyMap(), Analytics.safeParams(mapOf("Tool" to "ping")))
    }

    @Test
    fun dropsOnlyTheUnsafeEntry() {
        assertEquals(
            mapOf(AnalyticsParam.TOOL to "ping"),
            Analytics.safeParams(mapOf(AnalyticsParam.TOOL to "ping", AnalyticsParam.VALUE to "8.8.8.8")),
        )
    }
}

class SearchTermTest {
    @Test
    fun keepsToolNames() {
        assertEquals("конвертер валют", Analytics.searchTerm("  Конвертер   Валют "))
        assertEquals("pdf", Analytics.searchTerm("PDF"))
        assertEquals("sha-256", Analytics.searchTerm("sha-256"))
        assertEquals("mp4 в gif", Analytics.searchTerm("mp4 в gif"))
    }

    @Test
    fun dropsAnythingPersonal() {
        assertNull(Analytics.searchTerm("vasya@mail.ru"))
        assertNull(Analytics.searchTerm("+7 912 345 67 89"))
        assertNull(Analytics.searchTerm("89123456789"))
        assertNull(Analytics.searchTerm("192.168.1.1"))
        assertNull(Analytics.searchTerm("a"))
        assertNull(Analytics.searchTerm("x".repeat(31)))
    }

    @Test
    fun bucketsCounts() {
        assertEquals(listOf("0", "1-5", "6-10", "11-20", "21-40", "41-plus"), listOf(0, 3, 10, 11, 40, 41).map(Analytics::bucket))
    }
}

class ScrollDepthTest {
    @Test
    fun reportsEachQuarterOnce() {
        assertEquals(emptyList(), Analytics.depthsReached(20, reported = 0))
        assertEquals(listOf(25), Analytics.depthsReached(30, reported = 0))
        assertEquals(emptyList(), Analytics.depthsReached(40, reported = 25))
        assertEquals(listOf(50, 75), Analytics.depthsReached(80, reported = 25))
    }

    @Test
    fun aJumpToTheBottomCountsEveryQuarter() {
        assertEquals(listOf(25, 50, 75, 100), Analytics.depthsReached(100, reported = 0))
    }
}
