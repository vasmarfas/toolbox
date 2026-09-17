package com.vasmarfas.card.core

enum class AppPermission { LOCATION, ACTIVITY_RECOGNITION, MICROPHONE, BLUETOOTH, CAMERA }

expect suspend fun ensurePermission(permission: AppPermission): Boolean

expect fun hasPermission(permission: AppPermission): Boolean
