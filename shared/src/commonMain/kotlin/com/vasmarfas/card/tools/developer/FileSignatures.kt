package com.vasmarfas.card.tools.developer

class FileKind(val mime: String, val extension: String)

object FileSignatures {
    private fun ByteArray.at(offset: Int, vararg values: Int): Boolean =
        size >= offset + values.size && values.indices.all { this[offset + it].toInt() and 0xFF == values[it] }

    private fun ByteArray.ascii(offset: Int, text: String): Boolean = at(offset, *text.map { it.code }.toIntArray())

    private fun ByteArray.holds(text: String): Boolean {
        val probe = text.encodeToByteArray()
        val window = minOf(size, 1 shl 20)
        outer@ for (i in 0..window - probe.size) {
            for (j in probe.indices) if (this[i + j] != probe[j]) continue@outer
            return true
        }
        val tail = maxOf(window, size - (1 shl 20))
        outer@ for (i in tail..size - probe.size) {
            for (j in probe.indices) if (this[i + j] != probe[j]) continue@outer
            return true
        }
        return false
    }

    fun detect(bytes: ByteArray): FileKind? = when {
        bytes.at(0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) -> FileKind("image/png", "png")
        bytes.at(0, 0xFF, 0xD8, 0xFF) -> FileKind("image/jpeg", "jpg")
        bytes.ascii(0, "GIF87a") || bytes.ascii(0, "GIF89a") -> FileKind("image/gif", "gif")
        bytes.ascii(0, "RIFF") && bytes.ascii(8, "WEBP") -> FileKind("image/webp", "webp")
        bytes.ascii(0, "RIFF") && bytes.ascii(8, "WAVE") -> FileKind("audio/wav", "wav")
        bytes.ascii(0, "RIFF") && bytes.ascii(8, "AVI ") -> FileKind("video/x-msvideo", "avi")
        bytes.ascii(4, "ftyp") -> ftyp(bytes)
        bytes.ascii(0, "%PDF-") -> FileKind("application/pdf", "pdf")
        bytes.at(0, 0x50, 0x4B, 0x03, 0x04) -> zip(bytes)
        bytes.at(0, 0x1F, 0x8B) -> FileKind("application/gzip", "gz")
        bytes.at(0, 0x37, 0x7A, 0xBC, 0xAF, 0x27, 0x1C) -> FileKind("application/x-7z-compressed", "7z")
        bytes.ascii(0, "Rar!") -> FileKind("application/vnd.rar", "rar")
        bytes.at(0, 0xFD, 0x37, 0x7A, 0x58, 0x5A, 0x00) -> FileKind("application/x-xz", "xz")
        bytes.ascii(0, "BZh") -> FileKind("application/x-bzip2", "bz2")
        bytes.ascii(257, "ustar") -> FileKind("application/x-tar", "tar")
        bytes.ascii(0, "fLaC") -> FileKind("audio/flac", "flac")
        bytes.ascii(0, "OggS") -> FileKind("audio/ogg", "ogg")
        bytes.ascii(0, "ID3") || bytes.size > 1 && bytes[0].toInt() and 0xFF == 0xFF && bytes[1].toInt() and 0xE6 == 0xE2 -> FileKind("audio/mpeg", "mp3")
        bytes.ascii(0, "MThd") -> FileKind("audio/midi", "mid")
        bytes.at(0, 0x1A, 0x45, 0xDF, 0xA3) -> if (bytes.copyOf(minOf(bytes.size, 64)).decodeToString().contains("webm")) FileKind("video/webm", "webm") else FileKind("video/x-matroska", "mkv")
        bytes.ascii(0, "SQLite format 3") -> FileKind("application/vnd.sqlite3", "sqlite")
        bytes.at(0, 0x7F, 0x45, 0x4C, 0x46) -> FileKind("application/x-elf", "")
        bytes.ascii(0, "MZ") -> FileKind("application/vnd.microsoft.portable-executable", "exe")
        bytes.at(0, 0xCA, 0xFE, 0xBA, 0xBE) -> FileKind("application/java-vm", "class")
        bytes.at(0, 0xCF, 0xFA, 0xED, 0xFE) || bytes.at(0, 0xCE, 0xFA, 0xED, 0xFE) -> FileKind("application/x-mach-binary", "")
        bytes.at(0, 0x00, 0x61, 0x73, 0x6D) -> FileKind("application/wasm", "wasm")
        bytes.ascii(0, "wOFF") -> FileKind("font/woff", "woff")
        bytes.ascii(0, "wOF2") -> FileKind("font/woff2", "woff2")
        bytes.ascii(0, "OTTO") -> FileKind("font/otf", "otf")
        bytes.at(0, 0x00, 0x01, 0x00, 0x00, 0x00) -> FileKind("font/ttf", "ttf")
        bytes.ascii(0, "8BPS") -> FileKind("image/vnd.adobe.photoshop", "psd")
        bytes.ascii(0, "AT&TFORM") -> FileKind("image/vnd.djvu", "djvu")
        bytes.ascii(0, "II*") && bytes.at(3, 0) || bytes.ascii(0, "MM") && bytes.at(2, 0, 0x2A) -> FileKind("image/tiff", "tif")
        bytes.at(0, 0x00, 0x00, 0x01, 0x00) -> FileKind("image/x-icon", "ico")
        bytes.ascii(0, "BM") && bytes.size > 26 -> FileKind("image/bmp", "bmp")
        bytes.ascii(0, "{\\rtf") -> FileKind("application/rtf", "rtf")
        else -> text(bytes)
    }

    private fun ftyp(bytes: ByteArray): FileKind {
        val brand = bytes.copyOfRange(8, minOf(bytes.size, 12)).decodeToString()
        return when (brand) {
            "heic", "heix", "hevc", "mif1", "msf1" -> FileKind("image/heic", "heic")
            "avif", "avis" -> FileKind("image/avif", "avif")
            "qt  " -> FileKind("video/quicktime", "mov")
            "M4A ", "M4B " -> FileKind("audio/mp4", "m4a")
            "3gp4", "3gp5", "3gp6", "3g2a" -> FileKind("video/3gpp", "3gp")
            else -> FileKind("video/mp4", "mp4")
        }
    }

    private fun zip(bytes: ByteArray): FileKind = when {
        bytes.ascii(30, "mimetypeapplication/epub+zip") -> FileKind("application/epub+zip", "epub")
        bytes.ascii(30, "mimetypeapplication/vnd.oasis.opendocument.text") -> FileKind("application/vnd.oasis.opendocument.text", "odt")
        bytes.ascii(30, "mimetypeapplication/vnd.oasis.opendocument.spreadsheet") -> FileKind("application/vnd.oasis.opendocument.spreadsheet", "ods")
        bytes.holds("word/document.xml") -> FileKind("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx")
        bytes.holds("xl/workbook.xml") -> FileKind("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx")
        bytes.holds("ppt/presentation.xml") -> FileKind("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx")
        bytes.holds("AndroidManifest.xml") -> FileKind("application/vnd.android.package-archive", "apk")
        bytes.holds("META-INF/MANIFEST.MF") -> FileKind("application/java-archive", "jar")
        else -> FileKind("application/zip", "zip")
    }

    private fun text(bytes: ByteArray): FileKind? {
        val start = bytes.copyOf(minOf(bytes.size, 4096)).decodeToString().trimStart('﻿')
        if (start.isEmpty() || start.dropLast(2).any { it == '�' || it.code < 32 && it !in "\n\r\t" }) return null
        val head = start.trimStart()
        return when {
            head.startsWith("<?xml") -> if (head.contains("<svg")) FileKind("image/svg+xml", "svg") else FileKind("application/xml", "xml")
            head.startsWith("<svg") -> FileKind("image/svg+xml", "svg")
            head.startsWith("<!DOCTYPE html", ignoreCase = true) || head.startsWith("<html", ignoreCase = true) -> FileKind("text/html", "html")
            head.startsWith("{") || head.startsWith("[") -> FileKind("application/json", "json")
            else -> FileKind("text/plain", "txt")
        }
    }
}
