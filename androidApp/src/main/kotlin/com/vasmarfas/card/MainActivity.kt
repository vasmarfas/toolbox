package com.vasmarfas.card

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.vasmarfas.card.core.ActivityHolder
import com.vasmarfas.card.core.AppContextHolder
import com.vasmarfas.card.core.PermissionBridge

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        PermissionBridge.onResult(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppContextHolder.init(this)
        ActivityHolder.activity = this
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        PermissionBridge.attach { permissions -> permissionLauncher.launch(permissions) }

        setContent {
            App()
        }
    }

    override fun onDestroy() {
        PermissionBridge.detach()
        if (ActivityHolder.activity === this) ActivityHolder.activity = null
        super.onDestroy()
    }
}
