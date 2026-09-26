package com.vasmarfas.card.core

import platform.Foundation.NSProcessInfo
import platform.Foundation.NSProcessInfoThermalState
import platform.Foundation.thermalState

private val levels = mapOf(
    NSProcessInfoThermalState.NSProcessInfoThermalStateNominal to ThermalLevel.NONE,
    NSProcessInfoThermalState.NSProcessInfoThermalStateFair to ThermalLevel.LIGHT,
    NSProcessInfoThermalState.NSProcessInfoThermalStateSerious to ThermalLevel.SEVERE,
    NSProcessInfoThermalState.NSProcessInfoThermalStateCritical to ThermalLevel.CRITICAL,
)

// iOS publishes a four-step throttling state and no temperature: the SoC sensors are private API
actual fun thermalSupported(): Boolean = true

actual fun readThermal(): ThermalReading? {
    val level = levels[NSProcessInfo.processInfo.thermalState] ?: return null
    return ThermalReading(celsius = null, level = level, source = ThermalSource.THROTTLING)
}
