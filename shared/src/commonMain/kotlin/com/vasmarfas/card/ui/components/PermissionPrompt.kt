package com.vasmarfas.card.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.vasmarfas.card.core.AppPermission
import com.vasmarfas.card.core.ensurePermission
import com.vasmarfas.card.core.hasPermission
import com.vasmarfas.card.core.openAppSettings
import com.vasmarfas.card.core.permissionBlocked
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import kotlinx.coroutines.launch

@Composable
fun PermissionPrompt(permission: AppPermission, text: String, onGranted: () -> Unit) {
    val scope = rememberCoroutineScope()
    var blocked by remember { mutableStateOf(permissionBlocked(permission)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (hasPermission(permission)) onGranted() else blocked = permissionBlocked(permission)
    }
    Text(text, style = MaterialTheme.typography.bodyMedium)
    if (blocked) {
        ActionButton(Res.string.open_settings.str(), onClick = { openAppSettings() }, icon = Icons.Filled.Settings)
    } else {
        ActionButton(
            Res.string.grant_permission.str(),
            onClick = { scope.launch { if (ensurePermission(permission)) onGranted() else blocked = permissionBlocked(permission) } },
        )
    }
}
