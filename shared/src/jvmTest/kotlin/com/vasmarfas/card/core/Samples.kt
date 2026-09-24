package com.vasmarfas.card.core

import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater
import kotlin.random.Random

enum class SampleKind { RANDOM, ENGLISH, RUSSIAN, SOURCE, RUNS }

object Samples {
    private val english = (
        "the of and to in a is that it for as with was on be by this are from at or an which have not but they his one all their were can there " +
            "been has more if when will would who so no out up into do only time its about than other some could them these two may first then " +
            "any like now my such make over our even most me state after also made many did must before back see through way where get much go well " +
            "your know should down work year because come people just say each those take day good how long man own too little use us very great " +
            "world still own between program data block stream buffer window length distance table value code file archive entry header offset"
        ).split(' ')

    private val russian = (
        "и в не на я что он с как а то это по но она к у же вы из за так от мы бы о все его ты для да только было уже был ее или ни когда " +
            "даже если они нет вот чтобы до там может есть себя мне него при сказал очень время можно теперь тут где раз ничего потом их надо " +
            "человек будет здесь тогда жизнь день один чем глаза дело после был лицо рука дом слово работа место город вопрос сторона голова " +
            "данные файл архив поток блок таблица длина смещение запись заголовок страница книга глава текст строка символ кодировка"
        ).split(' ')

    private val identifiers = "value index buffer result count offset length size data table entry name reader writer state position limit".split(' ')
    private val types = "Int Long String ByteArray IntArray Boolean List<String> Map<String, Int>".split(' ')

    private val cache = HashMap<Pair<SampleKind, Int>, ByteArray>()

    fun get(kind: SampleKind, size: Int): ByteArray = cache.getOrPut(kind to size) {
        when (kind) {
            SampleKind.RANDOM -> Random(size).nextBytes(size)
            SampleKind.ENGLISH -> prose(english, size, 1)
            SampleKind.RUSSIAN -> prose(russian, size, 2)
            SampleKind.SOURCE -> source(size)
            SampleKind.RUNS -> runs(size)
        }
    }

    private fun prose(words: List<String>, size: Int, seed: Int): ByteArray {
        val random = Random(seed)
        val sb = StringBuilder()
        var line = 0
        var sentenceStart = true
        while (sb.length < size) {
            val r = random.nextDouble()
            var word = words[(words.size * r * r * r).toInt()]
            if (sentenceStart) word = word.replaceFirstChar { it.uppercaseChar() }
            sb.append(word)
            line += word.length + 1
            sentenceStart = false
            when (random.nextInt(20)) {
                0 -> {
                    sb.append('.')
                    sentenceStart = true
                }
                1 -> sb.append(',')
                2 -> sb.append(' ').append(random.nextInt(2000))
            }
            if (line > 72) {
                sb.append('\n')
                line = 0
            } else {
                sb.append(' ')
            }
        }
        return sb.toString().encodeToByteArray().copyOf(size)
    }

    private fun source(size: Int): ByteArray {
        val random = Random(3)
        val sb = StringBuilder()
        fun id() = identifiers[random.nextInt(identifiers.size)] + if (random.nextInt(3) == 0) random.nextInt(10).toString() else ""
        while (sb.length < size) {
            val name = id()
            sb.append("    fun ").append(name).append("(").append(id()).append(": ").append(types[random.nextInt(types.size)])
                .append(", ").append(id()).append(": Int): ").append(types[random.nextInt(types.size)]).append(" {\n")
            repeat(2 + random.nextInt(6)) {
                when (random.nextInt(5)) {
                    0 -> sb.append("        val ").append(id()).append(" = ").append(id()).append(" + ").append(random.nextInt(256)).append('\n')
                    1 -> sb.append("        if (").append(id()).append(" >= ").append(id()).append(".size) return ").append(id()).append('\n')
                    2 -> sb.append("        for (i in 0 until ").append(id()).append(") ").append(id()).append("[i] = ").append(id()).append("[i]\n")
                    3 -> sb.append("        // ").append(id()).append(" is ").append(english[random.nextInt(40)]).append(' ').append(id()).append('\n')
                    else -> sb.append("        ").append(id()).append(".add(").append(id()).append(", \"").append(id()).append("\")\n")
                }
            }
            sb.append("        return ").append(name).append("\n    }\n\n")
        }
        return sb.toString().encodeToByteArray().copyOf(size)
    }

    private fun runs(size: Int): ByteArray {
        val random = Random(4)
        val out = ByteArray(size)
        var p = 0
        while (p < size) {
            val period = random.nextInt(1, 9)
            val pattern = random.nextBytes(period)
            val length = minOf(size - p, random.nextInt(1, 2000))
            for (k in 0 until length) out[p + k] = pattern[k % period]
            p += length
        }
        return out
    }
}

fun jdkDeflate(data: ByteArray, level: Int, strategy: Int = Deflater.DEFAULT_STRATEGY, nowrap: Boolean = true): ByteArray {
    val deflater = Deflater(level, nowrap)
    deflater.setStrategy(strategy)
    deflater.setInput(data)
    deflater.finish()
    val out = ByteArrayOutputStream(data.size / 2 + 64)
    val buffer = ByteArray(1 shl 16)
    while (!deflater.finished()) out.write(buffer, 0, deflater.deflate(buffer))
    deflater.end()
    return out.toByteArray()
}

fun jdkInflate(data: ByteArray, nowrap: Boolean = true): ByteArray {
    val inflater = Inflater(nowrap)
    inflater.setInput(if (nowrap) data + 0.toByte() else data)
    val out = ByteArrayOutputStream(data.size * 3 + 64)
    val buffer = ByteArray(1 shl 16)
    while (!inflater.finished()) {
        val n = inflater.inflate(buffer)
        check(n > 0 || inflater.finished() || !(inflater.needsInput() || inflater.needsDictionary())) { "JDK inflater ran out of input" }
        out.write(buffer, 0, n)
    }
    inflater.end()
    return out.toByteArray()
}
