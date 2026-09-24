package com.vasmarfas.card.tools.everyday

import kotlin.test.Test
import kotlin.test.assertEquals

class TeamsTest {
    @Test
    fun namesSkipBlankLines() {
        assertEquals(listOf("Аня", "Борис", "Вера"), Teams.names("Аня\n Борис \n\nВера\n"))
    }

    @Test
    fun teamsAreEvenKeepEveryoneAndRepeatForTheSameSeed() {
        val names = listOf("Аня", "Борис", "Вера", "Глеб", "Даша")
        val teams = Teams.split(names, 2, seed = 7)
        assertEquals(names.sorted(), teams.flatten().sorted())
        assertEquals(listOf(2, 3), teams.map { it.size }.sorted())
        assertEquals(teams, Teams.split(names, 2, seed = 7))
    }
}
