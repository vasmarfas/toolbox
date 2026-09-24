package com.vasmarfas.card.tools.fitness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.core.formatDurationMs
import com.vasmarfas.card.core.str
import com.vasmarfas.card.core.toDoubleLenient
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.DropdownChoice
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SoftDivider
import com.vasmarfas.card.ui.components.ToolInputField
import com.vasmarfas.card.ui.components.ToolSection

val workoutBuilderTool = Tool(
    id = "workout-builder",
    category = ToolCategory.FITNESS,
    title = Res.string.workout_builder,
    description = Res.string.workout_builder_description,
    icon = Icons.Filled.FitnessCenter,
    keywords = listOf("workout", "plan", "sets", "reps", "volume", "gym", "тренировка", "план", "подходы", "повторы", "тоннаж", "зал"),
) { WorkoutBuilderScreen() }

@Composable
private fun WorkoutBuilderScreen() {
    val untitledText = Res.string.untitled.str()
    val copy2Text = Res.string.workout_copy.str()
    val plans = remember { mutableStateListOf<WorkoutPlan>().apply { addAll(WorkoutPlans.load()) } }
    var selectedId by rememberSaveable { mutableStateOf(plans.firstOrNull()?.id ?: "") }
    var transfer by rememberSaveable { mutableStateOf("") }
    var importError by rememberSaveable { mutableStateOf(false) }

    fun persist() = WorkoutPlans.save(plans)

    fun addPlan(plan: WorkoutPlan) {
        plans.add(plan)
        selectedId = plan.id
        persist()
    }

    ActionButton(
        text = Res.string.new_plan.str(),
        onClick = {
            addPlan(
                WorkoutPlan(
                    id = WorkoutPlans.newId(),
                    name = "",
                    exercises = listOf(WorkoutExercise(id = WorkoutPlans.newId())),
                ),
            )
        },
        icon = Icons.Filled.Add,
    )
    if (plans.isEmpty()) {
        Text(
            text = Res.string.workout_no_plans_yet_create.str(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    val index = plans.indexOfFirst { it.id == selectedId }.let { if (it < 0) 0 else it }
    val plan = plans[index]
    DropdownChoice(
        options = plans.toList(),
        selected = plan,
        onSelect = { selectedId = it.id },
        label = Res.string.plan.str(),
        text = { it.name.ifBlank { Res.string.untitled.str() } },
    )
    ToolInputField(
        value = plan.name,
        onValueChange = {
            plans[index] = plan.copy(name = it)
            persist()
        },
        label = Res.string.plan_name.str(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        ActionButton(
            text = Res.string.duplicate.str(),
            onClick = {
                val base = plan.name.ifBlank { untitledText }
                addPlan(WorkoutPlans.withFreshIds(plan).copy(name = "$base · $copy2Text"))
            },
        )
        ActionButton(
            text = Res.string.delete_plan.str(),
            onClick = {
                plans.removeAt(index)
                selectedId = plans.firstOrNull()?.id ?: ""
                persist()
            },
            icon = Icons.Filled.Delete,
        )
    }

    fun updateExercise(position: Int, exercise: WorkoutExercise) {
        plans[index] = plan.copy(exercises = plan.exercises.toMutableList().also { it[position] = exercise })
        persist()
    }

    fun moveExercise(position: Int, delta: Int) {
        val target = position + delta
        if (target !in plan.exercises.indices) return
        val list = plan.exercises.toMutableList()
        list.add(target, list.removeAt(position))
        plans[index] = plan.copy(exercises = list)
        persist()
    }

    plan.exercises.forEachIndexed { position, exercise ->
        key(exercise.id) {
            ExerciseEditor(
                exercise = exercise,
                position = position,
                count = plan.exercises.size,
                onChange = { updateExercise(position, it) },
                onMove = { moveExercise(position, it) },
                onDelete = {
                    plans[index] = plan.copy(exercises = plan.exercises.filterIndexed { i, _ -> i != position })
                    persist()
                },
            )
        }
    }
    ActionButton(
        text = Res.string.add_exercise.str(),
        onClick = {
            plans[index] = plan.copy(exercises = plan.exercises + WorkoutExercise(id = WorkoutPlans.newId()))
            persist()
        },
        icon = Icons.Filled.Add,
    )

    ResultCard(Res.string.totals.str()) {
        KeyValueRow(Res.string.exercises.str(), plan.exercises.size.toString())
        KeyValueRow(Res.string.sets.str(), WorkoutPlans.totalSets(plan).toString())
        KeyValueRow(
            Res.string.estimated_duration.str(),
            formatDurationMs(WorkoutPlans.estimatedSeconds(plan) * 1000L),
        )
        KeyValueRow(Res.string.total_volume.str(), "${WorkoutPlans.totalVolume(plan).fmt(1, grouping = true)} ${Res.string.unit_kg.str()}")
    }

    ToolSection(Res.string.export_and_import.str()) {
        ToolInputField(
            value = transfer,
            onValueChange = {
                transfer = it
                importError = false
            },
            label = "JSON",
            singleLine = false,
            minLines = 3,
            monospace = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ActionButton(
                text = Res.string.export.str(),
                onClick = {
                    transfer = WorkoutPlans.encodeOne(plan)
                    importError = false
                },
            )
            ActionButton(
                text = Res.string.import_.str(),
                onClick = {
                    val imported = WorkoutPlans.decodeOne(transfer)
                    if (imported == null) {
                        importError = true
                    } else {
                        importError = false
                        addPlan(WorkoutPlans.withFreshIds(imported))
                    }
                },
                enabled = transfer.isNotBlank(),
            )
        }
        if (importError) {
            ErrorText(Res.string.workout_is_not_a_plan.str())
        }
    }
}

@Composable
private fun ExerciseEditor(
    exercise: WorkoutExercise,
    position: Int,
    count: Int,
    onChange: (WorkoutExercise) -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    var weightText by remember { mutableStateOf(if (exercise.weightKg == 0.0) "" else exercise.weightKg.fmt(2)) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        SoftDivider()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ToolInputField(
                value = exercise.name,
                onValueChange = { onChange(exercise.copy(name = it)) },
                label = "${position + 1}. ${Res.string.exercise.str()}",
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onMove(-1) }, enabled = position > 0) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = Res.string.workout_up.str())
            }
            IconButton(onClick = { onMove(1) }, enabled = position < count - 1) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = Res.string.workout_down.str())
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = Res.string.delete.str())
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            IntField(
                value = exercise.sets,
                onValueChange = { onChange(exercise.copy(sets = it)) },
                label = Res.string.workout_sets.str(),
                modifier = Modifier.weight(1f),
            )
            IntField(
                value = exercise.reps,
                onValueChange = { onChange(exercise.copy(reps = it)) },
                label = Res.string.reps.str(),
                modifier = Modifier.weight(1f),
            )
            IntField(
                value = exercise.durationSec,
                onValueChange = { onChange(exercise.copy(durationSec = it)) },
                label = Res.string.time_s.str(),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            NumberField(
                value = weightText,
                onValueChange = {
                    weightText = it
                    onChange(exercise.copy(weightKg = it.toDoubleLenient() ?: 0.0))
                },
                label = Res.string.weight.str(),
                modifier = Modifier.weight(1f),
                suffix = Res.string.unit_kg.str(),
                isError = weightText.isNotBlank() && weightText.toDoubleLenient() == null,
            )
            IntField(
                value = exercise.restSec,
                onValueChange = { onChange(exercise.copy(restSec = it)) },
                label = Res.string.rest_s.str(),
                modifier = Modifier.weight(1f),
            )
            IntField(
                value = exercise.restAfterSec,
                onValueChange = { onChange(exercise.copy(restAfterSec = it)) },
                label = Res.string.after_s.str(),
                modifier = Modifier.weight(1f),
            )
        }
        ToolInputField(
            value = exercise.notes,
            onValueChange = { onChange(exercise.copy(notes = it)) },
            label = Res.string.notes.str(),
        )
    }
}

@Composable
private fun IntField(value: Int, onValueChange: (Int) -> Unit, label: String, modifier: Modifier = Modifier) {
    ToolInputField(
        value = if (value == 0) "" else value.toString(),
        onValueChange = { text ->
            val digits = text.filter { it.isDigit() }.take(5)
            onValueChange(digits.toIntOrNull() ?: 0)
        },
        label = label,
        modifier = modifier,
        keyboardType = KeyboardType.Number,
    )
}
