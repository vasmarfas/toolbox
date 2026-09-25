package com.vasmarfas.card.store

import java.util.prefs.AbstractPreferences
import java.util.prefs.Preferences
import java.util.prefs.PreferencesFactory

class MemoryPreferencesFactory : PreferencesFactory {
    override fun systemRoot(): Preferences = root

    override fun userRoot(): Preferences = root

    companion object {
        val root = MemoryPreferences(null, "")
    }
}

class MemoryPreferences(parent: AbstractPreferences?, name: String) : AbstractPreferences(parent, name) {
    private val values = mutableMapOf<String, String>()
    private val children = mutableMapOf<String, MemoryPreferences>()

    override fun putSpi(key: String, value: String) {
        values[key] = value
    }

    override fun getSpi(key: String): String? = values[key]

    override fun removeSpi(key: String) {
        values.remove(key)
    }

    override fun removeNodeSpi() = Unit

    override fun keysSpi(): Array<String> = values.keys.toTypedArray()

    override fun childrenNamesSpi(): Array<String> = children.keys.toTypedArray()

    override fun childSpi(name: String): AbstractPreferences = children.getOrPut(name) { MemoryPreferences(this, name) }

    override fun syncSpi() = Unit

    override fun flushSpi() = Unit

    fun reset() {
        values.clear()
        children.values.forEach { it.reset() }
    }
}
