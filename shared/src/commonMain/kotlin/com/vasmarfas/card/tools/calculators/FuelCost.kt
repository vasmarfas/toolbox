package com.vasmarfas.card.tools.calculators

enum class ConsumptionUnit(val symbol: String) {
    L100KM("L/100 km"),
    KML("km/L"),
    MPG_US("mpg (US)"),
    MPG_UK("mpg (UK)"),
}

enum class DistanceUnit(val symbol: String, val km: Double) {
    KM("km", 1.0),
    MI("mi", 1.609344),
}

enum class PriceUnit(val symbol: String, val litres: Double) {
    LITRE("/L", 1.0),
    GALLON_US("/gal (US)", 3.785411784),
    GALLON_UK("/gal (UK)", 4.54609),
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
        passengers: Int,
        roundTrip: Boolean,
    ): FuelResult {
        val km = distance * distanceUnit.km * (if (roundTrip) 2 else 1)
        val litres = km / 100 * litresPer100km(consumption, consumptionUnit)
        val cost = litres * price / priceUnit.litres
        val costPer100km = if (km == 0.0) 0.0 else cost / km * 100
        return FuelResult(km, litres, cost, cost / passengers.coerceAtLeast(1), costPer100km)
    }
}
