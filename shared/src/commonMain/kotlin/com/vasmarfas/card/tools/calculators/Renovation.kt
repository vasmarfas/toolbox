package com.vasmarfas.card.tools.calculators

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

class WallpaperPlan(val rolls: Int, val strips: Int, val stripsPerRoll: Int, val stripLength: Double)

class PaintPlan(val liters: Double, val cans: Int)

class TilePlan(val tiles: Int, val boxes: Int?, val groutKg: Double)

// meters unless the name says otherwise, every count rounded up
object Renovation {
    const val TRIM_ALLOWANCE = 0.1

    private const val GROUT_DENSITY = 1.6

    private fun up(value: Double): Int = ceil(value - 1e-9).toInt()

    fun perimeter(length: Double, width: Double): Double = 2 * (length + width)

    fun wallArea(length: Double, width: Double, height: Double, openings: Double): Double =
        max(0.0, perimeter(length, width) * height - openings)

    // with a pattern every strip has to start at the same point of it, so a strip takes the height
    // rounded up to whole repeats
    fun wallpaper(perimeter: Double, height: Double, rollWidth: Double, rollLength: Double, repeat: Double): WallpaperPlan? {
        if (perimeter <= 0 || height <= 0 || rollWidth <= 0 || rollLength <= 0) return null
        val needed = height + TRIM_ALLOWANCE
        val stripLength = if (repeat > 0) up(needed / repeat) * repeat else needed
        val stripsPerRoll = floor(rollLength / stripLength + 1e-9).toInt()
        if (stripsPerRoll == 0) return null
        val strips = up(perimeter / rollWidth)
        return WallpaperPlan(up(strips.toDouble() / stripsPerRoll), strips, stripsPerRoll, stripLength)
    }

    fun paint(area: Double, coats: Int, coveragePerLiter: Double, canLiters: Double): PaintPlan? {
        if (area <= 0 || coats <= 0 || coveragePerLiter <= 0 || canLiters <= 0) return null
        val liters = area * coats / coveragePerLiter
        return PaintPlan(liters, up(liters / canLiters))
    }

    fun packs(area: Double, packArea: Double, wastePercent: Double): Int? =
        if (area <= 0 || packArea <= 0) null else up(area * (1 + wastePercent / 100) / packArea)

    fun skirtingPieces(perimeter: Double, doorways: Double, pieceLength: Double): Int? =
        if (pieceLength <= 0 || perimeter - doorways <= 0) null else up((perimeter - doorways) / pieceLength)

    // the usual maker formula: (length + width) / (length * width) * thickness * joint * 1.6 kg/m2, sizes in mm
    fun tiles(area: Double, lengthMm: Double, widthMm: Double, jointMm: Double, thicknessMm: Double, wastePercent: Double, perBox: Int?): TilePlan? {
        if (area <= 0 || lengthMm <= 0 || widthMm <= 0 || jointMm < 0) return null
        val tileArea = (lengthMm + jointMm) * (widthMm + jointMm) / 1_000_000
        val tiles = up(area / tileArea * (1 + wastePercent / 100))
        val grout = area * (lengthMm + widthMm) / (lengthMm * widthMm) * thicknessMm * jointMm * GROUT_DENSITY
        return TilePlan(tiles, perBox?.takeIf { it > 0 }?.let { up(tiles.toDouble() / it) }, grout)
    }
}
