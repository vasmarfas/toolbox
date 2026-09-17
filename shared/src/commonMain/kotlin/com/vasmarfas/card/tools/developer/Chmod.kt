package com.vasmarfas.card.tools.developer

object Chmod {
    const val SUID = 0x800
    const val SGID = 0x400
    const val STICKY = 0x200
    val bits = intArrayOf(0x100, 0x80, 0x40, 0x20, 0x10, 0x8, 0x4, 0x2, 0x1)

    fun octal(mode: Int): String {
        val digits = "${(mode shr 9) and 7}${(mode shr 6) and 7}${(mode shr 3) and 7}${mode and 7}"
        return if (mode and 0xE00 != 0) digits else digits.substring(1)
    }

    fun symbolic(mode: Int): String {
        val sb = StringBuilder(9)
        for (i in 0 until 9) {
            val set = mode and bits[i] != 0
            var c = if (set) "rwx"[i % 3] else '-'
            if (i == 2 && mode and SUID != 0) c = if (set) 's' else 'S'
            if (i == 5 && mode and SGID != 0) c = if (set) 's' else 'S'
            if (i == 8 && mode and STICKY != 0) c = if (set) 't' else 'T'
            sb.append(c)
        }
        return sb.toString()
    }

    fun parseOctal(text: String): Int? {
        val t = text.trim()
        if (t.isEmpty() || t.length > 4 || !t.all { it in '0'..'7' }) return null
        return t.toInt(8)
    }

    fun parseSymbolic(text: String): Int? {
        var t = text.trim()
        if (t.length == 10 && t[0] in "-dlcbps") t = t.substring(1)
        if (t.length != 9) return null
        var mode = 0
        for (i in 0 until 9) {
            val c = t[i]
            when {
                c == '-' -> {}
                c == "rwx"[i % 3] -> mode = mode or bits[i]
                i == 2 && (c == 's' || c == 'S') -> mode = mode or SUID or (if (c == 's') bits[i] else 0)
                i == 5 && (c == 's' || c == 'S') -> mode = mode or SGID or (if (c == 's') bits[i] else 0)
                i == 8 && (c == 't' || c == 'T') -> mode = mode or STICKY or (if (c == 't') bits[i] else 0)
                else -> return null
            }
        }
        return mode
    }

    fun symbolicCommand(mode: Int): String {
        fun who(offset: Int): String = buildString {
            if (mode and bits[offset] != 0) append('r')
            if (mode and bits[offset + 1] != 0) append('w')
            if (mode and bits[offset + 2] != 0) append('x')
        }
        val parts = mutableListOf("u=${who(0)}", "g=${who(3)}", "o=${who(6)}")
        if (mode and SUID != 0) parts += "u+s"
        if (mode and SGID != 0) parts += "g+s"
        if (mode and STICKY != 0) parts += "+t"
        return parts.joinToString(",")
    }
}
