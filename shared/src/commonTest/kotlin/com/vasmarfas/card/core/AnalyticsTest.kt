package com.vasmarfas.card.core

import com.vasmarfas.card.resources.matches
import com.vasmarfas.card.resources.russian
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class MemoryStore : KeyValueStore {
    private val values = mutableMapOf<String, String>()

    override fun get(key: String): String? = values[key]

    override fun put(key: String, value: String) {
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}

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

class AnalyticsConsentTest {
    @Test
    fun disabledByDefault() {
        val store = MemoryStore()
        assertFalse(Analytics.isEnabled(store))
        store.put("analytics.enabled", "yes")
        assertFalse(Analytics.isEnabled(store))
        store.put("analytics.enabled", "")
        assertFalse(Analytics.isEnabled(store))
    }

    @Test
    fun consentRoundTrip() {
        val store = MemoryStore()
        Analytics.setEnabled(true, store)
        assertEquals("true", store.get("analytics.enabled"))
        assertTrue(Analytics.isEnabled(store))
        Analytics.setEnabled(false, store)
        assertEquals("false", store.get("analytics.enabled"))
        assertFalse(Analytics.isEnabled(store))
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
