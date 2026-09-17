package com.vasmarfas.card.core

private fun jsCores(): Int = js("(navigator.hardwareConcurrency || 0)")

actual fun cpuCoreCount(): Int = jsCores().coerceAtLeast(1)
