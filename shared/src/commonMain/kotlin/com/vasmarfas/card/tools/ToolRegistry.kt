package com.vasmarfas.card.tools

import com.vasmarfas.card.tools.calculators.calculatorTools
import com.vasmarfas.card.tools.converters.converterTools
import com.vasmarfas.card.tools.design.designTools
import com.vasmarfas.card.tools.developer.developerTools
import com.vasmarfas.card.tools.device.deviceTools
import com.vasmarfas.card.tools.everyday.everydayTools
import com.vasmarfas.card.tools.fitness.fitnessTools
import com.vasmarfas.card.tools.measure.measureTools
import com.vasmarfas.card.tools.network.networkTools
import com.vasmarfas.card.tools.printing.printingTools
import com.vasmarfas.card.tools.security.securityTools
import com.vasmarfas.card.tools.text.textTools
import com.vasmarfas.card.tools.time.timeTools

object ToolRegistry {
    val all: List<Tool> by lazy {
        (networkTools + converterTools + calculatorTools + textTools + developerTools +
            securityTools + designTools + timeTools + measureTools + deviceTools + everydayTools +
            printingTools + fitnessTools)
            .also { list ->
                val duplicates = list.groupBy { it.id }.filter { it.value.size > 1 }.keys
                check(duplicates.isEmpty()) { "Duplicate tool ids: $duplicates" }
            }
    }

    private val byId: Map<String, Tool> by lazy { all.associateBy { it.id } }

    fun byId(id: String): Tool? = byId[id]

    fun byCategory(category: ToolCategory): List<Tool> = all.filter { it.category == category }
}
