package com.vasmarfas.card.tools.printing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FilamentGuideTest {
    @Test
    fun everyMaterialHasTraitsWithinTheScale() {
        assertEquals(FilamentMaterial.entries.toSet(), Filaments.traits.keys)
        Filaments.traits.values.forEach { t ->
            listOf(t.strength, t.impact, t.flexibility, t.ease, t.speed, t.uv, t.chemical, t.price).forEach { assertTrue(it in 1..5) }
        }
    }

    @Test
    fun everyMaterialHasItsDataSheetFigures() {
        assertEquals(FilamentMaterial.entries.toSet(), filamentSpecs.keys)
        filamentSpecs.values.forEach { spec ->
            val low = spec.hdtLowC
            val high = spec.hdtHighC
            if (low != null && high != null) assertTrue(low >= high)
        }
    }

    @Test
    fun aBeginnerOnAnOpenPrinterGetsPla() {
        assertEquals(FilamentMaterial.PLA, Filaments.pick(FilamentNeeds()).first().material)
    }

    @Test
    fun hardLimitsDropWhatCannotWork() {
        val hot = Filaments.pick(FilamentNeeds(heat = PartHeat.HOT))
        assertTrue(hot.isNotEmpty() && hot.all { it.traits.softeningC >= 85 })
        assertTrue(Filaments.pick(FilamentNeeds(stiffness = PartStiffness.RUBBER)).all { it.traits.flexibility >= 4 })
        val open = Filaments.pick(FilamentNeeds(load = PartLoad.HEAVY))
        assertTrue(open.none { FilamentFlag.ABRASIVE in it.traits.flags || it.traits.chamber >= Chamber.HEATED || it.material.nozzleC.first > 260 })
        assertTrue(Filaments.pick(FilamentNeeds(printer = PrinterKind.INDUSTRIAL, hardenedNozzle = true)).none { FilamentFlag.WATER_SOLUBLE in it.traits.flags })
    }

    @Test
    fun onlyAHotChamberTakesTheHighTemperaturePlastics() {
        val heated = Filaments.pick(FilamentNeeds(heat = PartHeat.VERY_HOT, printer = PrinterKind.HEATED, hardenedNozzle = true))
        assertTrue(heated.none { it.traits.chamber == Chamber.HOT })
        val industrial = Filaments.pick(FilamentNeeds(heat = PartHeat.VERY_HOT, printer = PrinterKind.INDUSTRIAL, hardenedNozzle = true))
        assertTrue(industrial.any { it.traits.chamber == Chamber.HOT })
    }

    @Test
    fun theSunLeavesOnlyWeatherproofPlastics() {
        val open = Filaments.pick(FilamentNeeds(outdoor = true))
        assertTrue(open.isNotEmpty() && open.all { FilamentFlag.OUTDOOR in it.traits.flags && it.traits.softeningC >= SUN_C })
        assertTrue(open.none { it.material == FilamentMaterial.PLA })
        assertEquals(FilamentMaterial.PETG, open.first().material)
        val car = Filaments.pick(FilamentNeeds(heat = PartHeat.HOT, outdoor = true, printer = PrinterKind.ENCLOSED))
        assertEquals(FilamentMaterial.ASA, car.first().material)
    }

    @Test
    fun chemicalsDropPlasticsThatSolventsAttack() {
        val picked = Filaments.pick(FilamentNeeds(chemicals = true, load = PartLoad.HEAVY))
        assertTrue(picked.isNotEmpty() && picked.all { it.traits.chemical >= 3 })
        assertTrue(picked.none { it.material == FilamentMaterial.PVB || it.material == FilamentMaterial.PLA })
    }

    @Test
    fun nylonIsOfferedForRigidLoadedParts() {
        val gears = Filaments.pick(FilamentNeeds(load = PartLoad.HEAVY, printer = PrinterKind.ENCLOSED))
        assertTrue(gears.any { it.material == FilamentMaterial.NYLON })
    }

    @Test
    fun printerLimitsAgreeWithTheChamberEachPlasticNeeds() {
        val nozzle = PrinterKind.entries.associate { it.chamber to it.maxNozzleC }
        Filaments.traits.forEach { (material, t) ->
            assertTrue(material.nozzleC.first <= nozzle.getValue(t.chamber), "${material.name} needs ${t.chamber} but its nozzle starts at ${material.nozzleC.first}")
        }
    }

    @Test
    fun knocksPushBrittlePlasticsDown() {
        val heavy = Filaments.pick(FilamentNeeds(load = PartLoad.HEAVY))
        assertTrue(heavy.first().material != FilamentMaterial.PLA)
        assertTrue(heavy.first().traits.impact >= 3)
    }
}
