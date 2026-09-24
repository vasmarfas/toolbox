package com.vasmarfas.card.tools.design

import kotlin.test.Test
import kotlin.test.assertEquals

class PaperSizesTest {
    private fun size(series: PaperSeries, name: String) = PaperSizes.series.getValue(series).first { it.name == name }

    @Test
    fun pixelsAtCommonResolutions() {
        val a4 = size(PaperSeries.ISO_A, "A4")
        assertEquals(2480 to 3508, a4.widthPx(300) to a4.heightPx(300))
        assertEquals(595 to 842, a4.widthPx(72) to a4.heightPx(72))
        val letter = size(PaperSeries.US, "Letter")
        assertEquals(2550 to 3300, letter.widthPx(300) to letter.heightPx(300))
        val photo = size(PaperSeries.PHOTO, "10×15")
        assertEquals(1200 to 1800, photo.widthPx(300) to photo.heightPx(300))
    }

    @Test
    fun isoSeriesHalveFromSizeToSize() {
        for (series in listOf(PaperSeries.ISO_A, PaperSeries.ISO_B, PaperSeries.ISO_C)) {
            val sizes = PaperSizes.series.getValue(series).filter { it.name.drop(1).toIntOrNull() != null }
            assertEquals(11, sizes.size)
            sizes.zipWithNext().forEach { (bigger, smaller) ->
                assertEquals(bigger.widthMm, smaller.heightMm, "${bigger.name} → ${smaller.name}")
            }
        }
    }
}
