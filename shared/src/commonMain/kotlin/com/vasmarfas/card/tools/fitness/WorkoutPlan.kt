package com.vasmarfas.card.tools.fitness

import com.vasmarfas.card.core.Net
import com.vasmarfas.card.core.Prefs
import com.vasmarfas.card.core.currentEpochMillis
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

@Serializable
data class WorkoutExercise(
    val id: String = "",
    val name: String = "",
    val sets: Int = 3,
    val reps: Int = 10,
    val durationSec: Int = 0,
    val weightKg: Double = 0.0,
    val restSec: Int = 60,
    val restAfterSec: Int = 120,
    val notes: String = "",
)

@Serializable
data class WorkoutPlan(
    val id: String = "",
    val name: String = "",
    val exercises: List<WorkoutExercise> = emptyList(),
)

object WorkoutPlans {
    const val PREF_KEY = "workout.plans"
    const val SECONDS_PER_REP = 3

    private var counter = 0

    fun newId(): String {
        counter++
        return "${currentEpochMillis().toString(36)}$counter"
    }

    fun withFreshIds(plan: WorkoutPlan): WorkoutPlan = plan.copy(
        id = newId(),
        exercises = plan.exercises.map { it.copy(id = newId()) },
    )

    fun workSeconds(exercise: WorkoutExercise): Int =
        if (exercise.durationSec > 0) exercise.durationSec else exercise.reps * SECONDS_PER_REP

    fun estimatedSeconds(plan: WorkoutPlan): Int = plan.exercises.mapIndexed { index, exercise ->
        val sets = exercise.sets.coerceAtLeast(1)
        val work = workSeconds(exercise) * sets
        val restBetweenSets = exercise.restSec * (sets - 1)
        val after = if (index == plan.exercises.lastIndex) 0 else exercise.restAfterSec
        work + restBetweenSets + after
    }.sum()

    fun totalVolume(plan: WorkoutPlan): Double = plan.exercises.sumOf {
        it.sets.coerceAtLeast(0) * it.reps.coerceAtLeast(0) * it.weightKg
    }

    fun totalSets(plan: WorkoutPlan): Int = plan.exercises.sumOf { it.sets.coerceAtLeast(0) }

    fun encode(plans: List<WorkoutPlan>): String =
        Net.json.encodeToString(ListSerializer(WorkoutPlan.serializer()), plans)

    fun decode(text: String?): List<WorkoutPlan>? =
        text?.let { runCatching { Net.json.decodeFromString(ListSerializer(WorkoutPlan.serializer()), it) }.getOrNull() }

    fun encodeOne(plan: WorkoutPlan): String = Net.json.encodeToString(WorkoutPlan.serializer(), plan)

    fun decodeOne(text: String): WorkoutPlan? =
        runCatching { Net.json.decodeFromString(WorkoutPlan.serializer(), text) }.getOrNull()

    fun load(): List<WorkoutPlan> = decode(Prefs.store.get(PREF_KEY)) ?: emptyList()

    fun save(plans: List<WorkoutPlan>) = Prefs.store.put(PREF_KEY, encode(plans))
}
