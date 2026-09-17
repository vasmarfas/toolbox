package com.vasmarfas.card.tools.developer

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private val people = Json.parseToJsonElement(
    """{"users":[{"name":"Ann","age":30,"admin":true},{"name":"Bob","age":25,"admin":false}],"total":2,"next":null}""",
)

private val numbers = Json.parseToJsonElement((1..500).joinToString(",", "[", "]"))

class JsonTreeTest {
    @Test
    fun expandsTwoLevelsByDefault() {
        val rows = JsonTree.rows(people, JsonTreeState()).rows
        assertEquals(listOf("\$", "\$.users", "\$.users[0]", "\$.users[1]", "\$.total", "\$.next"), rows.map { it.path })
        val users = assertNotNull(rows.firstOrNull { it.path == "\$.users" })
        assertEquals(JsonKind.ARRAY, users.kind)
        assertEquals(2, users.childCount)
        assertTrue(users.expanded)
        assertFalse(rows.first { it.path == "\$.users[0]" }.expanded)
        assertTrue(rows.first { it.path == "\$.users[0]" }.expandable)
        assertEquals(JsonKind.NULL, rows.first { it.path == "\$.next" }.kind)
        assertEquals("2", rows.first { it.path == "\$.total" }.value)
    }

    @Test
    fun overridesAndAutoDepthDriveExpansion() {
        val opened = JsonTree.rows(people, JsonTreeState().toggle("\$.users[0]", 2)).rows
        assertEquals("\"Ann\"", opened.first { it.path == "\$.users[0].name" }.value)
        assertEquals(JsonKind.BOOLEAN, opened.first { it.path == "\$.users[0].admin" }.kind)
        val collapsed = JsonTree.rows(people, JsonTreeState(autoDepth = 0)).rows
        assertEquals(listOf("\$"), collapsed.map { it.path })
        assertTrue(collapsed.single().expandable)
        assertEquals(12, JsonTree.rows(people, JsonTreeState(autoDepth = Int.MAX_VALUE)).rows.size)
    }

    @Test
    fun filterKeepsMatchesWithTheirParents() {
        assertEquals(
            listOf("\$", "\$.users", "\$.users[1]", "\$.users[1].name"),
            JsonTree.rows(people, JsonTreeState(query = "bob")).rows.map { it.path },
        )
        assertEquals(
            listOf("\$", "\$.users", "\$.users[0]", "\$.users[0].age", "\$.users[1]", "\$.users[1].age"),
            JsonTree.rows(people, JsonTreeState(query = "age")).rows.map { it.path },
        )
        assertTrue(JsonTree.rows(people, JsonTreeState(query = "nothing")).rows.isEmpty())
    }

    @Test
    fun largeArraysArePagedAndCapped() {
        val first = JsonTree.rows(numbers, JsonTreeState())
        assertEquals(JsonTree.PAGE + 2, first.rows.size)
        assertEquals(300, first.rows.last().more)
        val more = JsonTree.rows(numbers, JsonTreeState().showMore("\$"))
        assertEquals(2 * JsonTree.PAGE + 2, more.rows.size)
        assertEquals(100, more.rows.last().more)
        val capped = JsonTree.rows(numbers, JsonTreeState(), maxRows = 10)
        assertEquals(10, capped.rows.size)
        assertTrue(capped.truncated)
    }

    @Test
    fun pathsQuoteUnusualKeys() {
        val element = Json.parseToJsonElement("""{"my key":{"2nd":[1]},"ok_1":1}""")
        val rows = JsonTree.rows(element, JsonTreeState(autoDepth = Int.MAX_VALUE)).rows
        assertEquals(
            listOf("\$", "\$[\"my key\"]", "\$[\"my key\"][\"2nd\"]", "\$[\"my key\"][\"2nd\"][0]", "\$.ok_1"),
            rows.map { it.path },
        )
    }
}

class JsonUtf8Test {
    @Test
    fun countsUtf8Bytes() {
        assertEquals(0, JsonTools.utf8Length(""))
        assertEquals(5, JsonTools.utf8Length("plain"))
        assertEquals(12, JsonTools.utf8Length("привет"))
        assertEquals(3, JsonTools.utf8Length("€"))
        assertEquals(4, JsonTools.utf8Length("😀"))
        assertEquals("привет и €".encodeToByteArray().size, JsonTools.utf8Length("привет и €"))
    }
}
