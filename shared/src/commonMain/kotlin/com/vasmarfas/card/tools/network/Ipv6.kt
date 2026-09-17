package com.vasmarfas.card.tools.network

class Ipv6Address(val groups: IntArray) {
    init {
        require(groups.size == 8)
    }

    fun expanded(): String = groups.joinToString(":") { it.toString(16).padStart(4, '0') }

    fun compressed(): String {
        var bestStart = -1
        var bestLen = 0
        var i = 0
        while (i < 8) {
            if (groups[i] == 0) {
                var j = i
                while (j < 8 && groups[j] == 0) j++
                if (j - i > bestLen) {
                    bestLen = j - i
                    bestStart = i
                }
                i = j
            } else i++
        }
        if (bestLen < 2) return groups.joinToString(":") { it.toString(16) }
        val head = groups.take(bestStart).joinToString(":") { it.toString(16) }
        val tail = groups.drop(bestStart + bestLen).joinToString(":") { it.toString(16) }
        return "$head::$tail"
    }

    fun withPrefix(prefix: Int): Ipv6Address {
        val out = IntArray(8)
        for (g in 0 until 8) {
            val bitsForGroup = (prefix - g * 16).coerceIn(0, 16)
            val mask = if (bitsForGroup == 0) 0 else (0xFFFF shl (16 - bitsForGroup)) and 0xFFFF
            out[g] = groups[g] and mask
        }
        return Ipv6Address(out)
    }

    fun lastInPrefix(prefix: Int): Ipv6Address {
        val out = IntArray(8)
        for (g in 0 until 8) {
            val bitsForGroup = (prefix - g * 16).coerceIn(0, 16)
            val hostMask = (0xFFFF shr bitsForGroup)
            out[g] = groups[g] or hostMask
        }
        return Ipv6Address(out)
    }

    val type: String
        get() = when {
            groups.all { it == 0 } -> "Unspecified (::)"
            groups.take(7).all { it == 0 } && groups[7] == 1 -> "Loopback (::1)"
            groups[0] == 0xFE80 -> "Link-local (fe80::/10)"
            (groups[0] and 0xFE00) == 0xFC00 -> "Unique local (fc00::/7)"
            (groups[0] and 0xFF00) == 0xFF00 -> "Multicast (ff00::/8)"
            groups[0] == 0x2001 && groups[1] == 0x0DB8 -> "Documentation (2001:db8::/32)"
            groups[0] == 0x2002 -> "6to4 (2002::/16)"
            groups.take(5).all { it == 0 } && groups[5] == 0xFFFF -> "IPv4-mapped (::ffff:0:0/96)"
            (groups[0] and 0xE000) == 0x2000 -> "Global unicast (2000::/3)"
            else -> "Reserved"
        }

    companion object {
        fun parse(text: String): Ipv6Address? {
            var t = text.trim().lowercase()
            if (t.startsWith("[") && t.endsWith("]")) t = t.substring(1, t.length - 1)
            if (t.isEmpty() || t.count { it == ':' } < 2) return null
            val doubleColon = t.indexOf("::")
            if (doubleColon >= 0 && t.indexOf("::", doubleColon + 1) >= 0) return null
            fun parseGroups(part: String): List<Int>? {
                if (part.isEmpty()) return emptyList()
                val items = part.split(":")
                val out = mutableListOf<Int>()
                for ((index, item) in items.withIndex()) {
                    if (item.contains('.') && index == items.lastIndex) {
                        val v4 = Ipv4.parse(item) ?: return null
                        out.add(((v4 shr 16) and 0xFFFF).toInt())
                        out.add((v4 and 0xFFFF).toInt())
                    } else {
                        if (item.isEmpty() || item.length > 4) return null
                        out.add(item.toIntOrNull(16) ?: return null)
                    }
                }
                return out
            }
            val groups: List<Int> = if (doubleColon >= 0) {
                val head = parseGroups(t.substring(0, doubleColon)) ?: return null
                val tail = parseGroups(t.substring(doubleColon + 2)) ?: return null
                if (head.size + tail.size > 7) return null
                head + List(8 - head.size - tail.size) { 0 } + tail
            } else {
                parseGroups(t)?.takeIf { it.size == 8 } ?: return null
            }
            return Ipv6Address(groups.toIntArray())
        }

        fun parseWithPrefix(text: String): Pair<Ipv6Address, Int>? {
            val t = text.trim()
            val slash = t.indexOf('/')
            val address = parse(if (slash >= 0) t.substring(0, slash) else t) ?: return null
            val prefix = if (slash >= 0) t.substring(slash + 1).trim().toIntOrNull() ?: return null else 64
            if (prefix !in 0..128) return null
            return address to prefix
        }
    }
}
