package com.vasmarfas.card.core

actual suspend fun ensurePermission(permission: AppPermission): Boolean = true

actual fun hasPermission(permission: AppPermission): Boolean = true
