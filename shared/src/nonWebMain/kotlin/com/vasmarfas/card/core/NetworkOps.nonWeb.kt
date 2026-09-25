package com.vasmarfas.card.core

import io.ktor.client.plugins.timeout
import io.ktor.client.request.head

actual suspend fun httpPing(url: String, timeoutMs: Int): Long {
    val started = currentEpochMillis()
    Net.client.head(url) { timeout { requestTimeoutMillis = timeoutMs.toLong() } }
    return currentEpochMillis() - started
}
