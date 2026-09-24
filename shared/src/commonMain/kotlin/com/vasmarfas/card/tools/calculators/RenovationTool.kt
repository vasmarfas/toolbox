package com.vasmarfas.card.tools.calculators

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.vasmarfas.card.ui.components.SegmentedChoice
import com.vasmarfas.card.ui.components.ToolSection
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

val renovationTool = Tool(
    id = "renovation",
    category = ToolCategory.CALCULATORS,
    title = Res.string.renovation,
    description = Res.string.renovation_description,
    icon = Icons.Filled.FormatPaint,
    keywords = listOf(
        "renovation", "wallpaper", "paint", "laminate", "tile", "flooring", "grout", "skirting", "rolls",
        "ремонт", "обои", "краска", "ламинат", "плитка", "затирка", "плинтус", "рулоны", "сколько нужно", "стройка",
    ),
) { RenovationScreen() }

private enum class RenovationJob { WALLPAPER, PAINT, LAMINATE, TILE }

private enum class PaintSurface { WALLS, CEILING, BOTH }

private enum class TileSurface { FLOOR, WALLS }

private val rollWidths = listOf(0.53, 0.7, 1.06)
private val canSizes = listOf(0.9, 2.5, 5.0, 9.0)
private val wastes = listOf(5, 10, 15)
private val coatCounts = listOf(1, 2, 3)

private const val SKIRTING_LENGTH = 2.5

private class Room(val length: Double, val width: Double, val height: Double) {
    val floor: Double get() = length * width
    val perimeter: Double get() = Renovation.perimeter(length, width)
}

@Composable
private fun RenovationScreen() {
    var job by rememberSaveable { mutableStateOf(RenovationJob.WALLPAPER) }
    var paintSurface by rememberSaveable { mutableStateOf(PaintSurface.WALLS) }
    var tileSurface by rememberSaveable { mutableStateOf(TileSurface.FLOOR) }
    var lengthText by rememberSaveable { mutableStateOf("4") }
    var widthText by rememberSaveable { mutableStateOf("3") }
    var heightText by rememberSaveable { mutableStateOf("2.7") }
    SegmentedChoice(
        options = RenovationJob.entries,
        selected = job,
        onSelect = { job = it },
        label = {
            when (it) {
                RenovationJob.WALLPAPER -> Res.string.wallpaper.str()
                RenovationJob.PAINT -> Res.string.paint.str()
                RenovationJob.LAMINATE -> Res.string.laminate.str()
                RenovationJob.TILE -> Res.string.tiles.str()
            }
        },
    )
    val walls = when (job) {
        RenovationJob.WALLPAPER -> true
        RenovationJob.PAINT -> paintSurface != PaintSurface.CEILING
        RenovationJob.LAMINATE -> false
        RenovationJob.TILE -> tileSurface == TileSurface.WALLS
    }
    val meters = Res.string.unit_m.str()
    ToolSection(Res.string.room.str()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(lengthText, { lengthText = it }, Res.string.length.str(), Modifier.weight(1f), suffix = meters)
            NumberField(widthText, { widthText = it }, Res.string.width.str(), Modifier.weight(1f), suffix = meters)
            if (walls) NumberField(heightText, { heightText = it }, Res.string.height.str(), Modifier.weight(1f), suffix = meters)
        }
    }
    val length = lengthText.toDoubleLenient()?.takeIf { it > 0 }
    val width = widthText.toDoubleLenient()?.takeIf { it > 0 }
    val height = heightText.toDoubleLenient()?.takeIf { it > 0 }
    val room = if (length != null && width != null && (height != null || !walls)) Room(length, width, height ?: 0.0) else null
    when (job) {
        RenovationJob.WALLPAPER -> WallpaperJob(room)
        RenovationJob.PAINT -> PaintJob(room, paintSurface) { paintSurface = it }
        RenovationJob.LAMINATE -> LaminateJob(room)
        RenovationJob.TILE -> TileJob(room, tileSurface) { tileSurface = it }
    }
}

@Composable
private fun squareMeters(value: Double): String = "${value.fmt(2)} ${Res.string.unit_m2.str()}"

@Composable
private fun WallpaperJob(room: Room?) {
    var rollWidth by rememberSaveable { mutableStateOf(0.53) }
    var rollLengthText by rememberSaveable { mutableStateOf("10.05") }
    var repeatText by rememberSaveable { mutableStateOf("0") }
    val meters = Res.string.unit_m.str()
    ToolSection(Res.string.roll_width.str()) {
        ChoiceChips(options = rollWidths, selected = rollWidth, onSelect = { rollWidth = it }, label = { "${it.fmt(2)} $meters" })
    }
    NumberField(rollLengthText, { rollLengthText = it }, Res.string.roll_length.str(), suffix = meters)
    NumberField(
        repeatText,
        { repeatText = it },
        Res.string.pattern_repeat.str(),
        suffix = Res.string.unit_cm.str(),
        supportingText = Res.string.pattern_repeat_hint.str(),
    )
    val rollLength = rollLengthText.toDoubleLenient() ?: return
    val repeat = (repeatText.toDoubleLenient() ?: 0.0) / 100
    room ?: return
    val plan = Renovation.wallpaper(room.perimeter, room.height, rollWidth, rollLength, repeat)
    if (plan == null) {
        ErrorText(Res.string.strip_longer_than_roll.str())
        return
    }
    AnswerCard(
        value = pluralStringResource(Res.plurals.rolls_count, plan.rolls, plan.rolls),
        caption = stringResource(Res.string.wallpaper_rolls_caption, rollWidth.fmt(2), rollLength.fmt(2)),
    )
    ResultCard {
        KeyValueRow(Res.string.strips.str(), plan.strips.toString(), copyable = false)
        KeyValueRow(Res.string.strips_per_roll.str(), plan.stripsPerRoll.toString(), copyable = false)
        KeyValueRow(Res.string.strip_length_with_trim.str(), "${plan.stripLength.fmt(2)} $meters", copyable = false)
        KeyValueRow(Res.string.perimeter.str(), "${room.perimeter.fmt(2)} $meters", copyable = false)
        Text(Res.string.wallpaper_openings_note.str(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PaintJob(room: Room?, surface: PaintSurface, onSurface: (PaintSurface) -> Unit) {
    var openingsText by rememberSaveable { mutableStateOf("3.6") }
    var coats by rememberSaveable { mutableStateOf(2) }
    var coverageText by rememberSaveable { mutableStateOf("10") }
    var can by rememberSaveable { mutableStateOf(2.5) }
    val liter = Res.string.unit_l.str()
    ChoiceChips(
        options = PaintSurface.entries,
        selected = surface,
        onSelect = onSurface,
        label = {
            when (it) {
                PaintSurface.WALLS -> Res.string.walls.str()
                PaintSurface.CEILING -> Res.string.ceiling.str()
                PaintSurface.BOTH -> Res.string.walls_and_ceiling.str()
            }
        },
    )
    if (surface != PaintSurface.CEILING) {
        NumberField(openingsText, { openingsText = it }, Res.string.windows_and_doors.str(), suffix = Res.string.unit_m2.str(), supportingText = Res.string.openings_area_hint.str())
    }
    ToolSection(Res.string.coats.str()) {
        ChoiceChips(options = coatCounts, selected = coats, onSelect = { coats = it }, label = { it.toString() })
    }
    NumberField(coverageText, { coverageText = it }, Res.string.paint_coverage.str(), suffix = Res.string.m2_per_litre.str(), supportingText = Res.string.paint_coverage_hint.str())
    ToolSection(Res.string.can_size.str()) {
        ChoiceChips(options = canSizes, selected = can, onSelect = { can = it }, label = { "${it.fmt(1)} $liter" })
    }
    room ?: return
    val coverage = coverageText.toDoubleLenient() ?: return
    val openings = openingsText.toDoubleLenient() ?: 0.0
    val area = (if (surface != PaintSurface.CEILING) Renovation.wallArea(room.length, room.width, room.height, openings) else 0.0) +
        (if (surface != PaintSurface.WALLS) room.floor else 0.0)
    val plan = Renovation.paint(area, coats, coverage, can) ?: return
    AnswerCard(
        value = "${plan.liters.fmt(1)} $liter",
        caption = pluralStringResource(Res.plurals.cans_count, plan.cans, plan.cans, can.fmt(1)),
    )
    ResultCard {
        KeyValueRow(Res.string.area_to_paint.str(), squareMeters(area), copyable = false)
    }
}

@Composable
private fun LaminateJob(room: Room?) {
    var packAreaText by rememberSaveable { mutableStateOf("2.131") }
    var waste by rememberSaveable { mutableStateOf(10) }
    var doorwaysText by rememberSaveable { mutableStateOf("0.8") }
    val meters = Res.string.unit_m.str()
    NumberField(packAreaText, { packAreaText = it }, Res.string.area_per_pack.str(), suffix = Res.string.unit_m2.str(), supportingText = Res.string.written_on_the_pack.str())
    WasteChoice(waste) { waste = it }
    NumberField(doorwaysText, { doorwaysText = it }, Res.string.doorways_width.str(), suffix = meters, supportingText = Res.string.doorways_width_hint.str())
    room ?: return
    val packs = Renovation.packs(room.floor, packAreaText.toDoubleLenient() ?: return, waste.toDouble()) ?: return
    AnswerCard(
        value = pluralStringResource(Res.plurals.packs_count, packs, packs),
        caption = stringResource(Res.string.area_with_waste, squareMeters(room.floor * (1 + waste / 100.0)), "$waste%"),
    )
    ResultCard {
        KeyValueRow(Res.string.floor_area.str(), squareMeters(room.floor), copyable = false)
        val doorways = doorwaysText.toDoubleLenient() ?: 0.0
        Renovation.skirtingPieces(room.perimeter, doorways, SKIRTING_LENGTH)?.let { pieces ->
            KeyValueRow(
                Res.string.skirting.str(),
                "${(room.perimeter - doorways).fmt(2)} $meters, " + pluralStringResource(Res.plurals.skirting_pieces, pieces, pieces, SKIRTING_LENGTH.fmt(1)),
                copyable = false,
            )
        }
    }
}

@Composable
private fun TileJob(room: Room?, surface: TileSurface, onSurface: (TileSurface) -> Unit) {
    var openingsText by rememberSaveable { mutableStateOf("1.6") }
    var tileLengthText by rememberSaveable { mutableStateOf("30") }
    var tileWidthText by rememberSaveable { mutableStateOf("30") }
    var jointText by rememberSaveable { mutableStateOf("2") }
    var thicknessText by rememberSaveable { mutableStateOf("8") }
    var perBoxText by rememberSaveable { mutableStateOf("") }
    var waste by rememberSaveable { mutableStateOf(10) }
    val centimeters = Res.string.unit_cm.str()
    val millimeters = Res.string.unit_mm.str()
    ChoiceChips(
        options = TileSurface.entries,
        selected = surface,
        onSelect = onSurface,
        label = { if (it == TileSurface.FLOOR) Res.string.floor.str() else Res.string.walls.str() },
    )
    if (surface == TileSurface.WALLS) {
        NumberField(openingsText, { openingsText = it }, Res.string.windows_and_doors.str(), suffix = Res.string.unit_m2.str(), supportingText = Res.string.openings_area_hint.str())
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField(tileLengthText, { tileLengthText = it }, Res.string.tile_length.str(), Modifier.weight(1f), suffix = centimeters)
        NumberField(tileWidthText, { tileWidthText = it }, Res.string.tile_width.str(), Modifier.weight(1f), suffix = centimeters)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField(jointText, { jointText = it }, Res.string.joint_width.str(), Modifier.weight(1f), suffix = millimeters)
        NumberField(thicknessText, { thicknessText = it }, Res.string.tile_thickness.str(), Modifier.weight(1f), suffix = millimeters)
    }
    WasteChoice(waste) { waste = it }
    NumberField(perBoxText, { perBoxText = it }, Res.string.tiles_per_box.str(), supportingText = Res.string.optional.str())
    room ?: return
    val area = if (surface == TileSurface.FLOOR) room.floor else Renovation.wallArea(room.length, room.width, room.height, openingsText.toDoubleLenient() ?: 0.0)
    val plan = Renovation.tiles(
        area = area,
        lengthMm = (tileLengthText.toDoubleLenient() ?: return) * 10,
        widthMm = (tileWidthText.toDoubleLenient() ?: return) * 10,
        jointMm = jointText.toDoubleLenient() ?: 0.0,
        thicknessMm = thicknessText.toDoubleLenient() ?: 0.0,
        wastePercent = waste.toDouble(),
        perBox = perBoxText.trim().toIntOrNull(),
    ) ?: return
    AnswerCard(
        value = pluralStringResource(Res.plurals.tiles_count, plan.tiles, plan.tiles),
        caption = plan.boxes?.let { pluralStringResource(Res.plurals.boxes_count, it, it) }
            ?: stringResource(Res.string.area_with_waste, squareMeters(area * (1 + waste / 100.0)), "$waste%"),
    )
    ResultCard {
        KeyValueRow(Res.string.area.str(), squareMeters(area), copyable = false)
        if (plan.groutKg > 0) KeyValueRow(Res.string.grout.str(), "${plan.groutKg.fmt(1)} ${Res.string.unit_kg.str()}", copyable = false)
    }
}

@Composable
private fun WasteChoice(waste: Int, onWaste: (Int) -> Unit) {
    ToolSection(Res.string.cutting_waste.str()) {
        ChoiceChips(options = wastes, selected = waste, onSelect = onWaste, label = { "$it%" })
        Text(Res.string.cutting_waste_hint.str(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
