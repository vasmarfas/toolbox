package com.vasmarfas.card.tools.developer

import kotlin.math.nextDown
import kotlin.math.nextUp

enum class FloatFormat(val bits: Int, val exponentBits: Int) {
    SINGLE(32, 8),
    DOUBLE(64, 11);

    val mantissaBits: Int get() = bits - 1 - exponentBits
    val bias: Int get() = (1 shl (exponentBits - 1)) - 1
}

enum class FloatClass { ZERO, SUBNORMAL, NORMAL, INFINITE, NAN }

class FloatLayout(
    val format: FloatFormat,
    val raw: Long,
    val value: Double,
) {
    val sign: Int get() = (raw ushr (format.bits - 1)).toInt() and 1
    val exponentField: Int get() = ((raw ushr format.mantissaBits) and ((1L shl format.exponentBits) - 1)).toInt()
    val mantissaField: Long get() = raw and ((1L shl format.mantissaBits) - 1)
    val binary: String get() = raw.toULong().toString(2).padStart(format.bits, '0').takeLast(format.bits)
    val hex: String get() = raw.toULong().toString(16).uppercase().padStart(format.bits / 4, '0').takeLast(format.bits / 4)

    val kind: FloatClass get() = when {
        exponentField == (1 shl format.exponentBits) - 1 -> if (mantissaField == 0L) FloatClass.INFINITE else FloatClass.NAN
        exponentField == 0 -> if (mantissaField == 0L) FloatClass.ZERO else FloatClass.SUBNORMAL
        else -> FloatClass.NORMAL
    }

    // subnormals share the smallest normal exponent
    val exponent: Int get() = if (exponentField == 0) 1 - format.bias else exponentField - format.bias
}

object FloatBits {
    fun of(value: Double, format: FloatFormat): FloatLayout = when (format) {
        FloatFormat.SINGLE -> value.toFloat().let { FloatLayout(format, it.toRawBits().toLong() and 0xFFFF_FFFFL, it.toDouble()) }
        FloatFormat.DOUBLE -> FloatLayout(format, value.toRawBits(), value)
    }

    fun fromHex(hex: String, format: FloatFormat): FloatLayout? {
        val digits = hex.trim().removePrefix("0x").removePrefix("0X").replace("_", "")
        if (digits.isEmpty() || digits.length > format.bits / 4) return null
        val raw = digits.toULongOrNull(16)?.toLong() ?: return null
        return when (format) {
            FloatFormat.SINGLE -> FloatLayout(format, raw, Float.fromBits(raw.toInt()).toDouble())
            FloatFormat.DOUBLE -> FloatLayout(format, raw, Double.fromBits(raw))
        }
    }

    fun nextUp(layout: FloatLayout): Double = when (layout.format) {
        FloatFormat.SINGLE -> layout.value.toFloat().stepUp().toDouble()
        FloatFormat.DOUBLE -> layout.value.nextUp()
    }

    fun nextDown(layout: FloatLayout): Double = when (layout.format) {
        FloatFormat.SINGLE -> (-(-layout.value.toFloat()).stepUp()).toDouble()
        FloatFormat.DOUBLE -> layout.value.nextDown()
    }

    // Float.nextUp is JVM-only
    private fun Float.stepUp(): Float = when {
        isNaN() || this == Float.POSITIVE_INFINITY -> this
        this == 0f -> Float.fromBits(1)
        this > 0f -> Float.fromBits(toRawBits() + 1)
        else -> Float.fromBits(toRawBits() - 1)
    }

    // a finite double is m * 2^e with an integer m, for a negative e that is m * 5^-e shifted -e places
    // right. Digit arrays because common code has no BigDecimal
    fun exactDecimal(value: Double): String {
        require(value.isFinite())
        if (value == 0.0) return if (1.0 / value < 0) "-0" else "0"
        val bits = value.toRawBits()
        val exponentField = ((bits ushr 52) and 0x7FF).toInt()
        val fraction = bits and 0xF_FFFF_FFFF_FFFFL
        val mantissa = if (exponentField == 0) fraction else fraction or (1L shl 52)
        val exponent = (if (exponentField == 0) 1 else exponentField) - 1075
        val digits = mantissa.toString().map { it - '0' }.reversed().toMutableList()
        val multiplier = if (exponent >= 0) 2 else 5
        repeat(if (exponent >= 0) exponent else -exponent) { multiply(digits, multiplier) }
        val text = digits.reversed().joinToString("")
        val body = if (exponent >= 0) {
            text
        } else {
            val point = -exponent
            val padded = text.padStart(point + 1, '0')
            (padded.dropLast(point) + "." + padded.takeLast(point)).trimEnd('0').trimEnd('.')
        }
        return if (value < 0) "-$body" else body
    }

    private fun multiply(digits: MutableList<Int>, by: Int) {
        var carry = 0
        for (i in digits.indices) {
            val product = digits[i] * by + carry
            digits[i] = product % 10
            carry = product / 10
        }
        while (carry > 0) {
            digits.add(carry % 10)
            carry /= 10
        }
    }
}
