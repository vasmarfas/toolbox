package com.vasmarfas.card.tools.fitness

import com.vasmarfas.card.resources.*
import org.jetbrains.compose.resources.StringResource

class Activity(val title: StringResource, val group: StringResource, val met: Double)

private val walking = Res.string.walking
private val running = Res.string.running
private val cycling = Res.string.cycling
private val water = Res.string.water
private val gym = Res.string.gym
private val sports = Res.string.sports
private val outdoor = Res.string.outdoor
private val household = Res.string.everyday

val activities: List<Activity> = listOf(
    Activity(Res.string.walking_4_km_h, walking, 3.0),
    Activity(Res.string.walking_5_km_h, walking, 3.5),
    Activity(Res.string.walking_6_5_km_h, walking, 5.0),
    Activity(Res.string.nordic_walking, walking, 4.8),
    Activity(Res.string.stairs_going_up, walking, 8.8),
    Activity(Res.string.running_8_km_h, running, 8.3),
    Activity(Res.string.running_9_7_km_h, running, 9.8),
    Activity(Res.string.running_11_3_km_h, running, 11.0),
    Activity(Res.string.running_12_9_km_h, running, 11.8),
    Activity(Res.string.running_14_5_km_h, running, 12.8),
    Activity(Res.string.running_16_km_h, running, 14.5),
    Activity(Res.string.trail_running, running, 9.0),
    Activity(Res.string.cycling_16_19_km_h, cycling, 6.8),
    Activity(Res.string.cycling_19_22_km_h, cycling, 8.0),
    Activity(Res.string.cycling_22_25_km_h, cycling, 10.0),
    Activity(Res.string.cycling_25_30_km_h, cycling, 12.0),
    Activity(Res.string.mountain_biking, cycling, 8.5),
    Activity(Res.string.stationary_bike_moderate, cycling, 7.0),
    Activity(Res.string.swimming_leisurely, water, 6.0),
    Activity(Res.string.swimming_freestyle_moderate, water, 8.3),
    Activity(Res.string.swimming_freestyle_fast, water, 9.8),
    Activity(Res.string.water_aerobics, water, 5.5),
    Activity(Res.string.strength_training_light, gym, 3.5),
    Activity(Res.string.strength_training_vigorous, gym, 6.0),
    Activity(Res.string.circuit_training, gym, 8.0),
    Activity(Res.string.hiit, gym, 8.0),
    Activity(Res.string.rowing_machine_moderate, gym, 7.0),
    Activity(Res.string.elliptical, gym, 5.0),
    Activity(Res.string.jump_rope, gym, 11.8),
    Activity(Res.string.yoga, gym, 2.5),
    Activity(Res.string.stretching, gym, 2.3),
    Activity(Res.string.pilates, gym, 3.0),
    Activity(Res.string.football, sports, 7.0),
    Activity(Res.string.basketball, sports, 6.5),
    Activity(Res.string.volleyball, sports, 4.0),
    Activity(Res.string.tennis_singles, sports, 7.3),
    Activity(Res.string.table_tennis, sports, 4.0),
    Activity(Res.string.badminton, sports, 5.5),
    Activity(Res.string.boxing_bag_work, sports, 5.5),
    Activity(Res.string.boxing_sparring, sports, 7.8),
    Activity(Res.string.climbing, sports, 8.0),
    Activity(Res.string.dancing, sports, 5.0),
    Activity(Res.string.downhill_skiing, outdoor, 5.3),
    Activity(Res.string.cross_country_skiing, outdoor, 9.0),
    Activity(Res.string.snowboarding, outdoor, 5.3),
    Activity(Res.string.ice_skating, outdoor, 7.0),
    Activity(Res.string.hiking, outdoor, 6.0),
    Activity(Res.string.hiking_with_a_10_kg_pack, outdoor, 7.8),
    Activity(Res.string.shovelling_snow, household, 5.3),
    Activity(Res.string.gardening, household, 3.8),
    Activity(Res.string.cleaning_the_flat, household, 3.3),
    Activity(Res.string.carrying_boxes, household, 7.0),
    Activity(Res.string.desk_work, household, 1.5),
    Activity(Res.string.standing, household, 2.0),
)

object CalorieBurn {
    fun met(metValue: Double, weightKg: Double, minutes: Double): Double =
        metValue * 3.5 * weightKg / 200.0 * minutes

    fun keytel(sex: Sex, heartRate: Double, weightKg: Double, age: Int, minutes: Double): Double {
        val perMinute = if (sex == Sex.MALE) {
            -55.0969 + 0.6309 * heartRate + 0.1988 * weightKg + 0.2017 * age
        } else {
            -20.4022 + 0.4472 * heartRate - 0.1263 * weightKg + 0.074 * age
        }
        return (perMinute / 4.184 * minutes).coerceAtLeast(0.0)
    }
}
