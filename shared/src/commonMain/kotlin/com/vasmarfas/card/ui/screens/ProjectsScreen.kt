package com.vasmarfas.card.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.str
import com.vasmarfas.card.data.GithubStars
import com.vasmarfas.card.data.ProfileRepository
import com.vasmarfas.card.data.Project
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.ui.components.ContentColumn
import com.vasmarfas.card.ui.components.LayoutSize
import com.vasmarfas.card.ui.components.LinkChips
import com.vasmarfas.card.ui.components.LocalLayoutSize
import com.vasmarfas.card.ui.components.SegmentedChoice
import com.vasmarfas.card.ui.components.SelectableText
import com.vasmarfas.card.ui.components.TagChips
import org.jetbrains.compose.resources.StringResource

private enum class ProjectsTab(val title: StringResource) {
    PROJECTS(Res.string.projects),
    ARTICLES(Res.string.articles),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectsScreen() {
    val state by ProfileRepository.state.collectAsState()
    val profile = state.value
    var tab by remember { mutableStateOf(ProjectsTab.PROJECTS) }
    ContentColumn(verticalSpacing = 16.dp) {
        Text(Res.string.projects.str(), style = MaterialTheme.typography.headlineMedium)
        SegmentedChoice(
            options = ProjectsTab.entries,
            selected = tab,
            onSelect = { tab = it },
            label = { it.title.str() },
            modifier = Modifier.widthIn(max = 420.dp),
        )
        if (profile == null) return@ContentColumn
        when (tab) {
            ProjectsTab.PROJECTS -> FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
                maxItemsInEachRow = when (LocalLayoutSize.current) {
                    LayoutSize.COMPACT -> 1
                    LayoutSize.MEDIUM -> 2
                    LayoutSize.EXPANDED -> 3
                },
            ) {
                profile.projects.forEach { project ->
                    ProjectCard(project, modifier = Modifier.weight(1f).widthIn(min = 260.dp))
                }
            }

            ProjectsTab.ARTICLES -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                profile.articles.sortedByDescending { it.date }.forEach { ArticleRow(it) }
            }
        }
    }
}

@Composable
fun ProjectCard(project: Project, modifier: Modifier = Modifier) {
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
                    Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(2.dp))
                    Text(stars.toString(), style = MaterialTheme.typography.labelLarge)
                }
            }
            Text(project.tagline.str(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            SelectableText(project.description.str(), style = MaterialTheme.typography.bodyMedium)
            val meta = listOf(project.year, project.platforms.joinToString(" · ")).filter { it.isNotBlank() }.joinToString("  ·  ")
            if (meta.isNotBlank()) {
                Text(meta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (project.tech.isNotEmpty()) TagChips(project.tech)
            if (project.links.isNotEmpty()) LinkChips(project.links)
        }
    }
}
