package com.vasmarfas.card.core

import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleCountryCode
import platform.Foundation.NSUserDefaults

private const val LANGUAGES_KEY = "AppleLanguages"

actual fun applyPlatformLocale(tag: String) {
    NSUserDefaults.standardUserDefaults.setObject(listOf(tag), LANGUAGES_KEY)
}

actual fun regionName(code: String, lang: Lang): String? =
    NSLocale(localeIdentifier = lang.code).displayNameForKey(NSLocaleCountryCode, code)
