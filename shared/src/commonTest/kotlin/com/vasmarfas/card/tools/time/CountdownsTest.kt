package com.vasmarfas.card.tools.time

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CountdownsTest {
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
