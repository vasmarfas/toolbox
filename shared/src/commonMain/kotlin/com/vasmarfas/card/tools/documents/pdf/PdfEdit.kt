package com.vasmarfas.card.tools.documents.pdf

import kotlin.math.max
import kotlin.math.min

internal fun appearanceFit(rect: PdfRect, bbox: PdfRect, matrix: DoubleArray?): DoubleArray {
    val m = matrix?.takeIf { it.size == 6 } ?: doubleArrayOf(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
    val xs = DoubleArray(4)
    val ys = DoubleArray(4)
    val corners = arrayOf(bbox.left to bbox.bottom, bbox.right to bbox.bottom, bbox.left to bbox.top, bbox.right to bbox.top)
    for ((i, corner) in corners.withIndex()) {
        xs[i] = m[0] * corner.first + m[2] * corner.second + m[4]
        ys[i] = m[1] * corner.first + m[3] * corner.second + m[5]
    }
    val left = xs.min()
    val bottom = ys.min()
    val width = max(xs.max() - left, 1e-6)
    val height = max(ys.max() - bottom, 1e-6)
    val sx = rect.width / width
    val sy = rect.height / height
    return doubleArrayOf(sx, 0.0, 0.0, sy, rect.left - left * sx, rect.bottom - bottom * sy)
}

internal fun rectOf(array: PdfArray?, doc: PdfDocument?): PdfRect? {
    val v = array?.numbers(doc) ?: return null
    if (v.size < 4) return null
    return PdfRect(min(v[0], v[2]), min(v[1], v[3]), max(v[0], v[2]), max(v[1], v[3]))
}

internal fun PdfWriter.content(text: String): PdfRef = stream(PdfDict(), text.encodeToByteArray())
