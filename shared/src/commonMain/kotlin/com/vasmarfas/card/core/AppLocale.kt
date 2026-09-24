package com.vasmarfas.card.core

// Compose Resources takes the language from the platform, not from a composition local. AppSettings
// moves it outside composition, a skipped recomposition would leave the tree on the old language
expect fun applyPlatformLocale(tag: String)

expect fun regionName(code: String, lang: Lang): String?
