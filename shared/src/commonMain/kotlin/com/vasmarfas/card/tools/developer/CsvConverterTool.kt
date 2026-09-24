package com.vasmarfas.card.tools.developer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.tools.text.OutputCard
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.SegmentedChoice
import com.vasmarfas.card.ui.components.SwitchRow
import com.vasmarfas.card.ui.components.ToolInputField
import kotlinx.serialization.json.JsonArray
import org.jetbrains.compose.resources.StringResource

private enum class CsvTarget(val title: StringResource) {
    JSON(Res.string.csv_json),
    MARKDOWN(Res.string.csv_markdown),
    CSV(Res.string.json_csv),
}

val csvConverterTool = Tool(
    id = "csv-converter",
    category = ToolCategory.DEVELOPER,
    title = Res.string.csv_converter,
    description = Res.string.csv_converter_description,
    icon = Icons.Filled.TableChart,
    keywords = listOf("csv", "tsv", "json", "markdown", "table", "convert", "spreadsheet", "таблица", "конвертер", "разделитель"),
) { CsvConverterScreen() }

@Composable
private fun CsvConverterScreen() {
    var target by rememberSaveable { mutableStateOf(CsvTarget.JSON) }
    var input by rememberSaveable { mutableStateOf("") }
    var hasHeader by rememberSaveable { mutableStateOf(true) }
    var semicolonOut by rememberSaveable { mutableStateOf(false) }
    SegmentedChoice(options = CsvTarget.entries, selected = target, onSelect = { target = it }, label = { it.title.str() })
    ToolInputField(
        value = input,
        onValueChange = { input = it },
        label = if (target == CsvTarget.CSV) Res.string.json_array_of_objects.str() else "CSV / TSV",
        singleLine = false,
        minLines = 6,
        placeholder = if (target == CsvTarget.CSV) "[{\"name\": \"Ann\", \"age\": 30}]" else "name,age\nAnn,30",
        monospace = true,
    )
    if (target == CsvTarget.CSV) {
        SwitchRow(Res.string.use_semicolon_as_delimiter.str(), semicolonOut, { semicolonOut = it })
    } else {
        SwitchRow(Res.string.first_row_is_a_header.str(), hasHeader, { hasHeader = it })
    }
    if (input.isBlank()) return
    when (target) {
        CsvTarget.CSV -> {
            val parsed = remember(input) { JsonTools.parse(input) }
            val element = parsed.getOrNull()
            if (element !is JsonArray) {
                ErrorText(
                    if (element == null) Res.string.invalid_json.str() + JsonTools.errorMessage(parsed.exceptionOrNull()!!)
                    else Res.string.csv_expected_a_json_array.str(),
                )
                return
            }
            val csv = remember(element, semicolonOut) { Csv.fromJson(element, if (semicolonOut) ';' else ',') }
            if (csv == null) {
                ErrorText(Res.string.csv_array_must_contain_objects.str())
                return
            }
            OutputCard(csv)
            KeyValueRow(Res.string.rows.str(), element.size.toString(), copyable = false)
        }

        else -> {
            val delimiter = remember(input) { Csv.detectDelimiter(input) }
            val table = remember(input, delimiter) { Csv.parse(input, delimiter) }
            val delimiterName = when (delimiter) {
                '\t' -> Res.string.tab.str()
                ';' -> Res.string.semicolon.str()
                '|' -> Res.string.vertical_bar.str()
                else -> Res.string.comma.str()
            }
            KeyValueRow(Res.string.delimiter.str(), delimiterName, mono = false, copyable = false)
            KeyValueRow(Res.string.rows_columns.str(), "${table.rows.size} × ${table.columns}", copyable = false)
            val output = remember(table, hasHeader, target) {
                if (target == CsvTarget.JSON) JsonTools.format(Csv.toJson(table, hasHeader), 2) else Csv.toMarkdown(table, hasHeader)
            }
            OutputCard(output)
        }
    }
}
