package com.vasmarfas.card.tools.converters

import com.vasmarfas.card.resources.*
import kotlin.math.abs
import org.jetbrains.compose.resources.StringResource

enum class Ingredient(val title: StringResource, val density: Double) {
    WATER(Res.string.water, 1.0),
    FLOUR(Res.string.wheat_flour, 0.53),
    SUGAR(Res.string.sugar, 0.85),
    BUTTER(Res.string.butter, 0.91),
    MILK(Res.string.milk, 1.03),
    HONEY(Res.string.honey, 1.42),
    RICE(Res.string.rice, 0.78),
    OIL(Res.string.vegetable_oil, 0.92),
}

enum class CookingUnit(val title: StringResource, val symbol: StringResource, val ml: Double?, val grams: Double?) {
    CUP_US(Res.string.cup_us_240_ml, Res.string.unit_cup, 240.0, null),
    GRAM(Res.string.gram, Res.string.unit_g, null, 1.0),
    ML(Res.string.millilitre, Res.string.ml, 1.0, null),
    TSP(Res.string.teaspoon_5_ml, Res.string.unit_tsp, 5.0, null),
    TBSP(Res.string.tablespoon_15_ml, Res.string.unit_tbsp, 15.0, null),
    CUP_METRIC(Res.string.cup_metric_250_ml, Res.string.unit_cup, 250.0, null),
    GLASS_RU(Res.string.glass_200_ml, Res.string.cooking_symbol_glass, 200.0, null),
    FL_OZ(Res.string.fluid_ounce_us, Res.string.cooking_symbol_fl_oz, 29.5735, null),
    LITRE(Res.string.litre, Res.string.unit_l, 1000.0, null),
    KG(Res.string.kilogram, Res.string.unit_kg, null, 1000.0),
    OZ(Res.string.ounce, Res.string.cooking_symbol_oz, null, 28.349523),
    LB(Res.string.pound, Res.string.unit_lb, null, 453.59237),
}

class OvenSetting(val mark: String, val celsius: Int, val fahrenheit: Int, val description: StringResource)

object Cooking {
    val oven = listOf(
        OvenSetting("¼", 110, 225, Res.string.very_slow),
        OvenSetting("½", 120, 250, Res.string.very_slow),
        OvenSetting("1", 140, 275, Res.string.slow),
        OvenSetting("2", 150, 300, Res.string.slow),
        OvenSetting("3", 160, 325, Res.string.moderately_slow),
        OvenSetting("4", 180, 350, Res.string.moderate),
        OvenSetting("5", 190, 375, Res.string.moderately_hot),
        OvenSetting("6", 200, 400, Res.string.moderately_hot),
        OvenSetting("7", 220, 425, Res.string.hot),
        OvenSetting("8", 230, 450, Res.string.hot),
        OvenSetting("9", 240, 475, Res.string.very_hot),
    )

    const val FAN_OFFSET = 20

    fun convert(value: Double, from: CookingUnit, to: CookingUnit, ingredient: Ingredient): Double {
        val grams = if (from.grams != null) value * from.grams else value * from.ml!! * ingredient.density
        return if (to.grams != null) grams / to.grams else grams / ingredient.density / to.ml!!
    }

    fun celsiusToFahrenheit(c: Double): Double = c * 9 / 5 + 32

    fun fahrenheitToCelsius(f: Double): Double = (f - 32) * 5 / 9

    fun gasMark(celsius: Double): OvenSetting = oven.minBy { abs(it.celsius - celsius) }
}
