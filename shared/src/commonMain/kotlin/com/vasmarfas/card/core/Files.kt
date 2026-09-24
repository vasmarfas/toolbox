package com.vasmarfas.card.core

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.name

enum class PickKind { IMAGE, VIDEO, MEDIA, FILE }

suspend fun pickFiles(extensions: Set<String>, kind: PickKind = PickKind.FILE, multiple: Boolean = false): List<PlatformFile> {
    val type = when {
        currentPlatform != PlatformKind.IOS || kind == PickKind.FILE ->
            if (extensions.isEmpty() || !extensions.all(::canFilterByExtension)) FileKitType.File() else FileKitType.File(extensions)
        kind == PickKind.IMAGE -> FileKitType.Image
        kind == PickKind.VIDEO -> FileKitType.Video
        else -> FileKitType.ImageAndVideo
    }
    return if (multiple) {
        FileKit.openFilePicker(type, FileKitMode.Multiple()).orEmpty()
    } else {
        listOfNotNull(FileKit.openFilePicker(type))
    }
}

// the Android picker filters by MIME type, so extensions the system has no type for (.fb2, .kt) would
// be greyed out
internal expect fun canFilterByExtension(extension: String): Boolean

expect suspend fun saveBytes(bytes: ByteArray, fileName: String): Boolean

val PlatformFile.baseName: String get() = name.substringBeforeLast('.', name)

fun renamed(fileName: String, extension: String, suffix: String = ""): String {
    val base = fileName.substringBeforeLast('.', fileName).ifBlank { "file" }
    return "$base$suffix.$extension"
}
