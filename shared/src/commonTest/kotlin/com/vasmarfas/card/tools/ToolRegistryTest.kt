package com.vasmarfas.card.tools

import com.vasmarfas.card.core.PlatformKind
import com.vasmarfas.card.resources.english
import com.vasmarfas.card.resources.matches
import com.vasmarfas.card.resources.russian
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ToolRegistryTest {
    private val kebab = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")

    @Test
    fun idsAreUniqueAndWellFormed() {
        val ids = ToolRegistry.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "duplicate ids: " + ids.groupBy { it }.filter { it.value.size > 1 }.keys)
        ids.forEach { assertTrue(kebab.matches(it), "id is not kebab-case: $it") }
    }

    @Test
    fun everyToolIsDescribedInBothLanguages() {
        ToolRegistry.all.forEach { tool ->
            val titleEn = assertNotNull(tool.title.english(), "title is missing from values/strings.xml: ${tool.id}")
            val titleRu = assertNotNull(tool.title.russian(), "title is missing from values-ru/strings.xml: ${tool.id}")
            val descriptionEn = assertNotNull(tool.description.english(), "description is missing from values/strings.xml: ${tool.id}")
            val descriptionRu = assertNotNull(tool.description.russian(), "description is missing from values-ru/strings.xml: ${tool.id}")
            assertTrue(titleEn.isNotBlank() && titleRu.isNotBlank(), "empty title: ${tool.id}")
            assertTrue(descriptionRu.isNotBlank(), "empty description: ${tool.id}")
            assertTrue(descriptionEn.length > 30, "description too short: ${tool.id}")
            assertTrue(tool.keywords.isNotEmpty(), "no keywords: ${tool.id}")
            assertTrue(tool.platforms.isNotEmpty(), "no platforms: ${tool.id}")
        }
    }

    @Test
    fun everyCategoryHasTools() {
        ToolCategory.entries.forEach { category ->
            assertTrue(ToolRegistry.byCategory(category).isNotEmpty(), "empty category: ${category.id}")
        }
    }

    @Test
    fun searchFindsToolsByIdTitleAndKeyword() {
        assertTrue(ToolRegistry.all.any { it.matches("subnet") })
        assertTrue(ToolRegistry.all.any { it.matches("подсет") })
        assertTrue(ToolRegistry.all.any { it.matches("qr") })
        assertTrue(ToolRegistry.all.count { it.matches("") } == ToolRegistry.all.size)
        ToolRegistry.all.forEach { tool ->
            assertTrue(tool.matches(tool.id), "tool does not match its own id: ${tool.id}")
            val title = assertNotNull(tool.title.russian(), "untranslated title: ${tool.id}")
            assertTrue(tool.matches(title.take(6)), "tool does not match its own title: ${tool.id}")
        }
    }

    @Test
    fun mostToolsWorkInTheBrowser() {
        val webTools = ToolRegistry.all.count { PlatformKind.WEB in it.platforms }
        assertTrue(webTools * 2 > ToolRegistry.all.size, "too few web tools: $webTools of ${ToolRegistry.all.size}")
        assertTrue(ToolRegistry.all.all { PlatformKind.ANDROID in it.platforms }, "some tools are unavailable on Android")
    }

    @Test
    fun byIdResolvesEveryRegisteredTool() {
        ToolRegistry.all.forEach { assertEquals(it, ToolRegistry.byId(it.id)) }
        assertEquals(null, ToolRegistry.byId("no-such-tool"))
    }
}
