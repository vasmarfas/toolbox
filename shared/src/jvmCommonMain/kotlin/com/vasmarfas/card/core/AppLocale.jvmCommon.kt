package com.vasmarfas.card.core

import java.util.Locale

actual fun applyPlatformLocale(tag: String) {
    Locale.setDefault(Locale.forLanguageTag(tag))
}
