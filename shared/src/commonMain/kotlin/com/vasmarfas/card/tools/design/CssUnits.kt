package com.vasmarfas.card.tools.design

enum class AndroidDensity(val label: String, val scale: Double) {
    MDPI("mdpi ×1", 1.0),
    HDPI("hdpi ×1.5", 1.5),
    XHDPI("xhdpi ×2", 2.0),
    XXHDPI("xxhdpi ×3", 3.0),
    XXXHDPI("xxxhdpi ×4", 4.0),
}

object CssUnits {
    const val PT_PER_PX = 0.75

    fun pxToRem(px: Double, rootPx: Double): Double = px / rootPx

    fun remToPx(rem: Double, rootPx: Double): Double = rem * rootPx

    fun pxToPt(px: Double): Double = px * PT_PER_PX

    fun ptToPx(pt: Double): Double = pt / PT_PER_PX

    fun pxToPercent(px: Double, parentPx: Double): Double = px / parentPx * 100

    fun dpToPx(dp: Double, density: AndroidDensity): Double = dp * density.scale
}
