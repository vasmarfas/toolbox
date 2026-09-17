package com.vasmarfas.card.tools.everyday

import com.vasmarfas.card.core.Net
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

@Serializable
data class Counter(val name: String, val value: Int = 0)

object Tally {
    const val PREF_KEY = "tally.counters"

    fun encode(counters: List<Counter>): String = Net.json.encodeToString(ListSerializer(Counter.serializer()), counters)

    fun decode(text: String?): List<Counter>? =
        text?.let { runCatching { Net.json.decodeFromString(ListSerializer(Counter.serializer()), it) }.getOrNull() }
}
