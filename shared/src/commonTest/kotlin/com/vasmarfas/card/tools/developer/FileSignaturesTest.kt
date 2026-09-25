package com.vasmarfas.card.tools.developer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FileSignaturesTest {
    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    @Test
    fun picturesAndDocumentsByTheirFirstBytes() {
        assertEquals("image/png", FileSignatures.detect(bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0))?.mime)
        assertEquals("jpg", FileSignatures.detect(bytes(0xFF, 0xD8, 0xFF, 0xE0))?.extension)
        assertEquals("application/pdf", FileSignatures.detect("%PDF-1.7\n".encodeToByteArray())?.mime)
        assertEquals("image/heic", FileSignatures.detect(bytes(0, 0, 0, 0x18) + "ftypheic".encodeToByteArray())?.mime)
        assertEquals("video/mp4", FileSignatures.detect(bytes(0, 0, 0, 0x18) + "ftypisom".encodeToByteArray())?.mime)
    }

    @Test
    fun zipsAreToldApartByWhatTheyHold() {
        val header = bytes(0x50, 0x4B, 0x03, 0x04) + ByteArray(26)
        assertEquals("docx", FileSignatures.detect(header + "word/document.xml".encodeToByteArray())?.extension)
        assertEquals("epub", FileSignatures.detect(header + "mimetypeapplication/epub+zip".encodeToByteArray())?.extension)
        assertEquals("zip", FileSignatures.detect(header + "readme.txt".encodeToByteArray())?.extension)
    }

    @Test
    fun textWithoutASignatureIsReadAsText() {
        assertEquals("application/json", FileSignatures.detect("""{"a": 1}""".encodeToByteArray())?.mime)
        assertEquals("image/svg+xml", FileSignatures.detect("<?xml version=\"1.0\"?><svg/>".encodeToByteArray())?.mime)
        assertEquals("text/plain", FileSignatures.detect("Привет, мир".encodeToByteArray())?.mime)
        assertNull(FileSignatures.detect(bytes(1, 2, 3, 4, 5)))
    }
}
