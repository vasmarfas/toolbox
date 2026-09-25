package com.vasmarfas.card.tools.design

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.CopyIconButton
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SwitchRow
import com.vasmarfas.card.ui.components.ToolSection
import com.vasmarfas.card.ui.components.monoFamily

val paperSizesTool = Tool(
    id = "paper-sizes",
    category = ToolCategory.DESIGN,
    title = Res.string.paper_sizes,
    description = Res.string.paper_sizes_description,
    icon = Icons.Filled.CropPortrait,
    keywords = listOf(
        "paper size", "a4", "a3", "a5", "letter", "legal", "dpi", "print", "pixels", "photo size", "business card", "envelope",
        "формат бумаги", "а4", "а3", "размер листа", "в пикселях", "фото 10х15", "визитка", "конверт", "печать",
    ),
) { PaperSizesScreen() }

private val resolutions = listOf(72, 96, 150, 300, 600)

@Composable
private fun PaperSizesScreen() {
    var series by rememberSaveable { mutableStateOf(PaperSeries.ISO_A) }
    var dpi by rememberSaveable { mutableStateOf(300) }
    var landscape by rememberSaveable { mutableStateOf(false) }
    ChoiceChips(
        options = PaperSeries.entries,
        selected = series,
        onSelect = { series = it },
        label = {
            when (it) {
                PaperSeries.ISO_A -> "A"
                PaperSeries.ISO_B -> "B"
                PaperSeries.ISO_C -> Res.string.paper_series_c.str()
                PaperSeries.US -> Res.string.paper_series_us.str()
                PaperSeries.PHOTO -> Res.string.paper_series_photo.str()
                PaperSeries.CARDS -> Res.string.paper_series_cards.str()
            }
        },
    )
    ToolSection("DPI") {
        ChoiceChips(options = resolutions, selected = dpi, onSelect = { dpi = it }, label = { it.toString() })
        Text(Res.string.dpi_hint.str(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    SwitchRow(Res.string.landscape_orientation.str(), landscape, { landscape = it })
    if (series == PaperSeries.PHOTO) {
        Text(Res.string.photo_sizes_are_inches.str(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    ResultCard {
        PaperSizes.series.getValue(series).forEach { PaperRow(it, dpi, landscape) }
    }
}

@Composable
private fun PaperRow(size: PaperSize, dpi: Int, landscape: Boolean) {
    val width = if (landscape) size.heightMm else size.widthMm
    val height = if (landscape) size.widthMm else size.heightMm
    val pixels = if (landscape) "${size.heightPx(dpi)} × ${size.widthPx(dpi)}" else "${size.widthPx(dpi)} × ${size.heightPx(dpi)}"
    val millimeters = "${width.fmt(1)} × ${height.fmt(1)} ${Res.string.unit_mm.str()}"
    val inches = "${(width / PaperSizes.MM_PER_INCH).fmt(2)} × ${(height / PaperSizes.MM_PER_INCH).fmt(2)}″"
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(listOfNotNull(size.name, size.use?.let { useName(it) }).joinToString(", "), style = MaterialTheme.typography.titleSmall)
            Text(
                if (size.inches) "$inches · $millimeters" else "$millimeters · $inches",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("$pixels px", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFamily()))
        }
        CopyIconButton(pixels)
    }
}

@Composable
private fun useName(use: PaperUse): String = when (use) {
    PaperUse.ENVELOPE -> Res.string.paper_use_envelope.str()
    PaperUse.BUSINESS_CARD_RU -> Res.string.paper_use_business_card_ru.str()
    PaperUse.BUSINESS_CARD_EU -> Res.string.paper_use_business_card_eu.str()
    PaperUse.BUSINESS_CARD_US -> Res.string.paper_use_business_card_us.str()
    PaperUse.BANK_CARD -> Res.string.paper_use_bank_card.str()
    PaperUse.PASSPORT_PHOTO -> Res.string.paper_use_passport_photo.str()
    PaperUse.US_PASSPORT_PHOTO -> Res.string.paper_use_us_passport_photo.str()
}
