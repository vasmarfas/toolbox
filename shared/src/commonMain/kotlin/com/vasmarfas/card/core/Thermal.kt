package com.vasmarfas.card.core

enum class ThermalLevel { UNKNOWN, NONE, LIGHT, MODERATE, SEVERE, CRITICAL }

enum class ThermalSource { BATTERY, SENSOR, THROTTLING }

class ThermalReading(
    val celsius: Double?,
    val level: ThermalLevel,
    val source: ThermalSource,
    val label: String? = null,
)

expect fun thermalSupported(): Boolean

expect fun readThermal(): ThermalReading?
