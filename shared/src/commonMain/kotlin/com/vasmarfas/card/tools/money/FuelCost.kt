package com.vasmarfas.card.tools.money

import com.vasmarfas.card.resources.*
import org.jetbrains.compose.resources.StringResource

enum class ConsumptionUnit(val symbol: StringResource) {
    L100KM(Res.string.unit_l_100km),
    KML(Res.string.unit_km_l),
    MPG_US(Res.string.unit_mpg_us),
    MPG_UK(Res.string.unit_mpg_uk),
}

enum class DistanceUnit(val symbol: StringResource, val km: Double) {
    KM(Res.string.unit_km, 1.0),
    MI(Res.string.unit_mi, 1.609344),
}

enum class PriceUnit(val symbol: StringResource, val litres: Double) {
    LITRE(Res.string.price_per_liter, 1.0),
    GALLON_US(Res.string.price_per_gallon_us, 3.785411784),
    GALLON_UK(Res.string.price_per_gallon_uk, 4.54609),
}

class FuelResult(val distanceKm: Double, val litres: Double, val cost: Double, val perPerson: Double, val costPer100km: Double)

object FuelCost {
    fun litresPer100km(value: Double, unit: ConsumptionUnit): Double = when (unit) {
        ConsumptionUnit.L100KM -> value
        ConsumptionUnit.KML -> 100 / value
        ConsumptionUnit.MPG_US -> 235.214583 / value
        ConsumptionUnit.MPG_UK -> 282.480936 / value
    }

    fun compute(
        distance: Double,
        distanceUnit: DistanceUnit,
        consumption: Double,
        consumptionUnit: ConsumptionUnit,
        price: Double,
        priceUnit: PriceUnit,
        payers: Int,
        roundTrip: Boolean,
    ): FuelResult {
        val km = distance * distanceUnit.km * (if (roundTrip) 2 else 1)
        val litres = km / 100 * litresPer100km(consumption, consumptionUnit)
        val cost = litres * price / priceUnit.litres
        val costPer100km = if (km == 0.0) 0.0 else cost / km * 100
        return FuelResult(km, litres, cost, cost / payers.coerceAtLeast(1), costPer100km)
    }
}
