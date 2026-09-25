package com.vasmarfas.card.tools.documents.editor

import com.vasmarfas.card.tools.documents.pdf.PdfPage
import com.vasmarfas.card.tools.documents.pdf.PdfRect
import com.vasmarfas.card.tools.documents.pdf.formatReal
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

enum class MarkFont { SANS, SERIF, MONO }

enum class MarkAlign { START, CENTER, END }

enum class ShapeKind { RECTANGLE, ELLIPSE, LINE, ARROW, CHECK, CROSS, DOT }

enum class MarkupKind { HIGHLIGHT, UNDERLINE, STRIKEOUT, SQUIGGLY }

class MarkImage(val width: Int, val height: Int, val jpeg: ByteArray? = null, val rgb: ByteArray? = null, val alpha: ByteArray? = null, val gray: Boolean = false) {
    init {
        require(jpeg != null || rgb != null) { "An image needs JPEG or RGB data" }
    }
}

class InkPath(val xy: FloatArray) {
    val size: Int get() = xy.size / 2

    fun bounds(): PdfRect {
        var left = Float.MAX_VALUE
        var bottom = Float.MAX_VALUE
        var right = -Float.MAX_VALUE
        var top = -Float.MAX_VALUE
        for (i in 0 until xy.size - 1 step 2) {
            left = min(left, xy[i])
            right = max(right, xy[i])
            bottom = min(bottom, xy[i + 1])
            top = max(top, xy[i + 1])
        }
        return if (xy.size < 2) PdfRect(0.0, 0.0, 0.0, 0.0) else PdfRect(left.toDouble(), bottom.toDouble(), right.toDouble(), top.toDouble())
    }

    fun translated(dx: Double, dy: Double) = InkPath(FloatArray(xy.size) { if (it % 2 == 0) xy[it] + dx.toFloat() else xy[it] + dy.toFloat() })

    fun simplified(tolerance: Float): InkPath {
        val count = size
        if (count < 3) return this
        val keep = BooleanArray(count)
        keep[0] = true
        keep[count - 1] = true
        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.addLast(0 to count - 1)
        while (stack.isNotEmpty()) {
            val (from, to) = stack.removeLast()
            var worst = -1
            var distance = tolerance
            val ax = xy[2 * from]
            val ay = xy[2 * from + 1]
            val bx = xy[2 * to]
            val by = xy[2 * to + 1]
            val length = hypot(bx - ax, by - ay)
            for (i in from + 1 until to) {
                val px = xy[2 * i]
                val py = xy[2 * i + 1]
                val d = if (length < 1e-6f) hypot(px - ax, py - ay) else abs((bx - ax) * (ay - py) - (ax - px) * (by - ay)) / length
                if (d > distance) {
                    distance = d
                    worst = i
                }
            }
            if (worst > 0) {
                keep[worst] = true
                stack.addLast(from to worst)
                stack.addLast(worst to to)
            }
        }
        val out = ArrayList<Float>()
        for (i in 0 until count) {
            if (keep[i]) {
                out += xy[2 * i]
                out += xy[2 * i + 1]
            }
        }
        return InkPath(out.toFloatArray())
    }
}

class Quad(val points: DoubleArray) {
    init {
        require(points.size == 8) { "A quad has four points" }
    }

    fun bounds() = PdfRect(
        min(min(points[0], points[2]), min(points[4], points[6])),
        min(min(points[1], points[3]), min(points[5], points[7])),
        max(max(points[0], points[2]), max(points[4], points[6])),
        max(max(points[1], points[3]), max(points[5], points[7])),
    )

    fun translated(dx: Double, dy: Double) = Quad(DoubleArray(8) { if (it % 2 == 0) points[it] + dx else points[it] + dy })
}

sealed interface Mark {
    val id: Long
    val bounds: PdfRect

    fun translated(dx: Double, dy: Double): Mark
}

data class InkMark(override val id: Long, val path: InkPath, val color: Int, val width: Float, val highlighter: Boolean = false) : Mark {
    override val bounds: PdfRect get() = path.bounds().inflated(width / 2.0)

    override fun translated(dx: Double, dy: Double) = copy(path = path.translated(dx, dy))
}

data class ShapeMark(
    override val id: Long,
    val kind: ShapeKind,
    val x0: Double,
    val y0: Double,
    val x1: Double,
    val y1: Double,
    val color: Int,
    val width: Float,
    val fill: Int? = null,
    val angle: Int = 0,
) : Mark {
    val box: PdfRect get() = PdfRect(min(x0, x1), min(y0, y1), max(x0, x1), max(y0, y1))

    override val bounds: PdfRect get() = box.inflated(width * 2.0)

    override fun translated(dx: Double, dy: Double) = copy(x0 = x0 + dx, y0 = y0 + dy, x1 = x1 + dx, y1 = y1 + dy)
}

data class TextMark(
    override val id: Long,
    val box: PdfRect,
    val text: String,
    val size: Float,
    val color: Int,
    val font: MarkFont = MarkFont.SANS,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val align: MarkAlign = MarkAlign.START,
    val angle: Int = 0,
    val fill: Int? = null,
    val border: Boolean = false,
    val spacing: Float = LINE_SPACING,
) : Mark {
    override val bounds: PdfRect get() = box

    override fun translated(dx: Double, dy: Double) = copy(box = box.translated(dx, dy))
}

data class ImageMark(override val id: Long, val box: PdfRect, val image: MarkImage, val angle: Int = 0, val opacity: Float = 1f) : Mark {
    override val bounds: PdfRect get() = box

    override fun translated(dx: Double, dy: Double) = copy(box = box.translated(dx, dy))
}

data class SignatureMark(override val id: Long, val box: PdfRect, val paths: List<InkPath>, val color: Int, val width: Float, val angle: Int = 0) : Mark {
    override val bounds: PdfRect get() = box

    override fun translated(dx: Double, dy: Double) = copy(box = box.translated(dx, dy))
}

data class NoteMark(override val id: Long, val x: Double, val y: Double, val text: String, val color: Int) : Mark {
    override val bounds: PdfRect get() = PdfRect(x, y - NOTE_SIZE, x + NOTE_SIZE, y)

    override fun translated(dx: Double, dy: Double) = copy(x = x + dx, y = y + dy)

    companion object {
        const val NOTE_SIZE = 20.0
    }
}

data class CoverMark(override val id: Long, val box: PdfRect, val color: Int, val redact: Boolean = false) : Mark {
    override val bounds: PdfRect get() = box

    override fun translated(dx: Double, dy: Double) = copy(box = box.translated(dx, dy))
}

data class MarkupMark(override val id: Long, val kind: MarkupKind, val quads: List<Quad>, val color: Int, val text: String = "") : Mark {
    override val bounds: PdfRect get() = quads.map { it.bounds() }.reduceOrNull { a, b -> a.union(b) } ?: PdfRect(0.0, 0.0, 0.0, 0.0)

    override fun translated(dx: Double, dy: Double) = copy(quads = quads.map { it.translated(dx, dy) })
}

sealed interface PageSource {
    val mediaBox: PdfRect
    val cropBox: PdfRect
    val rotation: Int
}

class SourcePage(val page: PdfPage) : PageSource {
    override val mediaBox: PdfRect get() = page.mediaBox
    override val cropBox: PdfRect get() = page.cropBox
    override val rotation: Int get() = page.rotation
}

class BlankPage(val width: Double, val height: Double) : PageSource {
    override val mediaBox = PdfRect(0.0, 0.0, width, height)
    override val cropBox = mediaBox
    override val rotation = 0
}

class ImagePage(val image: MarkImage, val width: Double, val height: Double) : PageSource {
    override val mediaBox = PdfRect(0.0, 0.0, width, height)
    override val cropBox = mediaBox
    override val rotation = 0
}

data class ObjectEdit(val removed: Boolean = false, val transform: Affine? = null)

data class EditPage(
    val id: Long,
    val source: PageSource,
    val turn: Int = 0,
    val crop: PdfRect? = null,
    val marks: List<Mark> = emptyList(),
    val objects: Map<Int, ObjectEdit> = emptyMap(),
) {
    val rotation: Int get() = ((source.rotation + turn) % 360 + 360) % 360

    val box: PdfRect get() = crop ?: source.cropBox

    val frame: PageFrame get() = PageFrame(box, rotation)
}

class PageFrame(val box: PdfRect, val rotation: Int) {
    val turned: Boolean get() = rotation % 180 != 0

    val width: Double get() = if (turned) box.height else box.width

    val height: Double get() = if (turned) box.width else box.height

    fun toDisplayU(x: Double, y: Double): Double = when (rotation) {
        90 -> y - box.bottom
        180 -> box.right - x
        270 -> box.top - y
        else -> x - box.left
    }

    fun toDisplayV(x: Double, y: Double): Double = when (rotation) {
        90 -> x - box.left
        180 -> y - box.bottom
        270 -> box.right - x
        else -> box.top - y
    }

    fun toUserX(u: Double, v: Double): Double = when (rotation) {
        90 -> box.left + v
        180 -> box.right - u
        270 -> box.right - v
        else -> box.left + u
    }

    fun toUserY(u: Double, v: Double): Double = when (rotation) {
        90 -> box.bottom + u
        180 -> box.bottom + v
        270 -> box.top - u
        else -> box.top - v
    }

    fun displayAffine(): Affine = when (rotation) {
        90 -> Affine(0.0, 1.0, 1.0, 0.0, -box.bottom, -box.left)
        180 -> Affine(-1.0, 0.0, 0.0, 1.0, box.right, -box.bottom)
        270 -> Affine(0.0, -1.0, -1.0, 0.0, box.top, box.right)
        else -> Affine(1.0, 0.0, 0.0, -1.0, -box.left, box.top)
    }

    fun toUser(left: Double, top: Double, right: Double, bottom: Double): PdfRect {
        val x0 = toUserX(left, top)
        val y0 = toUserY(left, top)
        val x1 = toUserX(right, bottom)
        val y1 = toUserY(right, bottom)
        return PdfRect(min(x0, x1), min(y0, y1), max(x0, x1), max(y0, y1))
    }
}

class Affine(val a: Double, val b: Double, val c: Double, val d: Double, val e: Double, val f: Double) {
    fun x(lx: Double, ly: Double) = a * lx + c * ly + e

    fun y(lx: Double, ly: Double) = b * lx + d * ly + f

    fun operands(): String = listOf(a, b, c, d, e, f).joinToString(" ") { formatReal(it) }

    fun then(next: Affine) = Affine(
        next.a * a + next.c * b,
        next.b * a + next.d * b,
        next.a * c + next.c * d,
        next.b * c + next.d * d,
        next.a * e + next.c * f + next.e,
        next.b * e + next.d * f + next.f,
    )

    fun inverse(): Affine {
        val det = a * d - b * c
        if (det == 0.0) return this
        return Affine(d / det, -b / det, -c / det, a / det, (c * f - d * e) / det, (b * e - a * f) / det)
    }

    fun scaled(s: Double, dx: Double = 0.0, dy: Double = 0.0) = Affine(a * s, b * s, c * s, d * s, e * s + dx, f * s + dy)

    fun values() = doubleArrayOf(a, b, c, d, e, f)

    fun bounds(rect: PdfRect): PdfRect {
        val xs = listOf(x(rect.left, rect.bottom), x(rect.right, rect.bottom), x(rect.left, rect.top), x(rect.right, rect.top))
        val ys = listOf(y(rect.left, rect.bottom), y(rect.right, rect.bottom), y(rect.left, rect.top), y(rect.right, rect.top))
        return PdfRect(xs.min(), ys.min(), xs.max(), ys.max())
    }

    companion object {
        fun frameOf(box: PdfRect, angle: Int): Affine = when (((angle % 360) + 360) % 360) {
            90 -> Affine(0.0, 1.0, -1.0, 0.0, box.right, box.bottom)
            180 -> Affine(-1.0, 0.0, 0.0, -1.0, box.right, box.top)
            270 -> Affine(0.0, -1.0, 1.0, 0.0, box.left, box.top)
            else -> Affine(1.0, 0.0, 0.0, 1.0, box.left, box.bottom)
        }

        fun frameSize(box: PdfRect, angle: Int): Pair<Double, Double> = if (angle % 180 != 0) box.height to box.width else box.width to box.height
    }
}

enum class NumberPosition { TOP_LEFT, TOP_CENTER, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT }

data class PageNumbering(val format: String = "{n}", val position: NumberPosition = NumberPosition.BOTTOM_CENTER, val start: Int = 1, val size: Float = 10f, val color: Int = 0xFF444444.toInt(), val skipFirst: Boolean = false) {
    fun label(index: Int, total: Int): String = format.replace("{n}", (index + start).toString()).replace("{total}", (total + start - 1).toString())
}

data class Watermark(val text: String, val size: Float = 60f, val color: Int = 0xFFC62828.toInt(), val opacity: Float = 0.2f, val angle: Int = 45)

data class Security(val userPassword: String, val ownerPassword: String = "", val allowPrint: Boolean = true, val allowCopy: Boolean = true, val allowEdit: Boolean = true)

data class OutlineItem(val title: String, val pageId: Long?, val top: Double? = null, val children: List<OutlineItem> = emptyList(), val open: Boolean = false)

sealed interface FieldValue {
    data class Text(val value: String) : FieldValue

    data class Choice(val values: List<String>) : FieldValue

    data class Check(val state: String?) : FieldValue
}

data class DocumentEdit(
    val pages: List<EditPage>,
    val info: Map<String, String> = emptyMap(),
    val outline: List<OutlineItem> = emptyList(),
    val fields: Map<String, FieldValue> = emptyMap(),
    val numbering: PageNumbering? = null,
    val watermark: Watermark? = null,
    val security: Security? = null,
)

class SaveOptions(val keepMarksEditable: Boolean = false, val flattenForm: Boolean = false, val flattenAnnotations: Boolean = false, val removeAnnotations: Boolean = false)

fun PdfRect.inflated(by: Double) = PdfRect(left - by, bottom - by, right + by, top + by)

fun PdfRect.translated(dx: Double, dy: Double) = PdfRect(left + dx, bottom + dy, right + dx, top + dy)

fun PdfRect.union(other: PdfRect) = PdfRect(min(left, other.left), min(bottom, other.bottom), max(right, other.right), max(top, other.top))

fun PdfRect.contains(x: Double, y: Double) = x in left..right && y in bottom..top
