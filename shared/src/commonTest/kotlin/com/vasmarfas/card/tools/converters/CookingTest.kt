package com.vasmarfas.card.tools.converters

import kotlin.test.Test
import kotlin.test.assertEquals

class CookingTest {
    @Test
    fun ingredients() {
        assertEquals(127.2, Cooking.convert(1.0, CookingUnit.CUP_US, CookingUnit.GRAM, Ingredient.FLOUR), 1e-9)
        assertEquals(100.0, Cooking.convert(100.0, CookingUnit.GRAM, CookingUnit.ML, Ingredient.WATER), 1e-9)
        assertEquals(3.0, Cooking.convert(1.0, CookingUnit.TBSP, CookingUnit.TSP, Ingredient.HONEY), 1e-9)
        assertEquals(0.5, Cooking.convert(500.0, CookingUnit.GRAM, CookingUnit.KG, Ingredient.SUGAR), 1e-9)
    }

    @Test
    fun oven() {
        assertEquals("4", Cooking.gasMark(180.0).mark)
        assertEquals("4", Cooking.gasMark(Cooking.fahrenheitToCelsius(350.0)).mark)
        assertEquals("¼", Cooking.gasMark(100.0).mark)
        assertEquals(356.0, Cooking.celsiusToFahrenheit(180.0), 1e-9)
    }
}
