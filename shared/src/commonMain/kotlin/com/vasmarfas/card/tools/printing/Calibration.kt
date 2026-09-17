package com.vasmarfas.card.tools.printing

import com.vasmarfas.card.core.Tr
import com.vasmarfas.card.core.fmt
import kotlin.math.ceil
import kotlin.math.round

class LayerAdvice(
    val minLayer: Double,
    val maxLayer: Double,
    val recommendedWidth: Double,
    val minWidth: Double,
    val maxWidth: Double,
    val firstLayerHeight: Double,
    val firstLayerWidth: Double,
    val layerCount: Int,
    val exactHeight: Double,
    val warnings: List<Tr>,
)

class TowerSegment(
    val index: Int,
    val firstLayer: Int,
    val lastLayer: Int,
    val zStart: Double,
    val zEnd: Double,
    val temperature: Int,
)

object LayerSettings {
    fun advice(nozzleMm: Double, layerMm: Double, targetHeightMm: Double): LayerAdvice {
        val minLayer = nozzleMm * 0.25
        val maxLayer = nozzleMm * 0.75
        val firstLayer = round(maxLayer * 50.0) / 50.0
        val count = if (targetHeightMm <= firstLayer) 1 else 1 + ceil((targetHeightMm - firstLayer) / layerMm).toInt()
        val warnings = buildList {
            if (layerMm < minLayer) {
                add(
                    Tr(
                        "Layer height is below 25 % of the nozzle — the extruder cannot keep a stable flow.",
                        "Высота слоя ниже 25 % от сопла — экструдер не удержит стабильный поток.",
                    ),
                )
            }
            if (layerMm > maxLayer) {
                add(
                    Tr(
                        "Layer height is above 75 % of the nozzle — layers will not bond properly.",
                        "Высота слоя выше 75 % от сопла — слои не спекутся как надо.",
                    ),
                )
            }
            if (layerMm < 0.04) {
                add(
                    Tr(
                        "Below 0.04 mm most Z axes cannot position repeatably.",
                        "Ниже 0,04 мм большинство осей Z не позиционируются повторяемо.",
                    ),
                )
            }
            if (targetHeightMm > 0) {
                val exact = firstLayer + (count - 1) * layerMm
                if (exact - targetHeightMm > 1e-6) {
                    add(
                        Tr(
                            "The height is not a whole number of layers: the model ends at ${exact.fmt(3)} mm.",
                            "Высота не кратна слоям: модель закончится на ${exact.fmt(3)} мм.",
                        ),
                    )
                }
            }
        }
        return LayerAdvice(
            minLayer = minLayer,
            maxLayer = maxLayer,
            recommendedWidth = nozzleMm * 1.2,
            minWidth = nozzleMm,
            maxWidth = nozzleMm * 1.5,
            firstLayerHeight = firstLayer,
            firstLayerWidth = nozzleMm * 1.4,
            layerCount = count,
            exactHeight = firstLayer + (count - 1) * layerMm,
            warnings = warnings,
        )
    }
}

object Extrusion {
    fun newESteps(oldSteps: Double, requestedMm: Double, extrudedMm: Double): Double = oldSteps * requestedMm / extrudedMm

    fun newFlowPercent(currentPercent: Double, expectedMm: Double, measuredMm: Double): Double =
        currentPercent * expectedMm / measuredMm

    fun volumetricRate(layerMm: Double, widthMm: Double, speedMmS: Double): Double = layerMm * widthMm * speedMmS

    fun maxSpeed(maxRateMm3S: Double, layerMm: Double, widthMm: Double): Double = maxRateMm3S / (layerMm * widthMm)
}

object Shrinkage {
    fun scaleFactor(shrinkPercent: Double): Double = 100.0 / (100.0 - shrinkPercent)

    fun correctedSteps(oldSteps: Double, nominalMm: Double, measuredMm: Double): Double = oldSteps * nominalMm / measuredMm

    fun errorPercent(nominalMm: Double, measuredMm: Double): Double = (measuredMm - nominalMm) / nominalMm * 100.0
}

object TemperatureTower {
    fun plan(
        startC: Int,
        stepC: Int,
        layersPerSegment: Int,
        segments: Int,
        firstLayerMm: Double,
        layerMm: Double,
    ): List<TowerSegment> = (0 until segments).map { i ->
        val first = 1 + i * layersPerSegment
        val last = first + layersPerSegment - 1
        TowerSegment(
            index = i,
            firstLayer = first,
            lastLayer = last,
            zStart = firstLayerMm + (first - 1) * layerMm,
            zEnd = firstLayerMm + (last - 1) * layerMm,
            temperature = startC + i * stepC,
        )
    }

    fun slicerScript(segments: List<TowerSegment>): List<String> = segments.drop(1).map {
        "{if layer_num==${it.firstLayer - 1}}M104 S${it.temperature}{endif}"
    }

    fun marlinScript(segments: List<TowerSegment>): List<String> = segments.mapIndexed { i, s ->
        val command = if (i == 0) "M109 S${s.temperature}" else "M104 S${s.temperature}"
        "$command ; layer ${s.firstLayer}, Z=${s.zStart.fmt(2)}"
    }
}
