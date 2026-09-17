package com.vasmarfas.card

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.vasmarfas.card.core.Analytics
import com.vasmarfas.card.core.AnalyticsEvent
import com.vasmarfas.card.core.LocalLang
import com.vasmarfas.card.core.PlatformKind
import com.vasmarfas.card.core.currentPlatform
import com.vasmarfas.card.core.initAnalytics
import com.vasmarfas.card.core.setWindowTitle
import com.vasmarfas.card.core.str
import com.vasmarfas.card.data.AppSettings
import com.vasmarfas.card.data.GithubStars
import com.vasmarfas.card.data.LocalSettings
import com.vasmarfas.card.data.ProfileRepository
import com.vasmarfas.card.data.ResumeRepository
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.ToolRegistry
import com.vasmarfas.card.ui.components.ChromeState
import com.vasmarfas.card.ui.components.LayoutSize
import com.vasmarfas.card.ui.components.LocalChrome
import com.vasmarfas.card.ui.components.LocalLayoutSize
import com.vasmarfas.card.ui.components.layoutSizeFor
import com.vasmarfas.card.ui.navigation.HomeRoute
import com.vasmarfas.card.ui.navigation.ProjectsRoute
import com.vasmarfas.card.ui.navigation.ResumeRoute
import com.vasmarfas.card.ui.navigation.SettingsRoute
import com.vasmarfas.card.ui.navigation.ToolRoute
import com.vasmarfas.card.ui.navigation.ToolsRoute
import com.vasmarfas.card.ui.navigation.TopDestination
import com.vasmarfas.card.ui.screens.HomeScreen
import com.vasmarfas.card.ui.screens.ProjectsScreen
import com.vasmarfas.card.ui.screens.ResumeScreen
import com.vasmarfas.card.ui.screens.SettingsScreen
import com.vasmarfas.card.ui.screens.ToolScreen
import com.vasmarfas.card.ui.screens.ToolsScreen
import com.vasmarfas.card.ui.theme.VasmarfasTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString

@Composable
fun App(onNavHostReady: suspend (NavController) -> Unit = {}) {
    val settings = remember { AppSettings() }
    val chrome = remember { ChromeState() }
    LaunchedEffect(Unit) {
        initAnalytics()
        Analytics.log(AnalyticsEvent.APP_OPEN)
        withContext(Dispatchers.Default) { ToolRegistry.all.size }
    }
    LaunchedEffect(Unit) { ProfileRepository.load() }
    LaunchedEffect(Unit) { ResumeRepository.load() }
    val profileState by ProfileRepository.state.collectAsState()
    LaunchedEffect(profileState.value) {
        profileState.value?.projects?.let { GithubStars.refresh(it) }
    }
    CompositionLocalProvider(
        LocalSettings provides settings,
        LocalLang provides settings.lang,
        LocalChrome provides chrome,
    ) {
        key(settings.lang) {
            VasmarfasTheme(settings) {
                val onTools = remember { settings.startOnTools && currentPlatform != PlatformKind.WEB }
                val greet = remember { settings.firstRun && currentPlatform != PlatformKind.WEB }
                LaunchedEffect(Unit) { settings.markLaunched() }
                AppShell(rememberNavController(), onNavHostReady, onTools, greet)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppShell(
    navController: NavHostController,
    onNavHostReady: suspend (NavController) -> Unit,
    startOnTools: Boolean,
    greetOnStart: Boolean,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val topDestination = TopDestination.entries.firstOrNull { destination?.hasRoute(it.route::class) == true }
    val toolId = backStackEntry?.takeIf { destination?.hasRoute(ToolRoute::class) == true }?.toRoute<ToolRoute>()?.id
    val lang = LocalLang.current
    val chrome = LocalChrome.current

    LaunchedEffect(toolId) { chrome.immersive = false }

    LaunchedEffect(topDestination, toolId, lang) {
        val page = when {
            toolId != null -> ToolRegistry.byId(toolId)?.title?.let { getString(it) } ?: getString(Res.string.tools)
            topDestination == TopDestination.HOME -> null
            topDestination != null -> getString(topDestination.label)
            else -> null
        }
        setWindowTitle(if (page == null) "vasmarfas" else "$page · vasmarfas")
    }

    var greeting by rememberSaveable { mutableStateOf(greetOnStart) }

    val navigateTop = remember(navController) {
        { tab: TopDestination ->
            chrome.immersive = false
            navController.navigate(tab.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = tab == TopDestination.HOME
                    saveState = tab != TopDestination.HOME
                }
                launchSingleTop = true
                restoreState = tab != TopDestination.HOME
            }
        }
    }

    if (greeting) {
        AlertDialog(
            onDismissRequest = { greeting = false },
            title = { Text(Res.string.greeting_title.str()) },
            text = { Text(Res.string.greeting_body.str()) },
            confirmButton = {
                TextButton(onClick = { greeting = false }) { Text(Res.string.greeting_to_tools.str()) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        greeting = false
                        navigateTop(TopDestination.HOME)
                    },
                ) { Text(Res.string.greeting_about_developer.str()) }
            },
        )
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val layout = layoutSizeFor(maxWidth)
        CompositionLocalProvider(LocalLayoutSize provides layout) {
            val compact = layout == LayoutSize.COMPACT
            Row(Modifier.fillMaxSize()) {
                if (!compact && !chrome.immersive) {
                    WideNavigationRail(
                        state = rememberWideNavigationRailState(WideNavigationRailValue.Expanded),
                    ) {
                        TopDestination.visible.forEach { tab ->
                            WideNavigationRailItem(
                                selected = tab == topDestination,
                                onClick = { navigateTop(tab) },
                                icon = { Icon(if (tab == topDestination) tab.selectedIcon else tab.icon, contentDescription = null) },
                                label = { Text(tab.label.str()) },
                                railExpanded = true,
                                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                            )
                        }
                    }
                }
                Scaffold(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    bottomBar = {
                        if (compact && !chrome.immersive && topDestination != null) {
                            ShortNavigationBar {
                                TopDestination.visible.forEach { tab ->
                                    ShortNavigationBarItem(
                                        selected = tab == topDestination,
                                        onClick = { navigateTop(tab) },
                                        icon = { Icon(if (tab == topDestination) tab.selectedIcon else tab.icon, contentDescription = null) },
                                        label = { Text(tab.label.str()) },
                                    )
                                }
                            }
                        }
                    },
                ) { padding ->
                    AppNavHost(navController, navigateTop, onNavHostReady, startOnTools, Modifier.padding(padding))
                }
            }
        }
    }
}

// NavHost fades over 700 ms by default, which reads as a stall when the bottom bar switches tabs.
private const val NavFadeIn = 110
private const val NavFadeOut = 80

@Composable
private fun AppNavHost(
    navController: NavHostController,
    onNavigateTop: (TopDestination) -> Unit,
    onNavHostReady: suspend (NavController) -> Unit,
    startOnTools: Boolean,
    modifier: Modifier = Modifier,
) {
    val openTool: (String) -> Unit = { id -> navController.navigate(ToolRoute(id)) }
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        NavHost(
            navController = navController,
            startDestination = if (startOnTools) ToolsRoute else HomeRoute,
            enterTransition = { fadeIn(tween(NavFadeIn)) },
            exitTransition = { fadeOut(tween(NavFadeOut)) },
            popEnterTransition = { fadeIn(tween(NavFadeIn)) },
            popExitTransition = { fadeOut(tween(NavFadeOut)) },
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    onOpenProjects = { onNavigateTop(TopDestination.PROJECTS) },
                    onOpenResume = { onNavigateTop(TopDestination.RESUME) },
                    onOpenTools = { onNavigateTop(TopDestination.TOOLS) },
                    onOpenSettings = { onNavigateTop(TopDestination.SETTINGS) },
                    onOpenTool = openTool,
                )
            }
            composable<ProjectsRoute> { ProjectsScreen() }
            composable<ResumeRoute> { ResumeScreen() }
            composable<ToolsRoute> { ToolsScreen(onOpenTool = openTool) }
            composable<ToolRoute> { entry ->
                val route = entry.toRoute<ToolRoute>()
                ToolScreen(
                    toolId = route.id,
                    onBack = { if (!navController.popBackStack()) onNavigateTop(TopDestination.TOOLS) },
                )
            }
            composable<SettingsRoute> { SettingsScreen() }
        }
        LaunchedEffect(navController) { onNavHostReady(navController) }
    }
}
