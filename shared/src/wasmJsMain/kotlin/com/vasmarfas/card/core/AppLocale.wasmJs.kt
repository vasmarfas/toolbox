package com.vasmarfas.card.core

// Both properties are read-only by spec, so the only lever is shadowing them with own getters.
// ui-text reads navigator.languages[0], compose-resources reads navigator.language.
actual fun applyPlatformLocale(tag: String) {
    js(
        """{
        Object.defineProperty(navigator, 'language', { get: function () { return tag; }, configurable: true });
        Object.defineProperty(navigator, 'languages', { get: function () { return [tag]; }, configurable: true });
    }"""
    )
}
