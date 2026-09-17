package com.vasmarfas.card.tools.everyday

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    @Test
    fun countdownRemaining() {
        val r = Countdowns.remaining(1_000_000 + 90_061, 1_000_000)
        assertEquals(1, r.days)
        assertEquals(1, r.hours)
        assertEquals(1, r.minutes)
        assertFalse(r.past)
        assertTrue(Countdowns.remaining(10, 20).past)
        val events = listOf(CountdownEvent("New Year", 1_798_675_200))
        assertEquals(events, Countdowns.decode(Countdowns.encode(events)))
    }
}
