package com.vasmarfas.card.core

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.download

actual suspend fun saveBytes(bytes: ByteArray, fileName: String): Boolean {
    FileKit.download(bytes, fileName)
    return true
}
