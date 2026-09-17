package com.vasmarfas.card.tools.fitness

import com.vasmarfas.card.tools.calculators.Sex
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HealthMathTest {
    @Test
    fun ahaThresholdsMatchTheGuideline() {
        assertEquals(BpCategoryAha.NORMAL, BloodPressure.aha(118, 76))
        assertEquals(BpCategoryAha.ELEVATED, BloodPressure.aha(125, 78))
        assertEquals(BpCategoryAha.STAGE_1, BloodPressure.aha(132, 78))
        assertEquals(BpCategoryAha.STAGE_1, BloodPressure.aha(118, 84))
        assertEquals(BpCategoryAha.STAGE_2, BloodPressure.aha(145, 88))
        assertEquals(BpCategoryAha.CRISIS, BloodPressure.aha(190, 100))
    }

    @Test
    fun escGradesTheSameReadingDifferently() {
        // the point of showing both: 132/78 is hypertension for the AHA, high-normal for the ESC
        assertEquals(BpCategoryAha.STAGE_1, BloodPressure.aha(132, 78))
        assertEquals(BpCategoryEsc.HIGH_NORMAL, BloodPressure.esc(132, 78))

        assertEquals(BpCategoryEsc.OPTIMAL, BloodPressure.esc(115, 75))
        assertEquals(BpCategoryEsc.GRADE_1, BloodPressure.esc(150, 95))
        assertEquals(BpCategoryEsc.GRADE_2, BloodPressure.esc(165, 105))
        assertEquals(BpCategoryEsc.GRADE_3, BloodPressure.esc(185, 115))
        assertEquals(BpCategoryEsc.ISOLATED_SYSTOLIC, BloodPressure.esc(150, 82))
    }

    @Test
    fun meanArterialSitsNearerDiastole() {
        val map = BloodPressure.meanArterial(120, 80)
        assertTrue(abs(map - 93.33) < 0.01, "map $map")
        assertEquals(40, BloodPressure.pulsePressure(120, 80))
    }

    @Test
    fun a1cAndAverageGlucoseRoundTrip() {
        // ADAG: 7% maps to 154 mg/dL, the figure the paper's table prints
        assertTrue(abs(Glycemia.eagMgDl(7.0) - 154.2) < 0.1)
        assertTrue(abs(Glycemia.eagMmolL(7.0) - 8.54) < 0.01)
        val back = Glycemia.a1cFromEagMmolL(Glycemia.eagMmolL(7.0))
        assertTrue(abs(back - 7.0) < 1e-9, "round trip $back")
        // IFCC: 7% DCCT is 53 mmol/mol
        assertTrue(abs(Glycemia.ifccMmolMol(7.0) - 53.0) < 0.5)
        assertTrue(abs(Glycemia.dcctFromIfcc(53.0) - 7.0) < 0.05)
    }

    @Test
    fun widmarkFallsToZeroAndBackInStep() {
        // half a litre of 5% beer is just under 20 g of ethanol
        val grams = Widmark.gramsOfEthanol(500.0, 5.0)
        assertTrue(abs(grams - 19.7) < 0.1, "grams $grams")

        val atOnce = Widmark.promille(grams, 80.0, Sex.MALE, hoursSince = 0.0)
        val laterOn = Widmark.promille(grams, 80.0, Sex.MALE, hoursSince = 1.0)
        assertTrue(atOnce > laterOn)
        assertTrue(abs((atOnce - laterOn) - Widmark.EliminationTypical) < 1e-9)

        val clear = Widmark.hoursToClear(grams, 80.0, Sex.MALE)
        assertTrue(abs(Widmark.promille(grams, 80.0, Sex.MALE, clear)) < 1e-9)
        // never negative, however long you wait
        assertEquals(0.0, Widmark.promille(grams, 80.0, Sex.MALE, hoursSince = 100.0))
    }

    @Test
    fun aWomanReachesAHigherLevelFromTheSameDrink() {
        val grams = Widmark.gramsOfEthanol(500.0, 5.0)
        val male = Widmark.promille(grams, 70.0, Sex.MALE, 0.0)
        val female = Widmark.promille(grams, 70.0, Sex.FEMALE, 0.0)
        assertTrue(female > male, "$female vs $male")
    }

    @Test
    fun caffeineHalvesOnSchedule() {
        assertTrue(abs(Caffeine.remainingMg(200.0, 5.0) - 100.0) < 1e-9)
        assertTrue(abs(Caffeine.remainingMg(200.0, 10.0) - 50.0) < 1e-9)
        assertEquals(200.0, Caffeine.remainingMg(200.0, 0.0))

        val hours = Caffeine.hoursUntil(200.0, 50.0)!!
        assertTrue(abs(hours - 10.0) < 1e-9, "hours $hours")
        assertNull(Caffeine.hoursUntil(50.0, 200.0))
        assertNull(Caffeine.hoursUntil(200.0, 0.0))
    }

    @Test
    fun vo2MaxEstimatesLandInThePlausibleRange() {
        // 2400 m in 12 minutes is a fit recreational runner
        val cooper = Vo2Max.cooper(2400.0)
        assertTrue(cooper in 40.0..50.0, "cooper $cooper")
        assertTrue(abs(Vo2Max.mets(cooper) - cooper / 3.5) < 1e-9)

        val rockport = Vo2Max.rockport(80.0, 30, Sex.MALE, 13.0, 120)
        assertTrue(rockport in 30.0..60.0, "rockport $rockport")

        val ratio = Vo2Max.heartRateRatio(190, 50)
        assertTrue(abs(ratio - 58.14) < 0.01, "ratio $ratio")
    }
}
