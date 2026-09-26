package com.vasmarfas.card.tools.documents.pdf

sealed interface PdfObject

data object PdfNull : PdfObject

data class PdfBoolean(val value: Boolean) : PdfObject {
    override fun toString() = value.toString()

    companion object {
        private val yes = PdfBoolean(true)
        private val no = PdfBoolean(false)

        fun of(value: Boolean): PdfBoolean = if (value) yes else no
    }
}

data class PdfInt(val value: Long) : PdfObject {
    constructor(value: Int) : this(value.toLong())

    override fun toString() = value.toString()

    companion object {
        private val small = Array(256) { PdfInt(it.toLong()) }

        fun of(value: Long): PdfInt = if (value in 0L..255L) small[value.toInt()] else PdfInt(value)

        fun of(value: Int): PdfInt = of(value.toLong())
    }
}

data class PdfReal(val value: Double) : PdfObject {
    override fun toString() = formatReal(value)
}

// #xx escapes resolved, every byte kept as one char (U+0000..U+00FF) so the name round-trips exactly
data class PdfName(val name: String) : PdfObject {
    override fun toString() = "/$name"
}

class PdfString(val bytes: ByteArray, val hex: Boolean = false) : PdfObject {
    fun text(): String = PdfEncodings.decodeText(bytes)

    override fun equals(other: Any?) = other is PdfString && other.bytes.contentEquals(bytes)

    override fun hashCode() = bytes.contentHashCode()

    override fun toString() = "(${text()})"

    companion object {
        fun ofText(text: String): PdfString = PdfString(PdfEncodings.encodeText(text))
    }
}

class PdfArray(val items: MutableList<PdfObject>) : PdfObject {
    constructor(vararg values: PdfObject) : this(values.toMutableList())

    val size: Int get() = items.size

    operator fun get(index: Int): PdfObject = items[index]

    fun add(value: PdfObject) {
        items.add(value)
    }

    fun resolved(index: Int, doc: PdfDocument?): PdfObject {
        val value = items.getOrNull(index) ?: return PdfNull
        return doc?.resolve(value) ?: value
    }

    fun numbers(doc: PdfDocument? = null): DoubleArray? {
        val out = DoubleArray(items.size)
        for (i in items.indices) out[i] = resolved(i, doc).asDouble() ?: return null
        return out
    }

    override fun toString() = items.joinToString(" ", "[", "]")
}

// keys without the leading slash, insertion order kept for output
class PdfDict(val entries: MutableMap<String, PdfObject>) : PdfObject {
    constructor() : this(LinkedHashMap())

    constructor(vararg pairs: Pair<String, PdfObject>) : this(linkedMapOf(*pairs))

    val keys: Set<String> get() = entries.keys

    operator fun get(key: String): PdfObject? = entries[key]

    operator fun set(key: String, value: PdfObject) {
        entries[key] = value
    }

    operator fun contains(key: String) = entries.containsKey(key)

    fun remove(key: String): PdfObject? = entries.remove(key)

    fun resolved(key: String, doc: PdfDocument?): PdfObject? {
        val value = entries[key] ?: return null
        return doc?.resolve(value) ?: value
    }

    fun name(key: String, doc: PdfDocument? = null): String? = (resolved(key, doc) as? PdfName)?.name

    fun int(key: String, doc: PdfDocument? = null): Int? = resolved(key, doc).asInt()

    fun long(key: String, doc: PdfDocument? = null): Long? = when (val value = resolved(key, doc)) {
        is PdfInt -> value.value
        is PdfReal -> value.value.toLong()
        else -> null
    }

    fun number(key: String, doc: PdfDocument? = null): Double? = resolved(key, doc).asDouble()

    fun boolean(key: String, doc: PdfDocument? = null): Boolean? = (resolved(key, doc) as? PdfBoolean)?.value

    fun string(key: String, doc: PdfDocument? = null): PdfString? = resolved(key, doc) as? PdfString

    fun text(key: String, doc: PdfDocument? = null): String? = when (val value = resolved(key, doc)) {
        is PdfString -> value.text()
        is PdfName -> value.name
        else -> null
    }

    fun dict(key: String, doc: PdfDocument? = null): PdfDict? = resolved(key, doc) as? PdfDict

    fun array(key: String, doc: PdfDocument? = null): PdfArray? = resolved(key, doc) as? PdfArray

    fun stream(key: String, doc: PdfDocument? = null): PdfStream? = resolved(key, doc) as? PdfStream

    override fun toString() = entries.entries.joinToString(" ", "<<", ">>") { "/${it.key} ${it.value}" }
}

// data is still encoded with the stream's filters
class PdfStream(val dict: PdfDict, var data: ByteArray) : PdfObject {
    override fun toString() = "$dict stream[${data.size}]"
}

data class PdfRef(val number: Int, val generation: Int) : PdfObject {
    override fun toString() = "$number $generation R"
}

class PdfException(message: String) : Exception(message)

class PdfEncryptedException(message: String) : Exception(message)

internal fun PdfObject?.asDouble(): Double? = when (this) {
    is PdfInt -> value.toDouble()
    is PdfReal -> value
    else -> null
}

internal fun PdfObject?.asInt(): Int? = when (this) {
    is PdfInt -> value.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
    is PdfReal -> if (value.isNaN()) null else value.toInt()
    else -> null
}
