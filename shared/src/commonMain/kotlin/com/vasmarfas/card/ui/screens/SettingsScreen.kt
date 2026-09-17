package com.vasmarfas.card.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.Analytics
import com.vasmarfas.card.core.AppConfig
import com.vasmarfas.card.core.Lang
import com.vasmarfas.card.core.PlatformKind
import com.vasmarfas.card.core.currentPlatform
import com.vasmarfas.card.core.openUrl
import com.vasmarfas.card.core.platformInfo
import com.vasmarfas.card.core.str
import com.vasmarfas.card.core.supportsDynamicColor
import com.vasmarfas.card.data.GithubStars
import com.vasmarfas.card.data.LocalSettings
import com.vasmarfas.card.data.ProfileRepository
import com.vasmarfas.card.data.ResumeRepository
import com.vasmarfas.card.data.ThemeMode
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.ContentColumn
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.SegmentedChoice
import com.vasmarfas.card.ui.components.SwitchRow
import com.vasmarfas.card.ui.theme.seedPresets
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen() {
    val settings = LocalSettings.current
    val profileState by ProfileRepository.state.collectAsState()
    val scope = rememberCoroutineScope()
    val info = platformInfo()
    ContentColumn(maxWidth = 760.dp) {
        Text(Res.string.settings.str(), style = MaterialTheme.typography.headlineMedium)

        SettingsCard(Res.string.language.str()) {
            SegmentedChoice(
                options = Lang.entries,
                selected = settings.lang,
                onSelect = { settings.updateLang(it) },
                label = { it.nativeName },
            )
        }

        SettingsCard(Res.string.theme.str()) {
            SegmentedChoice(
                options = ThemeMode.entries,
                selected = settings.themeMode,
                onSelect = { settings.updateThemeMode(it) },
                label = {
                    when (it) {
                        ThemeMode.SYSTEM -> Res.string.system.str()
                        ThemeMode.LIGHT -> Res.string.light.str()
                        ThemeMode.DARK -> Res.string.dark.str()
                    }
                },
            )
            if (supportsDynamicColor()) {
                SwitchRow(
                    Res.string.dynamic_color.str(),
                    settings.dynamicColor,
                    { settings.updateDynamicColor(it) },
                    description = Res.string.dynamic_color_hint.str(),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(Res.string.accent_color.str(), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    seedPresets.forEach { (seed, _) ->
                        val selected = seed == settings.seedColor
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(seed))
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape,
                                )
                                .clickable { settings.updateSeedColor(seed) }
                                .pointerHoverIcon(PointerIcon.Hand),
                        )
                    }
                }
            }
            SwitchRow(
                Res.string.wide_tables.str(),
                settings.wideTables,
                { settings.updateWideTables(it) },
                description = Res.string.wide_tables_hint.str(),
            )
            if (currentPlatform != PlatformKind.WEB) {
                SwitchRow(
                    Res.string.open_on_tools.str(),
                    settings.startOnTools,
                    { settings.updateStartOnTools(it) },
                    description = Res.string.start_on_tools_hint.str(),
                )
            }
        }

        SettingsCard(Res.string.analytics.str()) {
            var analytics by remember { mutableStateOf(Analytics.isEnabled()) }
            SwitchRow(
                Res.string.send_usage_statistics.str(),
                analytics,
                {
                    analytics = it
                    Analytics.setEnabled(it)
                },
                description = Res.string.opening_screens_and_tools_language_and_theme.str(),
            )
        }

        SettingsCard(Res.string.profile_data.str()) {
            val profile = profileState.value
            KeyValueRow(Res.string.profile_data_updated.str(), profile?.updated ?: "—", mono = false, copyable = false)
            KeyValueRow(
                Res.string.source.str(),
                profileState.source.name.lowercase() + (profileState.error?.let { " · $it" } ?: ""),
                mono = false,
                copyable = false,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(
                    text = Res.string.refresh.str(),
                    onClick = { scope.launch { ProfileRepository.refresh(); ResumeRepository.refresh(); GithubStars.refresh(profileState.value?.projects.orEmpty(), force = true) } },
                    enabled = !profileState.refreshing,
                )
                TextButton(onClick = { openUrl(AppConfig.contentUrl("profile.json")) }) { Text("profile.json") }
            }
        }

        SettingsCard(Res.string.about.str()) {
            KeyValueRow(Res.string.version.str(), AppConfig.VERSION, mono = false, copyable = false)
            KeyValueRow(Res.string.platform.str(), "${info.kind.title.str()} · ${info.osName} ${info.osVersion} · ${info.deviceModel}", mono = false, copyable = false)
            Text(Res.string.built_with.str(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { openUrl(AppConfig.REPO_URL) }) { Text(Res.string.source_code.str()) }
                TextButton(onClick = { openUrl(AppConfig.SITE_COM) }) { Text("vasmarfas.com") }
                TextButton(onClick = { openUrl(AppConfig.SITE_RU) }) { Text("vasmarfas.ru") }
                TextButton(onClick = { openUrl("${AppConfig.REPO_URL}/blob/main/privacy-policy.md") }) { Text(Res.string.privacy_policy.str()) }
                TextButton(onClick = { openUrl("${AppConfig.REPO_URL}/blob/main/terms.md") }) { Text(Res.string.terms.str()) }
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
