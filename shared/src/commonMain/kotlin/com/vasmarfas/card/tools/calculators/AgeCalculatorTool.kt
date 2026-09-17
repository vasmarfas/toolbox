package com.vasmarfas.card.tools.calculators

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.Tr
import com.vasmarfas.card.core.fmtGrouped
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.tools.time.pad2
import com.vasmarfas.card.tools.time.title
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.ToolInputField
import kotlin.time.Clock
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

val ageCalculatorTool = Tool(
    id = "age-calculator",
    category = ToolCategory.CALCULATORS,
    title = Res.string.age_calculator,
    description = Res.string.exact_age_from_the_date_and_time_of_birth_ye,
    icon = Icons.Filled.Cake,
    keywords = listOf("age", "birthday", "date", "days", "seconds", "возраст", "день рождения", "дата", "дни", "секунды"),
) { AgeCalculatorScreen() }

@Composable
private fun AgeCalculatorScreen() {
    val zone = remember { TimeZone.currentSystemDefault() }
    var now by remember { mutableStateOf(Clock.System.now().toLocalDateTime(zone)) }
    var birthText by rememberSaveable { mutableStateOf("2000-01-01") }
    var hourText by rememberSaveable { mutableStateOf("00") }
    var minuteText by rememberSaveable { mutableStateOf("00") }
    var asOfText by rememberSaveable { mutableStateOf("") }
    val live = asOfText.isBlank()
    LaunchedEffect(live) {
        while (live) {
            delay(1000)
            now = Clock.System.now().toLocalDateTime(zone)
        }
    }
    val birthDate = remember(birthText) { Age.parse(birthText) }
    val birthTime = remember(hourText, minuteText) { Age.parseTime(hourText, minuteText) }
    val asOfDate = remember(asOfText) { if (live) null else Age.parse(asOfText) }

    ToolInputField(
        value = birthText,
        onValueChange = { birthText = it },
        label = Res.string.date_of_birth_yyyy_mm_dd.str(),
        placeholder = "2003-04-23",
        keyboardType = KeyboardType.Number,
        isError = birthText.isNotBlank() && birthDate == null,
        monospace = true,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField(
            value = hourText,
            onValueChange = { hourText = it },
            label = Res.string.hour_of_birth.str(),
            modifier = Modifier.weight(1f),
            isError = birthTime == null,
        )
        NumberField(
            value = minuteText,
            onValueChange = { minuteText = it },
            label = Res.string.minute.str(),
            modifier = Modifier.weight(1f),
            isError = birthTime == null,
        )
    }
    ToolInputField(
        value = asOfText,
        onValueChange = { asOfText = it },
        label = Res.string.as_of_date_empty_now.str(),
        placeholder = now.date.toString(),
        keyboardType = KeyboardType.Number,
        isError = !live && asOfDate == null,
        monospace = true,
    )

    if (birthDate == null || birthTime == null) return
    val reference = when {
        live -> now
        asOfDate != null -> LocalDateTime(asOfDate, LocalTime(0, 0))
        else -> return
    }
    val birth = LocalDateTime(birthDate, birthTime)
    val result = Age.lived(birth, reference)
    if (result == null) {
        ErrorText(Res.string.date_of_birth_is_after_the_reference_date.str())
        return
    }
    val next = Age.untilBirthday(birth, reference)
    ResultCard {
        KeyValueRow(
            Res.string.age.str(),
            Tr(
                "${result.years} years ${result.months} months ${result.days} days ${result.hours}:${result.minutes.pad2()}:${result.seconds.pad2()}",
                "${result.years} лет ${result.months} мес. ${result.days} дн. ${result.hours}:${result.minutes.pad2()}:${result.seconds.pad2()}",
            ).str(),
            mono = false,
        )
        KeyValueRow(Res.string.total_months.str(), result.totalMonths.fmtGrouped())
        KeyValueRow(Res.string.weeks.str(), result.totalWeeks.fmtGrouped())
        KeyValueRow(Res.string.total_days.str(), result.totalDays.fmtGrouped())
        KeyValueRow(Res.string.hours.str(), result.totalHours.fmtGrouped())
        KeyValueRow(Res.string.minutes.str(), result.totalMinutes.fmtGrouped())
        KeyValueRow(Res.string.seconds.str(), result.totalSeconds.fmtGrouped())
        KeyValueRow(Res.string.born_on.str(), birthDate.dayOfWeek.title().str(), mono = false, copyable = false)
    }
    ResultCard(Res.string.next_birthday.str()) {
        if (Age.isBirthday(birthDate, reference.date)) {
            val turning = reference.date.year - birthDate.year
            KeyValueRow(
                Res.string.today.str(),
                Tr("Happy birthday! Turning $turning", "С днём рождения! Исполняется $turning").str(),
                mono = false,
                copyable = false,
            )
        }
        KeyValueRow(Res.string.date.str(), "${next.date} · ${next.date.dayOfWeek.title().str()}", mono = false)
        KeyValueRow(
            Res.string.left.str(),
            Tr(
                "${next.days} d ${next.hours} h ${next.minutes} min ${next.seconds} s",
                "${next.days} дн ${next.hours} ч ${next.minutes} мин ${next.seconds} с",
            ).str(),
        )
        KeyValueRow(Res.string.turning.str(), next.age.toString())
    }
}
