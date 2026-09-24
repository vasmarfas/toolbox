package com.vasmarfas.card.tools.design

import com.vasmarfas.card.tools.media.Quantizer

class PaletteEntry(val color: Rgba, val share: Double)

enum class PaletteFormat { HEX, CSS, JSON }

object ImagePalette {
    fun extract(pixels: IntArray, width: Int, height: Int, count: Int): List<PaletteEntry> {
        val transparent = pixels.any { it ushr 24 == 0 }
        val palette = Quantizer.palette(pixels, (count + if (transparent) 1 else 0).coerceAtMost(256))
        val indices = Quantizer.remap(pixels, width, height, palette, dither = false)
        val counts = IntArray(palette.size)
        for (index in indices) counts[index.toInt() and 0xFF]++
        val covered = counts.indices.filter { it != palette.transparentIndex && counts[it] > 0 }
        val total = covered.sumOf { counts[it] }.toDouble()
        return covered.sortedByDescending { counts[it] }.map { i ->
            val argb = palette.colors[i]
            PaletteEntry(Rgba(argb shr 16 and 0xFF, argb shr 8 and 0xFF, argb and 0xFF, argb ushr 24), counts[i] / total)
        }
    }

    fun export(entries: List<PaletteEntry>, format: PaletteFormat): String = when (format) {
        PaletteFormat.HEX -> entries.joinToString("\n") { it.color.hex() }
        PaletteFormat.CSS -> entries.withIndex().joinToString("\n", ":root {\n", "\n}") { (i, e) -> "  --color-${i + 1}: ${e.color.hex()};" }
        PaletteFormat.JSON -> entries.joinToString(", ", "[", "]") { "\"${it.color.hex()}\"" }
    }
}
