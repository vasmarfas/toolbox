package com.vasmarfas.card.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.AppConfig
import com.vasmarfas.card.core.Lang
import com.vasmarfas.card.core.LocalLang
import com.vasmarfas.card.core.Net
import com.vasmarfas.card.core.openUrl
import com.vasmarfas.card.core.str
import com.vasmarfas.card.data.Article
import com.vasmarfas.card.data.Avatar as ProfileAvatar
import com.vasmarfas.card.data.GithubStars
import com.vasmarfas.card.data.LocalSettings
import com.vasmarfas.card.data.Profile
import com.vasmarfas.card.data.ProfileRepository
import com.vasmarfas.card.data.ThemeMode
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.resources.Res
import com.vasmarfas.card.resources.about_me
import com.vasmarfas.card.resources.experience_education_skills
import com.vasmarfas.card.resources.right_here_no_pdf_and_no_external_link
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.tools.ToolRegistry
import com.vasmarfas.card.ui.components.ContentColumn
import com.vasmarfas.card.ui.components.LayoutSize
import com.vasmarfas.card.ui.components.LinkChips
import com.vasmarfas.card.ui.components.LocalLayoutSize
import com.vasmarfas.card.ui.components.SectionTitle
import com.vasmarfas.card.ui.components.SelectableText
import com.vasmarfas.card.ui.components.linkIcon
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap

fun currentYear(): Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year

@Composable
fun HomeScreen(
    onOpenProjects: () -> Unit,
    onOpenResume: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTool: (String) -> Unit,
) {
    val state by ProfileRepository.state.collectAsState()
    ContentColumn(verticalSpacing = 24.dp) {
        HomeHeader(onOpenSettings)
        Crossfade(targetState = state.value) { profile ->
            if (profile == null) return@Crossfade
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                HeroCard(profile)
                AboutSection(profile)
                StatsRow(profile)
                FeaturedProjects(profile, onOpenProjects)
                LatestArticles(profile, onOpenProjects)
                ToolsTeaser(onOpenTools, onOpenTool)
                ResumeTeaser(onOpenResume)
                Footer()
            }
        }
    }
}

@Composable
private fun HomeHeader(onOpenSettings: () -> Unit) {
    val settings = LocalSettings.current
    val lang = LocalLang.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            Res.string.app_name.str(),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = { settings.updateLang(if (lang == Lang.RU) Lang.EN else Lang.RU) },
            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
        ) {
            Icon(Icons.Filled.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (lang == Lang.RU) "EN" else "RU")
        }
        val dark = settings.themeMode == ThemeMode.DARK
        IconButton(
            onClick = { settings.updateThemeMode(if (dark) ThemeMode.LIGHT else ThemeMode.DARK) },
            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
        ) {
            Icon(if (dark) Icons.Filled.LightMode else Icons.Filled.DarkMode, contentDescription = Res.string.theme.str())
        }
        if (LocalLayoutSize.current == LayoutSize.COMPACT) {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = Res.string.settings.str())
            }
        }
    }
}

@Composable
private fun HeroCard(profile: Profile) {
    val compact = LocalLayoutSize.current == LayoutSize.COMPACT
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        if (compact) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar(profile.avatar, 96.dp)
                HeroText(profile)
            }
        } else {
            Row(Modifier.padding(28.dp), horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.Top) {
                Avatar(profile.avatar, 160.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) { HeroText(profile) }
            }
        }
    }
}

/**
 * Both the picture and the switch that shows it come from profile.json, so the avatar can be
 * swapped or taken down without a release. The bundled copy is the fallback while the network
 * one loads, or for good if it never arrives.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
private fun Avatar(avatar: ProfileAvatar, size: Dp) {
    if (!avatar.active) return
    var bitmap by remember(avatar.url) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(avatar.url) {
        bitmap = runCatching { Res.readBytes(AppConfig.AVATAR_BUNDLED_PATH).decodeToImageBitmap() }.getOrNull()
        if (avatar.url.isBlank()) return@LaunchedEffect
        val url = if (avatar.url.startsWith("http")) avatar.url else AppConfig.contentUrl(avatar.url)
        runCatching { Net.client.get(url).readRawBytes().decodeToImageBitmap() }.onSuccess { bitmap = it }
    }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(CircleShape),
        )
    }
}

@Composable
private fun HeroText(profile: Profile) {
    val person = profile.person
    val colors = MaterialTheme.colorScheme
    Text(person.name.str(), style = MaterialTheme.typography.displaySmall, color = colors.onPrimaryContainer)
    Text(person.title.str(), style = MaterialTheme.typography.titleMedium, color = colors.onPrimaryContainer)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(18.dp), tint = colors.onPrimaryContainer)
        Spacer(Modifier.width(4.dp))
        Text(person.location.str(), style = MaterialTheme.typography.bodyMedium, color = colors.onPrimaryContainer)
    }
    SelectableText(
        person.bio.str(),
        style = MaterialTheme.typography.bodyLarge.copy(color = colors.onPrimaryContainer),
    )
    LinkChips(profile.links)
}

@Composable
private fun AboutSection(profile: Profile) {
    if (profile.person.about.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(Res.string.about_me.str())
        profile.person.about.forEach { paragraph ->
            SelectableText(
                paragraph.str(),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatsRow(profile: Profile) {
    val year = remember { currentYear() }
    val stars by GithubStars.stars.collectAsState()
    val totalStars = profile.projects.sumOf { project -> GithubStars.starsOf(project) ?: 0 }
    val stats = listOfNotNull(
        (year - profile.milestones.androidSince).takeIf { it > 0 }?.let { "$it+" to Res.string.years_in_android_kmp },
        (year - profile.milestones.sysadminSince).takeIf { it > 0 }?.let { "$it+" to Res.string.years_of_sysadmin_work },
        profile.projects.size.toString() to Res.string.public_projects,
        totalStars.takeIf { it > 0 }?.let { "$it" to Res.string.github_stars },
        profile.articles.size.toString() to Res.string.habr_articles,
        ToolRegistry.all.size.toString() to Res.string.tools_in_this_app,
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        stats.forEach { (value, label) ->
            Card(
                modifier = Modifier.widthIn(min = 150.dp).weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Text(label.str(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    if (stars.isEmpty()) Spacer(Modifier.height(0.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeaturedProjects(profile: Profile, onOpenProjects: () -> Unit) {
    val featured = profile.projects.filter { it.featured }.ifEmpty { profile.projects.take(4) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(Res.string.featured_projects.str(), action = { SeeAllButton(onOpenProjects) })
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = if (LocalLayoutSize.current == LayoutSize.COMPACT) 1 else 3,
        ) {
            featured.forEach { project ->
                ProjectCard(project, modifier = Modifier.weight(1f).widthIn(min = 260.dp))
            }
        }
    }
}

@Composable
private fun SeeAllButton(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)) {
        Text(Res.string.see_all.str())
        Spacer(Modifier.width(4.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun LatestArticles(profile: Profile, onOpenAll: () -> Unit) {
    if (profile.articles.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(Res.string.latest_articles.str(), action = { SeeAllButton(onOpenAll) })
        profile.articles.sortedByDescending { it.date }.take(3).forEach { ArticleRow(it) }
    }
}

@Composable
fun ArticleRow(article: Article, modifier: Modifier = Modifier) {
    Card(
        onClick = { openUrl(article.url) },
        modifier = modifier.fillMaxWidth().pointerHoverIcon(PointerIcon.Hand),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(linkIcon(article.source), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(article.title.str(), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                article.summary?.let {
                    Text(it.str(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                val meta = buildString {
                    append(article.date)
                    article.views?.let { append(" · ").append(it).append(' ').append(Res.string.views.str()) }
                }
                Text(meta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ToolsTeaser(onOpenTools: () -> Unit, onOpenTool: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(Res.string.tools.str(), action = { SeeAllButton(onOpenTools) })
        Text(Res.string.tools_intro.str(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolCategory.entries.forEach { category ->
                val count = ToolRegistry.byCategory(category).size
                if (count == 0) return@forEach
                FilledTonalButton(onClick = onOpenTools, modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)) {
                    Icon(category.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("${category.title.str()} · $count")
                }
            }
        }
        val popular = listOf("subnet-calculator", "dns-lookup", "unit-converter", "json-formatter", "qr-generator", "password-generator")
            .mapNotNull { ToolRegistry.byId(it) }
        if (popular.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                popular.forEach { tool ->
                    TextButton(onClick = { onOpenTool(tool.id) }, modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)) {
                        Icon(tool.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(tool.title.str())
                    }
                }
            }
        }
    }
}

@Composable
private fun ResumeTeaser(onOpenResume: () -> Unit) {
    Card(
        onClick = onOpenResume,
        modifier = Modifier.fillMaxWidth().pointerHoverIcon(PointerIcon.Hand),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(Res.string.resume_page.str(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(
                    Res.string.experience_education_skills.str(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    Res.string.right_here_no_pdf_and_no_external_link.str(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun Footer() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(Res.string.built_with.str(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FooterLink("vasmarfas.com", AppConfig.SITE_COM)
            FooterLink("vasmarfas.ru", AppConfig.SITE_RU)
            FooterLink(Res.string.source_code.str(), AppConfig.REPO_URL)
        }
        Spacer(Modifier.height(4.dp))
        Text("© ${currentYear()} vasmarfas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FooterLink(text: String, url: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable { openUrl(url) }.pointerHoverIcon(PointerIcon.Hand),
    )
}
