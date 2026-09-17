package com.vasmarfas.card.core

import com.vasmarfas.card.resources.*
import com.vasmarfas.card.resources.Res
import com.vasmarfas.card.resources.allStringResources
import com.vasmarfas.card.resources.english
import com.vasmarfas.card.resources.russian
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LocalisationTest {
    private fun <T> withLocale(tag: String, block: () -> T): T {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag(tag))
        try {
            return block()
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun resourcesFollowTheDefaultLocale() {
        assertEquals(assertNotNull(Res.string.home.russian()), withLocale("ru") { runBlocking { getString(Res.string.home) } })
        assertEquals(assertNotNull(Res.string.home.english()), withLocale("en") { runBlocking { getString(Res.string.home) } })
    }

    /**
     * The search index is parsed out of the same XML by a Gradle task rather than by the resource
     * loader, so the two decoders have to agree on every escape in every string.
     */
    @Test
    fun theSearchIndexMatchesWhatTheResourceLoaderReturns() {
        val all = Res.allStringResources.values.sortedBy { it.key }
        assertTrue(all.size > 2000, "expected the whole table, got ${all.size}")
        listOf("en" to { r: org.jetbrains.compose.resources.StringResource -> r.english() },
            "ru" to { r: org.jetbrains.compose.resources.StringResource -> r.russian() }).forEach { (tag, stored) ->
            withLocale(tag) {
                runBlocking {
                    all.forEach { resource ->
                        assertEquals(stored(resource), getString(resource), "${resource.key} differs in $tag")
                    }
                }
            }
        }
    }
}
