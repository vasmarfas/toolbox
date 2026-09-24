package com.vasmarfas.card.tools.documents

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.vasmarfas.card.core.ZipArchive
import com.vasmarfas.card.core.ZipEntryInfo
import com.vasmarfas.card.core.ZipWriter
import com.vasmarfas.card.core.formatBytes
import com.vasmarfas.card.core.saveBytes
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.tools.media.FileLine
import com.vasmarfas.card.tools.media.PickButton
import com.vasmarfas.card.tools.media.SaveButton
import com.vasmarfas.card.tools.media.uniqueName
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SegmentedChoice
import com.vasmarfas.card.ui.components.SwitchRow
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource

val zipArchiveTool = Tool(
    id = "zip-archive",
    category = ToolCategory.DOCUMENTS,
    title = Res.string.zip_archive,
    description = Res.string.zip_archive_description,
    icon = Icons.Filled.FolderZip,
    keywords = listOf(
        "zip", "unzip", "archive", "extract", "compress files", "pack",
        "архив", "распаковать", "разархивировать", "открыть zip", "запаковать", "сжать файлы в zip",
    ),
) { ZipArchiveScreen() }

private class OpenedZip(val name: String, val archive: ZipArchive, val files: List<ZipEntryInfo>)

@Composable
private fun ZipArchiveScreen() {
    var extracting by rememberSaveable { mutableStateOf(true) }
    SegmentedChoice(
        options = listOf(true, false),
        selected = extracting,
        onSelect = { extracting = it },
        label = { if (it) Res.string.zip_extract.str() else Res.string.zip_create.str() },
    )
    if (extracting) Extract() else Create()
}

@Composable
private fun Extract() {
    val scope = rememberCoroutineScope()
    var opened by remember { mutableStateOf<OpenedZip?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    PickButton(Res.string.choose_zip.str(), setOf("zip"), icon = Icons.Filled.FolderZip, empty = opened == null) { files ->
        val file = files.first()
        opened = null
        error = null
        scope.launch {
            runCatching {
                val archive = withContext(Dispatchers.Default) { ZipArchive(file.readBytes()) }
                OpenedZip(file.name, archive, archive.entries.filter { !it.isDirectory })
            }
                .onSuccess { opened = it }
                .onFailure { error = getString(Res.string.zip_not_readable) }
        }
    }
    error?.let { ErrorText(it) }
    val zip = opened ?: return
    ResultCard(title = zip.name) {
        Text(
            pluralStringResource(Res.plurals.file_count, zip.files.size, zip.files.size) + " · " + formatBytes(zip.files.sumOf { it.size }, binary = false),
            style = MaterialTheme.typography.bodyMedium,
        )
        zip.files.forEach { entry ->
            val ratio = if (entry.size > 0) " · ${entry.compressedSize * 100 / entry.size} %" else ""
            FileLine(
                name = entry.name,
                detail = formatBytes(entry.size, binary = false) + ratio,
                icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                trailing = { SaveEntry(zip.archive, entry) { error = it } },
            )
        }
    }
}

@Composable
private fun SaveEntry(archive: ZipArchive, entry: ZipEntryInfo, onError: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var saved by remember { mutableStateOf(false) }
    IconButton(onClick = {
        scope.launch {
            runCatching {
                val bytes = withContext(Dispatchers.Default) { archive.read(entry) }
                saveBytes(bytes, entry.name.substringAfterLast('/'))
            }
                .onSuccess { saved = it }
                .onFailure { onError(getString(Res.string.zip_not_readable)) }
        }
    }) {
        Icon(if (saved) Icons.Filled.Check else Icons.Filled.Save, contentDescription = Res.string.save_file.str())
    }
}

@Composable
private fun Create() {
    val files = remember { mutableStateListOf<PlatformFile>() }
    var compress by rememberSaveable { mutableStateOf(true) }

    PickButton(Res.string.choose_files.str(), emptySet(), multiple = true, icon = Icons.Filled.Add) { files += it }
    if (files.isEmpty()) return
    OrderedFiles(
        names = files.map { it.name },
        details = files.map { formatBytes(it.size(), binary = false) },
        onMove = { from, to -> files.add(to, files.removeAt(from)) },
        onRemove = { files.removeAt(it) },
        icon = Icons.AutoMirrored.Filled.InsertDriveFile,
    )
    SwitchRow(Res.string.zip_deflate.str(), compress, { compress = it }, description = Res.string.zip_deflate_hint.str())
    SaveButton(Res.string.make_zip.str()) {
        val taken = mutableSetOf<String>()
        val zip = ZipWriter()
        for (file in files.toList()) {
            val bytes = file.readBytes()
            withContext(Dispatchers.Default) { zip.add(uniqueName(file.name, taken), bytes, compress) }
        }
        saveBytes(withContext(Dispatchers.Default) { zip.toByteArray() }, "archive.zip")
    }
}
