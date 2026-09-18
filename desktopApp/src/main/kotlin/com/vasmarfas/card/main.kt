package com.vasmarfas.card

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.vasmarfas.card.resources.Res
import com.vasmarfas.card.resources.app_icon
import org.jetbrains.compose.resources.painterResource

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "vasmarfas",
        icon = painterResource(Res.drawable.app_icon),
        state = rememberWindowState(size = DpSize(1180.dp, 820.dp), position = WindowPosition.Aligned(Alignment.Center)),
    ) {
        App()
    }
}
