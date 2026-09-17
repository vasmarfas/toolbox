package com.vasmarfas.card.core

import java.time.Instant
import java.time.ZoneId

actual fun timeZoneIds(): List<String> = ZoneId.getAvailableZoneIds().filter { it.contains('/') && !it.startsWith("Etc/") && !it.startsWith("SystemV/") }.sorted()

actual fun timeZoneOffsetSeconds(zoneId: String, epochSeconds: Long): Int? = runCatching {
    ZoneId.of(zoneId).rules.getOffset(Instant.ofEpochSecond(epochSeconds)).totalSeconds
}.getOrNull()

actual fun systemTimeZoneId(): String = ZoneId.systemDefault().id
