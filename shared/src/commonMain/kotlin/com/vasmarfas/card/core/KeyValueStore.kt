package com.vasmarfas.card.core

interface KeyValueStore {
    fun get(key: String): String?
    fun put(key: String, value: String)
    fun remove(key: String)
}

expect fun createKeyValueStore(): KeyValueStore

object Prefs {
    val store: KeyValueStore by lazy { createKeyValueStore() }
}
