package com.vasmarfas.card.core

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.runBlocking
import org.bytedeco.ffmpeg.ffmpeg
import org.bytedeco.javacpp.Loader
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MediaEngineTest {
    private val dir: File = createTempDirectory("media-test").toFile()

    @AfterTest
    fun cleanUp() {
        dir.deleteRecursively()
    }

    private fun generate(name: String, vararg args: String): PlatformFile {
        val out = File(dir, name)
        val command = listOf(Loader.load(ffmpeg::class.java), "-hide_banner", "-loglevel", "error", "-y") + args + out.absolutePath
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val log = process.inputStream.bufferedReader().readText()
        assertEquals(0, process.waitFor(), log)
        return PlatformFile(out.absolutePath)
    }

    private fun clip(seconds: Int, size: String = "320x240", name: String = "clip$seconds-$size.mkv"): PlatformFile = generate(
        name,
        "-f", "lavfi", "-i", "testsrc=size=$size:rate=25:duration=$seconds",
        "-f", "lavfi", "-i", "sine=frequency=440:sample_rate=44100:duration=$seconds",
        "-c:v", "libopenh264", "-b:v", "500k", "-c:a", "aac", "-shortest",
    )

    private fun near(expected: Long, actual: Long, tolerance: Long = 150) =
        assertTrue(abs(expected - actual) <= tolerance, "expected about $expected ms, got $actual ms")

    @Test
    fun probesDurationSizeAndStreams() = runBlocking {
        val info = MediaEngine.probe(clip(3))
        near(3000, info.durationMs)
        assertEquals(320, info.width)
        assertEquals(240, info.height)
        assertEquals("h264", info.videoCodec)
        assertEquals("aac", info.audioCodec)
        assertEquals(44100, info.sampleRate)
    }

    @Test
    fun trimsAndReencodes() = runBlocking {
        val source = clip(4)
        var last = 0f
        val result = MediaEngine.export(
            MediaProject(MediaClip(source, ClipKind.VIDEO, 1000, 3000), MediaSpec(MediaFormat.MP4, height = 180)),
        ) { last = it }
        val info = MediaEngine.probe(PlatformFile(result.file.path))
        near(2000, info.durationMs)
        assertEquals(240, info.width)
        assertEquals(180, info.height)
        assertTrue(last > 0.9f, "progress reached $last")
        result.discard()
    }

    @Test
    fun remuxesWithoutReencodingWhenTheContainerAllowsIt() = runBlocking {
        val source = clip(2)
        val result = MediaEngine.export(
            MediaProject(MediaClip(source, ClipKind.VIDEO, 0, 2000), MediaSpec(MediaFormat.MP4, copyStreams = true)),
        ) {}
        val info = MediaEngine.probe(result.file)
        assertEquals("h264", info.videoCodec)
        assertEquals("aac", info.audioCodec)
        near(2000, info.durationMs)
    }

    @Test
    fun extractsAudioInEveryAudioFormat() = runBlocking {
        val source = clip(2)
        listOf(MediaFormat.MP3, MediaFormat.M4A, MediaFormat.WAV, MediaFormat.FLAC, MediaFormat.OGG).forEach { format ->
            val result = MediaEngine.export(
                MediaProject(MediaClip(source, ClipKind.VIDEO, 0, 2000), MediaSpec(format, audioBitrateKbps = 128)),
            ) {}
            val info = MediaEngine.probe(result.file)
            assertTrue(!info.hasVideo, "$format kept video")
            assertEquals(
                when (format) {
                    MediaFormat.MP3 -> "mp3"
                    MediaFormat.M4A -> "aac"
                    MediaFormat.WAV -> "pcm_s16le"
                    MediaFormat.FLAC -> "flac"
                    else -> "opus"
                },
                info.audioCodec,
            )
            near(2000, info.durationMs, 200)
        }
    }

    @Test
    fun joinsClipsImagesAndMusic() = runBlocking {
        val a = clip(2)
        val b = clip(3, size = "640x360")
        val image = generate("still.png", "-f", "lavfi", "-i", "color=c=orange:size=400x400", "-frames:v", "1")
        val music = generate("music.mp3", "-f", "lavfi", "-i", "sine=frequency=220:sample_rate=48000:duration=1.5", "-c:a", "libmp3lame")
        val project = MediaProject(
            listOf(
                MediaTrack(
                    TrackKind.VIDEO,
                    listOf(
                        MediaClip(a, ClipKind.VIDEO, 0, 2000, volume = 0.5f, fadeInMs = 300),
                        MediaClip(image, ClipKind.IMAGE, 0, 1500, atMs = 2000),
                        MediaClip(b, ClipKind.VIDEO, 500, 2500, atMs = 3500, fadeOutMs = 500),
                    ),
                ),
                MediaTrack(TrackKind.VIDEO, listOf(MediaClip(image, ClipKind.IMAGE, 0, 1000, atMs = 500, box = ClipBox(0.2f, 0.2f, 0.3f), opacity = 0.8f))),
                MediaTrack(
                    TrackKind.AUDIO,
                    listOf(
                        MediaClip(music, ClipKind.AUDIO, 0, 1500, atMs = 1000, volume = 0.3f),
                        MediaClip(music, ClipKind.AUDIO, 0, 1500, atMs = 2500, volume = 0.3f, fadeOutMs = 400),
                    ),
                ),
            ),
            MediaSpec(MediaFormat.MP4, width = 640, height = 360, frameRate = 30),
        )
        val result = MediaEngine.export(project) {}
        val info = MediaEngine.probe(result.file)
        near(5500, info.durationMs, 250)
        assertEquals(640, info.width)
        assertEquals(360, info.height)
        assertTrue(info.hasAudio)
    }

    private fun color(image: ImageBitmap, x: Int, y: Int): Triple<Int, Int, Int> {
        val pixel = image.toPixelMap()[x, y]
        return Triple((pixel.red * 255).toInt(), (pixel.green * 255).toInt(), (pixel.blue * 255).toInt())
    }

    @Test
    fun layersPicturesOverBlackAndOverEachOther() = runBlocking {
        val red = generate("red.png", "-f", "lavfi", "-i", "color=c=red:size=320x180", "-frames:v", "1")
        val blue = generate("blue.png", "-f", "lavfi", "-i", "color=c=blue:size=160x90", "-frames:v", "1")
        val project = MediaProject(
            listOf(
                MediaTrack(TrackKind.VIDEO, listOf(MediaClip(red, ClipKind.IMAGE, 0, 2000, atMs = 1000))),
                MediaTrack(TrackKind.VIDEO, listOf(MediaClip(blue, ClipKind.IMAGE, 0, 1000, atMs = 1500, box = ClipBox(0.25f, 0.25f, 0.5f)))),
            ),
            MediaSpec(MediaFormat.MP4, width = 320, height = 180, frameRate = 25, keepAudio = false),
        )
        val result = MediaEngine.export(project) {}
        near(3000, MediaEngine.probe(result.file).durationMs)
        val gap = MediaEngine.frame(result.file, 500, 320)!!
        assertTrue(color(gap, 160, 90).let { (r, g, b) -> r < 30 && g < 30 && b < 30 }, "the gap before the first clip is not black")
        val both = MediaEngine.frame(result.file, 2000, 320)!!
        assertTrue(color(both, 40, 22).let { (r, _, b) -> b > 180 && r < 80 }, "the overlay is not in the top left quarter: ${color(both, 40, 22)}")
        assertTrue(color(both, 240, 135).let { (r, _, b) -> r > 180 && b < 80 }, "the base picture is covered: ${color(both, 240, 135)}")
        val after = MediaEngine.frame(result.file, 2700, 320)!!
        assertTrue(color(after, 40, 22).let { (r, _, b) -> r > 180 && b < 80 }, "the overlay stays after its clip ends")
    }

    @Test
    fun mixesSoundFromEveryTrackAtItsPlace() = runBlocking {
        val tone = generate("tone.wav", "-f", "lavfi", "-i", "sine=frequency=500:sample_rate=48000:duration=1")
        val project = MediaProject(
            listOf(
                MediaTrack(TrackKind.AUDIO, listOf(MediaClip(tone, ClipKind.AUDIO, 0, 1000, atMs = 1000))),
                MediaTrack(TrackKind.AUDIO, listOf(MediaClip(tone, ClipKind.AUDIO, 0, 1000, atMs = 2500)), muted = true),
            ),
            MediaSpec(MediaFormat.WAV),
        )
        val pcm = MediaEngine.decodeAudio(MediaEngine.export(project) {}.file, 60_000)
        near(3500, pcm.durationMs, 60)
        fun loudness(fromMs: Int, toMs: Int): Int {
            val from = fromMs * pcm.sampleRate / 1000 * pcm.channels
            val to = minOf(pcm.samples.size, toMs * pcm.sampleRate / 1000 * pcm.channels)
            return (from until to).maxOf { abs(pcm.samples[it].toInt()) }
        }
        assertTrue(loudness(0, 900) < 50, "sound before the clip")
        assertTrue(loudness(1100, 1900) > 2000, "the clip is silent")
        assertTrue(loudness(2600, 3400) < 50, "the muted track is heard")
    }

    @Test
    fun previewGraphStartsInsideAFadingClip() = runBlocking {
        val source = clip(3)
        val project = MediaProject(MediaClip(source, ClipKind.VIDEO, 0, 3000, atMs = 500, fadeInMs = 1000, fadeOutMs = 1000), MediaSpec(MediaFormat.MP4))
        val infos = mapOf(source to MediaEngine.probe(source))
        val graph = FfmpegArgs.graph(project, infos, 160, 120, 10, 1200, 2200, video = true, audio = false)
        var frames = 0
        MediaEngine.execute(
            listOf(MediaEngine.ffmpegPath, "-hide_banner", "-nostdin", "-loglevel", "error") + graph.inputs +
                listOf("-filter_complex", graph.filters, "-map", "[vout]", "-f", "rawvideo", "-pix_fmt", "rgba", "pipe:1"),
        ) { stdout ->
            val frame = ByteArray(160 * 120 * 4)
            while (MediaEngine.readFully(stdout, frame)) frames++
        }
        assertEquals(10, frames)
    }

    @Test
    fun extractsFramesAtTheRequestedRate() = runBlocking {
        var count = 0
        var width = 0
        MediaEngine.frames(clip(2), 0, 1000, 5.0, 160) {
            count++
            width = it.width
        }
        assertEquals(5, count)
        assertEquals(160, width)
        val single = MediaEngine.frame(clip(2), 500, 100)
        assertEquals(100, single?.width)
    }

    @Test
    fun decodesAndEncodesPcm() = runBlocking {
        val pcm = MediaEngine.decodeAudio(clip(2), 60_000)
        assertEquals(44100, pcm.sampleRate)
        assertTrue(pcm.channels in 1..2)
        near(2000, pcm.durationMs, 100)
        val flac = MediaEngine.encodeAudio(pcm.asSource(), MediaFormat.FLAC, 0) {}
        val info = MediaEngine.probe(flac.file)
        assertEquals("flac", info.audioCodec)
        near(2000, info.durationMs, 100)
    }
}
