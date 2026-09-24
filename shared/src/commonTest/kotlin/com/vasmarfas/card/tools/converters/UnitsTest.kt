package com.vasmarfas.card.tools.converters

import kotlin.test.Test
import kotlin.test.assertEquals

class UnitsTest {
    private fun unit(category: UnitCategory, id: String): ConvUnit = category.units.first { it.id == id }

    @Test
    fun linearUnits() {
        assertEquals(1.609344, Units.convert(1.0, unit(UnitCategory.LENGTH, "mi"), unit(UnitCategory.LENGTH, "km")), 1e-9)
        assertEquals(1048.576, Units.convert(1.0, unit(UnitCategory.DATA, "mib"), unit(UnitCategory.DATA, "kb")), 1e-9)
        assertEquals(3.6, Units.convert(1.0, unit(UnitCategory.SPEED, "ms"), unit(UnitCategory.SPEED, "kmh")), 1e-9)
    }

    @Test
    fun nonLinearUnits() {
        assertEquals(212.0, Units.convert(100.0, unit(UnitCategory.TEMPERATURE, "c"), unit(UnitCategory.TEMPERATURE, "f")), 1e-9)
        assertEquals(273.15, Units.convert(32.0, unit(UnitCategory.TEMPERATURE, "f"), unit(UnitCategory.TEMPERATURE, "k")), 1e-9)
        assertEquals(23.5214583, Units.convert(10.0, unit(UnitCategory.FUEL, "l100km"), unit(UnitCategory.FUEL, "mpgus")), 1e-6)
        assertEquals(10.0, Units.convert(10.0, unit(UnitCategory.FUEL, "kml"), unit(UnitCategory.FUEL, "l100km")), 1e-9)
    }

    @Test
    fun significantFormatting() {
        assertEquals("1.5", 1.5.fmtSig())
        assertEquals("123456789", 123456789.0.fmtSig())
        assertEquals("0.000001234", 0.000001234.fmtSig())
        assertEquals("1e20", 1e20.fmtSig())
        assertEquals("0.3333", (1.0 / 3).fmtSig(4))
        assertEquals("0", 0.0.fmtSig())
    }

    @Test
    fun readableFormatting() {
        assertEquals("1 000 000 000", 1e9.fmtReadable().text)
        assertEquals("-12 345.678", (-12345.678).fmtReadable().text)
        assertEquals("1234.5", 1234.5.fmtReadable().text)
        assertEquals("6.6845871 × 10-12", 6.6845871e-12.fmtReadable().text)
        assertEquals("1 × 1020", 1e20.fmtReadable().text)
    }
}
