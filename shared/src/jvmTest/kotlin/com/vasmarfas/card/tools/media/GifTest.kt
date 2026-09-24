package com.vasmarfas.card.tools.media

import java.awt.image.IndexColorModel
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.imageio.metadata.IIOMetadataNode
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTimedValue

class GifTest {
    class Frame(val left: Int, val top: Int, val width: Int, val height: Int, val delayCs: Int, val disposal: String, val transparent: Boolean)

    class DecodedGif(val frames: List<Frame>, val composited: List<IntArray>, val loopCount: Int?)

    companion object {
        fun decodeGif(bytes: ByteArray, width: Int, height: Int): DecodedGif {
            val reader = ImageIO.getImageReadersByFormatName("gif").next()
            val frames = ArrayList<Frame>()
            val composited = ArrayList<IntArray>()
            var loop: Int? = null
            ImageIO.createImageInputStream(ByteArrayInputStream(bytes)).use { input ->
                reader.input = input
                val canvas = IntArray(width * height)
                for (i in 0 until reader.getNumImages(true)) {
                    val img = reader.read(i)
                    val tree = reader.getImageMetadata(i).getAsTree("javax_imageio_gif_image_1.0") as IIOMetadataNode
                    val desc = tree.getElementsByTagName("ImageDescriptor").item(0) as IIOMetadataNode
                    val gce = tree.getElementsByTagName("GraphicControlExtension").item(0) as IIOMetadataNode
                    val apps = tree.getElementsByTagName("ApplicationExtension")
                    for (a in 0 until apps.length) {
                        val app = apps.item(a) as IIOMetadataNode
                        if (app.getAttribute("applicationID") == "NETSCAPE") {
                            val data = app.userObject as ByteArray
                            loop = (data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8)
                        }
                    }
                    val frame = Frame(
                        desc.getAttribute("imageLeftPosition").toInt(),
                        desc.getAttribute("imageTopPosition").toInt(),
                        desc.getAttribute("imageWidth").toInt(),
                        desc.getAttribute("imageHeight").toInt(),
                        gce.getAttribute("delayTime").toInt(),
                        gce.getAttribute("disposalMethod"),
                        gce.getAttribute("transparentColorFlag") == "TRUE",
                    )
                    val transparentIndex = gce.getAttribute("transparentColorIndex").toInt()
                    val model = img.colorModel as IndexColorModel
                    val raster = img.raster
                    for (y in 0 until frame.height) {
                        for (x in 0 until frame.width) {
                            val index = raster.getSample(x, y, 0)
                            if (frame.transparent && index == transparentIndex) continue
                            canvas[(frame.top + y) * width + frame.left + x] = model.getRGB(index) or -0x1000000
                        }
                    }
                    frames += frame
                    composited += canvas.copyOf()
                    if (frame.disposal == "restoreToBackgroundColor") {
                        for (y in frame.top until frame.top + frame.height) canvas.fill(0, y * width + frame.left, y * width + frame.left + frame.width)
                    }
                }
            }
            return DecodedGif(frames, composited, loop)
        }

        fun sprite(pixels: IntArray, width: Int, left: Int, top: Int, w: Int, h: Int) {
            for (y in top until top + h) {
                for (x in left until left + w) {
                    pixels[y * width + x] = if ((x - left) / 4 % 2 == (y - top) / 4 % 2) argb(255, 250, 220, 30) else argb(255, 20, 30, 200)
                }
            }
        }
    }

    @Test
    fun singleFrameWithFewColorsIsExact() {
        val w = 97
        val h = 61
        val src = poster(w, h, 200)
        val gif = GifWriter(w, h).apply { addFrame(src, 100) }.finish()
        val decoded = decodeGif(gif, w, h)
        assertEquals(1, decoded.frames.size)
        assertEquals(10, decoded.frames[0].delayCs)
        assertEquals(0, decoded.loopCount)
        assertContentEquals(src, decoded.composited[0])
    }

    @Test
    fun exactlyTwoHundredFiftySixColorsSurvive() {
        val w = 64
        val h = 64
        val src = IntArray(w * h) { argb(255, (it / 16) % 256, 255 - (it / 16) % 256, 77) }
        val gif = GifWriter(w, h).apply { addFrame(src, 100, dither = true) }.finish()
        assertContentEquals(src, decodeGif(gif, w, h).composited[0])
    }

    @Test
    fun photoFrameStaysCloseToSource() {
        val w = 320
        val h = 240
        val src = photo(w, h)
        for (dither in listOf(false, true)) {
            val gif = GifWriter(w, h).apply { addFrame(src, 50, dither) }.finish()
            val out = decodeGif(gif, w, h).composited[0]
            val err = channelError(src, out)
            val block = blockError(src, out, w, h)
            if (dither) {
                assertTrue(block < 2.5, "block error $block")
                assertTrue(err.mean < 6.0, "mean error ${err.mean}")
            } else {
                assertTrue(err.mean < 4.0, "mean error ${err.mean}")
                assertTrue(err.max < 64, "max error ${err.max}")
            }
        }
    }

    @Test
    fun delaysAreCentisecondsWithBrowserMinimum() {
        val w = 8
        val h = 8
        val writer = GifWriter(w, h, loopCount = 3)
        listOf(100, 40, 15, 5).forEachIndexed { i, delay -> writer.addFrame(IntArray(w * h) { argb(255, i * 60, 0, 0) }, delay) }
        val decoded = decodeGif(writer.finish(), w, h)
        assertEquals(listOf(10, 4, 2, 2), decoded.frames.map { it.delayCs })
        assertEquals(3, decoded.loopCount)
    }

    @Test
    fun thirtyFramesPerSecondKeepsItsSpeed() {
        val writer = GifWriter(4, 4)
        repeat(30) { i -> writer.addFrame(IntArray(16) { argb(255, i * 8, 0, 0) }, 33) }
        val delays = decodeGif(writer.finish(), 4, 4).frames.map { it.delayCs }
        assertEquals(99, delays.sum())
        assertTrue(delays.all { it == 3 || it == 4 })
    }

    @Test
    fun loopExtensionFollowsLoopCount() {
        val frame = poster(16, 16, 4)
        assertEquals(0, decodeGif(GifWriter(16, 16).apply { addFrame(frame, 50) }.finish(), 16, 16).loopCount)
        val once = GifWriter(16, 16, loopCount = -1).apply { addFrame(frame, 50) }.finish()
        assertNull(decodeGif(once, 16, 16).loopCount)
        assertEquals(-1, once.indexOf(ascii("NETSCAPE2.0")))
    }

    @Test
    fun transparencyIsPreserved() {
        val w = 40
        val h = 30
        val src = IntArray(w * h) {
            val x = it % w
            val y = it / w
            when {
                (x + y) % 5 == 0 -> 0
                x < 10 -> 0x40FF0000
                else -> argb(255, (x / 4) * 30, (y / 5) * 40, 100)
            }
        }
        val decoded = decodeGif(GifWriter(w, h).apply { addFrame(src, 100) }.finish(), w, h)
        val expected = IntArray(src.size) { if (src[it] ushr 24 < 128) 0 else src[it] }
        assertContentEquals(expected, decoded.composited[0])
        assertTrue(decoded.frames[0].transparent)
    }

    @Test
    fun frameDifferenceShrinksTheFileAndKeepsFramesIdentical() {
        val w = 200
        val h = 120
        val background = poster(w, h, 40)
        val frames = List(12) { i -> background.copyOf().also { sprite(it, w, 20 + i * 12, 40 + (i % 3) * 5, 24, 18) } }
        val gif = GifWriter(w, h).apply { frames.forEach { addFrame(it, 80) } }.finish()
        val decoded = decodeGif(gif, w, h)
        assertEquals(frames.size, decoded.frames.size)
        frames.forEachIndexed { i, f -> assertContentEquals(f, decoded.composited[i], "frame $i") }
        val independent = frames.sumOf { f -> GifWriter(w, h).apply { addFrame(f, 80) }.finish().size }
        assertTrue(gif.size * 3 < independent)
        assertTrue(decoded.frames.drop(1).all { it.width * it.height < w * h / 4 })
    }

    @Test
    fun spriteOnTransparentCanvasErasesItsOldPosition() {
        val w = 120
        val h = 80
        val frames = List(8) { i -> IntArray(w * h).also { sprite(it, w, 10 + i * 12, 20 + (i % 2) * 10, 20, 16) } } + listOf(IntArray(w * h))
        val gif = GifWriter(w, h).apply { frames.forEach { addFrame(it, 60) } }.finish()
        val decoded = decodeGif(gif, w, h)
        assertEquals(frames.size, decoded.frames.size)
        frames.forEachIndexed { i, f -> assertContentEquals(f, decoded.composited[i], "frame $i") }
        assertTrue(decoded.frames.any { it.disposal == "restoreToBackgroundColor" })
    }

    @Test
    fun movingTransparentHoleOverPhoto() {
        val w = 96
        val h = 64
        val base = poster(w, h, 60)
        val frames = List(6) { i ->
            base.copyOf().also { f ->
                for (y in 10 until 40) for (x in 5 + i * 10 until 25 + i * 10) f[y * w + x] = 0
            }
        }
        val decoded = decodeGif(GifWriter(w, h).apply { frames.forEach { addFrame(it, 50) } }.finish(), w, h)
        frames.forEachIndexed { i, f -> assertContentEquals(f, decoded.composited[i], "frame $i") }
    }

    @Test
    fun repeatedFramesMergeIntoOneLongerFrame() {
        val a = poster(30, 20, 10)
        val b = poster(30, 20, 10, seed = 9)
        val writer = GifWriter(30, 20)
        writer.addFrame(a, 100)
        writer.addFrame(a.copyOf(), 100)
        repeat(3) { writer.addFrame(b, 50) }
        val decoded = decodeGif(writer.finish(), 30, 20)
        assertEquals(listOf(20, 15), decoded.frames.map { it.delayCs })
        assertContentEquals(b, decoded.composited[1])
    }

    @Test
    fun fullyTransparentFramesStillCount() {
        val w = 10
        val h = 10
        val writer = GifWriter(w, h)
        writer.addFrame(IntArray(w * h), 100)
        writer.addFrame(poster(w, h, 3), 100)
        writer.addFrame(IntArray(w * h), 100)
        val decoded = decodeGif(writer.finish(), w, h)
        assertEquals(3, decoded.frames.size)
        assertContentEquals(IntArray(w * h), decoded.composited[0])
        assertContentEquals(IntArray(w * h), decoded.composited[2])
    }

    @Test
    fun misuseIsRejected() {
        assertFailsWith<IllegalStateException> { GifWriter(4, 4).finish() }
        assertFailsWith<IllegalArgumentException> { GifWriter(4, 4).addFrame(IntArray(3), 10) }
        val writer = GifWriter(2, 2).apply { addFrame(IntArray(4), 10) }
        writer.finish()
        assertFailsWith<IllegalStateException> { writer.addFrame(IntArray(4), 10) }
    }

    @Test
    fun hundredTwentyFramesAt480p() {
        val w = 480
        val h = 270
        val frames = List(120) { photo(w, h, seed = 5, shift = it * 0.004) }
        val (gif, time) = measureTimedValue {
            val writer = GifWriter(w, h)
            frames.forEach { writer.addFrame(it, 40) }
            writer.finish()
        }
        val reader = ImageIO.getImageReadersByFormatName("gif").next()
        ImageIO.createImageInputStream(ByteArrayInputStream(gif)).use {
            reader.input = it
            assertEquals(120, reader.getNumImages(true))
        }
        assertTrue(time < 60.seconds)
    }
}
