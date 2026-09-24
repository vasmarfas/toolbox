package com.vasmarfas.card.tools.money

import com.vasmarfas.card.core.Net
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

@Serializable
data class SplitPerson(val name: String = "", val paid: String = "")

data class Transfer(val from: String, val to: String, val amount: Double)

object Split {
    const val PREF_KEY = "split.session"

    fun settle(balances: List<Pair<String, Double>>): List<Transfer> {
        val debtors = balances.filter { it.second < -0.005 }.map { it.first to -it.second }.sortedByDescending { it.second }
        val creditors = balances.filter { it.second > 0.005 }.sortedByDescending { it.second }
        val debt = debtors.map { it.second }.toMutableList()
        val credit = creditors.map { it.second }.toMutableList()
        val result = mutableListOf<Transfer>()
        var i = 0
        var j = 0
        while (i < debtors.size && j < creditors.size) {
            val amount = minOf(debt[i], credit[j])
            result += Transfer(debtors[i].first, creditors[j].first, amount)
            debt[i] -= amount
            credit[j] -= amount
            if (debt[i] < 0.005) i++
            if (credit[j] < 0.005) j++
        }
        return result
    }

    fun encode(people: List<SplitPerson>): String = Net.json.encodeToString(ListSerializer(SplitPerson.serializer()), people)

    fun decode(text: String?): List<SplitPerson>? =
        text?.let { runCatching { Net.json.decodeFromString(ListSerializer(SplitPerson.serializer()), it) }.getOrNull() }
}
