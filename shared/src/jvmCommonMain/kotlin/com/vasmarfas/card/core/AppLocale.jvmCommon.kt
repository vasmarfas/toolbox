package com.vasmarfas.card.core

import java.util.Locale

actual fun applyPlatformLocale(tag: String) {
    Locale.setDefault(Locale.forLanguageTag(tag))
}

actual fun regionName(code: String, lang: Lang): String? =
    Locale.Builder().setRegion(code).build().getDisplayCountry(Locale.forLanguageTag(lang.code)).takeIf { it.isNotEmpty() && it != code }
