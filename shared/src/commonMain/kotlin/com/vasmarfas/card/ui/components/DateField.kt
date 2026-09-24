package com.vasmarfas.card.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

// the picker works in UTC days, so the day picked is the day written whatever the time zone
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "YYYY-MM-DD",
    isError: Boolean = value.isNotBlank() && runCatching { LocalDate.parse(value.trim()) }.isFailure,
) {
    var picking by remember { mutableStateOf(false) }
    ToolInputField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        placeholder = placeholder,
        keyboardType = KeyboardType.Number,
        isError = isError,
        trailingIcon = {
            IconButton(onClick = { picking = true }) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = Res.string.pick_date.str())
            }
        },
        monospace = true,
    )
    if (!picking) return
    val initial = runCatching { LocalDate.parse(value.trim()) }.getOrNull()?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds()
    val state = rememberDatePickerState(initialSelectedDateMillis = initial)
    DatePickerDialog(
        onDismissRequest = { picking = false },
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { onValueChange(Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date.toString()) }
                    picking = false
                },
            ) { Text(Res.string.done.str()) }
        },
        dismissButton = { TextButton(onClick = { picking = false }) { Text(Res.string.cancel.str()) } },
    ) {
        DatePicker(state)
    }
}
