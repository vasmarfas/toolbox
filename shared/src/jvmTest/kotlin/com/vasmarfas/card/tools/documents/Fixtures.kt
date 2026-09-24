package com.vasmarfas.card.tools.documents

import kotlin.test.assertNotNull

internal fun fixture(name: String): ByteArray = assertNotNull(Fixtures::class.java.getResourceAsStream("/fixtures/$name")).use { it.readBytes() }

private object Fixtures
