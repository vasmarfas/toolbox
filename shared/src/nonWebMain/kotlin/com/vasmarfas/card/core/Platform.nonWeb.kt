package com.vasmarfas.card.core

actual val pullToReload: Boolean = false

actual fun reloadPage() = Unit

actual fun listenForFindShortcut(open: () -> Boolean) = Unit
