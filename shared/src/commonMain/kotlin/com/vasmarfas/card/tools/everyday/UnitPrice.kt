package com.vasmarfas.card.tools.everyday

enum class Dimension(val baseLabel: String) {
    MASS("kg"),
    VOLUME("l"),
    COUNT("pc"),
}

enum class QuantityUnit(val label: String, val dimension: Dimension, val toBase: Double) {
    G("g", Dimension.MASS, 0.001),
    KG("kg", Dimension.MASS, 1.0),
    ML("ml", Dimension.VOLUME, 0.001),
    L("l", Dimension.VOLUME, 1.0),
    PCS("pcs", Dimension.COUNT, 1.0),
}

data class PriceItem(val name: String = "", val price: String = "", val quantity: String = "", val unit: QuantityUnit = QuantityUnit.G)

data class UnitPriceResult(val index: Int, val perBase: Double, val dimension: Dimension)

object UnitPrice {
    fun perBaseUnit(price: Double, quantity: Double, unit: QuantityUnit): Double? =
        if (price < 0 || quantity <= 0) null else price / (quantity * unit.toBase)

    fun cheapest(results: List<UnitPriceResult>): Map<Dimension, UnitPriceResult> =
        results.groupBy { it.dimension }.mapValues { (_, list) -> list.minBy { it.perBase } }

    fun extraPercent(value: Double, best: Double): Double = if (best <= 0) 0.0 else (value - best) / best * 100
}
