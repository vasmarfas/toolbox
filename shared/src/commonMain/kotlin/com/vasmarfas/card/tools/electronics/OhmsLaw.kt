package com.vasmarfas.card.tools.electronics

import kotlin.math.sqrt

class OhmsResult(val voltage: Double, val current: Double, val resistance: Double, val power: Double)

object OhmsLaw {
    fun solve(v: Double?, i: Double?, r: Double?, p: Double?): OhmsResult? {
        if (listOfNotNull(v, i, r, p).size != 2) return null
        return when {
            v != null && i != null -> OhmsResult(v, i, v / i, v * i)
            v != null && r != null -> OhmsResult(v, v / r, r, v * v / r)
            v != null && p != null -> OhmsResult(v, p / v, v * v / p, p)
            i != null && r != null -> OhmsResult(i * r, i, r, i * i * r)
            i != null && p != null -> OhmsResult(p / i, i, p / (i * i), p)
            else -> {
                val power = p!!
                val resistance = r!!
                OhmsResult(sqrt(power * resistance), sqrt(power / resistance), resistance, power)
            }
        }
    }
}
