package com.vasmarfas.card.tools.everyday

import kotlin.random.Random

data class DiceTerm(val count: Int, val sides: Int)

data class DiceExpr(val terms: List<DiceTerm>, val modifier: Int) {
    val notation: String
        get() = terms.joinToString("+") { "${it.count}d${it.sides}" } + when {
            modifier > 0 -> "+$modifier"
            modifier < 0 -> "$modifier"
            else -> ""
        }
}

data class RollResult(val expr: DiceExpr, val rolls: List<List<Int>>) {
    val total: Int get() = rolls.sumOf { it.sum() } + expr.modifier
    val detail: String
        get() = rolls.joinToString(" + ") { it.joinToString(", ", "[", "]") } + when {
            expr.modifier > 0 -> " + ${expr.modifier}"
            expr.modifier < 0 -> " − ${-expr.modifier}"
            else -> ""
        }
}

object Dice {
    private val term = Regex("""^(\d*)d(\d+)$""")
    private val token = Regex("""[+-]?[^+-]+""")

    fun parse(text: String): DiceExpr? {
        val cleaned = text.lowercase().replace(" ", "")
        if (cleaned.isEmpty()) return null
        val tokens = token.findAll(cleaned).map { it.value }.toList()
        if (tokens.joinToString("") != cleaned) return null
        val terms = mutableListOf<DiceTerm>()
        var modifier = 0
        for (t in tokens) {
            val negative = t.startsWith("-")
            val body = t.trimStart('+', '-')
            val m = term.find(body)
            if (m != null) {
                if (negative) return null
                val count = m.groupValues[1].ifEmpty { "1" }.toIntOrNull() ?: return null
                val sides = m.groupValues[2].toIntOrNull() ?: return null
                if (count !in 1..100 || sides !in 2..1000) return null
                terms += DiceTerm(count, sides)
            } else {
                val n = body.toIntOrNull() ?: return null
                modifier += if (negative) -n else n
            }
        }
        if (terms.isEmpty()) return null
        return DiceExpr(terms, modifier)
    }

    fun roll(expr: DiceExpr, random: Random = Random.Default): RollResult =
        RollResult(expr, expr.terms.map { t -> List(t.count) { random.nextInt(1, t.sides + 1) } })
}
