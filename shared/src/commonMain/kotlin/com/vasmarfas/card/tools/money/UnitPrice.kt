package com.vasmarfas.card.tools.money

import com.vasmarfas.card.resources.*
import org.jetbrains.compose.resources.StringResource

enum class Dimension(val baseLabel: StringResource, val hundredLabel: StringResource?) {
    MASS(Res.string.unit_kg, Res.string.unit_g),
    VOLUME(Res.string.unit_l, Res.string.ml),
    COUNT(Res.string.unit_pcs, null),
}

enum class QuantityUnit(val label: StringResource, val dimension: Dimension, val toBase: Double) {
    G(Res.string.unit_g, Dimension.MASS, 0.001),
    KG(Res.string.unit_kg, Dimension.MASS, 1.0),
    ML(Res.string.ml, Dimension.VOLUME, 0.001),
    L(Res.string.unit_l, Dimension.VOLUME, 1.0),
    PCS(Res.string.unit_pcs, Dimension.COUNT, 1.0),
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
