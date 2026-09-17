package com.vasmarfas.card.tools.converters

import com.vasmarfas.card.core.fmt
import kotlin.math.floor

enum class DataGroup { BITS, DECIMAL, BINARY }

enum class DataUnit(val symbol: String, val bits: Double, val group: DataGroup) {
    BIT("bit", 1.0, DataGroup.BITS),
    KBIT("kbit", 1e3, DataGroup.BITS),
    MBIT("Mbit", 1e6, DataGroup.BITS),
    GBIT("Gbit", 1e9, DataGroup.BITS),
    TBIT("Tbit", 1e12, DataGroup.BITS),
    BYTE("B", 8.0, DataGroup.DECIMAL),
    KB("kB", 8e3, DataGroup.DECIMAL),
    MB("MB", 8e6, DataGroup.DECIMAL),
    GB("GB", 8e9, DataGroup.DECIMAL),
    TB("TB", 8e12, DataGroup.DECIMAL),
    PB("PB", 8e15, DataGroup.DECIMAL),
    KIB("KiB", 8.0 * 1024, DataGroup.BINARY),
    MIB("MiB", 8.0 * 1048576, DataGroup.BINARY),
    GIB("GiB", 8.0 * 1073741824, DataGroup.BINARY),
    TIB("TiB", 8.0 * 1099511627776, DataGroup.BINARY),
    PIB("PiB", 8.0 * 1125899906842624, DataGroup.BINARY),
}

enum class SpeedUnit(val symbol: String, val bitsPerSecond: Double) {
    KBIT_S("kbit/s", 1e3),
    MBIT_S("Mbit/s", 1e6),
    GBIT_S("Gbit/s", 1e9),
    KB_S("kB/s", 8e3),
    MB_S("MB/s", 8e6),
    GB_S("GB/s", 8e9),
    MIB_S("MiB/s", 8.0 * 1048576),
}

object DataSize {
    fun convert(value: Double, from: DataUnit, to: DataUnit): Double = value * from.bits / to.bits

    fun transferSeconds(value: Double, unit: DataUnit, speed: Double, speedUnit: SpeedUnit): Double =
        value * unit.bits / (speed * speedUnit.bitsPerSecond)

    fun formatDuration(seconds: Double): String {
        if (seconds.isNaN() || seconds.isInfinite()) return "∞"
        if (seconds < 1) return "${(seconds * 1000).fmt(1)} ms"
        var rest = seconds
        val days = floor(rest / 86400)
        rest -= days * 86400
        val hours = floor(rest / 3600)
        rest -= hours * 3600
        val minutes = floor(rest / 60)
        rest -= minutes * 60
        return buildString {
            if (days > 0) append(days.fmt(0)).append(" d ")
            if (days > 0 || hours > 0) append(hours.fmt(0)).append(" h ")
            if (days > 0 || hours > 0 || minutes > 0) append(minutes.fmt(0)).append(" min ")
            append(rest.fmt(1)).append(" s")
        }
    }
}
