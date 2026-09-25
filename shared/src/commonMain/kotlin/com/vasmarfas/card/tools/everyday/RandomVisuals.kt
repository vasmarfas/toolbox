package com.vasmarfas.card.tools.everyday

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val TUMBLE_MS = 750
private const val FLICKER_MS = 60L

@Composable
fun DiceTray(values: List<Int>, faces: Int, rolls: Int) {
    var flicker by remember { mutableStateOf<List<Int>?>(null) }
    val turns = remember(values.size) { List(values.size) { Animatable(0f) } }
    val drift = remember(values.size) { List(values.size) { Animatable(Offset.Zero, Offset.VectorConverter) } }
    LaunchedEffect(rolls) {
        if (rolls == 0) return@LaunchedEffect
        coroutineScope {
            values.indices.forEach { i ->
                launch {
                    val start = Random.nextFloat() * 360f
                    turns[i].snapTo(start)
                    turns[i].animateTo((start / 360f).toInt() * 360f + 360f * (1 + Random.nextInt(2)) + Random.nextFloat() * 20f - 10f, tween(TUMBLE_MS, easing = FastOutSlowInEasing))
                }
                launch {
                    drift[i].snapTo(Offset(Random.nextFloat() * 160f - 80f, Random.nextFloat() * 80f - 40f))
                    drift[i].animateTo(Offset.Zero, tween(TUMBLE_MS, easing = FastOutSlowInEasing))
                }
            }
            launch {
                repeat((TUMBLE_MS / FLICKER_MS).toInt()) {
                    flicker = List(values.size) { Random.nextInt(1, faces + 1) }
                    delay(FLICKER_MS)
                }
                flicker = null
            }
        }
    }
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        (flicker ?: values).forEachIndexed { i, value ->
            Die(
                value,
                faces,
                Modifier.graphicsLayer {
                    rotationZ = turns.getOrNull(i)?.value ?: 0f
                    translationX = drift.getOrNull(i)?.value?.x?.times(density) ?: 0f
                    translationY = drift.getOrNull(i)?.value?.y?.times(density) ?: 0f
                },
            )
        }
    }
}

private val pips = mapOf(
    1 to listOf(4),
    2 to listOf(0, 8),
    3 to listOf(0, 4, 8),
    4 to listOf(0, 2, 6, 8),
    5 to listOf(0, 2, 4, 6, 8),
    6 to listOf(0, 2, 3, 5, 6, 8),
)

@Composable
private fun Die(value: Int, faces: Int, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier.size(64.dp).shadow(4.dp, shape).background(colors.surface, shape).border(1.5.dp, colors.outline, shape),
        contentAlignment = Alignment.Center,
    ) {
        val dots = pips[value]
        if (faces == 6 && dots != null) {
            val ink = colors.onSurface
            Canvas(Modifier.fillMaxSize().padding(12.dp)) {
                val step = size.width / 2
                for (cell in dots) drawCircle(ink, size.width / 10, Offset(cell % 3 * step, cell / 3 * step))
            }
        } else {
            Text("$value", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun Coin(heads: Boolean, flips: Int, headsText: String, tailsText: String) {
    val rest = if (heads) 0f else 180f
    val spin = remember { Animatable(rest) }
    LaunchedEffect(flips) {
        if (flips == 0) return@LaunchedEffect
        spin.snapTo(0f)
        spin.animateTo(1800f + rest, tween(900, easing = FastOutSlowInEasing))
    }
    val angle = spin.value % 360f
    val front = angle < 90f || angle > 270f
    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(128.dp)
                .graphicsLayer {
                    rotationY = spin.value
                    cameraDistance = 12 * density
                }
                .shadow(6.dp, CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFFF3D27A), Color(0xFFC99A2E))), CircleShape)
                .border(3.dp, Color(0xFFA67C1B), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (front) headsText else tailsText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5A3E08),
                modifier = Modifier.graphicsLayer { rotationY = if (front) 0f else 180f },
            )
        }
    }
}

@Composable
fun RollingText(value: String, rolls: Int, style: TextStyle, modifier: Modifier = Modifier, lag: Int = 0, candidate: () -> String) {
    var flicker by remember { mutableStateOf<String?>(null) }
    val scale = remember { Animatable(1f) }
    LaunchedEffect(rolls) {
        if (rolls == 0) return@LaunchedEffect
        repeat(((TUMBLE_MS + lag) / FLICKER_MS).toInt()) {
            flicker = candidate()
            delay(FLICKER_MS)
        }
        flicker = null
        scale.snapTo(1.25f)
        scale.animateTo(1f, tween(250))
    }
    Text(
        flicker ?: value,
        style = style,
        textAlign = TextAlign.Center,
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        },
    )
}
