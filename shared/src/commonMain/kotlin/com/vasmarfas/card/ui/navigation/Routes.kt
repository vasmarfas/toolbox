package com.vasmarfas.card.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.ui.graphics.vector.ImageVector
import com.vasmarfas.card.core.PlatformKind
import com.vasmarfas.card.core.currentPlatform
import com.vasmarfas.card.resources.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource

@Serializable
@SerialName("home")
data object HomeRoute

@Serializable
@SerialName("projects")
data object ProjectsRoute

@Serializable
@SerialName("resume")
data object ResumeRoute

@Serializable
@SerialName("tools")
data object ToolsRoute

@Serializable
@SerialName("tool")
data class ToolRoute(val id: String)

@Serializable
@SerialName("settings")
data object SettingsRoute

enum class TopDestination(
    val route: Any,
    private val siteLabel: StringResource,
    private val siteIcon: ImageVector,
    private val siteSelectedIcon: ImageVector,
    val urlFragment: String,
) {
    HOME(HomeRoute, Res.string.home, Icons.Outlined.Home, Icons.Filled.Home, "home"),
    PROJECTS(ProjectsRoute, Res.string.projects, Icons.Outlined.Widgets, Icons.Filled.Widgets, "projects"),
    RESUME(ResumeRoute, Res.string.resume_page, Icons.Outlined.Description, Icons.Filled.Description, "resume"),
    TOOLS(ToolsRoute, Res.string.tools_short, Icons.Outlined.Build, Icons.Filled.Build, "tools"),
    SETTINGS(SettingsRoute, Res.string.settings, Icons.Outlined.Settings, Icons.Filled.Settings, "settings"),
    ;

    private val asAboutPage get() = this == HOME && currentPlatform != PlatformKind.WEB

    val label: StringResource get() = if (asAboutPage) Res.string.about_me else siteLabel
    val icon: ImageVector get() = if (asAboutPage) Icons.Outlined.Person else siteIcon
    val selectedIcon: ImageVector get() = if (asAboutPage) Icons.Filled.Person else siteSelectedIcon

    companion object {
        /**
         * The site keeps the personal pages and the order they have always had. The store builds
         * ship as a toolbox: projects and the resume stay reachable by link but leave the bar.
         */
        val visible: List<TopDestination>
            get() = if (currentPlatform == PlatformKind.WEB) entries.toList() else listOf(TOOLS, HOME, SETTINGS)
    }
}

object UrlRoutes {
    const val TOOL_PREFIX = "tools/"

    fun fragmentFor(destination: TopDestination): String = "#${destination.urlFragment}"

    fun fragmentForTool(id: String): String = "#$TOOL_PREFIX$id"

    fun parse(fragment: String): Any? {
        val f = fragment.trim().trimStart('#').trim('/')
        if (f.isEmpty()) return null
        if (f.startsWith(TOOL_PREFIX)) {
            val id = f.removePrefix(TOOL_PREFIX).substringBefore('/').substringBefore('?')
            return if (id.isNotBlank()) ToolRoute(id) else ToolsRoute
        }
        return TopDestination.entries.firstOrNull { it.urlFragment == f.substringBefore('/') }?.route
    }
}
