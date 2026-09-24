package com.vasmarfas.card.core

import com.vasmarfas.card.resources.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.test.Test
import kotlin.test.assertTrue

// the bundled paths are plain strings, only reading them proves they still point at a file
@OptIn(ExperimentalResourceApi::class)
class BundledContentTest {
    @Test
    fun everyBundledPathResolves() = runBlocking {
        listOf(
            AppConfig.PROFILE_BUNDLED_PATH,
            AppConfig.RESUME_BUNDLED_PATH,
            AppConfig.AVATAR_BUNDLED_PATH,
        ).forEach { path ->
            assertTrue(Res.readBytes(path).isNotEmpty(), "empty or missing: $path")
        }
    }
}
