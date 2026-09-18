package com.vasmarfas.card.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.str
import com.vasmarfas.card.data.GithubStars
import com.vasmarfas.card.data.ProfileRepository
import com.vasmarfas.card.data.Project
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.ui.components.ContentColumn
import com.vasmarfas.card.ui.components.LinkChips
import com.vasmarfas.card.ui.components.PageMaxWidth
import com.vasmarfas.card.ui.components.SegmentedChoice
import com.vasmarfas.card.ui.components.SelectableText
import com.vasmarfas.card.ui.components.TagChips
import org.jetbrains.compose.resources.StringResource

private enum class ProjectsTab(val title: StringResource) {
    PROJECTS(Res.string.projects),
    ARTICLES(Res.string.articles),
}

@Composable
fun ProjectsScreen() {
    val state by ProfileRepository.state.collectAsState()
    val profile = state.value
    var tab by remember { mutableStateOf(ProjectsTab.PROJECTS) }
    ContentColumn(maxWidth = PageMaxWidth, verticalSpacing = 16.dp) {
        Text(Res.string.projects.str(), style = MaterialTheme.typography.headlineSmall)
        SegmentedChoice(
            options = ProjectsTab.entries,
            selected = tab,
            onSelect = { tab = it },
            label = { it.title.str() },
            modifier = Modifier.widthIn(max = 420.dp),
        )
        if (profile == null) return@ContentColumn
        when (tab) {
            ProjectsTab.PROJECTS -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                profile.projects.forEach { project -> ProjectCard(project) }
            }

            ProjectsTab.ARTICLES -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                profile.articles.sortedByDescending { it.date }.forEach { ArticleRow(it) }
            }
        }
    }
}

@Composable
fun ProjectCard(project: Project, modifier: Modifier = Modifier, condensed: Boolean = false) {
    val liveStars by GithubStars.stars.collectAsState()
    val stars = GithubStars.repoOf(project)?.let { liveStars[it] } ?: project.stars
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(project.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (stars != null && stars > 0) {
                    Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text(stars.toString(), style = MaterialTheme.typography.labelLarge)
                }
            }
            Text(
                project.tagline.str(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (condensed) 3 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis,
            )
            if (!condensed) {
                SelectableText(project.description.str(), style = MaterialTheme.typography.bodyMedium)
            }
            val meta = listOf(project.year, project.platforms.joinToString(" · ")).filter { it.isNotBlank() }.joinToString("  ·  ")
            if (meta.isNotBlank()) {
                Text(meta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (project.tech.isNotEmpty()) TagChips(project.tech)
            if (project.links.isNotEmpty()) LinkChips(project.links)
        }
    }
}
