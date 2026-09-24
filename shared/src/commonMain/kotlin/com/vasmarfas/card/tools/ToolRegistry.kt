package com.vasmarfas.card.tools

import com.vasmarfas.card.tools.calculators.calculatorTools
import com.vasmarfas.card.tools.converters.converterTools
import com.vasmarfas.card.tools.design.designTools
import com.vasmarfas.card.tools.developer.developerTools
import com.vasmarfas.card.tools.device.deviceTools
import com.vasmarfas.card.tools.documents.documentTools
import com.vasmarfas.card.tools.electronics.electronicsTools
import com.vasmarfas.card.tools.everyday.everydayTools
import com.vasmarfas.card.tools.fitness.fitnessTools
import com.vasmarfas.card.tools.measure.measureTools
import com.vasmarfas.card.tools.media.mediaTools
import com.vasmarfas.card.tools.money.moneyTools
import com.vasmarfas.card.tools.network.networkTools
import com.vasmarfas.card.tools.printing.printingTools
import com.vasmarfas.card.tools.security.securityTools
import com.vasmarfas.card.tools.sound.soundTools
import com.vasmarfas.card.tools.text.textTools
import com.vasmarfas.card.tools.time.timeTools

object ToolRegistry {
    val all: List<Tool> by lazy {
        listOf(
            measureTools, calculatorTools, converterTools, moneyTools, mediaTools, documentTools, timeTools, everydayTools,
            textTools, fitnessTools, soundTools, designTools, securityTools, deviceTools, developerTools, networkTools,
            electronicsTools, printingTools,
        ).flatten()
            .also { list ->
                val duplicates = list.groupBy { it.id }.filter { it.value.size > 1 }.keys
                check(duplicates.isEmpty()) { "Duplicate tool ids: $duplicates" }
            }
    }

    private val byId: Map<String, Tool> by lazy { all.associateBy { it.id } }

    // in the order the Popular shelf shows them
    val popular: List<Tool> by lazy {
        listOf(
            "calculator", "percentage", "unit-converter", "currency-converter", "ruler", "bubble-level", "compass",
            "countdown-timer", "stopwatch", "qr-generator", "photo-editor", "image-compressor", "image-converter",
            "video-editor", "video-converter", "images-to-pdf", "merge-pdf", "document-converter", "password-generator", "speed-test",
        ).map { id -> checkNotNull(byId(id)) { "Unknown popular tool $id" } }
    }

    fun byId(id: String): Tool? = byId[id]

    fun byCategory(category: ToolCategory): List<Tool> = all.filter { it.category == category }
}
