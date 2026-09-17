package com.vasmarfas.card.tools.everyday

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.Prefs
import com.vasmarfas.card.core.currentEpochMillis
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.tools.time.iso
import com.vasmarfas.card.tools.time.pad2
import com.vasmarfas.card.tools.time.parseDate
import com.vasmarfas.card.tools.time.parseHhMm
import com.vasmarfas.card.tools.time.shortTitle
import com.vasmarfas.card.tools.time.today
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.ToolInputField
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

val reminderCountdownTool = Tool(
    id = "reminder-countdown",
    category = ToolCategory.EVERYDAY,
    title = Res.string.event_countdown,
    description = Res.string.days_hours_and_minutes_left_to_your_events_o,
    icon = Icons.Filled.Event,
    keywords = listOf("countdown", "event", "days until", "reminder", "anniversary", "отсчёт", "событие", "дней до", "напоминание"),
) { ReminderCountdownScreen() }

@Composable
private fun ReminderCountdownScreen() {
    val events = remember {
        mutableStateListOf<CountdownEvent>().apply { addAll(Countdowns.decode(Prefs.store.get(Countdowns.PREF_KEY)) ?: emptyList()) }
    }
    fun persist() = Prefs.store.put(Countdowns.PREF_KEY, Countdowns.encode(events))
    var now by remember { mutableStateOf(currentEpochMillis() / 1000) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = currentEpochMillis() / 1000
        }
    }
    var name by rememberSaveable { mutableStateOf("") }
    var dateText by rememberSaveable { mutableStateOf(today().iso()) }
    var timeText by rememberSaveable { mutableStateOf("09:00") }
    val zone = TimeZone.currentSystemDefault()

    ToolInputField(name, { name = it }, Res.string.event_name.str())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        ToolInputField(
            value = dateText,
            onValueChange = { dateText = it },
            label = Res.string.date.str(),
            modifier = Modifier.weight(1f),
            placeholder = "YYYY-MM-DD",
            isError = parseDate(dateText) == null,
            monospace = true,
        )
        ToolInputField(
            value = timeText,
            onValueChange = { timeText = it },
            label = Res.string.time.str(),
            modifier = Modifier.weight(1f),
            placeholder = "HH:MM",
            isError = parseHhMm(timeText) == null,
            monospace = true,
        )
    }
    val date = parseDate(dateText)
    val minutes = parseHhMm(timeText)
    if (date == null || minutes == null) {
        ErrorText(Res.string.enter_a_date_as_yyyy_mm_dd_and_a_time_as_hh.str())
    }
    ActionButton(
        text = Res.string.add_event.str(),
        onClick = {
            if (date != null && minutes != null) {
                val epoch = LocalDateTime(date, LocalTime(minutes / 60, minutes % 60)).toInstant(zone).epochSeconds
                events.add(CountdownEvent(name.trim().ifEmpty { dateText }, epoch))
                events.sortBy { it.epochSeconds }
                persist()
                name = ""
            }
        },
        enabled = date != null && minutes != null,
        icon = Icons.Filled.Add,
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        events.forEachIndexed { index, event ->
            val r = Countdowns.remaining(event.epochSeconds, now)
            val at = Instant.fromEpochSeconds(event.epochSeconds).toLocalDateTime(zone)
            Card(colors = CardDefaults.cardColors(containerColor = if (r.past) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.primaryContainer)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(event.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${at.date.iso()} ${at.hour.pad2()}:${at.minute.pad2()} (${at.date.dayOfWeek.shortTitle().str()})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val parts = "${r.days} ${Res.string.d.str()} ${r.hours} ${Res.string.h.str()} ${r.minutes} ${Res.string.min.str()}"
                        Text(
                            text = if (r.past) "$parts ${Res.string.ago.str()}" else "$parts ${Res.string.left_2.str()}",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (r.past) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    IconButton(
                        onClick = {
                            events.removeAt(index)
                            persist()
                        },
                    ) { Icon(Icons.Filled.Delete, contentDescription = Res.string.delete.str()) }
                }
            }
        }
    }
}
