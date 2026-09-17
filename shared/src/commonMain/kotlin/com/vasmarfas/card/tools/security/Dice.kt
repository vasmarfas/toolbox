package com.vasmarfas.card.tools.security

data class DiceRoll(val notation: String, val rolls: List<Int>, val modifier: Int) {
    val total: Int get() = rolls.sum() + modifier
}

object Dice {
    private val notation = Regex("^(\\d*)[dD](\\d+)([+-]\\d+)?$")

    fun roll(text: String): DiceRoll? {
        val m = notation.find(text.trim().replace(" ", "")) ?: return null
        val count = if (m.groupValues[1].isEmpty()) 1 else m.groupValues[1].toIntOrNull() ?: return null
        val sides = m.groupValues[2].toIntOrNull() ?: return null
        if (count !in 1..100 || sides !in 2..1000) return null
        val modifier = m.groupValues[3].takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 0
        return DiceRoll(text.trim(), List(count) { PasswordGen.randomInt(sides) + 1 }, modifier)
    }

    fun intInRange(from: Int, to: Int): Int = from + PasswordGen.randomInt(to - from + 1)

    fun uniqueInts(from: Int, to: Int, count: Int): List<Int>? {
        val size = to - from + 1
        if (count > size) return null
        val pool = (from..to).toMutableList()
        val out = mutableListOf<Int>()
        repeat(count) {
            val index = PasswordGen.randomInt(pool.size)
            out += pool.removeAt(index)
        }
        return out
    }

    fun <T> pick(items: List<T>): T? = if (items.isEmpty()) null else items[PasswordGen.randomInt(items.size)]

    fun <T> shuffled(items: List<T>): List<T> {
        val copy = items.toMutableList()
        PasswordGen.shuffle(copy)
        return copy
    }
}
