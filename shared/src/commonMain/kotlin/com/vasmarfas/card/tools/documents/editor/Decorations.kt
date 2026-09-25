package com.vasmarfas.card.tools.documents.editor

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

internal class LinePlacement(val x: Double, val y: Double, val angle: Double)

internal fun watermarkPlacement(page: EditPage, watermark: Watermark, width: Double): LinePlacement {
    val box = page.box
    val angle = (watermark.angle + page.rotation).toDouble()
    val rad = angle * PI / 180
    val up = watermark.size * 0.35
    val cx = (box.left + box.right) / 2 + sin(rad) * up
    val cy = (box.bottom + box.top) / 2 - cos(rad) * up
    return LinePlacement(cx - cos(rad) * width / 2, cy - sin(rad) * width / 2, angle)
}

internal fun numberPlacement(page: EditPage, numbering: PageNumbering, width: Double): LinePlacement {
    val frame = page.frame
    val size = numbering.size
    val margin = max(18.0, size * 1.8)
    val top = numbering.position == NumberPosition.TOP_LEFT || numbering.position == NumberPosition.TOP_CENTER || numbering.position == NumberPosition.TOP_RIGHT
    val v = if (top) margin + size * 0.8 else frame.height - margin
    val u = when (numbering.position) {
        NumberPosition.TOP_LEFT, NumberPosition.BOTTOM_LEFT -> margin
        NumberPosition.TOP_CENTER, NumberPosition.BOTTOM_CENTER -> (frame.width - width) / 2
        NumberPosition.TOP_RIGHT, NumberPosition.BOTTOM_RIGHT -> frame.width - margin - width
    }
    return LinePlacement(frame.toUserX(u, v), frame.toUserY(u, v), page.rotation.toDouble())
}
