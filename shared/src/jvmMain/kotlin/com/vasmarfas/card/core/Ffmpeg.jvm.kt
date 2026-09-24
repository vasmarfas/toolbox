package com.vasmarfas.card.core

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

internal object FfmpegArgs {
    const val SAMPLE_RATE = 48_000

    private val mp4Video = setOf("h264", "hevc", "av1", "mpeg4")
    private val mp4Audio = setOf("aac", "mp3", "opus", "alac", "flac", "ac3", "eac3")
    private val webmVideo = setOf("vp8", "vp9", "av1")
    private val webmAudio = setOf("opus", "vorbis")

    fun export(project: MediaProject, infos: Map<PlatformFile, MediaInfo>, output: String): List<String> {
        val spec = project.spec
        val base = listOf("-hide_banner", "-nostdin", "-y", "-progress", "pipe:1", "-nostats")
        val single = project.single
        if (spec.copyStreams && single != null && single.kind != ClipKind.IMAGE) {
            return base + remux(single, infos.getValue(single.file), spec, output)
        }
        val video = spec.format.video
        val first = project.firstPicture?.let { infos[it.file] }
        val (width, height) = spec.frameSize(first)
        val fps = spec.frameRate(first)
        val graph = graph(project, infos, width, height, fps, 0, project.durationMs, video, spec.keepAudio)
        val args = base.toMutableList()
        args += graph.inputs
        args += listOf("-filter_complex", graph.filters)
        if (video) args += listOf("-map", "[vout]")
        if (spec.keepAudio) args += listOf("-map", "[aout]")
        if (video) args += videoCodec(spec, width, height, fps, project.durationMs)
        if (spec.keepAudio) args += audioCodec(spec)
        args += containerFlags(spec.format)
        args += output
        return args
    }

    class Graph(val inputs: List<String>, val filters: String)

    // a clip cut by fromMs keeps its own time through the fades by shifting timestamps, so the preview
    // starts anywhere without decoding the skipped part
    fun graph(project: MediaProject, infos: Map<PlatformFile, MediaInfo>, width: Int, height: Int, fps: Int, fromMs: Long, toMs: Long, video: Boolean, audio: Boolean): Graph {
        val inputs = mutableListOf<String>()
        val filters = mutableListOf<String>()
        val length = seconds((toMs - fromMs).coerceAtLeast(1))
        var count = 0
        fun visible(clip: MediaClip) = clip.endAtMs > fromMs && clip.atMs < toMs
        fun open(clip: MediaClip): Int {
            val skip = (fromMs - clip.atMs).coerceAtLeast(0)
            val duration = seconds(minOf(clip.endAtMs, toMs) - clip.atMs - skip)
            inputs += if (clip.kind == ClipKind.IMAGE) {
                listOf("-loop", "1", "-framerate", fps.toString(), "-t", duration, "-i", clip.file.absolutePath())
            } else {
                listOf("-ss", seconds(clip.startMs + skip), "-t", duration, "-i", clip.file.absolutePath())
            }
            return count++
        }
        val opened = HashMap<MediaClip, Int>()
        if (video) {
            filters += "color=c=black:s=${width}x$height:r=$fps:d=$length,format=yuv420p[base]"
            var below = "[base]"
            var layer = 0
            project.pictures.forEach { track ->
                track.clips.filter(::visible).forEach { clip ->
                    val info = infos[clip.file]
                    val input = open(clip).also { opened[clip] = it }
                    val skip = seconds((fromMs - clip.atMs).coerceAtLeast(0))
                    val offset = seconds((clip.atMs - fromMs).coerceAtLeast(0))
                    val box = clip.box.place(info?.width ?: width, info?.height ?: height, width, height, clip.fill)
                    val chain = StringBuilder("[$input:v]setpts=PTS-STARTPTS+$skip/TB")
                    if (clip.fill) chain.append(",scale=$width:$height:force_original_aspect_ratio=increase,crop=$width:$height")
                    chain.append(",scale=").append(box.width.roundToInt().coerceAtLeast(1)).append(':').append(box.height.roundToInt().coerceAtLeast(1))
                    chain.append(",setsar=1,format=rgba")
                    if (clip.fadeInMs > 0) chain.append(",fade=t=in:st=0:d=").append(seconds(clip.fadeInMs)).append(":alpha=1")
                    if (clip.fadeOutMs > 0) {
                        chain.append(",fade=t=out:st=").append(seconds((clip.durationMs - clip.fadeOutMs).coerceAtLeast(0))).append(":d=").append(seconds(clip.fadeOutMs)).append(":alpha=1")
                    }
                    if (clip.opacity < 1f) chain.append(",colorchannelmixer=aa=").append(clip.opacity.toDouble().fmt(3))
                    chain.append(",setpts=PTS-STARTPTS+$offset/TB[c$layer]")
                    filters += chain.toString()
                    filters += "$below[c$layer]overlay=x=${box.left.roundToInt()}:y=${box.top.roundToInt()}:eof_action=pass:format=auto[o$layer]"
                    below = "[o$layer]"
                    layer++
                }
            }
            filters += "${below}format=yuv420p[vout]"
        }
        if (audio) {
            val sounds = project.sounds.filter { visible(it) && (it.kind == ClipKind.AUDIO || infos[it.file]?.hasAudio == true) }
            sounds.forEachIndexed { i, clip ->
                val input = opened[clip] ?: open(clip)
                val skip = seconds((fromMs - clip.atMs).coerceAtLeast(0))
                val chain = StringBuilder("[$input:a]asetpts=PTS-STARTPTS+$skip/TB,")
                chain.append("aresample=$SAMPLE_RATE,aformat=sample_fmts=fltp:channel_layouts=stereo")
                if (clip.volume != 1f) chain.append(",volume=").append(clip.volume.toDouble().fmt(3))
                if (clip.fadeInMs > 0) chain.append(",afade=t=in:st=0:d=").append(seconds(clip.fadeInMs))
                if (clip.fadeOutMs > 0) {
                    chain.append(",afade=t=out:st=").append(seconds((clip.durationMs - clip.fadeOutMs).coerceAtLeast(0))).append(":d=").append(seconds(clip.fadeOutMs))
                }
                chain.append(",asetpts=PTS-STARTPTS")
                val delay = (clip.atMs - fromMs).coerceAtLeast(0)
                if (delay > 0) chain.append(",adelay=").append(delay).append(":all=1")
                chain.append("[s$i]")
                filters += chain.toString()
            }
            filters += if (sounds.isEmpty()) {
                "anullsrc=r=$SAMPLE_RATE:cl=stereo,atrim=duration=$length[aout]"
            } else {
                sounds.indices.joinToString("") { "[s$it]" } + "amix=inputs=${sounds.size}:duration=longest:dropout_transition=0:normalize=0,apad,atrim=duration=$length[aout]"
            }
        }
        return Graph(inputs, filters.joinToString(";"))
    }

    private fun remux(clip: MediaClip, info: MediaInfo, spec: MediaSpec, output: String): List<String> {
        val args = mutableListOf<String>()
        if (clip.startMs > 0) args += listOf("-ss", seconds(clip.startMs))
        if (info.durationMs > 0 && clip.endMs < info.durationMs) args += listOf("-t", seconds(clip.durationMs))
        args += listOf("-i", clip.file.absolutePath())
        val format = spec.format
        if (format.video && info.hasVideo) {
            args += listOf("-map", "0:v:0")
            val keep = when (format) {
                MediaFormat.MP4, MediaFormat.MOV -> info.videoCodec in mp4Video
                MediaFormat.WEBM -> info.videoCodec in webmVideo
                else -> true
            }
            args += if (keep) listOf("-c:v", "copy") else videoCodec(spec, info.width, info.height, info.frameRate.roundToInt().coerceIn(1, 60), clip.durationMs)
        } else {
            args += "-vn"
        }
        if (spec.keepAudio && info.hasAudio) {
            args += listOf("-map", "0:a?")
            val keep = when (format) {
                MediaFormat.MP4, MediaFormat.MOV, MediaFormat.M4A -> info.audioCodec in mp4Audio
                MediaFormat.WEBM -> info.audioCodec in webmAudio
                MediaFormat.MKV -> true
                MediaFormat.MP3 -> info.audioCodec == "mp3"
                MediaFormat.FLAC -> info.audioCodec == "flac"
                MediaFormat.OGG -> info.audioCodec == "opus" || info.audioCodec == "vorbis"
                MediaFormat.WAV -> info.audioCodec?.startsWith("pcm_") == true
            }
            args += if (keep) listOf("-c:a", "copy") else audioCodec(spec)
        } else {
            args += "-an"
        }
        args += listOf("-sn", "-dn")
        args += containerFlags(format)
        args += output
        return args
    }

    private fun videoCodec(spec: MediaSpec, width: Int, height: Int, fps: Int, durationMs: Long): List<String> {
        val bitrate = spec.videoBitrate(width, height, fps, durationMs)
        return if (spec.format == MediaFormat.WEBM) {
            listOf("-c:v", "libvpx-vp9", "-b:v", bitrate.toString(), "-deadline", "good", "-cpu-used", "4", "-row-mt", "1")
        } else {
            listOf(
                "-c:v", "libopenh264", "-rc_mode", "bitrate", "-b:v", bitrate.toString(), "-maxrate", (bitrate * 3 / 2).toString(),
                "-profile:v", "high", "-pix_fmt", "yuv420p",
            )
        }
    }

    fun audioCodec(spec: MediaSpec): List<String> {
        val bitrate = "${spec.audioBitrateKbps}k"
        val codec = when (spec.format) {
            MediaFormat.WEBM, MediaFormat.OGG -> listOf("-c:a", "libopus", "-b:a", bitrate)
            MediaFormat.MP3 -> listOf("-c:a", "libmp3lame", "-b:a", bitrate)
            MediaFormat.WAV -> listOf("-c:a", "pcm_s16le")
            MediaFormat.FLAC -> listOf("-c:a", "flac")
            else -> listOf("-c:a", "aac", "-b:a", bitrate)
        }
        val rate = if (spec.sampleRate > 0 && spec.format != MediaFormat.OGG && spec.format != MediaFormat.WEBM) listOf("-ar", spec.sampleRate.toString()) else emptyList()
        val channels = if (spec.channels > 0) listOf("-ac", spec.channels.toString()) else emptyList()
        return codec + rate + channels
    }

    fun containerFlags(format: MediaFormat): List<String> = when (format) {
        MediaFormat.MP4, MediaFormat.MOV, MediaFormat.M4A -> listOf("-movflags", "+faststart")
        else -> emptyList()
    }

    fun seconds(ms: Long): String = (ms / 1000.0).fmt(3)

    fun parseProbe(json: JsonObject): MediaInfo {
        val format = json["format"]?.jsonObject
        val streams = (json["streams"] as? JsonArray).orEmpty().map { it.jsonObject }
        val videoStream = streams.firstOrNull { it.string("codec_type") == "video" && it["disposition"]?.jsonObject?.get("attached_pic")?.jsonPrimitive?.content != "1" }
        val audioStream = streams.firstOrNull { it.string("codec_type") == "audio" }
        val rotation = videoStream?.let { stream ->
            stream["side_data_list"]?.jsonArray?.firstNotNullOfOrNull { it.jsonObject.string("rotation")?.toDoubleOrNull() }
                ?: stream["tags"]?.jsonObject?.string("rotate")?.toDoubleOrNull()
        } ?: 0.0
        val quarter = abs(rotation.roundToInt()) % 180 == 90
        val w = videoStream?.string("width")?.toIntOrNull() ?: 0
        val h = videoStream?.string("height")?.toIntOrNull() ?: 0
        val duration = format?.string("duration")?.toDoubleOrNull()
            ?: videoStream?.string("duration")?.toDoubleOrNull()
            ?: audioStream?.string("duration")?.toDoubleOrNull()
            ?: 0.0
        return MediaInfo(
            durationMs = (duration * 1000).roundToLong(),
            width = if (quarter) h else w,
            height = if (quarter) w else h,
            frameRate = videoStream?.string("avg_frame_rate")?.let(::ratio)?.takeIf { it > 0 }
                ?: videoStream?.string("r_frame_rate")?.let(::ratio) ?: 0.0,
            videoCodec = videoStream?.string("codec_name"),
            audioCodec = audioStream?.string("codec_name"),
            sampleRate = audioStream?.string("sample_rate")?.toIntOrNull() ?: 0,
            channels = audioStream?.string("channels")?.toIntOrNull() ?: 0,
            bitrate = format?.string("bit_rate")?.toLongOrNull() ?: 0,
        )
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.content

    private fun ratio(value: String): Double {
        val num = value.substringBefore('/').toDoubleOrNull() ?: return 0.0
        val den = value.substringAfter('/', "1").toDoubleOrNull() ?: return 0.0
        return if (den == 0.0) 0.0 else num / den
    }
}
