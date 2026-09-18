package com.vasmarfas.card.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.Lang
import com.vasmarfas.card.core.LocalLang
import com.vasmarfas.card.core.openUrl
import com.vasmarfas.card.core.str
import com.vasmarfas.card.data.Education
import com.vasmarfas.card.data.Experience
import com.vasmarfas.card.data.ResumeRepository
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.ui.components.ContentColumn
import com.vasmarfas.card.ui.components.EmptyState
import com.vasmarfas.card.ui.components.PageMaxWidth
import com.vasmarfas.card.ui.components.SectionTitle
import com.vasmarfas.card.ui.components.SelectableText
import com.vasmarfas.card.ui.components.TagChips

// newest start first; when two jobs start the same month the one still running goes above
private val byRecency = compareByDescending<Experience> { it.from }.thenByDescending { it.to ?: "9999-99" }

@Composable
fun ResumeScreen() {
    val state by ResumeRepository.state.collectAsState()
    val resume = state.value
    ContentColumn(maxWidth = PageMaxWidth, verticalSpacing = 20.dp) {
        Text(Res.string.resume_page.str(), style = MaterialTheme.typography.headlineSmall)
        if (resume == null) return@ContentColumn
        if (resume.isEmpty) {
            EmptyState(
                icon = Icons.Filled.Description,
                title = Res.string.resume_page.str(),
                description = Res.string.resume_empty.str(),
            )
            return@ContentColumn
        }
        resume.summary?.let { SelectableText(it.str(), style = MaterialTheme.typography.bodyLarge) }
        if (resume.experience.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(Res.string.experience.str())
                resume.experience.sortedWith(byRecency).forEach { ExperienceCard(it) }
            }
        }
        if (resume.education.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(Res.string.education.str())
                resume.education.forEach { EducationCard(it) }
            }
        }
        if (resume.courses.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(Res.string.courses_and_certificates.str())
                resume.courses.forEach { EducationCard(it) }
            }
        }
        if (resume.skills.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(Res.string.skills.str())
                resume.skills.forEach { group ->
                    Text(group.title.str(), style = MaterialTheme.typography.titleMedium)
                    TagChips(group.items)
                }
            }
        }
        if (resume.languages.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(Res.string.languages.str())
                TagChips(resume.languages.map { "${it.name.str()} — ${it.level.str()}" })
            }
        }
    }
}

private fun formatMonth(value: String, lang: Lang): String {
    val parts = value.split('-')
    val year = parts.getOrNull(0) ?: return value
    val month = parts.getOrNull(1)?.toIntOrNull() ?: return year
    val names = if (lang == Lang.RU)
        listOf("янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
    else
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    return "${names.getOrNull(month - 1) ?: month} $year"
}

@Composable
private fun ExperienceCard(item: Experience) {
    val lang = LocalLang.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item.role.str(), style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.company.str(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                item.url?.let { url ->
                    IconButton(onClick = { openUrl(url) }, modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)) {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = Res.string.open.str(),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            val period = "${formatMonth(item.from, lang)} — ${item.to?.let { formatMonth(it, lang) } ?: Res.string.present.str()}" +
                (item.location?.let { " · ${it.str()}" } ?: "")
            Text(period, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            item.summary?.let { SelectableText(it.str(), style = MaterialTheme.typography.bodyMedium) }
            item.bullets.forEach { bullet ->
                Row {
                    Text("•  ", style = MaterialTheme.typography.bodyMedium)
                    SelectableText(bullet.str(), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (item.tags.isNotEmpty()) TagChips(item.tags)
        }
    }
}

@Composable
private fun EducationCard(item: Education) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.degree.str(), style = MaterialTheme.typography.titleMedium)
            Text(item.institution.str(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            val meta = listOfNotNull(item.year, item.note?.str()).joinToString(" · ")
            Text(meta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
