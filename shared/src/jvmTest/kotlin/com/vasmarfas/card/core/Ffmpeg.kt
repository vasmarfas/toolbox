package com.vasmarfas.card.core

import org.bytedeco.ffmpeg.ffmpeg
import org.bytedeco.ffmpeg.ffprobe
import org.bytedeco.javacpp.Loader
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

object Ffmpeg {
    private val ffmpegPath: String by lazy { Loader.load(ffmpeg::class.java) }
    private val ffprobePath: String by lazy { Loader.load(ffprobe::class.java) }

    class Result(val exit: Int, val stdout: ByteArray, val stderr: String)

    val work: File = File("build", "mp3-encoder-tests").absoluteFile.apply { mkdirs() }

    fun run(vararg args: String, input: ByteArray? = null): Result {
        val process = ProcessBuilder(*args).start()
        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()
        val readers = listOf(
            thread { process.inputStream.copyTo(stdout) },
            thread { process.errorStream.copyTo(stderr) },
        )
        process.outputStream.use { stream -> input?.let { stream.write(it) } }
        check(process.waitFor(5, TimeUnit.MINUTES)) { "${args.first()} timed out" }
        readers.forEach { it.join() }
        return Result(process.exitValue(), stdout.toByteArray(), stderr.toString(Charsets.UTF_8))
    }

    fun probe(file: File): Pair<Map<String, String>, String> {
        val result = run(
            ffprobePath, "-v", "error", "-show_entries",
            "stream=codec_name,sample_rate,channels,bit_rate,duration:format=duration,bit_rate",
            "-of", "flat", file.path,
        )
        val fields = result.stdout.toString(Charsets.UTF_8).lines().filter { '=' in it }.associate { line ->
            val (key, value) = line.split('=', limit = 2)
            key to value.trim('"')
        }
        return fields to (result.stderr + if (result.exit != 0) "exit ${result.exit}" else "")
    }

    fun decode(file: File, channels: Int? = null): Pair<ShortArray, String> {
        val args = mutableListOf(ffmpegPath, "-v", "error", "-i", file.path)
        if (channels != null) args += listOf("-ac", channels.toString())
        args += listOf("-f", "s16le", "-")
        val result = run(*args.toTypedArray())
        val buffer = ByteBuffer.wrap(result.stdout).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val samples = ShortArray(buffer.remaining())
        buffer.get(samples)
        return samples to (result.stderr + if (result.exit != 0) "exit ${result.exit}" else "")
    }

    fun generate(graph: String, sampleRate: Int, channels: Int, seconds: Double): ShortArray {
        val result = run(
            ffmpegPath, "-v", "error", "-f", "lavfi", "-i", graph, "-t", seconds.toString(),
            "-ar", sampleRate.toString(), "-ac", channels.toString(), "-f", "s16le", "-",
        )
        check(result.exit == 0) { result.stderr }
        val buffer = ByteBuffer.wrap(result.stdout).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        return ShortArray(buffer.remaining()).also { buffer.get(it) }
    }
}
