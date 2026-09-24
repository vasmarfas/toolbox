package com.vasmarfas.card.core

import platform.Foundation.NSProcessInfo
import platform.Foundation.NSProcessInfoThermalState
import platform.Foundation.thermalState

// iOS publishes a four-step throttling state and no temperature: the SoC sensors are private API
actual fun thermalSupported(): Boolean = true

actual fun readThermal(): ThermalReading? {
    val level = when (NSProcessInfo.processInfo.thermalState) {
        NSProcessInfoThermalState.NSProcessInfoThermalStateNominal -> ThermalLevel.NONE
        NSProcessInfoThermalState.NSProcessInfoThermalStateFair -> ThermalLevel.LIGHT
        NSProcessInfoThermalState.NSProcessInfoThermalStateSerious -> ThermalLevel.SEVERE
        NSProcessInfoThermalState.NSProcessInfoThermalStateCritical -> ThermalLevel.CRITICAL
        else -> return null
    }
    return ThermalReading(celsius = null, level = level, source = ThermalSource.THROTTLING)
}
