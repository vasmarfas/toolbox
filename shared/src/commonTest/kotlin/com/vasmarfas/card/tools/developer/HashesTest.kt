package com.vasmarfas.card.tools.developer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HashesTest {
    private fun hash(algorithm: HashAlgorithm, text: String) = algorithm.digest(text.encodeToByteArray()).toHex()

    @Test
    fun md5() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", hash(HashAlgorithm.MD5, ""))
        assertEquals("900150983cd24fb0d6963f7d28e17f72", hash(HashAlgorithm.MD5, "abc"))
    }

    @Test
    fun sha1() {
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", hash(HashAlgorithm.SHA1, ""))
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", hash(HashAlgorithm.SHA1, "abc"))
    }

    @Test
    fun sha256() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hash(HashAlgorithm.SHA256, ""))
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", hash(HashAlgorithm.SHA256, "abc"))
    }

    @Test
    fun sha512() {
        assertEquals(
            "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e",
            hash(HashAlgorithm.SHA512, ""),
        )
        assertEquals(
            "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f",
            hash(HashAlgorithm.SHA512, "abc"),
        )
    }

    @Test
    fun crc32() {
        assertEquals(0, Crc32.compute(ByteArray(0)))
        assertEquals("352441c2", Crc32.digest("abc".encodeToByteArray()).toHex())
    }

    @Test
    fun hmacSha256() {
        val mac = hmac(HashAlgorithm.SHA256, "key".encodeToByteArray(), "The quick brown fox jumps over the lazy dog".encodeToByteArray())
        assertEquals("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8", mac.toHex())
    }

    @Test
    fun hmacSha1() {
        val mac = hmac(HashAlgorithm.SHA1, "key".encodeToByteArray(), "The quick brown fox jumps over the lazy dog".encodeToByteArray())
        assertEquals("de7c9b85b8b78aa6bc8a7a36f70a90701c9db4d9", mac.toHex())
    }

    @Test
    fun hexRoundTrip() {
        val bytes = byteArrayOf(0, 15, 16, -1)
        assertEquals("000f10ff", bytes.toHex())
        assertEquals(bytes.toList(), hexToBytes("00:0f-10 ff")!!.toList())
        assertNull(hexToBytes("abc"))
        assertNull(hexToBytes("zz"))
    }
}
