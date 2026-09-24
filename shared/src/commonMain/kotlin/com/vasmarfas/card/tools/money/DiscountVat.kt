package com.vasmarfas.card.tools.money

class VatResult(val net: Double, val vat: Double, val gross: Double)

object DiscountVat {
    fun discounted(price: Double, percent: Double): Double = price * (1 - percent / 100)

    fun addVat(net: Double, rate: Double): VatResult = VatResult(net, net * rate / 100, net * (1 + rate / 100))

    fun removeVat(gross: Double, rate: Double): VatResult {
        val net = gross / (1 + rate / 100)
        return VatResult(net, gross - net, gross)
    }
}
