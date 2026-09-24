package com.vasmarfas.card.core

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.write

actual suspend fun saveBytes(bytes: ByteArray, fileName: String): Boolean {
    val target = FileKit.openFileSaver(
        suggestedName = fileName.substringBeforeLast('.', fileName),
        defaultExtension = fileName.substringAfterLast('.', "").ifEmpty { null },
    ) ?: return false
    target.write(bytes)
    return true
}
