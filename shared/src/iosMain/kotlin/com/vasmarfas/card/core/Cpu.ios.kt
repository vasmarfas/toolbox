package com.vasmarfas.card.core

import platform.Foundation.NSProcessInfo

actual fun cpuCoreCount(): Int =
    NSProcessInfo.processInfo.processorCount.toInt().coerceAtLeast(1)
