package com.vasmarfas.card.tools.measure

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.vasmarfas.card.core.LocalLang
import com.vasmarfas.card.core.SensorReading
import com.vasmarfas.card.core.SensorType
import com.vasmarfas.card.core.availableSensors
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.core.requestMotionAccess
import com.vasmarfas.card.core.sensorFlow
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.ChartKind
import com.vasmarfas.card.ui.components.ChartSeries
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.InteractiveChart
import kotlinx.coroutines.flow.catch

object MeasureStrings {
    val noSensor = Res.string.this_device_has_no_such_sensor
    val waiting = Res.string.waiting_for_sensor_data
    val motionPermission = Res.string.the_browser_needs_permission_to_read_motion
    val start = Res.string.start
    val value = Res.string.value_
    val sample = Res.string.sample_2
}

private val axisNames = listOf("X", "Y", "Z")

class SensorSession(val reading: SensorReading?, val error: String?, val supported: Boolean, val ready: Boolean)

@Composable
fun rememberSensor(type: SensorType, history: MutableList<SensorReading>? = null, historySize: Int = 200): SensorSession {
    val supported = remember { type in availableSensors() }
    var reading by remember { mutableStateOf<SensorReading?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(type, supported) {
        if (!supported) return@LaunchedEffect
        ready = requestMotionAccess()
        if (!ready) return@LaunchedEffect
        sensorFlow(type)
            .catch { error = it.message ?: it.toString() }
            .collect { r ->
                reading = r
                history?.let {
                    it.add(r)
                    if (it.size > historySize) it.removeAt(0)
                }
            }
    }
    return SensorSession(reading, error, supported, ready)
}

@Composable
fun SensorGate(session: SensorSession, content: @Composable (SensorReading) -> Unit) {
    when {
        !session.supported -> Text(MeasureStrings.noSensor.str(), style = MaterialTheme.typography.bodyLarge)
        session.error != null -> ErrorText(session.error)
        session.reading == null -> Text(MeasureStrings.waiting.str(), style = MaterialTheme.typography.bodyMedium)
        else -> content(session.reading)
    }
}

@Composable
fun MotionPermissionButton(onGranted: () -> Unit) {
    val sampleText = MeasureStrings.sample.str()
    Text(MeasureStrings.motionPermission.str(), style = MaterialTheme.typography.bodyMedium)
    ActionButton(text = Res.string.grant_permission.str(), onClick = onGranted)
}

@Composable
fun LineChart(
    series: List<List<Float>>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    symmetric: Boolean = true,
    kind: ChartKind = ChartKind.LINE,
) {
    val sampleText = MeasureStrings.sample.str()
    val fallback = MaterialTheme.colorScheme.primary
    val single = MeasureStrings.value.str()
    InteractiveChart(
        series = series.mapIndexed { index, values ->
            ChartSeries(
                label = if (series.size > 1) axisNames.getOrElse(index) { (index + 1).toString() } else single,
                color = colors.getOrElse(index) { fallback },
                points = values,
            )
        },
        modifier = modifier,
        kind = kind,
        symmetric = symmetric,
        xLabel = { "${sampleText} $it" },
        yFormat = { it.toDouble().fmt(2) },
    )
}
