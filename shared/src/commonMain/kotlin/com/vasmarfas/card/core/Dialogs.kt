package com.vasmarfas.card.core

import androidx.compose.ui.window.DialogProperties

// together with Modifier.imePadding() keeps a dialog with a text field above the keyboard
expect fun keyboardDialogProperties(): DialogProperties
