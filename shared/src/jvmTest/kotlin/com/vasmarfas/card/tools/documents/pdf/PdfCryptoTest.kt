package com.vasmarfas.card.tools.documents.pdf

import com.vasmarfas.card.tools.developer.Sha384
import com.vasmarfas.card.tools.developer.toHex
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy

class PdfCryptoTest {
    private val random = Random(99)

    @Test
    fun rc4KnownVectors() {
        assertEquals("BBF316E8D940AF0AD3", rc4(latin1("Key"), latin1("Plaintext")).toHex(upper = true))
        assertEquals("1021BF0420", rc4(latin1("Wiki"), latin1("pedia")).toHex(upper = true))
        assertEquals("45A01F645FC35B383552544B9BF5", rc4(latin1("Secret"), latin1("Attack at dawn")).toHex(upper = true))
    }

    private fun jdkCipher(transformation: String, key: ByteArray, iv: ByteArray): Cipher =
        Cipher.getInstance(transformation).apply { init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv)) }

    @Test
    fun aesMatchesJdk() {
        val fips = Aes(ByteArray(16) { it.toByte() })
        val block = ByteArray(16)
        fips.encryptBlock(ByteArray(16) { (it * 0x11).toByte() }, 0, block, 0)
        assertEquals("69c4e0d86a7b0430d8cdb78070b4c55a", block.toHex())
        for (keySize in intArrayOf(16, 24, 32)) {
            repeat(10) {
                val key = random.nextBytes(keySize)
                val iv = random.nextBytes(16)
                val data = random.nextBytes(16 * (1 + random.nextInt(20)))
                val jdk = jdkCipher("AES/CBC/NoPadding", key, iv).doFinal(data)
                assertContentEquals(jdk, aesCbc(key, iv, data, encrypt = true))
                assertContentEquals(data, aesCbc(key, iv, jdk, encrypt = false))
                val plain = random.nextBytes(random.nextInt(100))
                val padded = jdkCipher("AES/CBC/PKCS5Padding", key, iv).doFinal(plain)
                assertContentEquals(plain, aesDecrypt(key, iv + padded))
            }
        }
    }

    @Test
    fun sha384MatchesJdk() {
        assertEquals(
            "cb00753f45a35e8bb5a03d699ac65007272c32ab0eded1631a8b605a43ff5bed8086072ba1e7cc2358baeca134c825a7",
            Sha384.digest(latin1("abc")).toHex(),
        )
        for (size in intArrayOf(0, 1, 111, 112, 127, 128, 129, 1000)) {
            val data = random.nextBytes(size)
            assertContentEquals(MessageDigest.getInstance("SHA-384").digest(data), Sha384.digest(data))
        }
    }

    private class Variant(val keyLength: Int, val aes: Boolean, val revision: Int)

    // PDFBox writes revision 2 only when none of the revision 3 permission bits is granted
    private fun encrypted(variant: Variant, mode: SaveMode, user: String = ""): ByteArray = pdf(mode) { doc ->
        doc.textPage(helvetica(), listOf("Secret page one", "with (parens) and \\ backslash"))
        doc.textPage(doc.arial(), listOf("Секретная страница два"))
        doc.documentInformation.title = "Зашифровано"
        doc.documentInformation.author = "Owner"
        val permissions = AccessPermission()
        if (variant.revision == 2) {
            permissions.setCanFillInForm(false)
            permissions.setCanExtractForAccessibility(false)
            permissions.setCanAssembleDocument(false)
            permissions.setCanPrintFaithful(false)
        }
        val policy = StandardProtectionPolicy("owner-secret", user, permissions)
        policy.encryptionKeyLength = variant.keyLength
        policy.isPreferAES = variant.aes
        doc.protect(policy)
    }

    private val variants = listOf(Variant(40, false, 2), Variant(40, false, 3), Variant(128, false, 3), Variant(128, true, 4), Variant(256, true, 6))

    @Test
    fun decryptsOwnerPasswordOnlyFiles() {
        for (variant in variants) {
            for (mode in SaveMode.entries) {
                val keyLength = variant.keyLength
                val aes = variant.aes
                val bytes = encrypted(variant, mode)
                val revision = variant.revision
                Loader.loadPDF(bytes).use { assertEquals(revision, it.encryption.revision) }
                val doc = PdfDocument.parse(bytes)
                assertTrue(doc.encrypted)
                assertEquals("Зашифровано", doc.info["Title"])
                assertEquals("Owner", doc.info["Author"])
                for (i in 0 until 2) assertEquals(normalize(pdfboxText(bytes, i)), normalize(PdfText.extract(doc, i)), "$keyLength/$aes/$mode page $i")
                val output = PdfAssembler.assemble(listOf(PageRef(doc, 1), PageRef(doc, 0), PageRef(doc, 1, 90)), mapOf("Title" to doc.info.getValue("Title")))
                assertFalse("/Encrypt" in String(output, Charsets.ISO_8859_1))
                loadCleanly(output) { result ->
                    assertFalse(result.isEncrypted)
                    assertEquals("Зашифровано", result.documentInformation.title)
                    assertEquals(pdfboxText(bytes, 1), pdfboxText(result, 0))
                    assertEquals(pdfboxText(bytes, 0), pdfboxText(result, 1))
                    assertEquals(squeeze(pdfboxText(bytes, 1)), squeeze(pdfboxText(result, 2)))
                }
            }
        }
    }

    @Test
    fun userPasswordIsRequired() {
        for (variant in variants) {
            val bytes = encrypted(variant, SaveMode.CLASSIC, user = "user-pass")
            assertFailsWith<PdfEncryptedException> { PdfDocument.parse(bytes) }
            assertFailsWith<PdfEncryptedException> { PdfDocument.parse(bytes, "wrong") }
            for (password in listOf("user-pass", "owner-secret")) {
                val doc = PdfDocument.parse(bytes, password)
                assertEquals("Зашифровано", doc.info["Title"])
                assertEquals("Секретная страница два", PdfText.extract(doc, 1))
            }
        }
    }

    @Test
    fun unsupportedHandlers() {
        val pdf = RawPdf()
        pdf.obj(1, "<< /Type /Catalog /Pages 2 0 R >>")
        pdf.obj(2, "<< /Type /Pages /Kids [] /Count 0 >>")
        pdf.obj(3, "<< /Filter /Adobe.PubSec /V 4 /R 4 /Recipients [<00>] >>")
        val pubSec = pdf.finish("<< /Size 4 /Root 1 0 R /Encrypt 3 0 R /ID [<01><01>] >>")
        assertFailsWith<PdfEncryptedException> { PdfDocument.parse(pubSec) }
        val other = RawPdf()
        other.obj(1, "<< /Type /Catalog /Pages 2 0 R >>")
        other.obj(2, "<< /Type /Pages /Kids [] /Count 0 >>")
        other.obj(3, "<< /Filter /Standard /V 7 /R 9 /O <00> /U <00> /P -4 >>")
        assertFailsWith<PdfEncryptedException> { PdfDocument.parse(other.finish("<< /Size 4 /Root 1 0 R /Encrypt 3 0 R >>")) }
    }
}
