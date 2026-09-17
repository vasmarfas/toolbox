package com.vasmarfas.card.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyticsEventsTest {
    @Test
    fun namesAreSnakeCase() {
        val pattern = Regex("^[a-z]+(_[a-z]+)*$")
        val names = AnalyticsEvent.entries.map { it.eventName }
        assertTrue(names.all { pattern.matches(it) }, names.toString())
        assertEquals(names.size, names.toSet().size)
        assertTrue(names.size <= 10)
    }

    @Test
    fun namesAreFixed() {
        assertEquals(
            listOf("app_open", "screen_view", "tool_open", "tool_action", "language_change", "theme_change"),
            AnalyticsEvent.entries.map { it.eventName },
        )
    }

    @Test
    fun parameterKeysAreSnakeCase() {
        val keys = listOf(AnalyticsParam.SCREEN, AnalyticsParam.TOOL, AnalyticsParam.ACTION, AnalyticsParam.VALUE)
        assertEquals(keys, Analytics.safeParams(keys.associateWith { it }).keys.toList())
    }
}

class AnalyticsParamsTest {
    @Test
    fun keepsRegistryIdentifiers() {
        val params = mapOf(AnalyticsParam.TOOL to "dns-lookup", AnalyticsParam.ACTION to "run")
        assertEquals(params, Analytics.safeParams(params))
        assertEquals(
            mapOf(AnalyticsParam.SCREEN to "tools"),
            Analytics.safeParams(mapOf(AnalyticsParam.SCREEN to "tools")),
        )
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
