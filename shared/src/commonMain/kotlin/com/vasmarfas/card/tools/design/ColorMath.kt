package com.vasmarfas.card.tools.design

import kotlin.math.abs
import kotlin.math.roundToInt

data class Rgba(val r: Int, val g: Int, val b: Int, val a: Int = 255) {
    val argb: Int get() = (a shl 24) or (r shl 16) or (g shl 8) or b

    fun hex(withAlpha: Boolean = false): String = "#" + hex2(r) + hex2(g) + hex2(b) + (if (withAlpha) hex2(a) else "")

    val androidHex: String get() = "#" + hex2(a) + hex2(r) + hex2(g) + hex2(b)

    val composeLiteral: String get() = "Color(0x" + hex2(a) + hex2(r) + hex2(g) + hex2(b) + ")"

    val alphaFraction: Double get() = a / 255.0
}

data class Hsl(val h: Double, val s: Double, val l: Double)

data class Hsv(val h: Double, val s: Double, val v: Double)

data class Cmyk(val c: Double, val m: Double, val y: Double, val k: Double)

private fun hex2(v: Int): String = v.coerceIn(0, 255).toString(16).padStart(2, '0').uppercase()

object ColorMath {
    private val function = Regex("""^(rgba?|hsla?|hsva?|hsba?|cmyk)\((.*)\)$""")

    fun fromRgbInt(rgb: Int): Rgba = Rgba((rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF)

    fun parse(text: String): Rgba? {
        val t = text.trim().lowercase()
        if (t.isEmpty()) return null
        CssColors.byName[t.replace(" ", "")]?.let { return fromRgbInt(it) }
        parseHex(t)?.let { return it }
        val m = function.find(t) ?: return null
        val args = m.groupValues[2].replace("/", " ").split(',', ' ').map { it.trim() }.filter { it.isNotEmpty() }
        return when (m.groupValues[1]) {
            "rgb", "rgba" -> parseRgb(args)
            "hsl", "hsla" -> parseHsl(args, hsv = false)
            "hsv", "hsva", "hsb", "hsba" -> parseHsl(args, hsv = true)
            else -> parseCmyk(args)
        }
    }

    private fun parseHex(t: String): Rgba? {
        val compose = t.startsWith("0x")
        val digits = t.removePrefix("#").removePrefix("0x")
        if (digits.isEmpty() || digits.any { it !in '0'..'9' && it !in 'a'..'f' }) return null
        val full = when (digits.length) {
            3, 4 -> digits.map { "$it$it" }.joinToString("")
            6, 8 -> digits
            else -> return null
        }
        val v = full.toLong(16)
        return if (full.length == 6) {
            Rgba(((v shr 16) and 0xFF).toInt(), ((v shr 8) and 0xFF).toInt(), (v and 0xFF).toInt())
        } else if (compose) {
            Rgba(((v shr 16) and 0xFF).toInt(), ((v shr 8) and 0xFF).toInt(), (v and 0xFF).toInt(), ((v shr 24) and 0xFF).toInt())
        } else {
            Rgba(((v shr 24) and 0xFF).toInt(), ((v shr 16) and 0xFF).toInt(), ((v shr 8) and 0xFF).toInt(), (v and 0xFF).toInt())
        }
    }

    private fun number(s: String): Double? = s.removeSuffix("%").removeSuffix("deg").toDoubleOrNull()

    private fun channel(s: String): Int? {
        val n = number(s) ?: return null
        val v = if (s.endsWith("%")) n * 255 / 100 else n
        return v.roundToInt().takeIf { it in 0..255 }
    }

    private fun alpha(s: String?): Int? {
        if (s == null) return 255
        val n = number(s) ?: return null
        val v = if (s.endsWith("%")) n / 100 else n
        return (v * 255).roundToInt().takeIf { it in 0..255 }
    }

    private fun fraction(s: String): Double? = number(s)?.let { if (it > 1 || s.endsWith("%")) it / 100 else it }?.takeIf { it in 0.0..1.0 }

    private fun parseRgb(args: List<String>): Rgba? {
        if (args.size !in 3..4) return null
        val r = channel(args[0]) ?: return null
        val g = channel(args[1]) ?: return null
        val b = channel(args[2]) ?: return null
        val a = alpha(args.getOrNull(3)) ?: return null
        return Rgba(r, g, b, a)
    }

    private fun parseHsl(args: List<String>, hsv: Boolean): Rgba? {
        if (args.size !in 3..4) return null
        val h = number(args[0]) ?: return null
        val s = fraction(args[1]) ?: return null
        val l = fraction(args[2]) ?: return null
        val a = alpha(args.getOrNull(3)) ?: return null
        return if (hsv) fromHsv(Hsv(h, s, l), a) else fromHsl(Hsl(h, s, l), a)
    }

    private fun parseCmyk(args: List<String>): Rgba? {
        if (args.size != 4) return null
        val parts = args.map { fraction(it) ?: return null }
        return fromCmyk(Cmyk(parts[0], parts[1], parts[2], parts[3]))
    }

    fun toHsl(c: Rgba): Hsl {
        val r = c.r / 255.0
        val g = c.g / 255.0
        val b = c.b / 255.0
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val l = (max + min) / 2
        val d = max - min
        if (d == 0.0) return Hsl(0.0, 0.0, l)
        val s = if (l > 0.5) d / (2 - max - min) else d / (max + min)
        return Hsl(hue(r, g, b, max, d), s, l)
    }

    fun toHsv(c: Rgba): Hsv {
        val r = c.r / 255.0
        val g = c.g / 255.0
        val b = c.b / 255.0
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val d = max - min
        val s = if (max == 0.0) 0.0 else d / max
        return Hsv(if (d == 0.0) 0.0 else hue(r, g, b, max, d), s, max)
    }

    private fun hue(r: Double, g: Double, b: Double, max: Double, d: Double): Double {
        val h = when (max) {
            r -> (g - b) / d + (if (g < b) 6 else 0)
            g -> (b - r) / d + 2
            else -> (r - g) / d + 4
        }
        return h * 60
    }

    fun fromHsl(hsl: Hsl, a: Int = 255): Rgba {
        val c = (1 - abs(2 * hsl.l - 1)) * hsl.s
        val m = hsl.l - c / 2
        return fromChroma(hsl.h, c, m, a)
    }

    fun fromHsv(hsv: Hsv, a: Int = 255): Rgba {
        val c = hsv.v * hsv.s
        val m = hsv.v - c
        return fromChroma(hsv.h, c, m, a)
    }

    private fun fromChroma(h: Double, c: Double, m: Double, a: Int): Rgba {
        val hh = h.mod(360.0) / 60
        val x = c * (1 - abs(hh.mod(2.0) - 1))
        val (r1, g1, b1) = when {
            hh < 1 -> Triple(c, x, 0.0)
            hh < 2 -> Triple(x, c, 0.0)
            hh < 3 -> Triple(0.0, c, x)
            hh < 4 -> Triple(0.0, x, c)
            hh < 5 -> Triple(x, 0.0, c)
            else -> Triple(c, 0.0, x)
        }
        return Rgba(((r1 + m) * 255).roundToInt().coerceIn(0, 255), ((g1 + m) * 255).roundToInt().coerceIn(0, 255), ((b1 + m) * 255).roundToInt().coerceIn(0, 255), a)
    }

    fun toCmyk(c: Rgba): Cmyk {
        val r = c.r / 255.0
        val g = c.g / 255.0
        val b = c.b / 255.0
        val k = 1 - maxOf(r, g, b)
        if (k >= 1.0) return Cmyk(0.0, 0.0, 0.0, 1.0)
        return Cmyk((1 - r - k) / (1 - k), (1 - g - k) / (1 - k), (1 - b - k) / (1 - k), k)
    }

    fun fromCmyk(cmyk: Cmyk, a: Int = 255): Rgba = Rgba(
        (255 * (1 - cmyk.c) * (1 - cmyk.k)).roundToInt().coerceIn(0, 255),
        (255 * (1 - cmyk.m) * (1 - cmyk.k)).roundToInt().coerceIn(0, 255),
        (255 * (1 - cmyk.y) * (1 - cmyk.k)).roundToInt().coerceIn(0, 255),
        a,
    )

    fun nearestCssName(c: Rgba): Pair<String, Boolean> {
        val best = CssColors.entries.minBy { (_, rgb) ->
            val o = fromRgbInt(rgb)
            (o.r - c.r) * (o.r - c.r) + (o.g - c.g) * (o.g - c.g) + (o.b - c.b) * (o.b - c.b)
        }
        val exact = fromRgbInt(best.second) == c.copy(a = 255)
        return best.first to exact
    }

    fun rgbString(c: Rgba): String =
        if (c.a == 255) "rgb(${c.r}, ${c.g}, ${c.b})" else "rgba(${c.r}, ${c.g}, ${c.b}, ${fmtFraction(c.alphaFraction)})"

    fun hslString(c: Rgba): String {
        val hsl = toHsl(c)
        val channels = "${hsl.h.roundToInt()}, ${pct(hsl.s)}, ${pct(hsl.l)}"
        return if (c.a == 255) "hsl($channels)" else "hsla($channels, ${fmtFraction(c.alphaFraction)})"
    }

    fun hsvString(c: Rgba): String {
        val hsv = toHsv(c)
        val channels = "${hsv.h.roundToInt()}, ${pct(hsv.s)}, ${pct(hsv.v)}"
        return if (c.a == 255) "hsv($channels)" else "hsva($channels, ${fmtFraction(c.alphaFraction)})"
    }

    fun cmykString(c: Rgba): String {
        val k = toCmyk(c)
        return "cmyk(${pct(k.c)}, ${pct(k.m)}, ${pct(k.y)}, ${pct(k.k)})"
    }

    private fun pct(v: Double): String = "${(v * 100).roundToInt()}%"

    private fun fmtFraction(v: Double): String {
        val hundredths = (v * 100).roundToInt()
        return if (hundredths % 100 == 0) (hundredths / 100).toString() else "0.${hundredths.toString().padStart(2, '0').trimEnd('0')}"
    }
}
