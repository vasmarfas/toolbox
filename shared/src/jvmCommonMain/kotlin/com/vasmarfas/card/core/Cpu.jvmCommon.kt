package com.vasmarfas.card.core

actual fun cpuCoreCount(): Int = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
