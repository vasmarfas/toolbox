package com.vasmarfas.card.core

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred

object PermissionBridge {
    private var launcher: ((Array<String>) -> Unit)? = null
    private var pending: CompletableDeferred<Map<String, Boolean>>? = null

    fun attach(launch: (Array<String>) -> Unit) {
        launcher = launch
    }

    fun detach() {
        launcher = null
    }

    fun onResult(result: Map<String, Boolean>) {
        pending?.complete(result)
        pending = null
    }

    suspend fun request(permissions: Array<String>): Map<String, Boolean> {
        val launch = launcher ?: return permissions.associateWith { false }
        pending?.cancel()
        val deferred = CompletableDeferred<Map<String, Boolean>>()
        pending = deferred
        launch(permissions)
        return deferred.await()
    }
}

private fun manifestPermissions(permission: AppPermission): Array<String> = when (permission) {
    AppPermission.LOCATION -> arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    AppPermission.ACTIVITY_RECOGNITION -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) arrayOf(Manifest.permission.ACTIVITY_RECOGNITION) else emptyArray()
    AppPermission.MICROPHONE -> arrayOf(Manifest.permission.RECORD_AUDIO)
    AppPermission.BLUETOOTH -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    AppPermission.CAMERA -> arrayOf(Manifest.permission.CAMERA)
}

actual fun hasPermission(permission: AppPermission): Boolean {
    val context = AppContextHolder.context
    val required = manifestPermissions(permission)
    if (required.isEmpty()) return true
    return required.any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
}

actual suspend fun ensurePermission(permission: AppPermission): Boolean {
    if (hasPermission(permission)) return true
    val required = manifestPermissions(permission)
    if (required.isEmpty()) return true
    val result = PermissionBridge.request(required)
    return result.values.any { it }
}
