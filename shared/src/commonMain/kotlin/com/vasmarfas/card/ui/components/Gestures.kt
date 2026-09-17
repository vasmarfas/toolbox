package com.vasmarfas.card.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope

/**
 * Tracks a touch from the first press, consuming every event so a scrolling parent never takes the
 * gesture over. Canvases that are dragged along the vertical axis need this: `detectDragGestures`
 * loses to the surrounding `verticalScroll` as soon as the touch slop is passed.
 */
suspend fun PointerInputScope.trackTouch(onStart: (Offset) -> Unit, onMove: (Offset) -> Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        onStart(down.position)
        down.consume()
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            onMove(change.position)
            change.consume()
        }
    }
}
