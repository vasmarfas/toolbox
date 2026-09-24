package com.vasmarfas.card.tools.documents

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PageRangesTest {
    @Test
    fun numbersAndRangesKeepTheOrderGiven() {
        assertEquals(listOf(0, 1, 2, 6), parsePages("1-3, 7", 10))
        assertEquals(listOf(6, 0), parsePages("7 1", 10))
        assertEquals(listOf(7, 8, 9), parsePages("8-", 10))
        assertEquals(listOf(0, 1), parsePages("-2", 10))
        assertEquals(listOf(1, 2, 3), parsePages("2-3; 3-4", 10))
        assertEquals(emptyList(), parsePages("  ", 10))
    }

    @Test
    fun anythingOutsideTheDocumentIsRejected() {
        assertNull(parsePages("0", 10))
        assertNull(parsePages("11", 10))
        assertNull(parsePages("5-3", 10))
        assertNull(parsePages("1-x", 10))
        assertNull(parsePages("два", 10))
    }
}
