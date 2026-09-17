package com.vasmarfas.card.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.AppConfig
import com.vasmarfas.card.core.openUrl
import com.vasmarfas.card.core.setSystemBarsHidden
import com.vasmarfas.card.core.str
import com.vasmarfas.card.data.LocalSettings
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.resources.Res
import com.vasmarfas.card.resources.the_tool_needs_a_system_api_the_browser_does
import com.vasmarfas.card.tools.ToolRegistry
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.ContentColumn
import com.vasmarfas.card.ui.components.LocalChrome
import com.vasmarfas.card.ui.components.PlatformBadges

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ToolScreen(toolId: String, onBack: () -> Unit) {
    val tool = ToolRegistry.byId(toolId)
    val settings = LocalSettings.current
    val chrome = LocalChrome.current
    LaunchedEffect(toolId) { if (tool != null) settings.markRecent(toolId) }
    LaunchedEffect(chrome.immersive) { setSystemBarsHidden(chrome.immersive) }
    DisposableEffect(Unit) { onDispose { setSystemBarsHidden(false) } }
    BackHandler(enabled = chrome.immersive) { chrome.immersive = false }
    Scaffold(
        topBar = {
            if (chrome.immersive) return@Scaffold
            TopAppBar(
                title = {
                    Text(tool?.title?.str() ?: Res.string.nothing_found.str(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Res.string.back.str())
                    }
                },
                actions = {
                    if (tool?.expandable == true) {
                        IconButton(
                            onClick = { chrome.immersive = true },
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                        ) {
                            Icon(Icons.Filled.OpenInFull, contentDescription = Res.string.fullscreen.str())
                        }
                    }
                    if (tool != null) {
                        val favorite = tool.id in settings.favorites
                        IconButton(onClick = { settings.toggleFavorite(tool.id) }, modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)) {
                            Icon(
                                if (favorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = if (favorite) Res.string.remove_from_favorites.str() else Res.string.add_to_favorites.str(),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        if (tool == null) {
            Column(Modifier.padding(padding).padding(16.dp)) { Text(Res.string.nothing_found.str()) }
            return@Scaffold
        }
        Box(Modifier.padding(padding).fillMaxSize()) {
            ContentColumn(
                modifier = Modifier.fillMaxSize(),
                maxWidth = if (chrome.immersive) 4000.dp else 860.dp,
                contentPadding = if (chrome.immersive) PaddingValues(0.dp) else PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalSpacing = if (chrome.immersive) 0.dp else 16.dp,
            ) {
                if (!chrome.immersive) {
                    Text(tool.description.str(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (tool.availableHere) {
                    tool.content()
                } else {
                    Text(Res.string.not_available_on_this_platform.str(), style = MaterialTheme.typography.titleMedium)
                    Text(Res.string.available_on.str() + ":", style = MaterialTheme.typography.bodyMedium)
                    PlatformBadges(tool.platforms)
                    Text(
                        Res.string.the_tool_needs_a_system_api_the_browser_does.str(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ActionButton(text = Res.string.get_the_app.str(), onClick = { openUrl(AppConfig.REPO_URL + "/releases") })
                }
            }
            if (chrome.immersive) {
                FilledTonalIconButton(
                    onClick = { chrome.immersive = false },
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp).pointerHoverIcon(PointerIcon.Hand),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                    ),
                ) {
                    Icon(Icons.Filled.CloseFullscreen, contentDescription = Res.string.exit_fullscreen.str())
                }
            }
        }
    }
}
