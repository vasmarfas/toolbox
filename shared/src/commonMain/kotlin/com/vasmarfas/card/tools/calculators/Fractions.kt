package com.vasmarfas.card.tools.calculators

import kotlin.math.abs

// kept as written, 3/6 stays 3/6 until reduced() so the solution can show every step
data class Fraction(val numerator: Long, val denominator: Long) {
    init {
        require(denominator > 0) { "denominator must be positive" }
    }

    val isWhole: Boolean get() = numerator % denominator == 0L

    fun reduced(): Fraction {
        val g = Fractions.gcd(numerator, denominator)
        return if (g <= 1) this else Fraction(numerator / g, denominator / g)
    }

    override fun toString(): String = if (denominator == 1L) "$numerator" else "$numerator/$denominator"

    fun mixed(): String {
        val whole = numerator / denominator
        val rest = abs(numerator % denominator)
        return when {
            rest == 0L -> "$whole"
            whole == 0L -> toString()
            else -> "$whole $rest/$denominator"
        }
    }
}

enum class FractionOp(val symbol: String) { ADD("+"), SUBTRACT("−"), MULTIPLY("×"), DIVIDE("÷"), COMPARE("?") }

sealed interface FractionStep {
    class FromDecimal(val text: String, val fraction: Fraction) : FractionStep
    class ToImproper(val mixed: String, val improper: Fraction) : FractionStep
    class CommonDenominator(val left: Fraction, val right: Fraction, val lcm: Long, val leftScaled: Fraction, val rightScaled: Fraction) : FractionStep
    class Combine(val op: FractionOp, val left: Fraction, val right: Fraction, val result: Fraction) : FractionStep
    class Compare(val left: Fraction, val right: Fraction, val sign: Int) : FractionStep
    class Flip(val left: Fraction, val divisor: Fraction, val flipped: Fraction) : FractionStep
    class Multiply(val left: Fraction, val right: Fraction, val result: Fraction) : FractionStep
    class Reduce(val from: Fraction, val gcd: Long, val to: Fraction) : FractionStep
    class WholePart(val fraction: Fraction) : FractionStep
}

// value is null for a comparison, comparison is the sign of left minus right
class FractionSolution(val value: Fraction?, val comparison: Int?, val steps: List<FractionStep>)

class DecimalExpansion(val negative: Boolean, val whole: Long, val fixed: String, val repeating: String, val cut: Boolean) {
    override fun toString(): String {
        val sign = if (negative) "-" else ""
        val tail = when {
            repeating.isNotEmpty() -> "$fixed($repeating)"
            cut -> "$fixed…"
            else -> fixed
        }
        return if (tail.isEmpty()) "$sign$whole" else "$sign$whole.$tail"
    }
}

object Fractions {
    private val decimalPattern = Regex("""([+-]?)(\d*)(?:[.,](\d*)(?:\((\d+)\))?)?""")

    fun gcd(a: Long, b: Long): Long {
        var x = abs(a)
        var y = abs(b)
        while (y != 0L) {
            val t = x % y
            x = y
            y = t
        }
        return x
    }

    fun lcm(a: Long, b: Long): Long = times(abs(a) / gcd(a, b), abs(b))

    fun times(a: Long, b: Long): Long {
        val r = a * b
        if (a != 0L && (r / a != b || a == -1L && b == Long.MIN_VALUE)) throw ArithmeticException("overflow")
        return r
    }

    private fun plus(a: Long, b: Long): Long {
        val r = a + b
        if ((a xor r) and (b xor r) < 0) throw ArithmeticException("overflow")
        return r
    }

    // the sign of the whole part covers the fraction: -2 1/3 is -7/3
    fun mixed(whole: Long, numerator: Long, denominator: Long): Fraction? {
        if (denominator == 0L) return null
        val sign = if (denominator < 0) -1 else 1
        val num = numerator * sign
        val den = abs(denominator)
        if (whole == 0L) return Fraction(num, den)
        val magnitude = plus(times(abs(whole), den), abs(num))
        return Fraction(if (whole < 0 || num < 0) -magnitude else magnitude, den)
    }

    fun solve(left: Fraction, op: FractionOp, right: Fraction, leftMixed: String? = null, rightMixed: String? = null): FractionSolution {
        val steps = mutableListOf<FractionStep>()
        leftMixed?.let { steps += FractionStep.ToImproper(it, left) }
        rightMixed?.let { steps += FractionStep.ToImproper(it, right) }
        val raw = when (op) {
            FractionOp.ADD, FractionOp.SUBTRACT, FractionOp.COMPARE -> {
                val (a, b) = if (left.denominator == right.denominator) {
                    left to right
                } else {
                    val common = lcm(left.denominator, right.denominator)
                    val a = Fraction(times(left.numerator, common / left.denominator), common)
                    val b = Fraction(times(right.numerator, common / right.denominator), common)
                    steps += FractionStep.CommonDenominator(left, right, common, a, b)
                    a to b
                }
                if (op == FractionOp.COMPARE) {
                    val sign = a.numerator.compareTo(b.numerator)
                    steps += FractionStep.Compare(a, b, sign)
                    return FractionSolution(null, sign, steps)
                }
                val numerator = if (op == FractionOp.ADD) plus(a.numerator, b.numerator) else plus(a.numerator, -b.numerator)
                Fraction(numerator, a.denominator).also { steps += FractionStep.Combine(op, a, b, it) }
            }
            FractionOp.MULTIPLY -> multiply(left, right, steps)
            FractionOp.DIVIDE -> {
                val flipped = if (right.numerator < 0) Fraction(-right.denominator, -right.numerator) else Fraction(right.denominator, right.numerator)
                steps += FractionStep.Flip(left, right, flipped)
                multiply(left, flipped, steps)
            }
        }
        val result = raw.reduced()
        if (result != raw) steps += FractionStep.Reduce(raw, gcd(raw.numerator, raw.denominator), result)
        if (abs(result.numerator) > result.denominator && !result.isWhole) steps += FractionStep.WholePart(result)
        return FractionSolution(result, null, steps)
    }

    fun simplify(fraction: Fraction, mixed: String? = null): FractionSolution {
        val steps = mutableListOf<FractionStep>()
        mixed?.let { steps += FractionStep.ToImproper(it, fraction) }
        val result = fraction.reduced()
        if (result != fraction) steps += FractionStep.Reduce(fraction, gcd(fraction.numerator, fraction.denominator), result)
        if (abs(result.numerator) > result.denominator && !result.isWhole) steps += FractionStep.WholePart(result)
        return FractionSolution(result, null, steps)
    }

    fun fromDecimal(text: String): FractionSolution? {
        val raw = parseDecimal(text) ?: return null
        val simplified = simplify(raw)
        return FractionSolution(simplified.value, null, listOf(FractionStep.FromDecimal(text.trim(), raw)) + simplified.steps)
    }

    private fun multiply(left: Fraction, right: Fraction, steps: MutableList<FractionStep>): Fraction =
        Fraction(times(left.numerator, right.numerator), times(left.denominator, right.denominator))
            .also { steps += FractionStep.Multiply(left, right, it) }

    fun expand(fraction: Fraction, maxDigits: Int = 60): DecimalExpansion {
        val n = abs(fraction.numerator)
        val d = fraction.denominator
        var remainder = n % d
        val digits = StringBuilder()
        val seen = mutableMapOf<Long, Int>()
        var periodStart = -1
        while (remainder != 0L && digits.length < maxDigits) {
            val at = seen[remainder]
            if (at != null) {
                periodStart = at
                break
            }
            seen[remainder] = digits.length
            val scaled = times(remainder, 10)
            digits.append(scaled / d)
            remainder = scaled % d
        }
        val negative = fraction.numerator < 0
        return if (periodStart >= 0) {
            DecimalExpansion(negative, n / d, digits.substring(0, periodStart), digits.substring(periodStart), cut = false)
        } else {
            DecimalExpansion(negative, n / d, digits.toString(), "", cut = remainder != 0L)
        }
    }

    // 0.375, -1,25, 0.(3), 1.2(34): the period goes in brackets
    fun parseDecimal(text: String): Fraction? {
        val match = decimalPattern.matchEntire(text.trim().replace(" ", "")) ?: return null
        val (sign, wholeDigits, fixedDigits, periodDigits) = match.destructured
        if (wholeDigits.isEmpty() && fixedDigits.isEmpty() && periodDigits.isEmpty()) return null
        if (wholeDigits.length + fixedDigits.length + periodDigits.length > 17) throw ArithmeticException("overflow")
        val whole = wholeDigits.toLongOrNull() ?: 0L
        val fixedScale = pow10(fixedDigits.length)
        val fixedValue = plus(times(whole, fixedScale), fixedDigits.toLongOrNull() ?: 0L)
        val fraction = if (periodDigits.isEmpty()) {
            Fraction(fixedValue, fixedScale)
        } else {
            val nines = pow10(periodDigits.length) - 1
            Fraction(plus(times(fixedValue, nines), periodDigits.toLong()), times(fixedScale, nines))
        }
        return if (sign == "-") Fraction(-fraction.numerator, fraction.denominator) else fraction
    }

    private fun pow10(n: Int): Long {
        var r = 1L
        repeat(n) { r = times(r, 10) }
        return r
    }

    fun approximate(fraction: Fraction, maxDenominator: Long): Fraction {
        val reduced = fraction.reduced()
        if (reduced.denominator <= maxDenominator) return reduced
        val negative = reduced.numerator < 0
        var n = abs(reduced.numerator)
        var d = reduced.denominator
        var p0 = 0L
        var q0 = 1L
        var p1 = 1L
        var q1 = 0L
        while (true) {
            val a = n / d
            val q2 = q0 + a * q1
            if (q2 > maxDenominator) break
            val p2 = p0 + a * p1
            p0 = p1
            q0 = q1
            p1 = p2
            q1 = q2
            val t = n - a * d
            n = d
            d = t
        }
        val k = (maxDenominator - q0) / q1
        val lower = Fraction(p0 + k * p1, q0 + k * q1)
        val upper = Fraction(p1, q1)
        val target = Fraction(abs(reduced.numerator), reduced.denominator)
        val best = if (distance(upper, target) <= distance(lower, target)) upper else lower
        return if (negative) Fraction(-best.numerator, best.denominator) else best
    }

    private fun distance(a: Fraction, b: Fraction): Double = abs(a.numerator.toDouble() / a.denominator - b.numerator.toDouble() / b.denominator)
}
