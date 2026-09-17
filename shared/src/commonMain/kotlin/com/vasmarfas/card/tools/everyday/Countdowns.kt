package com.vasmarfas.card.tools.everyday

import com.vasmarfas.card.core.Net
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlin.math.abs

@Serializable
data class CountdownEvent(val name: String, val epochSeconds: Long)

data class Remaining(val days: Long, val hours: Long, val minutes: Long, val past: Boolean)

object Countdowns {
    const val PREF_KEY = "countdowns.json"

    fun remaining(eventEpoch: Long, nowEpoch: Long): Remaining {
        val diff = eventEpoch - nowEpoch
        val a = abs(diff)
        return Remaining(a / 86_400, a % 86_400 / 3600, a % 3600 / 60, diff < 0)
    }

    fun encode(events: List<CountdownEvent>): String = Net.json.encodeToString(ListSerializer(CountdownEvent.serializer()), events)

    fun decode(text: String?): List<CountdownEvent>? =
        text?.let { runCatching { Net.json.decodeFromString(ListSerializer(CountdownEvent.serializer()), it) }.getOrNull() }
}
