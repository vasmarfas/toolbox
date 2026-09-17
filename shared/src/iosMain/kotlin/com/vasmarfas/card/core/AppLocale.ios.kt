package com.vasmarfas.card.core

import platform.Foundation.NSUserDefaults

private const val LANGUAGES_KEY = "AppleLanguages"

actual fun applyPlatformLocale(tag: String) {
    NSUserDefaults.standardUserDefaults.setObject(listOf(tag), LANGUAGES_KEY)
}
