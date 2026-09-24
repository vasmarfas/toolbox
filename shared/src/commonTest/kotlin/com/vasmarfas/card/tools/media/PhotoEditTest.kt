package com.vasmarfas.card.tools.media

import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhotoEditTest {
    private fun close(expected: Float, actual: Float, message: String = "") = assertTrue(abs(expected - actual) < 1e-4f, "$message: expected $expected, got $actual")

    private fun same(expected: Rect, actual: Rect) = assertTrue(
        abs(expected.left - actual.left) < 1e-5f && abs(expected.top - actual.top) < 1e-5f &&
            abs(expected.right - actual.right) < 1e-5f && abs(expected.bottom - actual.bottom) < 1e-5f,
        "expected $expected, got $actual",
    )

    private fun inside(rect: Rect) = assertTrue(rect.left >= -1e-5f && rect.top >= -1e-5f && rect.right <= 1f + 1e-5f && rect.bottom <= 1f + 1e-5f, "outside the frame: $rect")

    @Test
    fun fitCropIsCentredAndAsLargeAsTheFrameAllows() {
        val wide = PhotoEditor.fitCrop(2f)
        close(1f, wide.width, "width")
        close(0.5f, wide.height, "height")
        close(0.5f, wide.center.y, "centre")
        val tall = PhotoEditor.fitCrop(0.5f)
        close(0.5f, tall.width, "width")
        close(1f, tall.height, "height")
    }

    @Test
    fun freeCornerMovesOnlyItsOwnEdgesAndStaysInTheFrame() {
        val start = Rect(0.2f, 0.2f, 0.8f, 0.8f)
        val moved = PhotoEditor.dragCrop(start, CropHandle.TOP_LEFT, -0.1f, 0.05f, null, 0.04f)
        same(Rect(0.1f, 0.25f, 0.8f, 0.8f), moved)
        same(Rect(0.2f, 0.2f, 1f, 1f), PhotoEditor.dragCrop(start, CropHandle.BOTTOM_RIGHT, 5f, 5f, null, 0.04f))
        val collapsed = PhotoEditor.dragCrop(start, CropHandle.RIGHT, -5f, 0f, null, 0.04f)
        close(0.04f, collapsed.width, "minimum width")
    }

    @Test
    fun lockedCropKeepsItsRatioWhateverIsDragged() {
        val ratio = 1.5f
        val start = Rect(0.2f, 0.3f, 0.5f, 0.5f)
        for (handle in CropHandle.entries - CropHandle.MOVE) {
            for ((dx, dy) in listOf(0.3f to 0.1f, -0.4f to -0.3f, 2f to 2f, -2f to 0.5f)) {
                val rect = PhotoEditor.dragCrop(start, handle, dx, dy, ratio, 0.04f)
                close(ratio, rect.width / rect.height, "$handle by $dx, $dy")
                inside(rect)
            }
        }
    }

    @Test
    fun lockedEdgeGrowsAroundTheCentreOfTheOtherAxis() {
        val start = Rect(0.4f, 0.4f, 0.6f, 0.6f)
        val rect = PhotoEditor.dragCrop(start, CropHandle.RIGHT, 0.2f, 0f, 1f, 0.04f)
        close(0.4f, rect.left, "left edge stays")
        close(0.8f, rect.right, "right edge follows")
        close(0.5f, rect.center.y, "vertical centre")
        close(0.4f, rect.height, "height follows the ratio")
    }

    @Test
    fun movingStopsAtTheFrameEdge() {
        val start = Rect(0.1f, 0.1f, 0.4f, 0.4f)
        same(Rect(0f, 0f, 0.3f, 0.3f), PhotoEditor.dragCrop(start, CropHandle.MOVE, -1f, -1f, null, 0.04f))
        val right = PhotoEditor.dragCrop(start, CropHandle.MOVE, 1f, 0.2f, 2f, 0.04f)
        close(1f, right.right, "right")
        close(0.3f, right.top, "top")
    }

    @Test
    fun fourQuarterTurnsAndTwoFlipsLeaveTheEditAsItWas() {
        val edit = PhotoEdit(straighten = 3f, crop = Rect(0.1f, 0.2f, 0.6f, 0.9f))
        var turned = edit
        repeat(4) { turned = turned.turned(clockwise = true) }
        assertEquals(edit.quarterTurns, turned.quarterTurns)
        same(edit.crop, turned.crop)
        same(edit.crop, edit.turned(clockwise = true).turned(clockwise = false).crop)
        val sideways = edit.turned(clockwise = true)
        val back = sideways.flipped().flipped()
        assertEquals(sideways.copy(crop = back.crop), back)
        same(sideways.crop, back.crop)
        assertEquals(3, sideways.flipped().quarterTurns)
        assertEquals(-3f, sideways.flipped().straighten)
    }

    @Test
    fun anUntouchedEditNeedsNoColourPass() {
        assertNull(PhotoEditor.colorMatrix(PhotoEdit(crop = Rect(0.1f, 0.1f, 0.9f, 0.9f), quarterTurns = 1)))
        assertTrue(PhotoEditor.colorMatrix(PhotoEdit(filter = PhotoFilter.SEPIA)) != null)
    }
}
