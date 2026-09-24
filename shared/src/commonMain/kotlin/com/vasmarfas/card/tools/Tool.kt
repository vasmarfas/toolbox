package com.vasmarfas.card.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EmojiObjects
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.vasmarfas.card.core.PlatformKind
import com.vasmarfas.card.core.currentPlatform
import com.vasmarfas.card.resources.*
import org.jetbrains.compose.resources.StringResource

enum class ToolCategory(val id: String, val title: StringResource, val icon: ImageVector) {
    MEASURE("measure", Res.string.measure_and_sensors, Icons.Filled.Straighten),
    CALCULATORS("calculators", Res.string.calculators, Icons.Filled.Calculate),
    CONVERTERS("converters", Res.string.converters, Icons.Filled.SwapHoriz),
    MONEY("money", Res.string.money, Icons.Filled.Payments),
    MEDIA("media", Res.string.photo_video_audio, Icons.Filled.PermMedia),
    DOCUMENTS("documents", Res.string.documents_and_pdf, Icons.Filled.Description),
    TIME("time", Res.string.time, Icons.Filled.Schedule),
    EVERYDAY("everyday", Res.string.everyday, Icons.Filled.EmojiObjects),
    TEXT("text", Res.string.text, Icons.Filled.TextFields),
    FITNESS("fitness", Res.string.sport_and_health, Icons.Filled.FitnessCenter),
    SOUND("sound", Res.string.sound, Icons.AutoMirrored.Filled.VolumeUp),
    DESIGN("design", Res.string.color_and_design, Icons.Filled.Palette),
    SECURITY("security", Res.string.security, Icons.Filled.Security),
    DEVICE("device", Res.string.device, Icons.Filled.PhoneAndroid),
    DEVELOPER("developer", Res.string.developer, Icons.Filled.Code),
    NETWORK("network", Res.string.network, Icons.Filled.Lan),
    ELECTRONICS("electronics", Res.string.electronics, Icons.Filled.ElectricBolt),
    PRINTING("printing", Res.string.s_3d_printing, Icons.Filled.Print),
}

class Tool(
    val id: String,
    val category: ToolCategory,
    val title: StringResource,
    val description: StringResource,
    val icon: ImageVector,
    val keywords: List<String> = emptyList(),
    val platforms: Set<PlatformKind> = PlatformKind.all,
    val expandable: Boolean = false,
    val content: @Composable () -> Unit,
) {
    val availableHere: Boolean get() = currentPlatform in platforms

    fun matches(query: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return true
        return id.contains(q) ||
            title.matches(q) ||
            description.matches(q) ||
            keywords.any { it.lowercase().contains(q) }
    }
}
