package com.vasmarfas.card.tools.documents.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.Prefs
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.ui.components.trackTouch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.max
import kotlin.math.min

private const val KEY = "pdf.signatures"
private const val LIMIT = 3

internal class SavedSignature(val paths: List<InkPath>, val aspect: Double)

@Stable
internal class SignatureStore {
    var all by mutableStateOf(load())
        private set

    fun add(signature: SavedSignature) {
        all = (listOf(signature) + all).take(LIMIT)
        save()
    }

    fun remove(index: Int) {
        all = all.filterIndexed { i, _ -> i != index }
        save()
    }

    private fun save() {
        val json = JsonArray(
            all.map { signature ->
                JsonObject(
                    mapOf(
                        "a" to JsonPrimitive(signature.aspect),
                        "p" to JsonArray(signature.paths.map { path -> JsonArray(path.xy.map { JsonPrimitive(it) }) }),
                    ),
                )
            },
        )
        Prefs.store.put(KEY, json.toString())
    }

    private fun load(): List<SavedSignature> = runCatching {
        val raw = Prefs.store.get(KEY) ?: return emptyList()
        Json.parseToJsonElement(raw).jsonArray.map { element ->
            val obj = element.jsonObject
            SavedSignature(
                obj.getValue("p").jsonArray.map { path -> InkPath(path.jsonArray.map { it.jsonPrimitive.float }.toFloatArray()) },
                obj.getValue("a").jsonPrimitive.double,
            )
        }
    }.getOrDefault(emptyList())
}

@Composable
internal fun SignatureDialog(color: Int, onDismiss: () -> Unit, onSave: (SavedSignature) -> Unit) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Res.string.pdf_edit_draw_signature.str()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(2.6f)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            trackTouch(
                                onStart = { current = listOf(it) },
                                onMove = { point ->
                                    current = current + point
                                    if (current.size == 2) strokes += current else if (current.size > 2) strokes[strokes.lastIndex] = current
                                },
                            )
                        },
                ) {
                    size = IntSize(this.size.width.toInt(), this.size.height.toInt())
                    drawLine(Color(0xFFBDBDBD), Offset(this.size.width * 0.08f, this.size.height * 0.78f), Offset(this.size.width * 0.92f, this.size.height * 0.78f), 1.5f)
                    for (stroke in strokes) signatureStroke(stroke, Color(color))
                }
                Text(Res.string.pdf_edit_signature_hint.str(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(enabled = strokes.isNotEmpty(), onClick = { normalized(strokes, size.width.toFloat(), size.height.toFloat())?.let(onSave) }) {
                Text(Res.string.done.str())
            }
        },
        dismissButton = {
            TextButton(onClick = { if (strokes.isEmpty()) onDismiss() else strokes.clear() }) {
                Text(if (strokes.isEmpty()) Res.string.cancel.str() else Res.string.clear.str())
            }
        },
    )
}

private fun DrawScope.signatureStroke(points: List<Offset>, color: Color) {
    val xy = FloatArray(points.size * 2) { if (it % 2 == 0) points[it / 2].x else points[it / 2].y }
    drawPath(inkPath(xy) { x, y -> Offset(x.toFloat(), y.toFloat()) }, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun normalized(strokes: List<List<Offset>>, width: Float, height: Float): SavedSignature? {
    val points = strokes.flatten()
    if (points.isEmpty() || width <= 0f || height <= 0f) return null
    val pad = min(width, height) * 0.04f
    val left = points.minOf { it.x } - pad
    val top = points.minOf { it.y } - pad
    val w = max(points.maxOf { it.x } + pad - left, 1f)
    val h = max(points.maxOf { it.y } + pad - top, 1f)
    val paths = strokes.map { stroke ->
        InkPath(FloatArray(stroke.size * 2) { if (it % 2 == 0) (stroke[it / 2].x - left) / w else (stroke[it / 2].y - top) / h }).simplified(0.002f)
    }
    return SavedSignature(paths, (w / h).toDouble())
}
