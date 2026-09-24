package com.vasmarfas.card.tools.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Power
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.core.str
import com.vasmarfas.card.core.toDoubleLenient
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.AnswerCard
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard

val electricityCostTool = Tool(
    id = "electricity-cost",
    category = ToolCategory.MONEY,
    title = Res.string.electricity_cost,
    description = Res.string.electricity_cost_description,
    icon = Icons.Filled.Power,
    keywords = listOf(
        "electricity", "kwh", "power", "appliance", "bill", "energy", "watt",
        "электричество", "электроэнергия", "квт", "мощность", "прибор", "счёт", "свет",
    ),
) { ElectricityCostScreen() }

@Composable
private fun ElectricityCostScreen() {
    var wattsText by rememberSaveable { mutableStateOf("100") }
    var hoursText by rememberSaveable { mutableStateOf("5") }
    var daysText by rememberSaveable { mutableStateOf("30") }
    var priceText by rememberSaveable { mutableStateOf("6") }
    val watts = wattsText.toDoubleLenient()?.takeIf { it >= 0 }
    val hours = hoursText.toDoubleLenient()?.takeIf { it in 0.0..24.0 }
    val days = daysText.toDoubleLenient()?.takeIf { it in 0.0..31.0 }
    val price = priceText.toDoubleLenient()?.takeIf { it >= 0 }
    NumberField(
        value = wattsText,
        onValueChange = { wattsText = it },
        label = Res.string.appliance_power.str(),
        suffix = Res.string.unit_w.str(),
        isError = wattsText.isNotBlank() && watts == null,
    )
    ChoiceChips(
        options = appliances,
        selected = appliances.firstOrNull { it.watts.toDouble() == watts },
        onSelect = { wattsText = it.watts.toString() },
        label = { "${it.title.str()} · ${it.watts} ${Res.string.unit_w.str()}" },
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        NumberField(
            value = hoursText,
            onValueChange = { hoursText = it },
            label = Res.string.hours_per_day.str(),
            modifier = Modifier.weight(1f),
            suffix = Res.string.unit_h.str(),
            isError = hoursText.isNotBlank() && hours == null,
        )
        NumberField(
            value = daysText,
            onValueChange = { daysText = it },
            label = Res.string.days_per_month.str(),
            modifier = Modifier.weight(1f),
            isError = daysText.isNotBlank() && days == null,
        )
    }
    NumberField(
        value = priceText,
        onValueChange = { priceText = it },
        label = Res.string.price_per_kwh.str(),
        isError = priceText.isNotBlank() && price == null,
    )
    if (watts == null || hours == null || days == null || price == null) {
        ErrorText(Res.string.electricity_input_error.str())
        return
    }
    val use = ElectricityCost.of(watts, hours, days, price)
    val kwh = Res.string.unit_kwh.str()
    AnswerCard(
        use.costPerMonth.fmt(2, grouping = true),
        "${Res.string.electricity_per_month.str()} · ${use.kwhPerMonth.fmt(2)} $kwh",
        copyValue = use.costPerMonth.fmt(2),
    )
    ResultCard {
        KeyValueRow(Res.string.electricity_per_day.str(), "${use.costPerDay.fmt(2, grouping = true)} · ${use.kwhPerDay.fmt(3)} $kwh", copyValue = use.costPerDay.fmt(2))
        KeyValueRow(Res.string.electricity_per_year.str(), "${use.costPerYear.fmt(2, grouping = true)} · ${use.kwhPerYear.fmt(1)} $kwh", copyValue = use.costPerYear.fmt(2))
    }
}
