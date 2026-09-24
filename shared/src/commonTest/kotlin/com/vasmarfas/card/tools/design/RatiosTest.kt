package com.vasmarfas.card.tools.design

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RatiosTest {
    @Test
    fun ratios() {
        assertEquals(16 to 9, Ratios.simplify(1920, 1080))
        assertEquals(4 to 3, Ratios.simplify(1024, 768))
        assertEquals("16:9", Ratios.closest(1920.0 / 1080))
        assertEquals("9:19.5", Ratios.closest(1080.0 / 2340))
        assertNull(Ratios.closest(1.42))
    }

    @Test
    fun densityAndPpi() {
        assertEquals(144.0, Ratios.dpToPx(48.0, 3.0), 1e-9)
        assertEquals(48.0, Ratios.pxToDp(144.0, 3.0), 1e-9)
        assertEquals(63.0, Ratios.spToPx(14.0, 3.0, 1.5), 1e-9)
        assertEquals(14.0, Ratios.pxToSp(63.0, 3.0, 1.5), 1e-9)
        assertEquals(440.6, Ratios.ppi(1080, 1920, 5.0), 0.1)
        assertEquals("xxhdpi", Ratios.bucket(2.75))
    }
}
