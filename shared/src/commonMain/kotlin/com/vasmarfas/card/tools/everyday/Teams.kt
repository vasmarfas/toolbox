package com.vasmarfas.card.tools.everyday

import kotlin.random.Random

object Teams {
    fun names(text: String): List<String> = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

    fun split(names: List<String>, teams: Int, seed: Long): List<List<String>> {
        val shuffled = names.shuffled(Random(seed))
        return List(teams) { team -> shuffled.filterIndexed { index, _ -> index % teams == team } }
    }
}
