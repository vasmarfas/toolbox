package com.vasmarfas.card.tools.design

object Ean13 {
    fun checksum(payload: String): Int? {
        if (payload.any { !it.isDigit() }) return null
        var sum = 0
        payload.reversed().forEachIndexed { i, c ->
            sum += (c - '0') * if (i % 2 == 0) 3 else 1
        }
        return (10 - sum % 10) % 10
    }

    fun verify(code: String): Boolean {
        if (code.length < 2 || code.any { !it.isDigit() }) return false
        return checksum(code.dropLast(1)) == code.last() - '0'
    }

    fun complete(payload: String): String? = checksum(payload)?.let { payload + it }
}
