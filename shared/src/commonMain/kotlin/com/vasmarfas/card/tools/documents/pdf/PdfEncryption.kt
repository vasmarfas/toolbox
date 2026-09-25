package com.vasmarfas.card.tools.documents.pdf

import com.vasmarfas.card.core.secureRandomBytes

class PdfPermissions(val print: Boolean = true, val copy: Boolean = true, val edit: Boolean = true) {
    val value: Int
        get() {
            var p = 0xFFFFF0C0.toInt() or 512
            if (print) p = p or 4 or 2048
            if (copy) p = p or 16
            if (edit) p = p or 8 or 32 or 256 or 1024
            return p
        }
}

internal class PdfEncryptor private constructor(private val key: ByteArray, val dict: PdfDict) {
    fun encrypt(obj: PdfObject): PdfObject = when (obj) {
        is PdfString -> PdfString(encryptBytes(obj.bytes), obj.hex)
        is PdfArray -> PdfArray(obj.items.mapTo(ArrayList(obj.size)) { encrypt(it) })
        is PdfDict -> PdfDict(LinkedHashMap<String, PdfObject>().also { out -> for ((k, v) in obj.entries) out[k] = encrypt(v) })
        is PdfStream -> PdfStream(encrypt(obj.dict) as PdfDict, encryptBytes(obj.data))
        else -> obj
    }

    private fun encryptBytes(data: ByteArray): ByteArray {
        val iv = secureRandomBytes(16)
        val pad = 16 - data.size % 16
        val padded = data.copyOf(data.size + pad)
        for (i in data.size until padded.size) padded[i] = pad.toByte()
        return iv + aesCbc(key, iv, padded, encrypt = true)
    }

    companion object {
        fun aes256(userPassword: String, ownerPassword: String, permissions: PdfPermissions): PdfEncryptor {
            val key = secureRandomBytes(32)
            val user = secret(userPassword)
            val owner = secret(ownerPassword.ifEmpty { secureRandomBytes(24).joinToString("") { (it.toInt() and 0xFF).toString(16) } })
            val zero = ByteArray(16)
            val userSalts = secureRandomBytes(16)
            val u = PdfSecurity.hash(6, user, userSalts.copyOf(8), ByteArray(0)) + userSalts
            val ue = aesCbc(PdfSecurity.hash(6, user, userSalts.copyOfRange(8, 16), ByteArray(0)), zero, key, encrypt = true)
            val ownerSalts = secureRandomBytes(16)
            val o = PdfSecurity.hash(6, owner, ownerSalts.copyOf(8), u) + ownerSalts
            val oe = aesCbc(PdfSecurity.hash(6, owner, ownerSalts.copyOfRange(8, 16), u), zero, key, encrypt = true)
            val p = permissions.value
            val block = ByteArray(16)
            for (i in 0 until 4) block[i] = (p ushr (8 * i)).toByte()
            for (i in 4 until 8) block[i] = 0xFF.toByte()
            block[8] = 'T'.code.toByte()
            block[9] = 'a'.code.toByte()
            block[10] = 'd'.code.toByte()
            block[11] = 'b'.code.toByte()
            secureRandomBytes(4).copyInto(block, 12)
            val perms = aesCbc(key, zero, block, encrypt = true)
            val dict = PdfDict(
                "Filter" to PdfName("Standard"),
                "V" to PdfInt.of(5),
                "R" to PdfInt.of(6),
                "Length" to PdfInt.of(256),
                "P" to PdfInt.of(p),
                "O" to PdfString(o, hex = true),
                "U" to PdfString(u, hex = true),
                "OE" to PdfString(oe, hex = true),
                "UE" to PdfString(ue, hex = true),
                "Perms" to PdfString(perms, hex = true),
                "EncryptMetadata" to PdfBoolean.TRUE,
                "CF" to PdfDict(
                    "StdCF" to PdfDict(
                        "Type" to PdfName("CryptFilter"),
                        "CFM" to PdfName("AESV3"),
                        "AuthEvent" to PdfName("DocOpen"),
                        "Length" to PdfInt.of(32),
                    ),
                ),
                "StmF" to PdfName("StdCF"),
                "StrF" to PdfName("StdCF"),
            )
            return PdfEncryptor(key, dict)
        }

        private fun secret(password: String): ByteArray = password.encodeToByteArray().let { if (it.size > 127) it.copyOf(127) else it }
    }
}
