package com.vasmarfas.card.tools.device

import com.vasmarfas.card.core.PlatformKind
import com.vasmarfas.card.core.ThermalLevel
import com.vasmarfas.card.core.ThermalReading
import com.vasmarfas.card.core.currentPlatform
import com.vasmarfas.card.core.readThermal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.coroutines.coroutineContext
import kotlin.time.TimeSource

class StressSample(
    val second: Int,
    val mwips: Double,
    val reading: ThermalReading?,
)

class StressSummary(
    val samples: List<StressSample>,
) {
    val peakMwips: Double get() = samples.maxOfOrNull { it.mwips } ?: 0.0
    val lastMwips: Double get() = samples.lastOrNull()?.mwips ?: 0.0
    val averageMwips: Double get() = if (samples.isEmpty()) 0.0 else samples.sumOf { it.mwips } / samples.size

    val peakCelsius: Double? get() = samples.mapNotNull { it.reading?.celsius }.maxOrNull()
    val worstLevel: ThermalLevel
        get() = samples.mapNotNull { it.reading?.level }.maxByOrNull { it.ordinal } ?: ThermalLevel.UNKNOWN

    val retention: Double get() = if (peakMwips <= 0.0) 0.0 else lastMwips / peakMwips
}

private const val LoadLoop = 100

private const val SampleIntervalMs = 1000L

// one thread in the browser, so a worker there hands control back between units or the tab freezes
fun stressFlow(workers: Int, autoStopSeconds: Int?): Flow<StressSample> = callbackFlow {
    val completed = IntArray(workers)
    val slice = currentPlatform == PlatformKind.WEB

    repeat(workers) { index ->
        launch(Dispatchers.Default) {
            val whetstone = Whetstone()
            var sink = 0.0
            while (coroutineContext.isActive) {
                sink += whetstone.majorLoop(LoadLoop)
                completed[index]++
                if (slice) yield()
            }
            if (sink == Double.MAX_VALUE) completed[index] = 0
        }
    }

    launch {
        val started = TimeSource.Monotonic.markNow()
        var previousTotal = 0
        var previousElapsed = 0.0
        var second = 0
        while (coroutineContext.isActive) {
            delay(SampleIntervalMs)
            second++
            val total = completed.sum()
            val elapsed = started.elapsedNow().inWholeMicroseconds / 1_000_000.0
            val window = elapsed - previousElapsed
            val done = total - previousTotal
            previousTotal = total
            previousElapsed = elapsed
            val mwips = Whetstone.mwips(LoadLoop, done, window)
            trySend(StressSample(second, mwips, runCatching { readThermal() }.getOrNull()))
            if (autoStopSeconds != null && second >= autoStopSeconds) break
        }
        close()
    }

    awaitClose { }
}
