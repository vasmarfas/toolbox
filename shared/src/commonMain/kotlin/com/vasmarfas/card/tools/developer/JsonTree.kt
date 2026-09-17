package com.vasmarfas.card.tools.developer

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

enum class JsonKind { OBJECT, ARRAY, STRING, NUMBER, BOOLEAN, NULL }

data class JsonRow(
    val path: String,
    val label: String,
    val kind: JsonKind,
    val value: String,
    val childCount: Int,
    val depth: Int,
    val expandable: Boolean,
    val expanded: Boolean,
    val more: Int,
)

data class JsonTreeRows(val rows: List<JsonRow>, val truncated: Boolean)

data class JsonTreeState(
    val overrides: Map<String, Boolean> = emptyMap(),
    val autoDepth: Int = 2,
    val pages: Map<String, Int> = emptyMap(),
    val query: String = "",
) {
    fun isExpanded(path: String, depth: Int): Boolean = overrides[path] ?: (depth < autoDepth)

    fun limit(path: String): Int = pages[path] ?: JsonTree.PAGE

    fun toggle(path: String, depth: Int): JsonTreeState =
        copy(overrides = overrides + (path to !isExpanded(path, depth)))

    fun showMore(path: String): JsonTreeState = copy(pages = pages + (path to (limit(path) + JsonTree.PAGE)))
}

private class JsonChild(val label: String, val path: String, val element: JsonElement)

object JsonTree {
    const val ROOT = "\$"
    const val PAGE = 200
    const val MAX_ROWS = 800
    const val VALUE_CHARS = 120
    private const val VISIT_BUDGET = 200_000
    private val plainKey = Regex("^[A-Za-z_][A-Za-z0-9_]*\$")

    fun kindOf(element: JsonElement): JsonKind = when (element) {
        is JsonObject -> JsonKind.OBJECT
        is JsonArray -> JsonKind.ARRAY
        is JsonNull -> JsonKind.NULL
        is JsonPrimitive -> when {
            element.isString -> JsonKind.STRING
            element.content == "true" || element.content == "false" -> JsonKind.BOOLEAN
            else -> JsonKind.NUMBER
        }
    }

    fun childPath(parent: String, key: String): String =
        if (plainKey.matches(key)) "$parent.$key" else "$parent[\"${key.replace("\\", "\\\\").replace("\"", "\\\"")}\"]"

    fun value(element: JsonElement): String = when (element) {
        is JsonObject, is JsonArray -> ""
        is JsonNull -> "null"
        is JsonPrimitive -> {
            val content = if (element.content.length > VALUE_CHARS) element.content.take(VALUE_CHARS) + "…" else element.content
            if (element.isString) "\"$content\"" else content
        }
    }

    fun childCount(element: JsonElement): Int = when (element) {
        is JsonObject -> element.size
        is JsonArray -> element.size
        else -> 0
    }

    fun rows(root: JsonElement, state: JsonTreeState, maxRows: Int = MAX_ROWS): JsonTreeRows {
        val out = ArrayList<JsonRow>()
        val query = state.query.trim().lowercase()
        var visits = 0
        var truncated = false

        fun selfMatches(label: String, element: JsonElement): Boolean =
            label.lowercase().contains(query) || (element is JsonPrimitive && element.content.lowercase().contains(query))

        fun subtreeMatches(label: String, element: JsonElement): Boolean {
            if (visits >= VISIT_BUDGET) return false
            visits++
            if (selfMatches(label, element)) return true
            return when (element) {
                is JsonObject -> element.any { (key, child) -> subtreeMatches(key, child) }
                is JsonArray -> element.any { subtreeMatches("", it) }
                else -> false
            }
        }

        fun children(path: String, element: JsonElement): Sequence<JsonChild> = when (element) {
            is JsonObject -> element.entries.asSequence().map { JsonChild(it.key, childPath(path, it.key), it.value) }
            is JsonArray -> element.asSequence().mapIndexed { index, child -> JsonChild("[$index]", "$path[$index]", child) }
            else -> emptySequence()
        }

        fun walk(label: String, path: String, element: JsonElement, depth: Int, whole: Boolean) {
            val count = childCount(element)
            val expanded = count > 0 && (query.isNotEmpty() || state.isExpanded(path, depth))
            out.add(JsonRow(path, label, kindOf(element), value(element), count, depth, count > 0, expanded, 0))
            if (!expanded) return
            val limit = state.limit(path)
            val keepAll = whole || query.isEmpty() || selfMatches(label, element)
            val visible: List<JsonChild>
            val rest: Int
            if (keepAll) {
                visible = children(path, element).take(limit).toList()
                rest = count - visible.size
            } else {
                val matched = children(path, element).filter { subtreeMatches(it.label, it.element) }.toList()
                visible = matched.take(limit)
                rest = matched.size - visible.size
            }
            for (child in visible) {
                if (out.size >= maxRows) {
                    truncated = true
                    return
                }
                walk(child.label, child.path, child.element, depth + 1, keepAll && query.isNotEmpty())
            }
            if (rest > 0 && out.size < maxRows) {
                out.add(JsonRow(path, "", kindOf(element), "", count, depth + 1, false, false, rest))
            }
        }

        if (query.isNotEmpty() && !subtreeMatches(ROOT, root)) return JsonTreeRows(emptyList(), visits >= VISIT_BUDGET)
        walk(ROOT, ROOT, root, 0, false)
        return JsonTreeRows(out, truncated || visits >= VISIT_BUDGET)
    }
}
