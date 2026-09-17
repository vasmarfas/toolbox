package com.vasmarfas.card.tools.printing

import com.vasmarfas.card.resources.*
import kotlin.math.PI
import org.jetbrains.compose.resources.StringResource

enum class FilamentMaterial(
    val title: StringResource,
    val density: Double,
    val nozzleC: IntRange,
    val bedC: IntRange,
) {
    PLA(Res.string.pla, 1.24, 190..225, 50..60),
    PLA_HT(Res.string.mat_pla_ht, 1.25, 205..235, 55..70),
    PETG(Res.string.petg, 1.27, 225..255, 70..85),
    PCTG(Res.string.mat_pctg, 1.23, 240..265, 70..85),
    PET(Res.string.mat_pet, 1.38, 250..275, 70..85),
    ABS(Res.string.abs, 1.04, 230..260, 95..110),
    ASA(Res.string.asa, 1.07, 240..265, 95..110),
    HIPS(Res.string.mat_hips, 1.04, 230..250, 90..110),
    PVB(Res.string.mat_pvb, 1.08, 190..220, 60..75),
    PP(Res.string.mat_pp, 0.90, 220..250, 80..100),
    PMMA(Res.string.mat_pmma, 1.18, 230..260, 90..110),
    POM(Res.string.mat_pom, 1.41, 200..230, 100..130),
    PHA(Res.string.mat_pha, 1.25, 195..225, 50..70),
    TPU(Res.string.tpu, 1.21, 210..235, 35..60),
    TPU_85A(Res.string.mat_tpu_85a, 1.15, 200..225, 30..50),
    TPE(Res.string.mat_tpe, 1.20, 210..240, 40..60),
    PEBA(Res.string.mat_peba, 1.01, 220..250, 40..70),
    NYLON(Res.string.nylon, 1.14, 240..270, 70..90),
    PA12(Res.string.mat_pa12, 1.01, 240..270, 60..90),
    PC(Res.string.pc, 1.20, 260..300, 100..120),
    PC_ABS(Res.string.mat_pc_abs, 1.15, 250..280, 90..110),
    PVDF(Res.string.mat_pvdf, 1.78, 230..265, 90..110),
    PPS(Res.string.mat_pps, 1.35, 300..330, 120..150),
    PSU(Res.string.mat_psu, 1.24, 340..380, 140..160),
    PPSU(Res.string.mat_ppsu, 1.29, 360..390, 150..180),
    PEI_9085(Res.string.mat_pei_9085, 1.34, 350..390, 140..160),
    PEI_1010(Res.string.mat_pei_1010, 1.27, 360..400, 140..170),
    PEKK(Res.string.mat_pekk, 1.27, 340..380, 120..160),
    PEEK(Res.string.mat_peek, 1.30, 370..430, 120..160),
    PCL(Res.string.mat_pcl, 1.15, 70..110, 20..40),
    PLA_CF_15(Res.string.mat_pla_cf_15, 1.25, 200..230, 50..60),
    PLA_WOOD(Res.string.pla_wood, 1.28, 190..220, 50..60),
    PLA_CERAMIC(Res.string.mat_pla_ceramic, 1.60, 195..220, 50..60),
    PLA_GLOW(Res.string.mat_pla_glow, 1.25, 200..225, 50..60),
    PLA_COPPER(Res.string.mat_pla_copper, 4.00, 190..220, 50..60),
    PLA_BRONZE(Res.string.mat_pla_bronze, 3.90, 190..220, 50..60),
    PLA_STEEL(Res.string.mat_pla_steel, 3.00, 195..225, 50..60),
    CARBON(Res.string.carbon_filled, 1.30, 240..270, 70..100),
    PETG_CF_20(Res.string.mat_petg_cf_20, 1.30, 240..260, 70..85),
    PETG_GF_20(Res.string.mat_petg_gf_20, 1.42, 240..265, 70..85),
    ABS_CF_20(Res.string.mat_abs_cf_20, 1.11, 240..270, 95..110),
    ASA_CF_15(Res.string.mat_asa_cf_15, 1.12, 245..270, 95..110),
    PA6_CF_20(Res.string.mat_pa6_cf_20, 1.22, 260..290, 80..100),
    PA6_GF_30(Res.string.mat_pa6_gf_30, 1.35, 260..290, 80..100),
    PA12_CF_15(Res.string.mat_pa12_cf_15, 1.07, 250..280, 60..90),
    PC_CF_20(Res.string.mat_pc_cf_20, 1.28, 270..300, 100..120),
    PPS_CF_20(Res.string.mat_pps_cf_20, 1.42, 300..340, 120..150),
    PEI_CF_20(Res.string.mat_pei_cf_20, 1.32, 360..400, 140..170),
    PEEK_CF_30(Res.string.mat_peek_cf_30, 1.40, 380..430, 130..160),
    PVA(Res.string.mat_pva, 1.23, 190..220, 45..60),
    BVOH(Res.string.mat_bvoh, 1.25, 200..215, 50..60),
    ;

    val filled: Boolean get() = name.contains("_CF_") || name.contains("_GF_") ||
        this in setOf(CARBON, PLA_WOOD, PLA_CERAMIC, PLA_COPPER, PLA_BRONZE, PLA_STEEL)
}

val filamentDiameters = listOf(1.75, 2.85)

object Filament {
    fun volumeCm3(lengthM: Double, diameterMm: Double): Double = PI * diameterMm * diameterMm * lengthM / 4.0

    fun lengthFromVolumeM(volumeCm3: Double, diameterMm: Double): Double = volumeCm3 * 4.0 / (PI * diameterMm * diameterMm)

    fun grams(lengthM: Double, diameterMm: Double, density: Double): Double = volumeCm3(lengthM, diameterMm) * density

    fun lengthM(grams: Double, diameterMm: Double, density: Double): Double =
        lengthFromVolumeM(grams / density, diameterMm)

    fun gramsPerMetre(diameterMm: Double, density: Double): Double = grams(1.0, diameterMm, density)
}
