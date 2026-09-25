package com.vasmarfas.card.tools.network

import kotlin.test.Test
import kotlin.test.assertEquals

class DataSizeTest {
    @Test
    fun conversion() {
        assertEquals(1073.741824, DataSize.convert(1.0, DataUnit.GIB, DataUnit.MB), 1e-9)
        assertEquals(8000.0, DataSize.convert(1.0, DataUnit.KB, DataUnit.BIT), 1e-9)
        assertEquals(1024.0, DataSize.convert(1.0, DataUnit.MIB, DataUnit.KIB), 1e-9)
    }

    @Test
    fun transferTime() {
        assertEquals(8.0, DataSize.transferSeconds(100.0, DataUnit.MB, 100.0, SpeedUnit.MBIT_S), 1e-9)
        assertEquals("8 s", DataSize.formatDuration(8.0))
        assertEquals("1 h 2 min 5 s", DataSize.formatDuration(3725.0))
        assertEquals("500 ms", DataSize.formatDuration(0.5))
        assertEquals("2 d 0 h 0 min 0 s", DataSize.formatDuration(172800.0))
    }
}
