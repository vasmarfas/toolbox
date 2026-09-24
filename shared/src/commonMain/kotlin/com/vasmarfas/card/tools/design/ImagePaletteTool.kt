package com.vasmarfas.card.tools.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.PickKind
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.core.limitedTo
import com.vasmarfas.card.core.pixels
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.tools.media.PickButton
import com.vasmarfas.card.tools.media.Preview
import com.vasmarfas.card.tools.media.decodeImage
import com.vasmarfas.card.tools.media.imageExtensions
import com.vasmarfas.card.tools.text.OutputCard
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.LoadingRow
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SegmentedChoice
import com.vasmarfas.card.ui.components.ToolSection
import com.vasmarfas.card.ui.components.rememberCopy
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val imagePaletteTool = Tool(
    id = "image-palette",
    category = ToolCategory.DESIGN,
    title = Res.string.image_palette,
    description = Res.string.image_palette_description,
    icon = Icons.Filled.ColorLens,
    keywords = listOf(
        "palette", "colors from image", "colors from photo", "dominant color", "extract colors", "eyedropper",
        "палитра", "цвета из фото", "цвета картинки", "основной цвет", "подобрать цвета", "пипетка",
    ),
) { ImagePaletteScreen() }

private val colorCounts = listOf(5, 8, 12, 16)

private const val SWATCHES_PER_ROW = 4

@Composable
private fun ImagePaletteScreen() {
    val scope = rememberCoroutineScope()
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var count by rememberSaveable { mutableStateOf(8) }
    var format by rememberSaveable { mutableStateOf(PaletteFormat.HEX) }
    val copy = rememberCopy()
    val unreadable = Res.string.image_not_readable.str()
    PickButton(Res.string.open_photo.str(), imageExtensions, PickKind.IMAGE, icon = Icons.Filled.ImageSearch, empty = image == null) { files ->
        error = null
        scope.launch {
            val decoded = runCatching { decodeImage(files.first().readBytes())?.limitedTo(640) }.getOrNull()
            if (decoded == null) error = unreadable else image = decoded
        }
    }
    error?.let { ErrorText(it) }
    val current = image ?: return
    Preview(current)
    ToolSection(Res.string.number_of_colours.str()) {
        ChoiceChips(options = colorCounts, selected = count, onSelect = { count = it }, label = { it.toString() })
    }
    val entries by produceState<List<PaletteEntry>?>(null, current, count) {
        value = withContext(Dispatchers.Default) {
            val small = current.limitedTo(256)
            ImagePalette.extract(small.pixels(), small.width, small.height, count)
        }
    }
    val palette = entries
    if (palette == null) {
        LoadingRow()
        return
    }
    Row(Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(12.dp))) {
        palette.forEach { Box(Modifier.weight(it.share.toFloat()).fillMaxHeight().background(it.color.toColor())) }
    }
    ResultCard {
        palette.chunked(SWATCHES_PER_ROW).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { entry ->
                    ColorSwatch(
                        color = entry.color,
                        label = entry.color.hex(),
                        modifier = Modifier.weight(1f),
                        caption = "${(entry.share * 100).fmt(0)}%",
                        onClick = { copy(entry.color.hex()) },
                    )
                }
                repeat(SWATCHES_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    SegmentedChoice(
        options = PaletteFormat.entries,
        selected = format,
        onSelect = { format = it },
        label = {
            when (it) {
                PaletteFormat.HEX -> "HEX"
                PaletteFormat.CSS -> "CSS"
                PaletteFormat.JSON -> "JSON"
            }
        },
    )
    OutputCard(ImagePalette.export(palette, format))
}
