package com.vasmarfas.card.tools.everyday

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PersistedToolsTest {
    @Test
    fun countersRoundTrip() {
        val counters = listOf(Counter("Coffee", 3), Counter("Push-ups"))
        assertEquals(counters, Tally.decode(Tally.encode(counters)))
        assertNull(Tally.decode("not json"))
        assertNull(Tally.decode(null))
    }

    @Test
    fun textStatistics() {
        val s = textStats("Hello,  world\nsecond line")
        assertEquals(25, s.chars)
        assertEquals(21, s.charsNoSpaces)
        assertEquals(4, s.words)
        assertEquals(2, s.lines)
        assertEquals(TextStats(0, 0, 0, 0), textStats(""))
    }
}
