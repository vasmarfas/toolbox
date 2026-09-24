package com.vasmarfas.card.tools.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.PlatformKind
import com.vasmarfas.card.core.cpuCoreCount
import com.vasmarfas.card.core.currentPlatform
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.LoadingRow
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SegmentedChoice
import com.vasmarfas.card.ui.components.SoftDivider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

val benchmarkTool = Tool(
    id = "benchmarks",
    category = ToolCategory.DEVICE,
    title = Res.string.benchmarks,
    description = Res.string.benchmarks_description,
    icon = Icons.Filled.Speed,
    keywords = listOf(
        "benchmark", "whetstone", "linpack", "scimark", "mflops", "mwips", "performance", "cpu",
        "бенчмарк", "тест производительности", "производительность", "процессор", "флопс",
    ),
) { BenchmarkScreen() }

private fun benchmarkTitle(id: String): StringResource = when (id) {
    "whetstone" -> Res.string.bench_whetstone
    "linpack" -> Res.string.bench_linpack
    "scimark" -> Res.string.bench_scimark
    "sha256" -> Res.string.bench_sha256
    "memory" -> Res.string.bench_memory
    else -> Res.string.bench_mandelbrot
}

private fun benchmarkNote(id: String): StringResource = when (id) {
    "whetstone" -> Res.string.bench_whetstone_note
    "linpack" -> Res.string.bench_linpack_note
    "scimark" -> Res.string.bench_scimark_note
    "sha256" -> Res.string.bench_sha256_note
    "memory" -> Res.string.bench_memory_note
    else -> Res.string.bench_mandelbrot_note
}

@Composable
private fun unitLabel(unit: BenchmarkUnit): String = when (unit) {
    BenchmarkUnit.MWIPS -> Res.string.unit_mwips.str()
    BenchmarkUnit.MFLOPS -> Res.string.unit_mflops.str()
    BenchmarkUnit.MB_PER_SECOND -> Res.string.unit_mb_per_second.str()
    BenchmarkUnit.GB_PER_SECOND -> Res.string.unit_gb_per_second.str()
    BenchmarkUnit.MPIXELS_PER_SECOND -> Res.string.unit_mpixels_per_second.str()
}

private val durations = listOf(1, 3, 8)

@Composable
private fun BenchmarkScreen() {
    var seconds by rememberSaveable { mutableStateOf(if (currentPlatform == PlatformKind.WEB) 1 else 3) }
    var running by remember { mutableStateOf<String?>(null) }
    val results = remember { mutableStateMapOf<String, List<BenchmarkScore>>() }
    val scope = rememberCoroutineScope()

    val run: (List<Benchmark>) -> Unit = { queue ->
        scope.launch {
            for (benchmark in queue) {
                running = benchmark.id
                yield()
                val scores = withContext(Dispatchers.Default) { benchmark.run(seconds.toDouble()) }
                results[benchmark.id] = scores
            }
            running = null
        }
    }

    SegmentedChoice(
        options = durations,
        selected = seconds,
        onSelect = { seconds = it },
        label = { stringResource(Res.string.seconds_short, it) },
        modifier = Modifier.fillMaxWidth(),
    )
    ActionButton(
        text = Res.string.run_all_benchmarks.str(),
        onClick = { run(benchmarks) },
        enabled = running == null,
        icon = Icons.Filled.PlayArrow,
        modifier = Modifier.fillMaxWidth(),
    )

    Text(
        Res.string.benchmark_runtime_note.str(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    KeyValueRow(Res.string.cpu_cores.str(), cpuCoreCount().toString(), copyable = false)

    for (benchmark in benchmarks) {
        ResultCard(title = benchmarkTitle(benchmark.id).str()) {
            Text(
                benchmarkNote(benchmark.id).str(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val scores = results[benchmark.id]
            if (running == benchmark.id) {
                LoadingRow(Res.string.measuring.str())
            } else if (scores != null) {
                SoftDivider()
                Column {
                    for (score in scores) {
                        KeyValueRow(
                            label = score.name,
                            value = "${score.value.fmt(2)} ${unitLabel(score.unit)}",
                            copyable = false,
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ActionButton(
                    text = Res.string.run.str(),
                    onClick = { run(listOf(benchmark)) },
                    enabled = running == null,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
