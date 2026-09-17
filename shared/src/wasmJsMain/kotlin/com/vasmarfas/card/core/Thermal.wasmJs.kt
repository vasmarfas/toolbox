package com.vasmarfas.card.core

// No browser API exposes device temperature, and none is planned; the throttling a page can
// observe is scheduler jitter, not a thermal reading.
actual fun thermalSupported(): Boolean = false

actual fun readThermal(): ThermalReading? = null
