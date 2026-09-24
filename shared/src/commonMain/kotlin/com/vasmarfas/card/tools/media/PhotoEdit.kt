package com.vasmarfas.card.tools.media

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import com.vasmarfas.card.core.cropped
import com.vasmarfas.card.core.filtered
import com.vasmarfas.card.core.transformed
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

enum class PhotoFilter { NONE, MONO, NOIR, SEPIA, VINTAGE, WARM, COOL, VIVID, MATTE, INVERT }

val FullFrame = Rect(0f, 0f, 1f, 1f)

// applied as mirror, turn, straighten, crop, colour, vignette. crop is in fractions of the turned and
// straightened frame
data class PhotoEdit(
    val quarterTurns: Int = 0,
    val mirror: Boolean = false,
    val straighten: Float = 0f,
    val crop: Rect = FullFrame,
    val exposure: Float = 0f,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val warmth: Float = 0f,
    val tint: Float = 0f,
    val vignette: Float = 0f,
    val filter: PhotoFilter = PhotoFilter.NONE,
)

fun PhotoEdit.turned(clockwise: Boolean): PhotoEdit {
    val c = crop
    return copy(
        quarterTurns = (quarterTurns + if (clockwise) 1 else 3) % 4,
        crop = if (clockwise) Rect(1f - c.bottom, c.left, 1f - c.top, c.right) else Rect(c.top, 1f - c.right, c.bottom, 1f - c.left),
    )
}

// the stored mirror comes before the turn, so on a sideways picture the turn reverses and the
// straightening angle changes sign
fun PhotoEdit.flipped(): PhotoEdit = copy(
    quarterTurns = (4 - quarterTurns) % 4,
    mirror = !mirror,
    straighten = -straighten,
    crop = Rect(1f - crop.right, crop.top, 1f - crop.left, crop.bottom),
)

enum class CropHandle { MOVE, LEFT, TOP, RIGHT, BOTTOM, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

object PhotoEditor {
    fun framed(source: ImageBitmap, edit: PhotoEdit): ImageBitmap {
        val turned = source.transformed(edit.quarterTurns, edit.mirror)
        return if (edit.straighten == 0f) turned else straightened(turned, edit.straighten)
    }

    fun cut(frame: ImageBitmap, crop: Rect): ImageBitmap = if (crop == FullFrame) {
        frame
    } else {
        frame.cropped(
            (crop.left * frame.width).roundToInt(),
            (crop.top * frame.height).roundToInt(),
            (crop.width * frame.width).roundToInt(),
            (crop.height * frame.height).roundToInt(),
        )
    }

    fun render(source: ImageBitmap, edit: PhotoEdit): ImageBitmap {
        val cut = cut(framed(source, edit), edit.crop)
        val colored = colorMatrix(edit)?.let(cut::filtered) ?: cut
        if (edit.vignette <= 0f) return colored
        val out = ImageBitmap(colored.width, colored.height)
        val size = Size(colored.width.toFloat(), colored.height.toFloat())
        Canvas(out).apply {
            drawImage(colored, Offset.Zero, Paint())
            drawRect(0f, 0f, size.width, size.height, Paint().also { vignetteBrush(size, edit.vignette).applyTo(size, it, 1f) })
        }
        return out
    }

    private fun straightened(image: ImageBitmap, degrees: Float): ImageBitmap {
        val w = image.width.toFloat()
        val h = image.height.toFloat()
        val radians = abs(degrees) * PI / 180
        val cover = (cos(radians) + max(w / h, h / w) * sin(radians)).toFloat()
        val out = ImageBitmap(image.width, image.height)
        Canvas(out).apply {
            translate(w / 2, h / 2)
            rotate(degrees)
            scale(cover, cover)
            translate(-w / 2, -h / 2)
            drawImage(image, Offset.Zero, Paint().apply { filterQuality = FilterQuality.High })
        }
        return out
    }

    fun vignetteBrush(size: Size, strength: Float): Brush = Brush.radialGradient(
        0f to Color.Transparent,
        0.45f to Color.Transparent,
        1f to Color.Black.copy(alpha = (0.85f * strength).coerceIn(0f, 1f)),
        center = Offset(size.width / 2, size.height / 2),
        radius = hypot(size.width, size.height) / 2,
    )

    fun colorMatrix(edit: PhotoEdit): ColorMatrix? {
        val stages = listOfNotNull(
            if (edit.exposure != 0f) gain(2f.pow(edit.exposure)) else null,
            if (edit.contrast != 0f) contrast(1f + edit.contrast) else null,
            if (edit.brightness != 0f) offset(edit.brightness * 80f) else null,
            if (edit.saturation != 0f) saturation(1f + edit.saturation) else null,
            if (edit.warmth != 0f || edit.tint != 0f) balance(edit.warmth, edit.tint) else null,
            preset(edit.filter),
        )
        if (stages.isEmpty()) return null
        val result = ColorMatrix()
        stages.asReversed().forEach { result *= it }
        return result
    }

    private fun gain(factor: Float) = ColorMatrix().apply { setToScale(factor, factor, factor, 1f) }

    private fun saturation(factor: Float) = ColorMatrix().apply { setToSaturation(factor) }

    private fun offset(value: Float) = ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, value,
            0f, 1f, 0f, 0f, value,
            0f, 0f, 1f, 0f, value,
            0f, 0f, 0f, 1f, 0f,
        ),
    )

    private fun contrast(factor: Float): ColorMatrix {
        val shift = 128f * (1f - factor)
        return ColorMatrix(
            floatArrayOf(
                factor, 0f, 0f, 0f, shift,
                0f, factor, 0f, 0f, shift,
                0f, 0f, factor, 0f, shift,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
    }

    private fun balance(warmth: Float, tint: Float) = ColorMatrix().apply {
        setToScale(1f + 0.18f * warmth + 0.06f * tint, 1f + 0.04f * warmth - 0.14f * tint, 1f - 0.18f * warmth + 0.06f * tint, 1f)
    }

    private fun preset(filter: PhotoFilter): ColorMatrix? = when (filter) {
        PhotoFilter.NONE -> null
        PhotoFilter.MONO -> saturation(0f)
        PhotoFilter.NOIR -> contrast(1.35f).also { it *= saturation(0f) }
        PhotoFilter.SEPIA -> ColorMatrix(
            floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        PhotoFilter.VINTAGE -> ColorMatrix(
            floatArrayOf(
                0.7f, 0.25f, 0.05f, 0f, 15f,
                0.15f, 0.72f, 0.08f, 0f, 10f,
                0.12f, 0.22f, 0.56f, 0f, 20f,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        PhotoFilter.WARM -> balance(0.6f, 0f)
        PhotoFilter.COOL -> balance(-0.6f, 0f)
        PhotoFilter.VIVID -> contrast(1.12f).also { it *= saturation(1.45f) }
        PhotoFilter.MATTE -> offset(22f).also { it *= contrast(0.82f) }
        PhotoFilter.INVERT -> ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
    }

    fun fitCrop(ratio: Float): Rect {
        val w = min(1f, ratio)
        val h = w / ratio
        return Rect(0.5f - w / 2, 0.5f - h / 2, 0.5f + w / 2, 0.5f + h / 2)
    }

    fun dragCrop(start: Rect, handle: CropHandle, dx: Float, dy: Float, ratio: Float?, minSize: Float): Rect {
        if (handle == CropHandle.MOVE) {
            return start.translate(dx.coerceIn(-start.left, 1f - start.right), dy.coerceIn(-start.top, 1f - start.bottom))
        }
        val left = handle == CropHandle.LEFT || handle == CropHandle.TOP_LEFT || handle == CropHandle.BOTTOM_LEFT
        val right = handle == CropHandle.RIGHT || handle == CropHandle.TOP_RIGHT || handle == CropHandle.BOTTOM_RIGHT
        val top = handle == CropHandle.TOP || handle == CropHandle.TOP_LEFT || handle == CropHandle.TOP_RIGHT
        val bottom = handle == CropHandle.BOTTOM || handle == CropHandle.BOTTOM_LEFT || handle == CropHandle.BOTTOM_RIGHT
        val l = if (left) (start.left + dx).coerceIn(0f, start.right - minSize) else start.left
        val r = if (right) (start.right + dx).coerceIn(start.left + minSize, 1f) else start.right
        val t = if (top) (start.top + dy).coerceIn(0f, start.bottom - minSize) else start.top
        val b = if (bottom) (start.bottom + dy).coerceIn(start.top + minSize, 1f) else start.bottom
        if (ratio == null) return Rect(l, t, r, b)

        val horizontal = left || right
        val vertical = top || bottom
        val anchorX = if (left) start.right else start.left
        val anchorY = if (top) start.bottom else start.top
        val center = start.center
        val maxW = when {
            !horizontal -> 2 * min(center.x, 1f - center.x)
            left -> anchorX
            else -> 1f - anchorX
        }
        val maxH = when {
            !vertical -> 2 * min(center.y, 1f - center.y)
            top -> anchorY
            else -> 1f - anchorY
        }
        var w = r - l
        var h = b - t
        when {
            horizontal && vertical -> if (w / h > ratio) w = h * ratio else h = w / ratio
            horizontal -> h = w / ratio
            else -> w = h * ratio
        }
        if (w > maxW) {
            w = maxW
            h = w / ratio
        }
        if (h > maxH) {
            h = maxH
            w = h * ratio
        }
        val x = when {
            !horizontal -> center.x - w / 2
            left -> anchorX - w
            else -> anchorX
        }
        val y = when {
            !vertical -> center.y - h / 2
            top -> anchorY - h
            else -> anchorY
        }
        return Rect(x, y, x + w, y + h)
    }
}
