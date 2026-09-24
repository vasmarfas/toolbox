package com.vasmarfas.card.core

import android.webkit.MimeTypeMap

internal actual fun canFilterByExtension(extension: String): Boolean = MimeTypeMap.getSingleton().hasExtension(extension)
