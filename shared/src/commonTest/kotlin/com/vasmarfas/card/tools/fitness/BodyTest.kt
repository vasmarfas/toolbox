package com.vasmarfas.card.tools.fitness

import com.vasmarfas.card.resources.*
import kotlin.test.Test
import kotlin.test.assertEquals

class BodyTest {
    @Test
    fun bmiAndBmr() {
        assertEquals(22.86, Body.bmi(70.0, 175.0), 0.01)
        assertEquals("Normal", Body.bmiCategory(22.86).english())
        assertEquals("Obesity class I", Body.bmiCategory(32.0).english())
        assertEquals(1648.75, Body.bmr(Sex.MALE, 70.0, 175.0, 30), 1e-9)
        assertEquals(1482.75, Body.bmr(Sex.FEMALE, 70.0, 175.0, 30), 1e-9)
    }

    @Test
    fun idealWeightAndBodyFat() {
        assertEquals(70.47, Body.idealWeights(Sex.MALE, 175.0).first { it.formula == "Devine" }.kg, 0.01)
        assertEquals(16.94, Body.navyBodyFat(Sex.MALE, 175.0, 85.0, 38.0, 0.0)!!, 0.01)
        assertEquals(24.33, Body.navyBodyFat(Sex.FEMALE, 165.0, 70.0, 33.0, 95.0)!!, 0.01)
    }
}
