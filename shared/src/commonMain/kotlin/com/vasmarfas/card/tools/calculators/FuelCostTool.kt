package com.vasmarfas.card.tools.calculators

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.core.str
import com.vasmarfas.card.core.toDoubleLenient
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.DropdownChoice
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SwitchRow

val fuelCostTool = Tool(
    id = "fuel-cost",
    category = ToolCategory.CALCULATORS,
    title = Res.string.fuel_cost,
    description = Res.string.fuel_needed_and_trip_cost_from_distance_cons,
    icon = Icons.Filled.LocalGasStation,
    keywords = listOf("fuel", "petrol", "gas", "trip", "mileage", "consumption", "топливо", "бензин", "поездка", "расход", "пробег"),
) { FuelCostScreen() }

@Composable
private fun FuelCostScreen() {
    var distanceText by rememberSaveable { mutableStateOf("500") }
    var distanceUnit by rememberSaveable { mutableStateOf(DistanceUnit.KM) }
    var consumptionText by rememberSaveable { mutableStateOf("8") }
    var consumptionUnit by rememberSaveable { mutableStateOf(ConsumptionUnit.L100KM) }
    var priceText by rememberSaveable { mutableStateOf("60") }
    var priceUnit by rememberSaveable { mutableStateOf(PriceUnit.LITRE) }
    var passengersText by rememberSaveable { mutableStateOf("1") }
    var roundTrip by rememberSaveable { mutableStateOf(false) }
    val distance = distanceText.toDoubleLenient()?.takeIf { it >= 0 }
    val consumption = consumptionText.toDoubleLenient()?.takeIf { it > 0 }
    val price = priceText.toDoubleLenient()?.takeIf { it >= 0 }
    val passengers = passengersText.trim().toIntOrNull()?.takeIf { it >= 1 }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NumberField(
            value = distanceText,
            onValueChange = { distanceText = it },
            label = Res.string.distance.str(),
            modifier = Modifier.weight(1f),
            isError = distanceText.isNotBlank() && distance == null,
        )
        DropdownChoice(
            options = DistanceUnit.entries,
            selected = distanceUnit,
            onSelect = { distanceUnit = it },
            label = Res.string.unit.str(),
            text = { it.symbol },
            modifier = Modifier.weight(1f),
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NumberField(
            value = consumptionText,
            onValueChange = { consumptionText = it },
            label = Res.string.consumption.str(),
            modifier = Modifier.weight(1f),
            isError = consumptionText.isNotBlank() && consumption == null,
        )
        DropdownChoice(
            options = ConsumptionUnit.entries,
            selected = consumptionUnit,
            onSelect = { consumptionUnit = it },
            label = Res.string.unit.str(),
            text = { it.symbol },
            modifier = Modifier.weight(1f),
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NumberField(
            value = priceText,
            onValueChange = { priceText = it },
            label = Res.string.fuel_price.str(),
            modifier = Modifier.weight(1f),
            isError = priceText.isNotBlank() && price == null,
        )
        DropdownChoice(
            options = PriceUnit.entries,
            selected = priceUnit,
            onSelect = { priceUnit = it },
            label = Res.string.per.str(),
            text = { it.symbol },
            modifier = Modifier.weight(1f),
        )
    }
    NumberField(
        value = passengersText,
        onValueChange = { passengersText = it },
        label = Res.string.passengers_including_driver.str(),
        isError = passengersText.isNotBlank() && passengers == null,
    )
    SwitchRow(
        label = Res.string.round_trip.str(),
        checked = roundTrip,
        onCheckedChange = { roundTrip = it },
    )
    if (distance != null && consumption != null && price != null && passengers != null) {
        val result = FuelCost.compute(distance, distanceUnit, consumption, consumptionUnit, price, priceUnit, passengers, roundTrip)
        ResultCard {
            KeyValueRow(Res.string.fuel_needed.str(), "${result.litres.fmt(1)} L")
            KeyValueRow(Res.string.trip_cost.str(), result.cost.fmt(2, grouping = true))
            if (passengers > 1) KeyValueRow(Res.string.per_person.str(), result.perPerson.fmt(2, grouping = true))
            KeyValueRow(Res.string.cost_per_100_km.str(), result.costPer100km.fmt(2, grouping = true))
            KeyValueRow(Res.string.total_distance.str(), "${result.distanceKm.fmt(1)} km")
            KeyValueRow("L/100 km", FuelCost.litresPer100km(consumption, consumptionUnit).fmt(2), copyable = false)
        }
    }
}
