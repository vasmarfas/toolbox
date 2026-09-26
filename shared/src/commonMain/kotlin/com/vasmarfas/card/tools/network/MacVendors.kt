package com.vasmarfas.card.tools.network

import com.vasmarfas.card.core.Net
import com.vasmarfas.card.resources.*
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

class MacBlock(val prefix: String, val vendor: String, val country: String?) {
    val type: String
        get() = when {
            prefix.length == 6 -> "MA-L"
            prefix.length == 7 -> "MA-M"
            prefix.startsWith("0050C2") || prefix.startsWith("40D855") -> "IAB"
            else -> "MA-S"
        }
    val first: String get() = prefix.padEnd(12, '0')
    val last: String get() = prefix.padEnd(12, 'F')
}

class MacRegistry(val date: String, private val blocks: Map<String, MacBlock>) {
    fun find(hex: String): MacBlock? = listOf(9, 7, 6).firstNotNullOfOrNull { if (hex.length >= it) blocks[hex.take(it)] else null }

    companion object {
        fun parse(text: String): MacRegistry {
            val lines = text.lines()
            val blocks = HashMap<String, MacBlock>(lines.size * 2)
            for (line in lines) {
                if (line.isEmpty() || line.startsWith('#')) continue
                val fields = line.split('\t')
                blocks[fields[0]] = MacBlock(fields[0], fields[1], fields.getOrNull(2))
            }
            return MacRegistry(lines.first().removePrefix("#"), blocks)
        }
    }
}

class MacAnswer(val source: String, val vendor: String?, val failed: Boolean = false)

object MacVendors {
    private var registry: MacRegistry? = null

    suspend fun registry(): MacRegistry = registry ?: withContext(Dispatchers.Default) {
        MacRegistry.parse(Res.readBytes("files/mac-vendors.tsv").decodeToString())
    }.also { registry = it }

    fun online(hex: String): Flow<MacAnswer> {
        val prefix = hex.take(9)
        return merge(flow { emit(macLookup(prefix)) }, flow { emit(macVendors(prefix)) })
    }

    private suspend fun macLookup(prefix: String): MacAnswer = runCatching {
        val response = Net.client.get("https://api.maclookup.app/v2/macs/$prefix")
        if (response.status != HttpStatusCode.OK) return@runCatching MacAnswer("maclookup.app", null, failed = true)
        val json = Net.json.parseToJsonElement(response.bodyAsText()).jsonObject
        val found = (json["found"] as? JsonPrimitive)?.content == "true"
        MacAnswer("maclookup.app", (json["company"] as? JsonPrimitive)?.content?.takeIf { found && it.isNotBlank() })
    }.getOrElse { MacAnswer("maclookup.app", null, failed = true) }

    private suspend fun macVendors(prefix: String): MacAnswer = runCatching {
        val response = Net.client.get("https://api.macvendors.com/$prefix")
        when (response.status) {
            HttpStatusCode.OK -> MacAnswer("macvendors.com", response.bodyAsText().trim().ifEmpty { null })
            HttpStatusCode.NotFound -> MacAnswer("macvendors.com", null)
            else -> MacAnswer("macvendors.com", null, failed = true)
        }
    }.getOrElse { MacAnswer("macvendors.com", null, failed = true) }

    private val legalWords = setOf(
        "inc", "incorporated", "corp", "corporation", "co", "company", "ltd", "limited", "llc", "gmbh", "ag", "sa", "bv",
        "oy", "ab", "kk", "plc", "pte", "pty", "srl", "spa", "the",
    )

    fun sameVendor(a: String, b: String): Boolean {
        fun words(name: String) = name.lowercase().map { if (it.isLetterOrDigit()) it else ' ' }.joinToString("")
            .split(' ').filter { it.isNotEmpty() && it !in legalWords }
        val x = words(a)
        val y = words(b)
        return x.isNotEmpty() && y.isNotEmpty() && (x.take(y.size) == y || y.take(x.size) == x)
    }
}
