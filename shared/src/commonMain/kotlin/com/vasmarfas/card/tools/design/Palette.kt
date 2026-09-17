package com.vasmarfas.card.tools.design

object Palette {
    private fun rotate(base: Rgba, degrees: Double): Rgba {
        val hsl = ColorMath.toHsl(base)
        return ColorMath.fromHsl(Hsl((hsl.h + degrees).mod(360.0), hsl.s, hsl.l), base.a)
    }

    fun complementary(base: Rgba): List<Rgba> = listOf(base, rotate(base, 180.0))

    fun analogous(base: Rgba): List<Rgba> = listOf(rotate(base, -30.0), base, rotate(base, 30.0))

    fun triadic(base: Rgba): List<Rgba> = listOf(base, rotate(base, 120.0), rotate(base, 240.0))

    fun tetradic(base: Rgba): List<Rgba> = listOf(base, rotate(base, 90.0), rotate(base, 180.0), rotate(base, 270.0))

    fun splitComplementary(base: Rgba): List<Rgba> = listOf(base, rotate(base, 150.0), rotate(base, 210.0))

    fun tints(base: Rgba, count: Int = 5): List<Rgba> {
        val hsl = ColorMath.toHsl(base)
        return (1..count).map { ColorMath.fromHsl(Hsl(hsl.h, hsl.s, hsl.l + (1 - hsl.l) * it / (count + 1.0)), base.a) }
    }

    fun shades(base: Rgba, count: Int = 5): List<Rgba> {
        val hsl = ColorMath.toHsl(base)
        return (1..count).map { ColorMath.fromHsl(Hsl(hsl.h, hsl.s, hsl.l * (1 - it / (count + 1.0))), base.a) }
    }

    fun tonalRamp(base: Rgba, steps: Int = 10): List<Pair<Int, Rgba>> {
        val hsl = ColorMath.toHsl(base)
        return (1..steps).map { i ->
            val tone = i * 100 / (steps + 1)
            tone to ColorMath.fromHsl(Hsl(hsl.h, hsl.s, 1 - tone / 100.0), base.a)
        }
    }
}
