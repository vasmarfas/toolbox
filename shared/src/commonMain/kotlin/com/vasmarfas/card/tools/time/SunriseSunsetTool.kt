package com.vasmarfas.card.tools.time

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.core.str
import com.vasmarfas.card.core.systemTimeZoneId
import com.vasmarfas.card.core.timeZoneOffsetSeconds
import com.vasmarfas.card.core.toDoubleLenient
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.ToolInputField
import kotlin.math.roundToInt
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

private class City(val name: String, val lat: Double, val lon: Double)

private val cities = listOf(
    City("Simferopol", 44.95, 34.10),
    City("Moscow", 55.7558, 37.6173),
    City("Saint Petersburg", 59.9343, 30.3351),
    City("London", 51.5074, -0.1278),
    City("New York", 40.7128, -74.0060),
    City("Tokyo", 35.6762, 139.6503),
)

val sunriseSunsetTool = Tool(
    id = "sunrise-sunset",
    category = ToolCategory.TIME,
    title = Res.string.sunrise_and_sunset,
    description = Res.string.sunrise_sunset_solar_noon_day_length_and_civ,
    icon = Icons.Filled.WbSunny,
    keywords = listOf("sunrise", "sunset", "twilight", "solar noon", "day length", "восход", "закат", "сумерки", "долгота дня"),
) { SunriseSunsetScreen() }

@Composable
private fun SunriseSunsetScreen() {
    var latText by rememberSaveable { mutableStateOf("44.95") }
    var lonText by rememberSaveable { mutableStateOf("34.10") }
    var dateText by rememberSaveable { mutableStateOf(today().iso()) }
    val date = parseDate(dateText)
    val systemOffsetHours = remember(date) {
        val d = date ?: today()
        val noonUtc = LocalDateTime(d, LocalTime(12, 0)).toInstant(TimeZone.UTC).epochSeconds
        (timeZoneOffsetSeconds(systemTimeZoneId(), noonUtc) ?: 0) / 3600.0
    }
    var offsetText by rememberSaveable { mutableStateOf(systemOffsetHours.fmt(2)) }

    ChoiceChips(
        options = cities,
        selected = cities.firstOrNull { it.lat.toString() == latText.trim() && it.lon.toString() == lonText.trim() },
        onSelect = {
            latText = it.lat.toString()
            lonText = it.lon.toString()
        },
        label = { it.name },
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        NumberField(latText, { latText = it }, Res.string.latitude.str(), Modifier.weight(1f), suffix = "°")
        NumberField(lonText, { lonText = it }, Res.string.longitude.str(), Modifier.weight(1f), suffix = "°")
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        ToolInputField(
            value = dateText,
            onValueChange = { dateText = it },
            label = Res.string.date.str(),
            modifier = Modifier.weight(1f),
            placeholder = "YYYY-MM-DD",
            isError = date == null,
            monospace = true,
        )
        NumberField(offsetText, { offsetText = it }, Res.string.utc_offset_h.str(), Modifier.weight(1f))
    }

    val lat = latText.toDoubleLenient()
    val lon = lonText.toDoubleLenient()
    val offsetHours = offsetText.toDoubleLenient()
    when {
        lat == null || lat < -90 || lat > 90 -> ErrorText(Res.string.latitude_must_be_between_90_and_90.str())
        lon == null || lon < -180 || lon > 180 -> ErrorText(Res.string.longitude_must_be_between_180_and_180.str())
        date == null -> ErrorText(Res.string.use_the_yyyy_mm_dd_format.str())
        offsetHours == null || offsetHours < -14 || offsetHours > 14 -> ErrorText(Res.string.offset_must_be_between_14_and_14_hours.str())
        else -> {
            val offset = (offsetHours * 3600).roundToInt()
            val t = remember(date, lat, lon) { SolarMath.compute(date, lat, lon) }
            fun time(minutes: Double?) = if (minutes == null) "—" else SolarMath.formatMinutes(minutes, offset)
            ResultCard {
                KeyValueRow(Res.string.sunrise.str(), time(t.sunrise))
                KeyValueRow(Res.string.sunset.str(), time(t.sunset))
                KeyValueRow(Res.string.solar_noon.str(), time(t.solarNoon))
                val length = t.dayLengthMinutes
                KeyValueRow(
                    Res.string.day_length.str(),
                    when {
                        length != null -> SolarMath.durationParts(length).let { (h, m) -> "$h ${Res.string.h.str()} $m ${Res.string.min.str()}" }
                        t.polarDay -> Res.string.polar_day.str()
                        t.polarNight -> Res.string.polar_night.str()
                        else -> "—"
                    },
                    mono = false,
                    copyable = false,
                )
            }
            ResultCard(Res.string.twilight.str()) {
                KeyValueRow(Res.string.civil_dawn_dusk.str(), "${time(t.civilDawn)} – ${time(t.civilDusk)}")
                KeyValueRow(Res.string.nautical.str(), "${time(t.nauticalDawn)} – ${time(t.nauticalDusk)}")
                KeyValueRow(Res.string.astronomical.str(), "${time(t.astronomicalDawn)} – ${time(t.astronomicalDusk)}")
            }
        }
    }
}
