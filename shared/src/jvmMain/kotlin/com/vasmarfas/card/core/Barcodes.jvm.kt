package com.vasmarfas.card.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

actual val cameraScanning: Boolean = false

@Composable
actual fun CameraScanner(onFound: (ScannedCode) -> Unit, modifier: Modifier) = Unit
