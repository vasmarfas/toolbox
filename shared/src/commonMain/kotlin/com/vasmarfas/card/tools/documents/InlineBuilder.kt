package com.vasmarfas.card.tools.documents

internal val PLAIN = Inline.Text("")

internal val BOM = Char(0xFEFF)

internal fun Inline.Text.sameStyle(other: Inline.Text): Boolean =
    bold == other.bold && italic == other.italic && underline == other.underline && strike == other.strike &&
        code == other.code && script == other.script && link == other.link

internal class InlineBuilder {
    private val items = ArrayList<Inline>()
    private val run = StringBuilder()
    private var runStyle = PLAIN
    private var pendingSpace: Inline.Text? = null
    private var lineStart = true
    private var visible = false

    val isEmpty: Boolean get() = !visible

    fun collapsed(value: String, style: Inline.Text) {
        for (c in value) {
            if (c == ' ' || c == '\n' || c == '\t' || c == '\r' || c == '\u000C') {
                if (!lineStart && pendingSpace == null) pendingSpace = style
            } else if (c >= ' ') {
                pendingSpace?.let {
                    put(' ', it)
                    pendingSpace = null
                }
                put(c, style)
            }
        }
    }

    fun preserved(value: String, style: Inline.Text) {
        if (value.isEmpty()) return
        pendingSpace?.let {
            put(' ', it)
            pendingSpace = null
        }
        for (c in value) {
            when {
                c == '\n' -> lineBreak()
                c >= ' ' || c == '\t' -> put(c, style)
            }
        }
    }

    fun inline(item: Inline) {
        when (item) {
            is Inline.Text -> preserved(item.text, item)
            Inline.LineBreak -> lineBreak()
        }
    }

    fun lineBreak() {
        pendingSpace = null
        trimEnd()
        flushRun()
        items.add(Inline.LineBreak)
        lineStart = true
    }

    fun build(): List<Inline> {
        pendingSpace = null
        flushRun()
        while (true) {
            trimEnd()
            if (items.isNotEmpty() && items[items.lastIndex] == Inline.LineBreak) items.removeAt(items.lastIndex) else break
        }
        val result = if (visible) ArrayList(items) else emptyList()
        items.clear()
        lineStart = true
        visible = false
        return result
    }

    private fun put(c: Char, style: Inline.Text) {
        if (run.isNotEmpty() && !runStyle.sameStyle(style)) flushRun()
        if (run.isEmpty()) {
            val last = items.lastOrNull()
            if (last is Inline.Text && last.sameStyle(style)) {
                items.removeAt(items.lastIndex)
                run.append(last.text)
            }
            runStyle = style
        }
        run.append(c)
        lineStart = false
        if (!c.isWhitespace()) visible = true
    }

    private fun flushRun() {
        if (run.isEmpty()) return
        items.add(runStyle.copy(text = run.toString()))
        run.clear()
    }

    private fun trimEnd() {
        flushRun()
        while (items.isNotEmpty()) {
            val last = items[items.lastIndex] as? Inline.Text ?: return
            val trimmed = last.text.trimEnd()
            items.removeAt(items.lastIndex)
            if (trimmed.isNotEmpty()) {
                items.add(last.copy(text = trimmed))
                return
            }
        }
    }
}

internal class RunProps {
    var bold: Boolean? = null
    var italic: Boolean? = null
    var underline: Boolean? = null
    var strike: Boolean? = null
    var script: Script? = null
    var mono: Boolean? = null
    var hidden: Boolean? = null
    var font: String? = null

    fun merge(other: RunProps): RunProps {
        other.bold?.let { bold = it }
        other.italic?.let { italic = it }
        other.underline?.let { underline = it }
        other.strike?.let { strike = it }
        other.script?.let { script = it }
        other.mono?.let { mono = it }
        other.hidden?.let { hidden = it }
        other.font?.let { font = it }
        return this
    }

    fun copy(): RunProps = RunProps().merge(this)

    // link text is underlined by the link style anyway, that underline is not the author's
    fun style(link: String?): Inline.Text = Inline.Text(
        text = "",
        bold = bold == true,
        italic = italic == true,
        underline = underline == true && link == null,
        strike = strike == true,
        code = mono == true,
        script = script ?: Script.NORMAL,
        link = link,
    )
}

internal fun List<Inline>.plainText(): String = buildString {
    for (item in this@plainText) {
        when (item) {
            is Inline.Text -> append(item.text)
            Inline.LineBreak -> append('\n')
        }
    }
}

private val MONOSPACE_HINTS = listOf(
    "courier", "consolas", "menlo", "monaco", "lucida console", "typewriter", "source code", "fira code", "cascadia",
    "inconsolata", "fixedsys", "ocr a", "ocr-a", "letter gothic", "prestige elite",
)

// Monotype Corsiva is not monospace
internal fun isMonospaceFont(family: String?): Boolean {
    if (family.isNullOrBlank()) return false
    val first = family.substringBefore(',').trim().trim('"', '\'').lowercase()
    if (first.contains("mono") && !first.contains("monotype") && !first.contains("monoton")) return true
    return first == "hack" || first == "terminal" || MONOSPACE_HINTS.any { first.contains(it) }
}
