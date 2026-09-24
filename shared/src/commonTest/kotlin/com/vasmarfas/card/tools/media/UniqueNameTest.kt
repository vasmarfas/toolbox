package com.vasmarfas.card.tools.media

import kotlin.test.Test
import kotlin.test.assertEquals

class UniqueNameTest {
    @Test
    fun repeatedNamesGetANumberBeforeTheExtension() {
        val taken = mutableSetOf<String>()
        assertEquals(
            listOf("photo.jpg", "photo (2).jpg", "PHOTO (3).JPG", "README", "README (2)", ".env", ".env (2)"),
            listOf("photo.jpg", "photo.jpg", "PHOTO.JPG", "README", "README", ".env", ".env").map { uniqueName(it, taken) },
        )
    }
}
