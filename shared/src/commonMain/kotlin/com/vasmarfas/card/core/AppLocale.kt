package com.vasmarfas.card.core

/**
 * Compose Resources reads the language from the platform, not from a composition local: the JVM
 * default locale, the iOS preferred language list, the `navigator` properties in the browser.
 * AppSettings moves that value on construction and on every switch — outside composition, because
 * a recomposition may be skipped and a skipped call leaves the whole tree on the previous language.
 */
expect fun applyPlatformLocale(tag: String)
