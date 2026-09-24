package com.vasmarfas.card

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vasmarfas.card.core.ActivityHolder
import com.vasmarfas.card.core.AppContextHolder
import com.vasmarfas.card.core.PermissionBridge
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        PermissionBridge.onResult(result)
    }
    private var link by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        AppContextHolder.init(this)
        ActivityHolder.activity = this
        enableEdgeToEdge()
        // immersive mode hides the status bar, and DEFAULT cutout mode letterboxes the window as soon
        // as the cutout stops being covered by it — the ruler would lose the top of the screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        super.onCreate(savedInstanceState)
        PermissionBridge.attach { permissions -> permissionLauncher.launch(permissions) }
        FileKit.init(this)
        if (savedInstanceState == null && intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == 0) link = intent.dataString

        setContent {
            App(link = link, onLinkHandled = { link = null })
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.dataString?.let { link = it }
    }

    override fun onDestroy() {
        PermissionBridge.detach()
        if (ActivityHolder.activity === this) ActivityHolder.activity = null
        super.onDestroy()
    }
}
