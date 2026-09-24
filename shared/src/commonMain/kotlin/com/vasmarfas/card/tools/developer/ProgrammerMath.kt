package com.vasmarfas.card.tools.developer

enum class NumBase(val radix: Int, val label: String) {
    BIN(2, "BIN"),
    OCT(8, "OCT"),
    DEC(10, "DEC"),
    HEX(16, "HEX"),
}

enum class BitOp(val symbol: String, val unary: Boolean = false) {
    AND("AND"),
    OR("OR"),
    XOR("XOR"),
    NOT("NOT", true),
    SHL("<<"),
    SHR(">>>"),
    SAR(">>"),
    ROL("ROL"),
    ROR("ROR"),
    ADD("+"),
    SUB("−"),
    MUL("×"),
    DIV("÷"),
    MOD("MOD"),
}

object ProgrammerMath {
    val wordSizes = listOf(8, 16, 32, 64)

    fun mask(bits: Int): Long = if (bits >= 64) -1L else (1L shl bits) - 1

    fun truncate(value: Long, bits: Int): Long = value and mask(bits)

    fun signed(value: Long, bits: Int): Long {
        if (bits >= 64) return value
        val v = truncate(value, bits)
        return if (v and (1L shl (bits - 1)) != 0L) v - (1L shl bits) else v
    }

    fun parse(text: String, base: NumBase, bits: Int): Long? {
        var s = text.trim().replace(" ", "").replace("_", "").lowercase()
        if (s.isEmpty()) return null
        val negative = s.startsWith("-")
        if (negative) s = s.substring(1)
        val prefix = when (base) {
            NumBase.BIN -> "0b"
            NumBase.OCT -> "0o"
            NumBase.HEX -> "0x"
            NumBase.DEC -> null
        }
        if (prefix != null && s.startsWith(prefix)) s = s.substring(2)
        if (s.isEmpty()) return null
        val unsigned = s.toULongOrNull(base.radix) ?: return null
        if (bits < 64) {
            val limit = if (negative) 1UL shl (bits - 1) else mask(bits).toULong()
            if (unsigned > limit) return null
        }
        val value = unsigned.toLong()
        return truncate(if (negative) -value else value, bits)
    }

    fun format(value: Long, base: NumBase, bits: Int): String = when (base) {
        NumBase.BIN -> truncate(value, bits).toULong().toString(2).padStart(bits, '0')
        NumBase.OCT -> truncate(value, bits).toULong().toString(8)
        NumBase.DEC -> signed(value, bits).toString()
        NumBase.HEX -> truncate(value, bits).toULong().toString(16).uppercase().padStart(bits / 4, '0')
    }

    fun unsignedDecimal(value: Long, bits: Int): String = truncate(value, bits).toULong().toString()

    fun grouped(text: String, size: Int): String = text.reversed().chunked(size).joinToString(" ").reversed()

    fun apply(op: BitOp, a: Long, b: Long, bits: Int): Long? {
        val count = truncate(b, bits)
        val result = when (op) {
            BitOp.AND -> a and b
            BitOp.OR -> a or b
            BitOp.XOR -> a xor b
            BitOp.NOT -> a.inv()
            BitOp.SHL -> if (count >= bits) 0L else a shl count.toInt()
            BitOp.SHR -> if (count >= bits) 0L else truncate(a, bits) ushr count.toInt()
            BitOp.SAR -> if (count >= bits) (if (signed(a, bits) < 0) -1L else 0L) else signed(a, bits) shr count.toInt()
            BitOp.ROL -> rotateLeft(a, (count % bits).toInt(), bits)
            BitOp.ROR -> rotateLeft(a, ((bits - count % bits) % bits).toInt(), bits)
            BitOp.ADD -> a + b
            BitOp.SUB -> a - b
            BitOp.MUL -> a * b
            BitOp.DIV -> if (signed(b, bits) == 0L) return null else signed(a, bits) / signed(b, bits)
            BitOp.MOD -> if (signed(b, bits) == 0L) return null else signed(a, bits) % signed(b, bits)
        }
        return truncate(result, bits)
    }

    private fun rotateLeft(value: Long, count: Int, bits: Int): Long {
        val v = truncate(value, bits)
        if (count == 0) return v
        return truncate((v shl count) or (v ushr (bits - count)), bits)
    }
}
