package com.vasmarfas.card.tools.fitness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.vasmarfas.card.core.Prefs
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
import com.vasmarfas.card.ui.components.Hint
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.LocalOpenTool
import com.vasmarfas.card.ui.components.NumberField
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
    val openTool = LocalOpenTool.current
    val plans = remember { mutableStateListOf<WorkoutPlan>().apply { addAll(WorkoutPlans.load()) } }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var exportId by rememberSaveable { mutableStateOf("") }
    var transfer by rememberSaveable { mutableStateOf("") }
    var importError by rememberSaveable { mutableStateOf(false) }

    fun persist() = WorkoutPlans.save(plans)

    fun addPlan(plan: WorkoutPlan) {
        plans.add(plan)
        editingId = plan.id
        persist()
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
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
            modifier = Modifier.weight(1f),
        )
        ActionButton(
            text = Res.string.to_workout_timer.str(),
            onClick = { openTool("workout-timer") },
            icon = Icons.Filled.Timer,
            modifier = Modifier.weight(1f),
        )
    }
    if (plans.isEmpty()) {
        Hint(Res.string.workout_no_plans_yet_create.str())
        return
    }

    plans.toList().forEachIndexed { index, plan ->
        key(plan.id) {
            PlanCard(
                plan = plan,
                editing = plan.id == editingId,
                onEdit = { editingId = plan.id },
                onDone = { editingId = null },
                onStart = {
                    Prefs.store.put(WorkoutPlans.START_KEY, plan.id)
                    openTool("workout-timer")
                },
                onDuplicate = { addPlan(WorkoutPlans.withFreshIds(plan).copy(name = "${plan.name.ifBlank { untitledText }} · $copy2Text")) },
                onDelete = {
                    plans.removeAt(index)
                    editingId = null
                    persist()
                },
                onChange = {
                    plans[index] = it
                    persist()
                },
            )
        }
    }

    ToolSection(Res.string.export_and_import.str()) {
        val exported = plans.firstOrNull { it.id == exportId } ?: plans.first()
        DropdownChoice(
            options = plans.toList(),
            selected = exported,
            onSelect = { exportId = it.id },
            label = Res.string.plan.str(),
            text = { it.name.ifBlank { untitledText } },
        )
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
                    transfer = WorkoutPlans.encodeOne(exported)
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
private fun PlanCard(
    plan: WorkoutPlan,
    editing: Boolean,
    onEdit: () -> Unit,
    onDone: () -> Unit,
    onStart: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onChange: (WorkoutPlan) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(plan.name.ifBlank { Res.string.untitled.str() }, style = MaterialTheme.typography.titleMedium)
                    Hint(
                        "${Res.string.exercises.str()}: ${plan.exercises.size} · ${Res.string.sets.str()}: ${WorkoutPlans.totalSets(plan)} · " +
                            formatDurationMs(WorkoutPlans.estimatedSeconds(plan) * 1000L),
                    )
                }
                if (!editing) {
                    IconButton(onClick = onStart) { Icon(Icons.Filled.PlayArrow, contentDescription = Res.string.start.str()) }
                    IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = Res.string.edit.str()) }
                }
            }
            if (!editing) return@Column
            ToolInputField(
                value = plan.name,
                onValueChange = { onChange(plan.copy(name = it)) },
                label = Res.string.plan_name.str(),
            )

            fun update(position: Int, exercise: WorkoutExercise) = onChange(plan.copy(exercises = plan.exercises.toMutableList().also { it[position] = exercise }))

            fun move(position: Int, delta: Int) {
                val target = position + delta
                if (target !in plan.exercises.indices) return
                val list = plan.exercises.toMutableList()
                list.add(target, list.removeAt(position))
                onChange(plan.copy(exercises = list))
            }

            plan.exercises.forEachIndexed { position, exercise ->
                key(exercise.id) {
                    ExerciseEditor(
                        exercise = exercise,
                        position = position,
                        count = plan.exercises.size,
                        onChange = { update(position, it) },
                        onMove = { move(position, it) },
                        onDelete = { onChange(plan.copy(exercises = plan.exercises.filterIndexed { i, _ -> i != position })) },
                    )
                }
            }
            ActionButton(
                text = Res.string.add_exercise.str(),
                onClick = { onChange(plan.copy(exercises = plan.exercises + WorkoutExercise(id = WorkoutPlans.newId()))) },
                icon = Icons.Filled.Add,
            )
            KeyValueRow(Res.string.total_volume.str(), "${WorkoutPlans.totalVolume(plan).fmt(1, grouping = true)} ${Res.string.unit_kg.str()}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDuplicate) { Text(Res.string.duplicate.str()) }
                TextButton(onClick = onDelete) { Text(Res.string.delete_plan.str(), color = MaterialTheme.colorScheme.error) }
            }
            ActionButton(
                text = Res.string.finish_editing.str(),
                onClick = onDone,
                icon = Icons.Filled.Check,
                modifier = Modifier.fillMaxWidth(),
            )
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
