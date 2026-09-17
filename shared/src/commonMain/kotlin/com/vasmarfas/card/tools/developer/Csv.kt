package com.vasmarfas.card.tools.developer

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

data class CsvTable(val rows: List<List<String>>, val delimiter: Char) {
    val columns: Int get() = rows.maxOfOrNull { it.size } ?: 0
}

object Csv {
    private val delimiters = listOf(',', ';', '\t', '|')

    fun detectDelimiter(text: String): Char {
        val sample = text.lineSequence().take(20).joinToString("\n")
        return delimiters.maxByOrNull { d -> countOutsideQuotes(sample, d) } ?: ','
    }

    private fun countOutsideQuotes(text: String, delimiter: Char): Int {
        var count = 0
        var inQuotes = false
        for (c in text) {
            when {
                c == '"' -> inQuotes = !inQuotes
                c == delimiter && !inQuotes -> count++
            }
        }
        return count
    }

    fun parse(text: String, delimiter: Char): CsvTable {
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        var started = false
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes -> {
                    if (c == '"') {
                        if (text.getOrNull(i + 1) == '"') {
                            field.append('"')
                            i++
                        } else {
                            inQuotes = false
                        }
                    } else {
                        field.append(c)
                    }
                }
                c == '"' && field.isBlank() -> {
                    field.clear()
                    inQuotes = true
                }
                c == delimiter -> {
                    row += field.toString()
                    field.clear()
                    started = true
                }
                c == '\r' -> {}
                c == '\n' -> {
                    row += field.toString()
                    field.clear()
                    if (started || row.size > 1 || row[0].isNotEmpty()) rows += row.toList()
                    row.clear()
                    started = false
                }
                else -> {
                    field.append(c)
                    started = true
                }
            }
            i++
        }
        row += field.toString()
        if (started || row.size > 1 || row[0].isNotEmpty()) rows += row.toList()
        return CsvTable(rows, delimiter)
    }

    fun escape(value: String, delimiter: Char): String =
        if (value.any { it == delimiter || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    fun toJson(table: CsvTable, hasHeader: Boolean): JsonArray {
        if (table.rows.isEmpty()) return JsonArray(emptyList())
        val header = if (hasHeader) table.rows.first() else List(table.columns) { "column${it + 1}" }
        val body = if (hasHeader) table.rows.drop(1) else table.rows
        return buildJsonArray {
            body.forEach { row ->
                add(
                    buildJsonObject {
                        header.forEachIndexed { i, name ->
                            val key = name.ifBlank { "column${i + 1}" }
                            put(key, valueElement(row.getOrElse(i) { "" }))
                        }
                    },
                )
            }
        }
    }

    private fun valueElement(raw: String): JsonPrimitive {
        val t = raw.trim()
        return when {
            t.isEmpty() -> JsonPrimitive(raw)
            t == "true" || t == "false" -> JsonPrimitive(t == "true")
            t.toLongOrNull() != null -> JsonPrimitive(t.toLong())
            t.toDoubleOrNull() != null && t.none { it == 'x' || it == 'X' } -> JsonPrimitive(t.toDouble())
            else -> JsonPrimitive(raw)
        }
    }

    fun toMarkdown(table: CsvTable, hasHeader: Boolean): String {
        if (table.rows.isEmpty()) return ""
        val columns = table.columns
        val header = if (hasHeader) table.rows.first() else List(columns) { "Column ${it + 1}" }
        val body = if (hasHeader) table.rows.drop(1) else table.rows
        val widths = IntArray(columns) { i ->
            maxOf(3, header.getOrElse(i) { "" }.length, body.maxOfOrNull { it.getOrElse(i) { "" }.length } ?: 0)
        }
        fun line(cells: List<String>) = (0 until columns).joinToString(" | ", "| ", " |") { i ->
            cells.getOrElse(i) { "" }.replace("|", "\\|").padEnd(widths[i])
        }
        return buildString {
            appendLine(line(header))
            appendLine((0 until columns).joinToString(" | ", "| ", " |") { "-".repeat(widths[it]) })
            body.forEach { appendLine(line(it)) }
        }.trimEnd()
    }

    fun fromJson(array: JsonArray, delimiter: Char): String? {
        val objects = array.map { it as? JsonObject ?: return null }
        val header = LinkedHashSet<String>()
        objects.forEach { header += it.keys }
        val sb = StringBuilder()
        sb.append(header.joinToString(delimiter.toString()) { escape(it, delimiter) }).append('\n')
        objects.forEach { obj ->
            sb.append(
                header.joinToString(delimiter.toString()) { key ->
                    val value = obj[key]
                    val text = when (value) {
                        null -> ""
                        is JsonPrimitive -> if (value.isString) value.content else value.content
                        else -> JsonTools.minify(value)
                    }
                    escape(text, delimiter)
                },
            ).append('\n')
        }
        return sb.toString().trimEnd()
    }
}
