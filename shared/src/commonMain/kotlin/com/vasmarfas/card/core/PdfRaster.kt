package com.vasmarfas.card.core

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile

expect class PdfRaster {
    val pageCount: Int

    suspend fun render(page: Int, width: Int): ImageBitmap

    suspend fun render(page: Int, pageWidth: Int, x: Int, y: Int, w: Int, h: Int): ImageBitmap

    // safe mid-render, the renderer goes away after the page being drawn
    fun close()

    companion object {
        suspend fun open(bytes: ByteArray): PdfRaster
    }
}

// close() may come from the UI thread mid-render. Both sides set their flag before trying the lock,
// whichever comes second does the release
internal class RenderGate(private val release: () -> Unit) {
    private val lock = Mutex()

    @Volatile
    private var closing = false
    private var released = false

    suspend fun <T> use(block: suspend () -> T): T {
        try {
            return lock.withLock {
                check(!released) { "The document is closed" }
                block()
            }
        } finally {
            releaseIfClosing()
        }
    }

    fun close() {
        closing = true
        releaseIfClosing()
    }

    private fun releaseIfClosing() {
        if (!closing || !lock.tryLock()) return
        try {
            if (!released) {
                released = true
                release()
            }
        } finally {
            lock.unlock()
        }
    }
}
