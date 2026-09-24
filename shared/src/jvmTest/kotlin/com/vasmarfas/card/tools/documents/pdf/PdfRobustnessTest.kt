package com.vasmarfas.card.tools.documents.pdf

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy

class PdfRobustnessTest {
    private fun sources(): List<ByteArray> {
        val build = { mode: SaveMode, encrypt: Boolean ->
            pdf(mode) { doc ->
                doc.textPage(helvetica(), listOf("Robust page one", "second line"))
                doc.textPage(doc.arial(), listOf("Страница два"), PDRectangle.A5, rotation = 90)
                doc.textPage(times(), listOf("Third"))
                doc.documentInformation.title = "Fuzz"
                if (encrypt) doc.protect(StandardProtectionPolicy("owner", "", AccessPermission()).apply { encryptionKeyLength = 128 })
            }
        }
        return listOf(build(SaveMode.CLASSIC, false), build(SaveMode.COMPRESSED, false), build(SaveMode.CLASSIC, true), build(SaveMode.COMPRESSED, true))
    }

    private fun mutate(bytes: ByteArray, random: Random): ByteArray {
        val at = random.nextInt(bytes.size)
        val span = 1 + random.nextInt(minOf(400, bytes.size - at))
        return when (random.nextInt(6)) {
            0 -> bytes.copyOf().also { copy -> repeat(1 + random.nextInt(30)) { copy[random.nextInt(copy.size)] = random.nextInt(256).toByte() } }
            1 -> bytes.copyOf(at)
            2 -> bytes.copyOfRange(0, at) + bytes.copyOfRange(at + span, bytes.size)
            3 -> bytes.copyOfRange(0, at) + bytes.copyOfRange(at, at + span) + bytes.copyOfRange(at, bytes.size)
            4 -> bytes.copyOf().also { copy -> for (i in at until at + span) copy[i] = random.nextInt(256).toByte() }
            else -> bytes.copyOf().also { copy -> for (i in at until at + span) if (copy[i].toInt() in 48..57) copy[i] = (48 + random.nextInt(10)).toByte() }
        }
    }

    private fun exercise(bytes: ByteArray): Boolean {
        val doc = try {
            PdfDocument.parse(bytes)
        } catch (_: PdfException) {
            return false
        } catch (_: PdfEncryptedException) {
            return false
        }
        doc.info
        val count = doc.pageCount
        for (i in 0 until count) PdfText.extract(doc, i)
        if (count > 0) {
            val output = PdfAssembler.assemble((0 until count).map { PageRef(doc, it, 90) })
            val reread = PdfDocument.parse(output)
            assertTrue(!reread.repaired && reread.pageCount == count)
        }
        return true
    }

    @Test
    fun mutatedFilesFailOnlyWithPdfExceptions() {
        val random = Random(2024)
        val sources = sources()
        var opened = 0
        repeat(1200) { iteration ->
            if (exercise(mutate(sources[iteration % sources.size], random))) opened++
        }
        assertTrue(opened > 600, "only $opened mutated files opened")
    }
}
