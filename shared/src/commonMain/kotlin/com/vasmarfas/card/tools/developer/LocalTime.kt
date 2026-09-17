package com.vasmarfas.card.tools.developer

import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalTime::class)
fun localDateTime(epochMillis: Long): LocalDateTime =
    Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())

fun LocalDateTime.formatted(): String =
    "$year-${pad2(month.ordinal + 1)}-${pad2(day)} ${pad2(hour)}:${pad2(minute)}:${pad2(second)}"

fun pad2(value: Int): String = value.toString().padStart(2, '0')
