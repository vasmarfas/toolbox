package com.vasmarfas.card.tools.developer

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

data class JsonStats(
    val keys: Int,
    val depth: Int,
    val objects: Int,
    val arrays: Int,
    val strings: Int,
    val numbers: Int,
    val booleans: Int,
    val nulls: Int,
)

@OptIn(ExperimentalSerializationApi::class)
object JsonTools {
    private val pretty2 = Json { prettyPrint = true; prettyPrintIndent = "  " }
    private val pretty4 = Json { prettyPrint = true; prettyPrintIndent = "    " }

    fun parse(text: String): Result<JsonElement> = try {
        Result.success(Json.parseToJsonElement(text))
    } catch (e: Exception) {
        Result.failure(e)
    }

    // the parser adds a hint for the Json builder and a dump of the input after the first line, both are noise here
    fun errorMessage(e: Throwable): String = (e.message ?: "Invalid JSON").substringBefore("\n").removeSuffix(" at path: $")

    fun format(element: JsonElement, indent: Int): String =
        (if (indent == 4) pretty4 else pretty2).encodeToString(JsonElement.serializer(), element)

    fun minify(element: JsonElement): String = Json.encodeToString(JsonElement.serializer(), element)

    fun sortKeys(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> JsonObject(element.keys.sorted().associateWith { sortKeys(element.getValue(it)) })
        is JsonArray -> JsonArray(element.map { sortKeys(it) })
        else -> element
    }

    fun stats(element: JsonElement): JsonStats {
        var keys = 0
        var objects = 0
        var arrays = 0
        var strings = 0
        var numbers = 0
        var booleans = 0
        var nulls = 0
        var maxDepth = 0
        fun walk(e: JsonElement, depth: Int) {
            if (depth > maxDepth) maxDepth = depth
            when (e) {
                is JsonObject -> {
                    objects++
                    keys += e.size
                    e.values.forEach { walk(it, depth + 1) }
                }
                is JsonArray -> {
                    arrays++
                    e.forEach { walk(it, depth + 1) }
                }
                is JsonNull -> nulls++
                is JsonPrimitive -> when {
                    e.isString -> strings++
                    e.content == "true" || e.content == "false" -> booleans++
                    else -> numbers++
                }
            }
        }
        walk(element, 1)
        return JsonStats(keys, maxDepth, objects, arrays, strings, numbers, booleans, nulls)
    }

    fun utf8Length(text: String): Int {
        var length = 0
        var i = 0
        while (i < text.length) {
            val code = text[i].code
            length += when {
                code < 0x80 -> 1
                code < 0x800 -> 2
                code in 0xD800..0xDBFF && i + 1 < text.length -> {
                    i++
                    4
                }
                else -> 3
            }
            i++
        }
        return length
    }

    fun escapeString(text: String): String =
        Json.encodeToString(JsonElement.serializer(), JsonPrimitive(text)).removeSurrounding("\"")

    fun unescapeString(text: String): String? {
        val t = text.trim()
        val quoted = if (t.length >= 2 && t.startsWith("\"") && t.endsWith("\"")) t else "\"$t\""
        return try {
            val element = Json.parseToJsonElement(quoted).jsonPrimitive
            if (element.isString) element.content else null
        } catch (e: Exception) {
            null
        }
    }
}
