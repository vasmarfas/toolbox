package com.vasmarfas.card.tools.documents.pdf

import com.vasmarfas.card.core.Zlib
import com.vasmarfas.card.tools.developer.Md5
import kotlin.math.roundToLong

private val longPowers = LongArray(11).also { p ->
    p[0] = 1
    for (i in 1 until p.size) p[i] = p[i - 1] * 10
}

internal fun formatReal(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "0"
    val negative = value < 0
    val magnitude = if (negative) -value else value
    if (magnitude >= 9.0e17) {
        var mantissa = magnitude
        var zeros = 0
        while (mantissa >= 9.0e17) {
            mantissa /= 10
            zeros++
        }
        return (if (negative) "-" else "") + mantissa.roundToLong() + "0".repeat(zeros)
    }
    var decimals = 10
    while (decimals > 0 && magnitude * powerOfTen(decimals) >= 9.0e17) decimals--
    val scaled = (magnitude * powerOfTen(decimals)).roundToLong()
    if (scaled == 0L) return "0"
    val unit = longPowers[decimals]
    val sb = StringBuilder(24)
    if (negative) sb.append('-')
    sb.append(scaled / unit)
    val fraction = scaled % unit
    if (fraction != 0L) {
        sb.append('.')
        val digits = fraction.toString()
        for (i in digits.length until decimals) sb.append('0')
        sb.append(digits.trimEnd('0'))
    }
    return sb.toString()
}

internal object PdfSyntax {
    private const val HEX = "0123456789ABCDEF"

    fun serialize(obj: PdfObject): ByteArray = ByteSink().also { write(it, obj) }.toByteArray()

    fun write(out: ByteSink, obj: PdfObject) {
        when (obj) {
            is PdfNull -> token(out, "null")
            is PdfBoolean -> token(out, if (obj.value) "true" else "false")
            is PdfInt -> token(out, obj.value.toString())
            is PdfReal -> token(out, formatReal(obj.value))
            is PdfName -> writeName(out, obj.name)
            is PdfString -> writeString(out, obj)
            is PdfRef -> {
                token(out, obj.number.toString())
                out.write(' '.code)
                out.writeAscii(obj.generation.toString())
                out.writeAscii(" R")
            }
            is PdfArray -> {
                out.write('['.code)
                for (item in obj.items) write(out, item)
                out.write(']'.code)
            }
            is PdfDict -> writeDict(out, obj, -1)
            is PdfStream -> throw PdfException("A stream can only be written as an indirect object")
        }
    }

    fun writeDict(out: ByteSink, dict: PdfDict, length: Int) {
        out.writeAscii("<<")
        for ((key, value) in dict.entries) {
            if (length >= 0 && key == "Length") continue
            writeName(out, key)
            write(out, value)
        }
        if (length >= 0) {
            writeName(out, "Length")
            token(out, length.toString())
        }
        out.writeAscii(">>")
    }

    private fun token(out: ByteSink, text: String) {
        if (out.size > 0 && isRegular(out.last())) out.write(' '.code)
        out.writeAscii(text)
    }

    fun writeName(out: ByteSink, name: String) {
        out.write('/'.code)
        val bytes = if (name.all { it.code < 256 }) null else name.encodeToByteArray()
        val count = bytes?.size ?: name.length
        for (i in 0 until count) {
            val c = if (bytes != null) bytes[i].toInt() and 0xFF else name[i].code
            if (c < 0x21 || c > 0x7E || c == '#'.code || isDelimiter(c)) {
                out.write('#'.code)
                out.write(HEX[c shr 4].code)
                out.write(HEX[c and 0xF].code)
            } else {
                out.write(c)
            }
        }
    }

    private fun writeString(out: ByteSink, string: PdfString) {
        val bytes = string.bytes
        if (string.hex) {
            out.write('<'.code)
            for (b in bytes) {
                val c = b.toInt() and 0xFF
                out.write(HEX[c shr 4].code)
                out.write(HEX[c and 0xF].code)
            }
            out.write('>'.code)
            return
        }
        out.write('('.code)
        for (b in bytes) {
            when (val c = b.toInt() and 0xFF) {
                '('.code, ')'.code, '\\'.code -> {
                    out.write('\\'.code)
                    out.write(c)
                }
                10 -> out.writeAscii("\\n")
                13 -> out.writeAscii("\\r")
                9 -> out.writeAscii("\\t")
                8 -> out.writeAscii("\\b")
                12 -> out.writeAscii("\\f")
                else -> if (c < 0x20 || c == 0x7F) {
                    out.write('\\'.code)
                    out.write('0'.code + (c shr 6))
                    out.write('0'.code + ((c shr 3) and 7))
                    out.write('0'.code + (c and 7))
                } else {
                    out.write(c)
                }
            }
        }
        out.write(')'.code)
    }
}

class PdfWriter {
    private val objects = ArrayList<PdfObject?>()

    fun reserve(): PdfRef {
        objects.add(null)
        return PdfRef(objects.size, 0)
    }

    fun add(obj: PdfObject): PdfRef {
        objects.add(obj)
        return PdfRef(objects.size, 0)
    }

    operator fun set(ref: PdfRef, obj: PdfObject) {
        require(ref.number in 1..objects.size && ref.generation == 0) { "$ref was not created by this writer" }
        objects[ref.number - 1] = obj
    }

    operator fun get(ref: PdfRef): PdfObject? = objects.getOrNull(ref.number - 1)

    fun resolve(obj: PdfObject?): PdfObject? = if (obj is PdfRef) get(obj) else obj

    fun stream(dict: PdfDict, data: ByteArray, compress: Boolean = true): PdfRef {
        var bytes = data
        if (compress) {
            bytes = Zlib.compress(data)
            val flate = PdfName("FlateDecode")
            when (val existing = dict["Filter"]) {
                null -> dict["Filter"] = flate
                is PdfArray -> {
                    existing.items.add(0, flate)
                    (dict["DecodeParms"] as? PdfArray)?.items?.add(0, PdfNull)
                }
                else -> {
                    dict["Filter"] = PdfArray(flate, existing)
                    dict["DecodeParms"]?.let { dict["DecodeParms"] = PdfArray(PdfNull, it) }
                }
            }
        }
        dict["Length"] = PdfInt.of(bytes.size)
        return add(PdfStream(dict, bytes))
    }

    internal fun toByteArray(root: PdfRef, info: PdfRef?, encryptor: PdfEncryptor?): ByteArray {
        if (encryptor == null) return toByteArray(root, info)
        val encrypt = add(encryptor.dict)
        return write(root, info, encrypt) { number, obj -> if (number == encrypt.number) obj else encryptor.encrypt(obj) }
    }

    fun toByteArray(root: PdfRef, info: PdfRef? = null): ByteArray = write(root, info, null) { _, obj -> obj }

    private fun write(root: PdfRef, info: PdfRef?, encrypt: PdfRef?, transform: (Int, PdfObject) -> PdfObject): ByteArray {
        var payload = 0L
        for (obj in objects) if (obj is PdfStream) payload += obj.data.size
        val out = ByteSink((payload + objects.size * 96L + 1024).coerceAtMost(Int.MAX_VALUE - 64L).toInt())
        out.writeAscii("%PDF-1.7\n")
        for (b in intArrayOf('%'.code, 0xE2, 0xE3, 0xCF, 0xD3, '\n'.code)) out.write(b)
        val offsets = IntArray(objects.size)
        val payloads = IntList()
        for (i in objects.indices) {
            offsets[i] = out.size
            out.writeAscii((i + 1).toString())
            out.writeAscii(" 0 obj\n")
            when (val obj = transform(i + 1, objects[i] ?: PdfNull)) {
                is PdfStream -> {
                    PdfSyntax.writeDict(out, obj.dict, obj.data.size)
                    out.writeAscii("\nstream\n")
                    payloads.add(out.size)
                    out.write(obj.data)
                    payloads.add(out.size)
                    out.writeAscii("\nendstream")
                }
                else -> PdfSyntax.write(out, obj)
            }
            out.writeAscii("\nendobj\n")
        }
        val xrefOffset = out.size
        out.writeAscii("xref\n0 ")
        out.writeAscii((objects.size + 1).toString())
        out.writeAscii("\n0000000000 65535 f\r\n")
        for (offset in offsets) {
            val digits = offset.toString()
            for (k in digits.length until 10) out.write('0'.code)
            out.writeAscii(digits)
            out.writeAscii(" 00000 n\r\n")
        }
        val id = PdfString(fingerprint(out, payloads), hex = true)
        val trailer = PdfDict("Size" to PdfInt.of(objects.size + 1), "Root" to root)
        if (info != null) trailer["Info"] = info
        if (encrypt != null) trailer["Encrypt"] = encrypt
        trailer["ID"] = PdfArray(id, id)
        out.writeAscii("trailer\n")
        PdfSyntax.write(out, trailer)
        out.writeAscii("\nstartxref\n")
        out.writeAscii(xrefOffset.toString())
        out.writeAscii("\n%%EOF\n")
        return out.toByteArray()
    }

    // MD5 over everything written except the middle of each stream payload, its size and ends still count
    private fun fingerprint(out: ByteSink, payloads: IntList): ByteArray {
        val sample = ByteSink(out.size / 8 + 1024)
        var from = 0
        for (k in 0 until payloads.size step 2) {
            val start = payloads[k]
            val end = payloads[k + 1]
            if (end - start <= 2048) continue
            out.copyInto(sample, from, start + 1024)
            sample.writeAscii((end - start).toString())
            from = end - 1024
        }
        out.copyInto(sample, from, out.size)
        return Md5.digest(sample.toByteArray())
    }
}
