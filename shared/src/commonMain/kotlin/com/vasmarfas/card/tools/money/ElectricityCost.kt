package com.vasmarfas.card.tools.money

import com.vasmarfas.card.resources.*
import org.jetbrains.compose.resources.StringResource

class Appliance(val title: StringResource, val watts: Int)

val appliances = listOf(
    Appliance(Res.string.appliance_led_bulb, 10),
    Appliance(Res.string.appliance_router, 12),
    Appliance(Res.string.appliance_laptop, 60),
    Appliance(Res.string.appliance_fridge, 50),
    Appliance(Res.string.appliance_tv, 100),
    Appliance(Res.string.appliance_pc, 300),
    Appliance(Res.string.appliance_ac, 1000),
    Appliance(Res.string.appliance_heater, 1500),
    Appliance(Res.string.appliance_kettle, 2000),
)

class EnergyUse(val kwhPerDay: Double, val daysPerMonth: Double, val pricePerKwh: Double) {
    val kwhPerMonth: Double get() = kwhPerDay * daysPerMonth
    val kwhPerYear: Double get() = kwhPerMonth * 12
    val costPerDay: Double get() = kwhPerDay * pricePerKwh
    val costPerMonth: Double get() = kwhPerMonth * pricePerKwh
    val costPerYear: Double get() = kwhPerYear * pricePerKwh
}

object ElectricityCost {
    fun of(watts: Double, hoursPerDay: Double, daysPerMonth: Double, pricePerKwh: Double): EnergyUse =
        EnergyUse(watts * hoursPerDay / 1000, daysPerMonth, pricePerKwh)
}
