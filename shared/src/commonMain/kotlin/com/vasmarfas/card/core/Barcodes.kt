package com.vasmarfas.card.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

class ScannedCode(val text: String, val format: String)

expect suspend fun scanCodes(image: ByteArray): List<ScannedCode>

expect val cameraScanning: Boolean

@Composable
expect fun CameraScanner(onFound: (ScannedCode) -> Unit, modifier: Modifier = Modifier)
