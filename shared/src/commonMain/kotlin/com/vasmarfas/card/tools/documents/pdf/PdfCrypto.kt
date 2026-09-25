package com.vasmarfas.card.tools.documents.pdf

import com.vasmarfas.card.tools.developer.Md5
import com.vasmarfas.card.tools.developer.Sha256
import com.vasmarfas.card.tools.developer.Sha384
import com.vasmarfas.card.tools.developer.Sha512

internal enum class CryptMethod { NONE, RC4, AES_128, AES_256 }

// standard security handler, revisions 2 to 6. RC4 and AESV2 keys are salted with the object number
// and generation, AESV3 uses the file key directly
internal class PdfSecurity private constructor(
    private val key: ByteArray,
    private val stringMethod: CryptMethod,
    private val streamMethod: CryptMethod,
    private val filters: Map<String, CryptMethod>,
    private val encryptMetadata: Boolean,
) {
    fun decrypt(obj: PdfObject, number: Int, generation: Int): PdfObject = when (obj) {
        is PdfString -> PdfString(decryptBytes(stringMethod, obj.bytes, number, generation), obj.hex)
        is PdfArray -> {
            for (i in obj.items.indices) obj.items[i] = decrypt(obj.items[i], number, generation)
            obj
        }
        is PdfDict -> {
            decryptDict(obj, number, generation)
            obj
        }
        is PdfStream -> {
            decryptStream(obj, number, generation)
            obj
        }
        else -> obj
    }

    // signature dictionaries keep /Contents in the clear
    private fun decryptDict(dict: PdfDict, number: Int, generation: Int) {
        val signature = dict["ByteRange"] != null && dict["Contents"] is PdfString
        for (entry in dict.entries) {
            if (signature && entry.key == "Contents") continue
            entry.setValue(decrypt(entry.value, number, generation))
        }
    }

    private fun decryptStream(stream: PdfStream, number: Int, generation: Int) {
        val dict = stream.dict
        val type = dict.name("Type")
        if (type == "XRef") return
        decryptDict(dict, number, generation)
        if (type == "Metadata" && !encryptMetadata) return
        var method = streamMethod
        val chain = when (val filter = dict["Filter"]) {
            is PdfName -> listOf(filter)
            is PdfArray -> filter.items
            else -> emptyList()
        }
        val crypt = chain.indexOfFirst { (it as? PdfName)?.name == "Crypt" }
        if (crypt >= 0) {
            val parms = when (val p = dict["DecodeParms"]) {
                is PdfDict -> if (crypt == 0) p else null
                is PdfArray -> p.items.getOrNull(crypt) as? PdfDict
                else -> null
            }
            val name = parms?.name("Name") ?: "Identity"
            method = if (name == "Identity") CryptMethod.NONE else filters[name] ?: streamMethod
            removeFilter(dict, crypt)
        }
        stream.data = decryptBytes(method, stream.data, number, generation)
    }

    private fun removeFilter(dict: PdfDict, index: Int) {
        val filter = dict["Filter"]
        if (filter is PdfArray && filter.size > 1) {
            filter.items.removeAt(index)
            (dict["DecodeParms"] as? PdfArray)?.items?.let { if (index < it.size) it.removeAt(index) }
        } else {
            dict.remove("Filter")
            dict.remove("DecodeParms")
        }
    }

    private fun decryptBytes(method: CryptMethod, data: ByteArray, number: Int, generation: Int): ByteArray = when (method) {
        CryptMethod.NONE -> data
        CryptMethod.RC4 -> rc4(objectKey(number, generation, aes = false), data)
        CryptMethod.AES_128 -> aesDecrypt(objectKey(number, generation, aes = true), data)
        CryptMethod.AES_256 -> aesDecrypt(key, data)
    }

    private fun objectKey(number: Int, generation: Int, aes: Boolean): ByteArray {
        val input = ByteArray(key.size + if (aes) 9 else 5)
        key.copyInto(input)
        var p = key.size
        input[p++] = number.toByte()
        input[p++] = (number shr 8).toByte()
        input[p++] = (number shr 16).toByte()
        input[p++] = generation.toByte()
        input[p++] = (generation shr 8).toByte()
        if (aes) for (c in "sAlT") input[p++] = c.code.toByte()
        return Md5.digest(input).copyOf(minOf(key.size + 5, 16))
    }

    companion object {
        private val padding = intArrayOf(
            0x28, 0xBF, 0x4E, 0x5E, 0x4E, 0x75, 0x8A, 0x41, 0x64, 0x00, 0x4E, 0x56, 0xFF, 0xFA, 0x01, 0x08,
            0x2E, 0x2E, 0x00, 0xB6, 0xD0, 0x68, 0x3E, 0x80, 0x2F, 0x0C, 0xA9, 0xFE, 0x64, 0x53, 0x69, 0x7A,
        ).let { codes -> ByteArray(codes.size) { codes[it].toByte() } }

        fun open(dict: PdfDict, id: ByteArray, password: String, doc: PdfDocument): PdfSecurity {
            val handler = dict.name("Filter", doc)
            if (handler != "Standard") throw PdfEncryptedException("Unsupported security handler: ${handler ?: "none"}")
            val revision = dict.int("R", doc) ?: throw PdfEncryptedException("Encryption revision is missing")
            val o = dict.string("O", doc)?.bytes ?: throw PdfEncryptedException("Owner key is missing")
            val u = dict.string("U", doc)?.bytes ?: throw PdfEncryptedException("User key is missing")
            val encryptMetadata = dict.boolean("EncryptMetadata", doc) ?: true
            return when (revision) {
                2, 3, 4 -> openRc4OrAes128(dict, doc, revision, o, u, id, password, encryptMetadata)
                5, 6 -> openAes256(dict, doc, revision, o, u, password, encryptMetadata)
                else -> throw PdfEncryptedException("Unsupported encryption revision $revision")
            }
        }

        private fun cryptFilters(dict: PdfDict, doc: PdfDocument): Map<String, Pair<CryptMethod, Int?>> {
            val result = HashMap<String, Pair<CryptMethod, Int?>>()
            val cf = dict.dict("CF", doc) ?: return result
            for ((name, value) in cf.entries) {
                val filter = doc.resolve(value) as? PdfDict ?: continue
                val method = when (filter.name("CFM", doc)) {
                    "V2" -> CryptMethod.RC4
                    "AESV2" -> CryptMethod.AES_128
                    "AESV3" -> CryptMethod.AES_256
                    else -> CryptMethod.NONE
                }
                result[name] = method to filter.int("Length", doc)
            }
            return result
        }

        private fun method(name: String?, filters: Map<String, Pair<CryptMethod, Int?>>, fallback: CryptMethod): CryptMethod = when (name) {
            null -> fallback
            "Identity" -> CryptMethod.NONE
            else -> filters[name]?.first ?: fallback
        }

        private fun openRc4OrAes128(
            dict: PdfDict,
            doc: PdfDocument,
            revision: Int,
            o: ByteArray,
            u: ByteArray,
            id: ByteArray,
            password: String,
            encryptMetadata: Boolean,
        ): PdfSecurity {
            val version = dict.int("V", doc) ?: 0
            val permissions = (dict.long("P", doc) ?: 0L).toInt()
            var length = if (version == 1 || revision == 2) 5 else (dict.int("Length", doc) ?: 40) / 8
            var stringMethod = CryptMethod.RC4
            var streamMethod = CryptMethod.RC4
            val filters = cryptFilters(dict, doc)
            if (version >= 4) {
                val streamFilter = dict.name("StmF", doc)
                stringMethod = method(dict.name("StrF", doc), filters, CryptMethod.NONE)
                streamMethod = method(streamFilter, filters, CryptMethod.NONE)
                val declared = filters[streamFilter]?.second ?: filters[dict.name("StrF", doc)]?.second
                length = when {
                    declared == null -> if (dict["Length"] == null) 16 else length
                    declared <= 32 -> declared
                    else -> declared / 8
                }
            }
            length = length.coerceIn(5, 16)
            val secret = password.map { if (it.code < 256) it.code.toByte() else '?'.code.toByte() }.toByteArray()
            val userKey = fileKey(pad(secret), o, permissions, id, revision, length, encryptMetadata)
            val methods = filters.mapValues { it.value.first }
            if (userMatches(userKey, u, id, revision)) return PdfSecurity(userKey, stringMethod, streamMethod, methods, encryptMetadata)
            val ownerKey = fileKey(userPasswordFromOwner(secret, o, revision, length), o, permissions, id, revision, length, encryptMetadata)
            if (userMatches(ownerKey, u, id, revision)) return PdfSecurity(ownerKey, stringMethod, streamMethod, methods, encryptMetadata)
            throw PdfEncryptedException("The document is protected with a password")
        }

        private fun pad(password: ByteArray): ByteArray {
            val out = padding.copyOf()
            password.copyInto(out, 0, 0, minOf(32, password.size))
            if (password.size < 32) padding.copyInto(out, password.size, 0, 32 - password.size)
            return out
        }

        private fun fileKey(padded: ByteArray, o: ByteArray, permissions: Int, id: ByteArray, revision: Int, length: Int, encryptMetadata: Boolean): ByteArray {
            val input = ByteSink(96)
            input.write(padded, 0, 32)
            input.write(o, 0, minOf(32, o.size))
            for (shift in intArrayOf(0, 8, 16, 24)) input.write(permissions shr shift)
            input.write(id)
            if (revision >= 4 && !encryptMetadata) repeat(4) { input.write(0xFF) }
            var hash = Md5.digest(input.toByteArray())
            if (revision >= 3) repeat(50) { hash = Md5.digest(hash.copyOf(length)) }
            return hash.copyOf(length)
        }

        private fun userMatches(key: ByteArray, u: ByteArray, id: ByteArray, revision: Int): Boolean {
            if (revision == 2) return rc4(key, padding).contentEquals(u.copyOf(32))
            var value = rc4(key, Md5.digest(padding + id))
            for (i in 1..19) value = rc4(xor(key, i), value)
            return value.copyOf(16).contentEquals(u.copyOf(16))
        }

        // Algorithm 7. The 50 rounds hash only the first key-length bytes, as Acrobat does, the spec text
        // differs for 40-bit keys
        private fun userPasswordFromOwner(owner: ByteArray, o: ByteArray, revision: Int, length: Int): ByteArray {
            var hash = Md5.digest(pad(owner))
            if (revision >= 3) repeat(50) { hash = Md5.digest(hash.copyOf(length)) }
            val key = hash.copyOf(if (revision == 2) 5 else length)
            var user = o.copyOf(32)
            if (revision == 2) {
                user = rc4(key, user)
            } else {
                for (i in 19 downTo 0) user = rc4(xor(key, i), user)
            }
            return user
        }

        private fun xor(key: ByteArray, value: Int) = ByteArray(key.size) { (key[it].toInt() xor value).toByte() }

        private fun openAes256(
            dict: PdfDict,
            doc: PdfDocument,
            revision: Int,
            o: ByteArray,
            u: ByteArray,
            password: String,
            encryptMetadata: Boolean,
        ): PdfSecurity {
            if (o.size < 48 || u.size < 48) throw PdfEncryptedException("Malformed encryption dictionary")
            val secret = password.encodeToByteArray().let { if (it.size > 127) it.copyOf(127) else it }
            val userData = u.copyOf(48)
            val none = ByteArray(0)
            val ue = dict.string("UE", doc)?.bytes
            val oe = dict.string("OE", doc)?.bytes
            val key = when {
                ue != null && hash(revision, secret, u.copyOfRange(32, 40), none).contentEquals(u.copyOf(32)) ->
                    aesCbc(hash(revision, secret, u.copyOfRange(40, 48), none), ByteArray(16), ue.copyOf(32), encrypt = false)
                oe != null && hash(revision, secret, o.copyOfRange(32, 40), userData).contentEquals(o.copyOf(32)) ->
                    aesCbc(hash(revision, secret, o.copyOfRange(40, 48), userData), ByteArray(16), oe.copyOf(32), encrypt = false)
                else -> throw PdfEncryptedException("The document is protected with a password")
            }
            val filters = cryptFilters(dict, doc)
            return PdfSecurity(
                key,
                method(dict.name("StrF", doc), filters, CryptMethod.AES_256),
                method(dict.name("StmF", doc), filters, CryptMethod.AES_256),
                filters.mapValues { it.value.first },
                encryptMetadata,
            )
        }

        // revision 5 hashes once with SHA-256, revision 6 runs the iterated hash of ISO 32000-2, 7.6.4.3.4
        internal fun hash(revision: Int, password: ByteArray, salt: ByteArray, userData: ByteArray): ByteArray {
            var k = Sha256.digest(password + salt + userData)
            if (revision == 5) return k
            var round = 0
            while (true) {
                val block = password + k + userData
                val repeated = ByteArray(block.size * 64)
                for (i in 0 until 64) block.copyInto(repeated, i * block.size)
                val e = aesCbc(k.copyOf(16), k.copyOfRange(16, 32), repeated, encrypt = true)
                var sum = 0
                for (i in 0 until 16) sum += e[i].toInt() and 0xFF
                k = when (sum % 3) {
                    0 -> Sha256.digest(e)
                    1 -> Sha384.digest(e)
                    else -> Sha512.digest(e)
                }
                round++
                if (round >= 64 && (e[e.size - 1].toInt() and 0xFF) <= round - 32) break
            }
            return k.copyOf(32)
        }
    }
}

internal fun rc4(key: ByteArray, data: ByteArray): ByteArray {
    val s = IntArray(256) { it }
    var j = 0
    for (i in 0 until 256) {
        j = (j + s[i] + (key[i % key.size].toInt() and 0xFF)) and 0xFF
        val t = s[i]
        s[i] = s[j]
        s[j] = t
    }
    val out = ByteArray(data.size)
    var i = 0
    j = 0
    for (k in data.indices) {
        i = (i + 1) and 0xFF
        j = (j + s[i]) and 0xFF
        val t = s[i]
        s[i] = s[j]
        s[j] = t
        out[k] = (data[k].toInt() xor s[(s[i] + s[j]) and 0xFF]).toByte()
    }
    return out
}

internal fun aesDecrypt(key: ByteArray, data: ByteArray): ByteArray {
    if (data.size < 32) return ByteArray(0)
    val body = (data.size - 16) / 16 * 16
    val out = aesCbc(key, data.copyOf(16), data.copyOfRange(16, 16 + body), encrypt = false)
    val pad = out[out.size - 1].toInt() and 0xFF
    if (pad !in 1..16) return out
    for (i in out.size - pad until out.size) if (out[i].toInt() and 0xFF != pad) return out
    return out.copyOf(out.size - pad)
}

internal fun aesCbc(key: ByteArray, iv: ByteArray, data: ByteArray, encrypt: Boolean): ByteArray {
    val aes = Aes(key)
    val out = ByteArray(data.size / 16 * 16)
    val chain = iv.copyOf(16)
    val block = ByteArray(16)
    for (offset in 0 until out.size step 16) {
        if (encrypt) {
            for (i in 0 until 16) block[i] = (data[offset + i].toInt() xor chain[i].toInt()).toByte()
            aes.encryptBlock(block, 0, out, offset)
            out.copyInto(chain, 0, offset, offset + 16)
        } else {
            aes.decryptBlock(data, offset, out, offset)
            for (i in 0 until 16) out[offset + i] = (out[offset + i].toInt() xor chain[i].toInt()).toByte()
            data.copyInto(chain, 0, offset, offset + 16)
        }
    }
    return out
}

internal class Aes(key: ByteArray) {
    private val rounds: Int
    private val encryptKeys: IntArray
    private val decryptKeys: IntArray

    init {
        require(key.size == 16 || key.size == 24 || key.size == 32) { "AES key must be 16, 24 or 32 bytes" }
        val nk = key.size / 4
        rounds = nk + 6
        val w = IntArray(4 * (rounds + 1))
        for (i in 0 until nk) w[i] = readInt(key, i * 4)
        var rcon = 1
        for (i in nk until w.size) {
            var t = w[i - 1]
            if (i % nk == 0) {
                t = subWord((t shl 8) or (t ushr 24)) xor (rcon shl 24)
                rcon = xtime(rcon)
            } else if (nk > 6 && i % nk == 4) {
                t = subWord(t)
            }
            w[i] = w[i - nk] xor t
        }
        encryptKeys = w
        val d = IntArray(w.size)
        for (round in 0..rounds) {
            for (j in 0 until 4) {
                val source = w[4 * (rounds - round) + j]
                d[4 * round + j] = if (round == 0 || round == rounds) source else invMix(source)
            }
        }
        decryptKeys = d
    }

    fun encryptBlock(input: ByteArray, inOffset: Int, output: ByteArray, outOffset: Int) {
        val k = encryptKeys
        var s0 = readInt(input, inOffset) xor k[0]
        var s1 = readInt(input, inOffset + 4) xor k[1]
        var s2 = readInt(input, inOffset + 8) xor k[2]
        var s3 = readInt(input, inOffset + 12) xor k[3]
        var p = 4
        for (round in 1 until rounds) {
            val t0 = TE0[s0 ushr 24] xor TE1[(s1 ushr 16) and 0xFF] xor TE2[(s2 ushr 8) and 0xFF] xor TE3[s3 and 0xFF] xor k[p]
            val t1 = TE0[s1 ushr 24] xor TE1[(s2 ushr 16) and 0xFF] xor TE2[(s3 ushr 8) and 0xFF] xor TE3[s0 and 0xFF] xor k[p + 1]
            val t2 = TE0[s2 ushr 24] xor TE1[(s3 ushr 16) and 0xFF] xor TE2[(s0 ushr 8) and 0xFF] xor TE3[s1 and 0xFF] xor k[p + 2]
            val t3 = TE0[s3 ushr 24] xor TE1[(s0 ushr 16) and 0xFF] xor TE2[(s1 ushr 8) and 0xFF] xor TE3[s2 and 0xFF] xor k[p + 3]
            s0 = t0
            s1 = t1
            s2 = t2
            s3 = t3
            p += 4
        }
        writeInt(output, outOffset, lastRound(SBOX, s0, s1, s2, s3) xor k[p])
        writeInt(output, outOffset + 4, lastRound(SBOX, s1, s2, s3, s0) xor k[p + 1])
        writeInt(output, outOffset + 8, lastRound(SBOX, s2, s3, s0, s1) xor k[p + 2])
        writeInt(output, outOffset + 12, lastRound(SBOX, s3, s0, s1, s2) xor k[p + 3])
    }

    fun decryptBlock(input: ByteArray, inOffset: Int, output: ByteArray, outOffset: Int) {
        val k = decryptKeys
        var s0 = readInt(input, inOffset) xor k[0]
        var s1 = readInt(input, inOffset + 4) xor k[1]
        var s2 = readInt(input, inOffset + 8) xor k[2]
        var s3 = readInt(input, inOffset + 12) xor k[3]
        var p = 4
        for (round in 1 until rounds) {
            val t0 = TD0[s0 ushr 24] xor TD1[(s3 ushr 16) and 0xFF] xor TD2[(s2 ushr 8) and 0xFF] xor TD3[s1 and 0xFF] xor k[p]
            val t1 = TD0[s1 ushr 24] xor TD1[(s0 ushr 16) and 0xFF] xor TD2[(s3 ushr 8) and 0xFF] xor TD3[s2 and 0xFF] xor k[p + 1]
            val t2 = TD0[s2 ushr 24] xor TD1[(s1 ushr 16) and 0xFF] xor TD2[(s0 ushr 8) and 0xFF] xor TD3[s3 and 0xFF] xor k[p + 2]
            val t3 = TD0[s3 ushr 24] xor TD1[(s2 ushr 16) and 0xFF] xor TD2[(s1 ushr 8) and 0xFF] xor TD3[s0 and 0xFF] xor k[p + 3]
            s0 = t0
            s1 = t1
            s2 = t2
            s3 = t3
            p += 4
        }
        writeInt(output, outOffset, lastRound(INVERSE, s0, s3, s2, s1) xor k[p])
        writeInt(output, outOffset + 4, lastRound(INVERSE, s1, s0, s3, s2) xor k[p + 1])
        writeInt(output, outOffset + 8, lastRound(INVERSE, s2, s1, s0, s3) xor k[p + 2])
        writeInt(output, outOffset + 12, lastRound(INVERSE, s3, s2, s1, s0) xor k[p + 3])
    }

    private companion object {
        val SBOX = IntArray(256)
        val INVERSE = IntArray(256)
        val TE0 = IntArray(256)
        val TE1 = IntArray(256)
        val TE2 = IntArray(256)
        val TE3 = IntArray(256)
        val TD0 = IntArray(256)
        val TD1 = IntArray(256)
        val TD2 = IntArray(256)
        val TD3 = IntArray(256)

        init {
            var p = 1
            var q = 1
            do {
                p = (p xor (p shl 1) xor (if (p and 0x80 != 0) 0x1B else 0)) and 0xFF
                q = (q xor (q shl 1)) and 0xFF
                q = (q xor (q shl 2)) and 0xFF
                q = (q xor (q shl 4)) and 0xFF
                if (q and 0x80 != 0) q = q xor 0x09
                val x = q xor rotl8(q, 1) xor rotl8(q, 2) xor rotl8(q, 3) xor rotl8(q, 4)
                SBOX[p] = (x xor 0x63) and 0xFF
            } while (p != 1)
            SBOX[0] = 0x63
            for (i in 0 until 256) INVERSE[SBOX[i]] = i
            for (i in 0 until 256) {
                val s = SBOX[i]
                val e = (multiply(s, 2) shl 24) or (s shl 16) or (s shl 8) or multiply(s, 3)
                TE0[i] = e
                TE1[i] = e.rotateRight(8)
                TE2[i] = e.rotateRight(16)
                TE3[i] = e.rotateRight(24)
                val v = INVERSE[i]
                val d = (multiply(v, 14) shl 24) or (multiply(v, 9) shl 16) or (multiply(v, 13) shl 8) or multiply(v, 11)
                TD0[i] = d
                TD1[i] = d.rotateRight(8)
                TD2[i] = d.rotateRight(16)
                TD3[i] = d.rotateRight(24)
            }
        }

        fun rotl8(x: Int, shift: Int) = ((x shl shift) or (x ushr (8 - shift))) and 0xFF

        fun xtime(a: Int) = ((a shl 1) xor (if (a and 0x80 != 0) 0x1B else 0)) and 0xFF

        fun multiply(a: Int, b: Int): Int {
            var result = 0
            var x = a
            var y = b
            while (y != 0) {
                if (y and 1 != 0) result = result xor x
                x = xtime(x)
                y = y ushr 1
            }
            return result
        }

        fun subWord(w: Int) =
            (SBOX[w ushr 24] shl 24) or (SBOX[(w ushr 16) and 0xFF] shl 16) or (SBOX[(w ushr 8) and 0xFF] shl 8) or SBOX[w and 0xFF]

        fun invMix(w: Int) = TD0[SBOX[w ushr 24]] xor TD1[SBOX[(w ushr 16) and 0xFF]] xor TD2[SBOX[(w ushr 8) and 0xFF]] xor TD3[SBOX[w and 0xFF]]

        fun lastRound(box: IntArray, a: Int, b: Int, c: Int, d: Int) =
            (box[a ushr 24] shl 24) or (box[(b ushr 16) and 0xFF] shl 16) or (box[(c ushr 8) and 0xFF] shl 8) or box[d and 0xFF]

        fun readInt(b: ByteArray, off: Int) =
            ((b[off].toInt() and 0xFF) shl 24) or ((b[off + 1].toInt() and 0xFF) shl 16) or
                ((b[off + 2].toInt() and 0xFF) shl 8) or (b[off + 3].toInt() and 0xFF)

        fun writeInt(out: ByteArray, off: Int, v: Int) {
            out[off] = (v ushr 24).toByte()
            out[off + 1] = (v ushr 16).toByte()
            out[off + 2] = (v ushr 8).toByte()
            out[off + 3] = v.toByte()
        }
    }
}
