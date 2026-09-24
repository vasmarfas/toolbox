package com.vasmarfas.card.tools.converters

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.resources.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import org.jetbrains.compose.resources.StringResource

class ConvUnit(
    val id: String,
    val name: StringResource,
    val symbol: StringResource,
    val toBase: (Double) -> Double,
    val fromBase: (Double) -> Double,
) {
    constructor(id: String, name: StringResource, symbol: StringResource, factor: Double) :
        this(id, name, symbol, { it * factor }, { it / factor })
}

enum class UnitCategory(val title: StringResource, val units: List<ConvUnit>) {
    LENGTH(
        Res.string.length,
        listOf(
            ConvUnit("m", Res.string.metre, Res.string.unit_m, 1.0),
            ConvUnit("ft", Res.string.foot, Res.string.unit_ft, 0.3048),
            ConvUnit("km", Res.string.kilometre, Res.string.unit_km, 1000.0),
            ConvUnit("cm", Res.string.centimetre, Res.string.unit_cm, 0.01),
            ConvUnit("mm", Res.string.millimetre, Res.string.unit_mm, 0.001),
            ConvUnit("um", Res.string.micrometre, Res.string.unit_um, 1e-6),
            ConvUnit("nm", Res.string.nanometre, Res.string.unit_nm, 1e-9),
            ConvUnit("in", Res.string.inch, Res.string.unit_in, 0.0254),
            ConvUnit("yd", Res.string.yard, Res.string.unit_yd, 0.9144),
            ConvUnit("mi", Res.string.mile, Res.string.unit_mi, 1609.344),
            ConvUnit("nmi", Res.string.nautical_mile, Res.string.unit_nmi, 1852.0),
            ConvUnit("au", Res.string.astronomical_unit, Res.string.unit_au, 1.495978707e11),
            ConvUnit("ly", Res.string.light_year, Res.string.unit_ly, 9.4607304725808e15),
        ),
    ),
    MASS(
        Res.string.mass,
        listOf(
            ConvUnit("kg", Res.string.kilogram, Res.string.unit_kg, 1.0),
            ConvUnit("lb", Res.string.pound, Res.string.unit_lb, 0.45359237),
            ConvUnit("g", Res.string.gram, Res.string.unit_g, 0.001),
            ConvUnit("mg", Res.string.milligram, Res.string.unit_mg, 1e-6),
            ConvUnit("t", Res.string.tonne, Res.string.unit_t, 1000.0),
            ConvUnit("oz", Res.string.ounce, Res.string.unit_oz, 0.028349523125),
            ConvUnit("st", Res.string.stone, Res.string.unit_st, 6.35029318),
            ConvUnit("ct", Res.string.carat, Res.string.unit_ct, 0.0002),
            ConvUnit("uston", Res.string.short_ton_us, Res.string.unit_short_ton, 907.18474),
            ConvUnit("ukton", Res.string.long_ton_uk, Res.string.unit_long_ton, 1016.0469088),
        ),
    ),
    TEMPERATURE(
        Res.string.temperature,
        listOf(
            ConvUnit("c", Res.string.celsius, Res.string.unit_celsius, { it }, { it }),
            ConvUnit("f", Res.string.fahrenheit, Res.string.unit_fahrenheit, { (it - 32) * 5 / 9 }, { it * 9 / 5 + 32 }),
            ConvUnit("k", Res.string.kelvin, Res.string.unit_kelvin, { it - 273.15 }, { it + 273.15 }),
            ConvUnit("r", Res.string.rankine, Res.string.unit_rankine, { (it - 491.67) * 5 / 9 }, { (it + 273.15) * 9 / 5 }),
            ConvUnit("re", Res.string.r_aumur, Res.string.unit_reaumur, { it * 1.25 }, { it * 0.8 }),
        ),
    ),
    AREA(
        Res.string.area,
        listOf(
            ConvUnit("m2", Res.string.square_metre, Res.string.unit_m2, 1.0),
            ConvUnit("ft2", Res.string.square_foot, Res.string.unit_ft2, 0.09290304),
            ConvUnit("km2", Res.string.square_kilometre, Res.string.unit_km2, 1e6),
            ConvUnit("ha", Res.string.hectare, Res.string.unit_ha, 1e4),
            ConvUnit("a", Res.string.are_sotka, Res.string.unit_are, 100.0),
            ConvUnit("cm2", Res.string.square_centimetre, Res.string.unit_cm2, 1e-4),
            ConvUnit("mm2", Res.string.square_millimetre, Res.string.unit_mm2, 1e-6),
            ConvUnit("in2", Res.string.square_inch, Res.string.unit_in2, 0.00064516),
            ConvUnit("yd2", Res.string.square_yard, Res.string.unit_yd2, 0.83612736),
            ConvUnit("acre", Res.string.acre, Res.string.unit_acre, 4046.8564224),
            ConvUnit("mi2", Res.string.square_mile, Res.string.unit_mi2, 2589988.110336),
        ),
    ),
    VOLUME(
        Res.string.volume,
        listOf(
            ConvUnit("l", Res.string.litre, Res.string.unit_liter, 1.0),
            ConvUnit("galus", Res.string.gallon_us, Res.string.unit_gal_us, 3.785411784),
            ConvUnit("ml", Res.string.millilitre, Res.string.unit_ml, 0.001),
            ConvUnit("m3", Res.string.cubic_metre, Res.string.unit_m3, 1000.0),
            ConvUnit("cm3", Res.string.cubic_centimetre, Res.string.unit_cm3, 0.001),
            ConvUnit("galuk", Res.string.gallon_uk, Res.string.unit_gal_uk, 4.54609),
            ConvUnit("qt", Res.string.quart_us, Res.string.unit_qt, 0.946352946),
            ConvUnit("pt", Res.string.pint_us, Res.string.unit_pt, 0.473176473),
            ConvUnit("cup", Res.string.cup_us, Res.string.unit_cup, 0.2365882365),
            ConvUnit("flozus", Res.string.fluid_ounce_us, Res.string.unit_fl_oz_us, 0.0295735295625),
            ConvUnit("flozuk", Res.string.fluid_ounce_uk, Res.string.unit_fl_oz_uk, 0.0284130625),
            ConvUnit("tbsp", Res.string.tablespoon, Res.string.unit_tbsp, 0.01478676478125),
            ConvUnit("tsp", Res.string.teaspoon, Res.string.unit_tsp, 0.00492892159375),
            ConvUnit("ft3", Res.string.cubic_foot, Res.string.unit_ft3, 28.316846592),
            ConvUnit("in3", Res.string.cubic_inch, Res.string.unit_in3, 0.016387064),
            ConvUnit("bbl", Res.string.oil_barrel, Res.string.unit_bbl, 158.987294928),
        ),
    ),
    SPEED(
        Res.string.speed,
        listOf(
            ConvUnit("kmh", Res.string.kilometre_per_hour, Res.string.unit_kmh, 1 / 3.6),
            ConvUnit("mph", Res.string.mile_per_hour, Res.string.unit_mph, 0.44704),
            ConvUnit("ms", Res.string.metre_per_second, Res.string.unit_mps, 1.0),
            ConvUnit("kn", Res.string.knot, Res.string.unit_knot, 1852.0 / 3600),
            ConvUnit("fts", Res.string.foot_per_second, Res.string.unit_fps, 0.3048),
            ConvUnit("mach", Res.string.mach_sea_level, Res.string.unit_mach, 340.29),
            ConvUnit("c", Res.string.speed_of_light, Res.string.unit_light_speed, 299792458.0),
        ),
    ),
    PRESSURE(
        Res.string.pressure,
        listOf(
            ConvUnit("bar", Res.string.bar, Res.string.unit_bar, 1e5),
            ConvUnit("psi", Res.string.pound_per_square_inch, Res.string.unit_psi, 6894.757293168),
            ConvUnit("pa", Res.string.pascal_unit, Res.string.unit_pa, 1.0),
            ConvUnit("kpa", Res.string.kilopascal, Res.string.unit_kpa, 1e3),
            ConvUnit("mpa", Res.string.megapascal, Res.string.unit_mpa, 1e6),
            ConvUnit("mbar", Res.string.millibar, Res.string.unit_mbar, 100.0),
            ConvUnit("atm", Res.string.atmosphere, Res.string.unit_atm, 101325.0),
            ConvUnit("mmhg", Res.string.millimetre_of_mercury, Res.string.unit_mmhg, 133.322387415),
            ConvUnit("torr", Res.string.torr, Res.string.unit_torr, 101325.0 / 760),
            ConvUnit("inhg", Res.string.inch_of_mercury, Res.string.unit_inhg, 3386.389),
            ConvUnit("kgfcm2", Res.string.kilogram_force_per_cm, Res.string.unit_kgf_cm2, 98066.5),
        ),
    ),
    ENERGY(
        Res.string.energy,
        listOf(
            ConvUnit("kcal", Res.string.kilocalorie, Res.string.unit_kcal, 4184.0),
            ConvUnit("kj", Res.string.kilojoule, Res.string.unit_kj, 1000.0),
            ConvUnit("j", Res.string.joule, Res.string.unit_j, 1.0),
            ConvUnit("cal", Res.string.calorie, Res.string.unit_cal, 4.184),
            ConvUnit("wh", Res.string.watt_hour, Res.string.unit_wh, 3600.0),
            ConvUnit("kwh", Res.string.kilowatt_hour, Res.string.unit_kwh, 3.6e6),
            ConvUnit("mj", Res.string.megajoule, Res.string.unit_mj, 1e6),
            ConvUnit("ev", Res.string.electronvolt, Res.string.unit_ev, 1.602176634e-19),
            ConvUnit("btu", Res.string.british_thermal_unit, Res.string.unit_btu, 1055.05585262),
            ConvUnit("ftlbf", Res.string.foot_pound, Res.string.unit_ft_lbf, 1.3558179483314004),
            ConvUnit("erg", Res.string.erg, Res.string.unit_erg, 1e-7),
            ConvUnit("tnt", Res.string.ton_of_tnt, Res.string.unit_tnt, 4.184e9),
        ),
    ),
    POWER(
        Res.string.power,
        listOf(
            ConvUnit("kw", Res.string.kilowatt, Res.string.unit_kw, 1000.0),
            ConvUnit("hp", Res.string.horsepower_mechanical, Res.string.unit_hp, 745.6998715822702),
            ConvUnit("w", Res.string.watt, Res.string.unit_w, 1.0),
            ConvUnit("mw", Res.string.milliwatt, Res.string.unit_mw, 0.001),
            ConvUnit("megaw", Res.string.megawatt, Res.string.unit_megawatt, 1e6),
            ConvUnit("hpm", Res.string.horsepower_metric, Res.string.unit_ps, 735.49875),
            ConvUnit("btuh", Res.string.btu_per_hour, Res.string.unit_btu_h, 0.29307107),
            ConvUnit("cals", Res.string.calorie_per_second, Res.string.unit_cal_s, 4.184),
            ConvUnit("ftlbfs", Res.string.foot_pound_per_second, Res.string.unit_ft_lbf_s, 1.3558179483314004),
        ),
    ),
    DATA(
        Res.string.data_size,
        listOf(
            ConvUnit("mb", Res.string.megabyte, Res.string.unit_mb, 1e6),
            ConvUnit("mib", Res.string.mebibyte, Res.string.unit_mib, 1048576.0),
            ConvUnit("bit", Res.string.bit, Res.string.unit_bit, 0.125),
            ConvUnit("byte", Res.string.byte, Res.string.unit_byte, 1.0),
            ConvUnit("kb", Res.string.kilobyte, Res.string.unit_kb, 1e3),
            ConvUnit("kib", Res.string.kibibyte, Res.string.unit_kib, 1024.0),
            ConvUnit("gb", Res.string.gigabyte, Res.string.unit_gb, 1e9),
            ConvUnit("gib", Res.string.gibibyte, Res.string.unit_gib, 1073741824.0),
            ConvUnit("tb", Res.string.terabyte, Res.string.unit_tb, 1e12),
            ConvUnit("tib", Res.string.tebibyte, Res.string.unit_tib, 1099511627776.0),
            ConvUnit("pb", Res.string.petabyte, Res.string.unit_pb, 1e15),
            ConvUnit("pib", Res.string.pebibyte, Res.string.unit_pib, 1125899906842624.0),
            ConvUnit("kbit", Res.string.kilobit, Res.string.unit_kbit, 125.0),
            ConvUnit("mbit", Res.string.megabit, Res.string.unit_mbit, 125000.0),
            ConvUnit("gbit", Res.string.gigabit, Res.string.unit_gbit, 1.25e8),
            ConvUnit("tbit", Res.string.terabit, Res.string.unit_tbit, 1.25e11),
        ),
    ),
    TIME(
        Res.string.time,
        listOf(
            ConvUnit("h", Res.string.hour, Res.string.unit_h, 3600.0),
            ConvUnit("min", Res.string.minute, Res.string.unit_min, 60.0),
            ConvUnit("s", Res.string.second, Res.string.unit_s, 1.0),
            ConvUnit("ms", Res.string.millisecond, Res.string.unit_ms, 1e-3),
            ConvUnit("us", Res.string.microsecond, Res.string.unit_us, 1e-6),
            ConvUnit("ns", Res.string.nanosecond, Res.string.unit_ns, 1e-9),
            ConvUnit("d", Res.string.day, Res.string.unit_day, 86400.0),
            ConvUnit("wk", Res.string.week, Res.string.unit_week, 604800.0),
            ConvUnit("mo", Res.string.month_average, Res.string.unit_month, 2629746.0),
            ConvUnit("yr", Res.string.year_gregorian, Res.string.unit_year, 31556952.0),
            ConvUnit("decade", Res.string.decade, Res.string.unit_decade, 315569520.0),
            ConvUnit("century", Res.string.century, Res.string.unit_century, 3155695200.0),
        ),
    ),
    ANGLE(
        Res.string.angle,
        listOf(
            ConvUnit("deg", Res.string.degree, Res.string.unit_deg, PI / 180),
            ConvUnit("rad", Res.string.radian, Res.string.unit_rad, 1.0),
            ConvUnit("grad", Res.string.gradian, Res.string.unit_gon, PI / 200),
            ConvUnit("arcmin", Res.string.arcminute, Res.string.unit_arcmin, PI / 10800),
            ConvUnit("arcsec", Res.string.arcsecond, Res.string.unit_arcsec, PI / 648000),
            ConvUnit("turn", Res.string.turn, Res.string.unit_turn, 2 * PI),
            ConvUnit("mil", Res.string.mil_nato_6400, Res.string.unit_mil, 2 * PI / 6400),
        ),
    ),
    FUEL(
        Res.string.fuel_economy,
        listOf(
            ConvUnit("l100km", Res.string.litres_per_100_km, Res.string.unit_l_100km, { it }, { it }),
            ConvUnit("mpgus", Res.string.miles_per_gallon_us, Res.string.unit_mpg_us, { 235.214583 / it }, { 235.214583 / it }),
            ConvUnit("mpguk", Res.string.miles_per_gallon_uk, Res.string.unit_mpg_uk, { 282.480936 / it }, { 282.480936 / it }),
            ConvUnit("kml", Res.string.kilometres_per_litre, Res.string.unit_km_l, { 100 / it }, { 100 / it }),
        ),
    ),
    FREQUENCY(
        Res.string.frequency,
        listOf(
            ConvUnit("mhz", Res.string.megahertz, Res.string.unit_mhz, 1e6),
            ConvUnit("ghz", Res.string.gigahertz, Res.string.unit_ghz, 1e9),
            ConvUnit("hz", Res.string.hertz, Res.string.unit_hz, 1.0),
            ConvUnit("khz", Res.string.kilohertz, Res.string.unit_khz, 1e3),
            ConvUnit("thz", Res.string.terahertz, Res.string.unit_thz, 1e12),
            ConvUnit("rpm", Res.string.revolutions_per_minute, Res.string.unit_rpm, 1 / 60.0),
            ConvUnit("rads", Res.string.radian_per_second, Res.string.unit_rad_s, 1 / (2 * PI)),
        ),
    ),
}

object Units {
    fun convert(value: Double, from: ConvUnit, to: ConvUnit): Double = to.fromBase(from.toBase(value))
}

fun Double.fmtSig(significant: Int = 8): String {
    if (isNaN()) return "NaN"
    if (isInfinite()) return if (this > 0) "∞" else "-∞"
    if (this == 0.0) return "0"
    val exponent = floor(log10(abs(this))).toInt()
    if (exponent >= 15 || exponent < -6) {
        var mantissa = this / 10.0.pow(exponent)
        var e = exponent
        if (abs(mantissa) >= 10) {
            mantissa /= 10
            e++
        }
        val m = mantissa.fmt(significant - 1)
        return if (m == "10" || m == "-10") "${m.dropLast(1)}e${e + 1}" else "${m}e$e"
    }
    return fmt((significant - 1 - exponent).coerceIn(0, 15))
}

// the exponent is a baseline shift, the web font has no superscript digits. Copy uses the plain fmtSig
fun Double.fmtReadable(significant: Int = 8): AnnotatedString = buildAnnotatedString {
    val plain = fmtSig(significant)
    val e = plain.indexOf('e')
    if (e >= 0) {
        append("${plain.substring(0, e)} × 10")
        withStyle(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 0.75.em)) { append(plain.substring(e + 1)) }
        return@buildAnnotatedString
    }
    val sign = if (plain.startsWith('-')) "-" else ""
    val digits = plain.removePrefix("-")
    val whole = digits.substringBefore('.')
    if (whole.length <= 4) append(plain)
    else append(sign + whole.reversed().chunked(3).joinToString("\u00A0").reversed() + digits.removePrefix(whole))
}
