package com.vasmarfas.card.tools.device

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vasmarfas.card.core.LocalLang
import com.vasmarfas.card.core.PlatformKind
import com.vasmarfas.card.core.setScreenBrightness
import com.vasmarfas.card.core.setTorch
import com.vasmarfas.card.core.str
import com.vasmarfas.card.core.torchSupported
import com.vasmarfas.card.core.vibrate
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.expandedHeight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val screenTestTool = Tool(
    id = "screen-test",
    category = ToolCategory.DEVICE,
    title = Res.string.screen_test,
    description = Res.string.full_screen_solid_colours_and_gradients_to_f,
    icon = Icons.Filled.Fullscreen,
    keywords = listOf("dead pixel", "burn-in", "backlight", "colors", "битые пиксели", "засветы", "дисплей"),
) { ScreenTestScreen() }

private val testFills: List<Pair<String, Brush>> = listOf(
    "White" to Brush.linearGradient(listOf(Color.White, Color.White)),
    "Black" to Brush.linearGradient(listOf(Color.Black, Color.Black)),
    "Red" to Brush.linearGradient(listOf(Color.Red, Color.Red)),
    "Green" to Brush.linearGradient(listOf(Color.Green, Color.Green)),
    "Blue" to Brush.linearGradient(listOf(Color.Blue, Color.Blue)),
    "Gray 50%" to Brush.linearGradient(listOf(Color.Gray, Color.Gray)),
    "Gray ramp" to Brush.horizontalGradient(listOf(Color.Black, Color.White)),
    "Rainbow" to Brush.horizontalGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)),
    "Checker gray" to Brush.linearGradient(listOf(Color(0xFF202020), Color(0xFF202020))),
)

@Composable
private fun ScreenTestScreen() {
    var open by remember { mutableStateOf(false) }
    var index by remember { mutableStateOf(0) }
    Text(Res.string.opens_a_full_screen_view_tap_anywhere_for_th.str())
    ChoiceChips(options = testFills.indices.toList(), selected = index, onSelect = { index = it }, label = { testFills[it].first })
    ActionButton(text = Res.string.open_full_screen.str(), onClick = { open = true })
    if (open) {
        FullScreenOverlay(onClose = { open = false }) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(testFills[index].second)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDownCompat()
                            val start = down.uptimeMillis
                            var released = false
                            while (!released) {
                                val event = awaitPointerEvent()
                                released = event.changes.all { !it.pressed }
                                if (!released && event.changes.first().uptimeMillis - start > 700) {
                                    open = false
                                    return@awaitEachGesture
                                }
                            }
                            index = (index + 1) % testFills.size
                        }
                    },
            ) {
                OverlayControls(
                    label = "${index + 1}/${testFills.size} · ${testFills[index].first}",
                    hint = Res.string.tap_next_fill_long_press_or_exit.str(),
                    onClose = { open = false },
                )
            }
        }
    }
}

@Composable
private fun BoxScope.OverlayControls(label: String, hint: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(12.dp)
            .clip(CircleShape)
            .background(Color(0xB3000000))
            .padding(start = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = Res.string.exit_fullscreen.str(), tint = Color.White)
        }
    }
    Text(
        hint,
        color = Color.White,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(24.dp)
            .clip(CircleShape)
            .background(Color(0xB3000000))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

private suspend fun AwaitPointerEventScope.awaitFirstDownCompat(): PointerInputChange {
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.pressed }
        if (change != null) return change
    }
}

@Composable
private fun FullScreenOverlay(onClose: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Box(Modifier.fillMaxSize()) { content() }
    }
}

val screenLightTool = Tool(
    id = "screen-light",
    category = ToolCategory.DEVICE,
    title = Res.string.screen_light_and_torch,
    description = Res.string.use_the_screen_as_a_lamp_with_adjustable_col,
    icon = Icons.Filled.Lightbulb,
    keywords = listOf("flashlight", "lamp", "light", "torch", "фонарик", "лампа", "свет"),
) { ScreenLightScreen() }

private val lightColors = listOf(
    "White" to Color.White,
    "Warm" to Color(0xFFFFE0B2),
    "Candle" to Color(0xFFFFB74D),
    "Red" to Color.Red,
    "Green" to Color.Green,
    "Blue" to Color.Blue,
    "SOS" to Color.White,
)

@Composable
private fun ScreenLightScreen() {
    val couldNotSwitchTheTorchText = Res.string.could_not_switch_the_torch.str()
    var open by remember { mutableStateOf(false) }
    var colorIndex by rememberSaveable { mutableStateOf(0) }
    var brightness by rememberSaveable { mutableStateOf(1f) }
    var torch by remember { mutableStateOf(false) }
    var torchError by remember { mutableStateOf<String?>(null) }
    val torchAvailable = remember { torchSupported() }
    ChoiceChips(options = lightColors.indices.toList(), selected = colorIndex, onSelect = { colorIndex = it }, label = { lightColors[it].first })
    Text(Res.string.brightness.str(), style = MaterialTheme.typography.labelLarge)
    Slider(value = brightness, onValueChange = { brightness = it }, valueRange = 0.05f..1f)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionButton(text = Res.string.light_up_the_screen.str(), onClick = { open = true }, icon = Icons.Filled.Lightbulb)
        if (torchAvailable) {
            ActionButton(
                text = if (torch) Res.string.torch_off.str() else Res.string.torch_on.str(),
                onClick = {
                    val ok = setTorch(!torch)
                    if (ok) torch = !torch else torchError = couldNotSwitchTheTorchText
                },
                icon = Icons.Filled.FlashlightOn,
            )
        }
    }
    torchError?.let { ErrorText(it) }
    DisposableEffect(Unit) { onDispose { if (torch) setTorch(false) } }
    if (open) {
        val sos = lightColors[colorIndex].first == "SOS"
        var sosOn by remember { mutableStateOf(true) }
        if (sos) {
            LaunchedEffect(Unit) {
                val pattern = listOf(200, 200, 200, 600, 600, 600, 200, 200, 200)
                while (true) {
                    for (d in pattern) {
                        sosOn = true; delay(d.toLong()); sosOn = false; delay(200)
                    }
                    delay(1000)
                }
            }
        }
        DisposableEffect(Unit) {
            setScreenBrightness(1f)
            onDispose { setScreenBrightness(null) }
        }
        FullScreenOverlay(onClose = { open = false }) {
            val base = lightColors[colorIndex].second
            val color = if (sos && !sosOn) Color.Black else Color(base.red * brightness, base.green * brightness, base.blue * brightness)
            Box(Modifier.fillMaxSize().background(color).clickable { open = false }) {
                OverlayControls(
                    label = lightColors[colorIndex].first,
                    hint = Res.string.tap_anywhere_or_to_exit.str(),
                    onClose = { open = false },
                )
            }
        }
    }
}

val touchTesterTool = Tool(
    id = "touch-tester",
    category = ToolCategory.DEVICE,
    title = Res.string.touch_and_pointer_tester,
    description = Res.string.shows_every_active_pointer_with_its_coordina,
    icon = Icons.Filled.TouchApp,
    keywords = listOf("multitouch", "digitizer", "pointer", "тач", "сенсор", "касание"),
    expandable = true,
) { TouchTesterScreen() }

@Composable
private fun TouchTesterScreen() {
    val pointers = remember { mutableStateMapOf<Long, Offset>() }
    val trail = remember { mutableStateListOf<Offset>() }
    var maxPointers by remember { mutableStateOf(0) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(expandedHeight(normal = 360.dp, reserved = 260.dp))
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .pointerInput(Unit) {
                awaitEachGesture {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { change ->
                            if (change.pressed) {
                                pointers[change.id.value] = change.position
                                trail.add(change.position)
                                if (trail.size > 3000) trail.removeAt(0)
                            } else pointers.remove(change.id.value)
                        }
                        if (pointers.size > maxPointers) maxPointers = pointers.size
                        if (event.changes.all { !it.pressed }) {
                            pointers.clear()
                            break
                        }
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            trail.forEach { drawCircle(Color(0x8000696D), 5f, it) }
            pointers.values.forEach { p ->
                drawCircle(Color(0xFF00696D), 36f, p)
                drawLine(Color(0xFF00696D), Offset(p.x, 0f), Offset(p.x, size.height), 1f)
                drawLine(Color(0xFF00696D), Offset(0f, p.y), Offset(size.width, p.y), 1f)
            }
        }
        Column(Modifier.padding(12.dp)) {
            pointers.entries.sortedBy { it.key }.forEach { (id, p) ->
                Text("#$id  ${p.x.toInt()}, ${p.y.toInt()}", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
    ResultCard {
        KeyValueRow(Res.string.active_pointers.str(), pointers.size.toString(), copyable = false)
        KeyValueRow(Res.string.max_simultaneous.str(), maxPointers.toString(), copyable = false)
    }
    TextButton(onClick = { trail.clear(); maxPointers = 0 }) { Text(Res.string.clear.str()) }
}

val vibrationTool = Tool(
    id = "vibration-test",
    category = ToolCategory.DEVICE,
    title = Res.string.vibration_test,
    description = Res.string.short_long_and_pattern_vibrations_to_check_t,
    icon = Icons.Filled.Vibration,
    keywords = listOf("haptic", "motor", "buzz", "вибро", "haptics"),
    platforms = PlatformKind.mobileAndWeb,
) { VibrationScreen() }

@Composable
private fun VibrationScreen() {
    val scope = rememberCoroutineScope()
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        ActionButton(text = "50 ms", onClick = { vibrate(50) })
        ActionButton(text = "200 ms", onClick = { vibrate(200) })
        ActionButton(text = "1 s", onClick = { vibrate(1000) })
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionButton(text = "SOS", onClick = { scope.launch { listOf(150, 150, 150, 400, 400, 400, 150, 150, 150).forEach { vibrate(it); delay(it + 150L) } } })
        ActionButton(text = Res.string.heartbeat.str(), onClick = { scope.launch { repeat(4) { vibrate(80); delay(180); vibrate(120); delay(600) } } })
    }
}
