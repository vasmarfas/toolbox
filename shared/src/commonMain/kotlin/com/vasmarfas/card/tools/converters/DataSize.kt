package com.vasmarfas.card.tools.converters

import com.vasmarfas.card.core.Lang
import com.vasmarfas.card.core.appLang
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.resources.*
import kotlin.math.floor
import org.jetbrains.compose.resources.StringResource

enum class DataGroup { BITS, DECIMAL, BINARY }

enum class DataUnit(val symbol: StringResource, val bits: Double, val group: DataGroup) {
    BIT(Res.string.unit_bit, 1.0, DataGroup.BITS),
    KBIT(Res.string.unit_kbit, 1e3, DataGroup.BITS),
    MBIT(Res.string.unit_mbit, 1e6, DataGroup.BITS),
    GBIT(Res.string.unit_gbit, 1e9, DataGroup.BITS),
    TBIT(Res.string.unit_tbit, 1e12, DataGroup.BITS),
    BYTE(Res.string.unit_byte, 8.0, DataGroup.DECIMAL),
    KB(Res.string.unit_kb, 8e3, DataGroup.DECIMAL),
    MB(Res.string.unit_mb, 8e6, DataGroup.DECIMAL),
    GB(Res.string.unit_gb, 8e9, DataGroup.DECIMAL),
    TB(Res.string.unit_tb, 8e12, DataGroup.DECIMAL),
    PB(Res.string.unit_pb, 8e15, DataGroup.DECIMAL),
    KIB(Res.string.unit_kib, 8.0 * 1024, DataGroup.BINARY),
    MIB(Res.string.unit_mib, 8.0 * 1048576, DataGroup.BINARY),
    GIB(Res.string.unit_gib, 8.0 * 1073741824, DataGroup.BINARY),
    TIB(Res.string.unit_tib, 8.0 * 1099511627776, DataGroup.BINARY),
    PIB(Res.string.unit_pib, 8.0 * 1125899906842624, DataGroup.BINARY),
}

enum class SpeedUnit(val symbol: StringResource, val bitsPerSecond: Double) {
    KBIT_S(Res.string.unit_kbit_s, 1e3),
    MBIT_S(Res.string.unit_mbit_s, 1e6),
    GBIT_S(Res.string.unit_gbit_s, 1e9),
    KB_S(Res.string.unit_kb_s, 8e3),
    MB_S(Res.string.unit_mb_per_second, 8e6),
    GB_S(Res.string.unit_gb_per_second, 8e9),
    MIB_S(Res.string.unit_mib_s, 8.0 * 1048576),
}

object DataSize {
    fun convert(value: Double, from: DataUnit, to: DataUnit): Double = value * from.bits / to.bits

    fun transferSeconds(value: Double, unit: DataUnit, speed: Double, speedUnit: SpeedUnit): Double =
        value * unit.bits / (speed * speedUnit.bitsPerSecond)

    fun formatDuration(seconds: Double): String {
        if (seconds.isNaN() || seconds.isInfinite()) return "∞"
        val ru = appLang == Lang.RU
        if (seconds < 1) return "${(seconds * 1000).fmt(1)} ${if (ru) "мс" else "ms"}"
        var rest = seconds
        val days = floor(rest / 86400)
        rest -= days * 86400
        val hours = floor(rest / 3600)
        rest -= hours * 3600
        val minutes = floor(rest / 60)
        rest -= minutes * 60
        return buildString {
            if (days > 0) append(days.fmt(0)).append(if (ru) " дн " else " d ")
            if (days > 0 || hours > 0) append(hours.fmt(0)).append(if (ru) " ч " else " h ")
            if (days > 0 || hours > 0 || minutes > 0) append(minutes.fmt(0)).append(if (ru) " мин " else " min ")
            append(rest.fmt(1)).append(if (ru) " с" else " s")
        }
    }
}
