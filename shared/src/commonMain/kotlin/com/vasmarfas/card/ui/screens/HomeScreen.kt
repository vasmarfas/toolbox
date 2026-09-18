package com.vasmarfas.card.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
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
import com.vasmarfas.card.ui.components.PageMaxWidth
import com.vasmarfas.card.ui.components.PromptChevron
import com.vasmarfas.card.ui.components.PromptMark
import com.vasmarfas.card.ui.components.SectionTitle
import com.vasmarfas.card.ui.components.SelectableText
import com.vasmarfas.card.ui.components.linkIcon
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource
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
    ContentColumn(maxWidth = PageMaxWidth, verticalSpacing = 24.dp) {
        HomeHeader(onOpenSettings)
        Crossfade(targetState = state.value) { profile ->
            if (profile == null) return@Crossfade
            // Thirty seconds: who, proof, where to write. The paragraphs wait for the reader who
            // got that far.
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Hero(profile)
                StatsStrip(profile, onOpenTools)
                FeaturedProjects(profile, onOpenProjects)
                LatestArticles(profile, onOpenProjects)
                AboutSection(profile)
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
        Icon(PromptMark, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
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
        // Effective darkness, not the stored mode: on SYSTEM in the dark the button used to offer
        // "go dark", and the first click changed nothing.
        val dark = when (settings.themeMode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }
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
private fun Hero(profile: Profile) {
    val person = profile.person
    val compact = LocalLayoutSize.current == LayoutSize.COMPACT
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Top) {
        Avatar(profile.avatar, if (compact) 72.dp else 120.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PromptLine(
                person.name.str(),
                if (compact) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displayLarge,
            )
            Text(person.title.str(), style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Place,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                Text(person.location.str(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinkChips(profile.links)
        }
    }
}

/** The mark's chevron, the name typed after it and a cursor still blinking: the icon, spelled out. */
@Composable
private fun PromptLine(text: String, style: TextStyle) {
    val glyph = with(LocalDensity.current) { (style.fontSize * 0.72f).toDp() }
    val cursor by rememberInfiniteTransition().animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                1f at 0
                1f at 499
                0f at 500
                0f at 999
            },
            repeatMode = RepeatMode.Restart,
        ),
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            PromptChevron,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(width = glyph * 170 / 295, height = glyph),
        )
        Text(text, style = style)
        Box(
            Modifier
                .size(width = 4.dp, height = glyph)
                .alpha(cursor)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
        )
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
private fun AboutSection(profile: Profile) {
    val person = profile.person
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(Res.string.about_me.str())
        (listOf(person.bio) + person.about).forEach { paragraph ->
            SelectableText(paragraph.str(), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private class Stat(val value: String, val label: StringResource, val onClick: (() -> Unit)? = null)

@Composable
private fun StatsStrip(profile: Profile, onOpenTools: () -> Unit) {
    val year = remember { currentYear() }
    val stars by GithubStars.stars.collectAsState()
    val totalStars = profile.projects.sumOf { project -> GithubStars.starsOf(project) ?: 0 }
    val stats = listOfNotNull(
        (year - profile.milestones.androidSince).takeIf { it > 0 }?.let { Stat("$it+", Res.string.years_in_android_kmp) },
        (year - profile.milestones.sysadminSince).takeIf { it > 0 }?.let { Stat("$it+", Res.string.years_of_sysadmin_work) },
        Stat(profile.projects.size.toString(), Res.string.public_projects),
        totalStars.takeIf { it > 0 }?.let { Stat("$it", Res.string.github_stars) },
        Stat(profile.articles.size.toString(), Res.string.habr_articles),
        Stat(ToolRegistry.all.size.toString(), Res.string.tools_in_this_app, onOpenTools),
    )
    // Three columns on a phone left ~100 dp per label, and "администрирования" broke mid-word.
    val columns = if (LocalLayoutSize.current == LayoutSize.COMPACT) 2 else 6
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            stats.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { stat -> StatCell(stat, Modifier.weight(1f)) }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
    if (stars.isEmpty()) Spacer(Modifier.height(0.dp))
}

@Composable
private fun StatCell(stat: Stat, modifier: Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .then(
                if (stat.onClick == null) {
                    Modifier
                } else {
                    Modifier.clickable(onClick = stat.onClick, role = Role.Button).pointerHoverIcon(PointerIcon.Hand)
                },
            )
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stat.value, style = MaterialTheme.typography.headlineMedium, color = primary)
            if (stat.onClick != null) {
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = primary, modifier = Modifier.size(18.dp))
            }
        }
        Text(stat.label.str(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FeaturedProjects(profile: Profile, onOpenProjects: () -> Unit) {
    val featured = profile.projects.filter { it.featured }.ifEmpty { profile.projects.take(4) }
    val columns = if (LocalLayoutSize.current == LayoutSize.COMPACT) 1 else 2
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(Res.string.featured_projects.str(), action = { SeeAllButton(onOpenProjects) })
        // A Row divides its width by weight; FlowRow keeps asking the card how wide it wants to be,
        // and a card holding a paragraph always answers "wider than the row", so it never paired up.
        featured.chunked(columns).forEach { row ->
            // IntrinsicSize.Min makes the row as tall as its tallest card, so the cards end level
            // instead of one hanging below the other by a link chip.
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { project ->
                    ProjectCard(project, modifier = Modifier.weight(1f).fillMaxHeight(), condensed = true)
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
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
        // Thirteen filled tonal buttons all leading to the same screen read as thirteen commands.
        // They are an inventory with a way in, which is what an assist chip is for.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolCategory.entries.forEach { category ->
                val count = ToolRegistry.byCategory(category).size
                if (count == 0) return@forEach
                AssistChip(
                    onClick = onOpenTools,
                    label = { Text("${category.title.str()} · $count") },
                    leadingIcon = { Icon(category.icon, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize)) },
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                )
            }
        }
        val popular = listOf("subnet-calculator", "dns-lookup", "unit-converter", "json-formatter", "qr-generator", "password-generator")
            .mapNotNull { ToolRegistry.byId(it) }
        if (popular.isNotEmpty()) {
            // Both groups are chips now, so the shortcuts need a word to tell them from the counts.
            Text(
                Res.string.popular.str(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                popular.forEach { tool ->
                    AssistChip(
                        onClick = { onOpenTool(tool.id) },
                        label = { Text(tool.title.str()) },
                        leadingIcon = { Icon(tool.icon, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize)) },
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                    )
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
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
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
        // A Row squeezed the three buttons until "Исходный код" broke mid-word at 360 dp.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
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
    TextButton(onClick = { openUrl(url) }, modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)) {
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}
