package com.vasmarfas.card.tools.everyday

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.tools.text.OutputCard
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.ToolInputField
import kotlin.random.Random
import org.jetbrains.compose.resources.stringResource

val teamSplitterTool = Tool(
    id = "team-splitter",
    category = ToolCategory.EVERYDAY,
    title = Res.string.team_splitter,
    description = Res.string.team_splitter_description,
    icon = Icons.Filled.Groups,
    keywords = listOf(
        "teams", "random", "draw", "split", "players", "groups", "shuffle",
        "команды", "жеребьёвка", "разбить", "игроки", "группы", "случайно",
    ),
) { TeamSplitterScreen() }

private val teamCounts = (2..6).toList()

@Composable
private fun TeamSplitterScreen() {
    var edited by rememberSaveable { mutableStateOf<String?>(null) }
    var teams by rememberSaveable { mutableStateOf(2) }
    var seed by rememberSaveable { mutableStateOf(Random.nextLong()) }
    val text = edited ?: Res.string.team_names_sample.str()
    ToolInputField(
        value = text,
        onValueChange = { edited = it },
        label = Res.string.team_names.str(),
        singleLine = false,
        minLines = 4,
    )
    Text(Res.string.team_count.str(), style = MaterialTheme.typography.labelLarge)
    ChoiceChips(options = teamCounts, selected = teams, onSelect = { teams = it }, label = { it.toString() })
    val names = Teams.names(text)
    if (names.size < 2) {
        ErrorText(Res.string.team_need_names.str())
        return
    }
    ActionButton(text = Res.string.team_shuffle.str(), onClick = { seed = Random.nextLong() }, icon = Icons.Filled.Shuffle)
    val split = Teams.split(names, minOf(teams, names.size), seed)
    val titles = split.indices.map { stringResource(Res.string.team_title, it + 1) }
    split.forEachIndexed { index, members ->
        ResultCard(titles[index]) {
            members.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
        }
    }
    OutputCard(
        split.mapIndexed { index, members -> "${titles[index]}: ${members.joinToString(", ")}" }.joinToString("\n"),
        title = Res.string.team_copy_all.str(),
    )
}
