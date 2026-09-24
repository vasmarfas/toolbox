package com.vasmarfas.card.tools.design

import kotlin.math.roundToInt

enum class PaperSeries { ISO_A, ISO_B, ISO_C, US, PHOTO, CARDS }

enum class PaperUse { ENVELOPE, BUSINESS_CARD_RU, BUSINESS_CARD_EU, BUSINESS_CARD_US, BANK_CARD, PASSPORT_PHOTO, US_PASSPORT_PHOTO }

// inches: formats defined in inches, their mm are rounded
class PaperSize(val name: String, val widthMm: Double, val heightMm: Double, val inches: Boolean = false, val use: PaperUse? = null) {
    fun widthPx(dpi: Int): Int = (widthMm / PaperSizes.MM_PER_INCH * dpi).roundToInt()

    fun heightPx(dpi: Int): Int = (heightMm / PaperSizes.MM_PER_INCH * dpi).roundToInt()
}

object PaperSizes {
    const val MM_PER_INCH = 25.4

    private fun inch(name: String, width: Double, height: Double, use: PaperUse? = null) =
        PaperSize(name, width * MM_PER_INCH, height * MM_PER_INCH, inches = true, use = use)

    private fun iso(prefix: String, sizes: List<Pair<Int, Int>>, envelopes: Set<Int> = emptySet()) =
        sizes.mapIndexed { i, (w, h) -> PaperSize("$prefix$i", w.toDouble(), h.toDouble(), use = PaperUse.ENVELOPE.takeIf { i in envelopes }) }

    val series: Map<PaperSeries, List<PaperSize>> = mapOf(
        PaperSeries.ISO_A to iso(
            "A",
            listOf(841 to 1189, 594 to 841, 420 to 594, 297 to 420, 210 to 297, 148 to 210, 105 to 148, 74 to 105, 52 to 74, 37 to 52, 26 to 37),
        ),
        PaperSeries.ISO_B to iso(
            "B",
            listOf(1000 to 1414, 707 to 1000, 500 to 707, 353 to 500, 250 to 353, 176 to 250, 125 to 176, 88 to 125, 62 to 88, 44 to 62, 31 to 44),
        ),
        PaperSeries.ISO_C to iso(
            "C",
            listOf(917 to 1297, 648 to 917, 458 to 648, 324 to 458, 229 to 324, 162 to 229, 114 to 162, 81 to 114, 57 to 81, 40 to 57, 28 to 40),
            envelopes = setOf(4, 5, 6),
        ) + PaperSize("DL", 110.0, 220.0, use = PaperUse.ENVELOPE),
        PaperSeries.US to listOf(
            inch("Letter", 8.5, 11.0),
            inch("Legal", 8.5, 14.0),
            inch("Tabloid", 11.0, 17.0),
            inch("Executive", 7.25, 10.5),
            inch("Half Letter", 5.5, 8.5),
            inch("Junior Legal", 5.0, 8.0),
        ),
        PaperSeries.PHOTO to listOf(
            inch("9×13", 3.5, 5.0),
            inch("10×15", 4.0, 6.0),
            inch("13×18", 5.0, 7.0),
            inch("15×20", 6.0, 8.0),
            inch("20×30", 8.0, 12.0),
            inch("30×40", 12.0, 16.0),
        ),
        PaperSeries.CARDS to listOf(
            PaperSize("90×50", 50.0, 90.0, use = PaperUse.BUSINESS_CARD_RU),
            PaperSize("85×55", 55.0, 85.0, use = PaperUse.BUSINESS_CARD_EU),
            inch("3.5×2″", 2.0, 3.5, PaperUse.BUSINESS_CARD_US),
            PaperSize("ID-1", 53.98, 85.6, use = PaperUse.BANK_CARD),
            PaperSize("35×45", 35.0, 45.0, use = PaperUse.PASSPORT_PHOTO),
            inch("2×2″", 2.0, 2.0, PaperUse.US_PASSPORT_PHOTO),
        ),
    )
}
