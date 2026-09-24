package com.vasmarfas.card.tools.documents

private const val TEXT = 0
private const val CODE = 1
private const val SOFT_BREAK = 2
private const val HARD_BREAK = 3
private const val DELIMITER = 4
private const val EMPHASIS = 5
private const val STRONG = 6
private const val STRIKE = 7
private const val LINK = 8
private const val IMAGE = 9
private const val TAG = 10

private val BARE_LINK = Regex("""(?:https?://|www\.)[^\s<]+""", RegexOption.IGNORE_CASE)
private val EMAIL_AUTOLINK = Regex("""[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*""")
private val STYLE_TAGS = setOf("b", "strong", "i", "em", "u", "ins", "s", "del", "strike", "sub", "sup", "code", "kbd", "samp", "tt", "br")

internal class LinkReference(val url: String)

internal fun normalizeLabel(label: String): String = label.trim().lowercase().collapseSpaces()

internal class MarkdownInline(private val src: String, private val refs: Map<String, LinkReference>) {
    private class Node(val kind: Int) {
        var prev: Node? = null
        var next: Node? = null
        var text = ""
        var char = ' '
        var count = 0
        var original = 0
        var canOpen = false
        var canClose = false
        var prevDelimiter: Node? = null
        var nextDelimiter: Node? = null
        var first: Node? = null
        var last: Node? = null
        var url = ""
        var closing = false
        var literal = false
    }

    // bracketAfter: another bracket opened inside, so this is no link label and no shortcut reference
    private class Bracket(val node: Node, val image: Boolean, val previousDelimiter: Node?, val contentStart: Int) {
        var active = true
        var bracketAfter = false
    }

    private var head: Node? = null
    private var tail: Node? = null
    private var lastDelimiter: Node? = null
    private val brackets = ArrayList<Bracket>()
    private val pending = StringBuilder()
    private var pos = 0
    private val backtickRuns = HashMap<Int, ArrayList<Int>>()
    private val backtickCursor = HashMap<Int, Int>()
    private val missing = HashMap<String, Int>()

    fun render(segments: Segments) {
        parse()
        emit(head, segments)
    }

    private fun parse() {
        indexBackticks()
        val n = src.length
        while (pos < n) {
            when (val c = src[pos]) {
                '\\' -> escape()
                '`' -> codeSpan()
                '*', '_', '~' -> delimiterRun(c)
                '[' -> openBracket(image = false, length = 1)
                '!' -> if (pos + 1 < n && src[pos + 1] == '[') openBracket(image = true, length = 2) else plain(c)
                ']' -> closeBracket()
                '<' -> angle()
                '&' -> entity()
                '\n' -> lineEnd()
                else -> {
                    var end = pos + 1
                    while (end < n && src[end] !in "\\`*_~[]!<&\n") end++
                    pending.append(src, pos, end)
                    pos = end
                }
            }
        }
        flushText()
        processEmphasis(null)
    }

    private fun plain(c: Char) {
        pending.append(c)
        pos++
    }

    private fun flushText(literal: Boolean = false) {
        if (pending.isEmpty()) return
        val node = Node(TEXT)
        node.text = pending.toString()
        node.literal = literal
        pending.clear()
        append(node)
    }

    private fun add(node: Node) {
        flushText()
        append(node)
    }

    private fun append(node: Node) {
        node.prev = tail
        node.next = null
        if (tail == null) head = node else tail!!.next = node
        tail = node
    }

    private fun escape() {
        val next = if (pos + 1 < src.length) src[pos + 1] else ' '
        when {
            next == '\n' -> {
                add(Node(HARD_BREAK))
                pos += 2
                skipLeadingSpaces()
            }
            isAsciiPunctuation(next) -> {
                flushText()
                pending.append(next)
                flushText(literal = true)
                pos += 2
            }
            else -> plain('\\')
        }
    }

    private fun lineEnd() {
        var spaces = 0
        while (pending.isNotEmpty() && pending[pending.length - 1] == ' ') {
            pending.setLength(pending.length - 1)
            spaces++
        }
        add(Node(if (spaces >= 2) HARD_BREAK else SOFT_BREAK))
        pos++
        skipLeadingSpaces()
    }

    private fun skipLeadingSpaces() {
        while (pos < src.length && (src[pos] == ' ' || src[pos] == '\t')) pos++
    }

    private fun indexBackticks() {
        var i = 0
        while (i < src.length) {
            if (src[i] == '`') {
                val start = i
                while (i < src.length && src[i] == '`') i++
                backtickRuns.getOrPut(i - start) { ArrayList() }.add(start)
            } else {
                i++
            }
        }
    }

    private fun codeSpan() {
        val start = pos
        while (pos < src.length && src[pos] == '`') pos++
        val length = pos - start
        val runs = backtickRuns[length]
        var k = backtickCursor[length] ?: 0
        while (runs != null && k < runs.size && runs[k] < pos) k++
        backtickCursor[length] = k
        if (runs == null || k >= runs.size) {
            pending.append(src, start, pos)
            return
        }
        val close = runs[k]
        var content = src.substring(pos, close).replace('\n', ' ')
        if (content.length >= 2 && content.startsWith(' ') && content.endsWith(' ') && content.isNotBlank()) content = content.substring(1, content.length - 1)
        val node = Node(CODE)
        node.text = content
        add(node)
        pos = close + length
    }

    private fun delimiterRun(c: Char) {
        val start = pos
        while (pos < src.length && src[pos] == c) pos++
        val count = pos - start
        if (c == '~' && count > 2) {
            pending.append(src, start, pos)
            return
        }
        val before = if (start == 0) '\n' else src[start - 1]
        val after = if (pos >= src.length) '\n' else src[pos]
        val left = !after.isWhitespace() && (!isPunctuation(after) || before.isWhitespace() || isPunctuation(before))
        val right = !before.isWhitespace() && (!isPunctuation(before) || after.isWhitespace() || isPunctuation(after))
        val node = Node(DELIMITER)
        node.char = c
        node.count = count
        node.original = count
        if (c == '_') {
            node.canOpen = left && (!right || isPunctuation(before))
            node.canClose = right && (!left || isPunctuation(after))
        } else {
            node.canOpen = left
            node.canClose = right
        }
        add(node)
        node.prevDelimiter = lastDelimiter
        lastDelimiter?.nextDelimiter = node
        lastDelimiter = node
    }

    private fun openBracket(image: Boolean, length: Int) {
        val node = Node(TEXT)
        node.text = if (image) "![" else "["
        node.literal = true
        add(node)
        pos += length
        brackets.lastOrNull()?.bracketAfter = true
        brackets.add(Bracket(node, image, lastDelimiter, pos))
    }

    private fun closeBracket() {
        val opener = brackets.lastOrNull()
        if (opener == null) {
            plain(']')
            return
        }
        if (!opener.active) {
            brackets.removeAt(brackets.lastIndex)
            plain(']')
            return
        }
        val labelEnd = pos
        var end = -1
        var url: String? = null
        if (labelEnd + 1 < src.length && src[labelEnd + 1] == '(') {
            inlineDestination(labelEnd + 1)?.let { (u, e) ->
                url = u
                end = e
            }
        }
        if (url == null) {
            val after = labelEnd + 1
            var label = if (opener.bracketAfter || labelEnd - opener.contentStart > 999) null else src.substring(opener.contentStart, labelEnd)
            var labelStop = after
            if (after < src.length && src[after] == '[') {
                val close = labelClose(after)
                if (close > 0) {
                    val explicit = src.substring(after + 1, close)
                    if (explicit.isNotBlank()) label = explicit
                    labelStop = close + 1
                }
            }
            label?.let { refs[normalizeLabel(it)] }?.let {
                url = it.url
                end = labelStop
            }
        }
        val destination = url
        if (destination == null) {
            brackets.removeAt(brackets.lastIndex)
            plain(']')
            return
        }
        flushText()
        processEmphasis(opener.previousDelimiter)
        val container = Node(if (opener.image) IMAGE else LINK)
        container.url = destination
        val inner = opener.node.next
        if (inner != null) {
            container.first = inner
            container.last = tail
            inner.prev = null
        }
        tail = opener.node
        opener.node.next = null
        remove(opener.node)
        append(container)
        brackets.removeAt(brackets.lastIndex)
        if (!opener.image) {
            for (i in brackets.indices.reversed()) {
                val b = brackets[i]
                if (b.image) continue
                if (!b.active) break
                b.active = false
            }
        }
        pos = end
    }

    private fun labelClose(open: Int): Int {
        var i = open + 1
        while (i < src.length && i - open <= 1000) {
            when (src[i]) {
                '\\' -> i++
                '[' -> return -1
                ']' -> return i
            }
            i++
        }
        return -1
    }

    private fun inlineDestination(open: Int): Pair<String, Int>? {
        var i = skipSpace(open + 1)
        val dest = StringBuilder()
        if (i < src.length && src[i] == '<') {
            i++
            while (i < src.length && src[i] != '>') {
                if (src[i] == '\n' || src[i] == '<') return null
                if (src[i] == '\\' && i + 1 < src.length && isAsciiPunctuation(src[i + 1])) i++
                dest.append(src[i])
                i++
            }
            if (i >= src.length) return null
            i++
        } else {
            var depth = 0
            while (i < src.length) {
                val c = src[i]
                if (c.isWhitespace() || c < ' ') break
                if (c == '\\' && i + 1 < src.length && isAsciiPunctuation(src[i + 1])) {
                    dest.append(src[i + 1])
                    i += 2
                    continue
                }
                if (c == '(') {
                    depth++
                    if (depth > 32) return null
                }
                if (c == ')') {
                    if (depth == 0) break
                    depth--
                }
                dest.append(c)
                i++
            }
            if (depth != 0) return null
        }
        val beforeTitle = i
        i = skipSpace(i)
        if (i < src.length && (src[i] == '"' || src[i] == '\'' || src[i] == '(')) {
            if (i == beforeTitle) return null
            val close = (if (src[i] == '(') ')' else src[i]).toString()
            var end = find(close, i + 1)
            while (end > 0 && escaped(end)) end = find(close, end + 1)
            if (end < 0) return null
            i = skipSpace(end + 1)
        }
        if (i >= src.length || src[i] != ')') return null
        return HtmlEntities.decode(dest.toString()) to i + 1
    }

    private fun escaped(at: Int): Boolean {
        var slashes = 0
        var k = at - 1
        while (k >= 0 && src[k] == '\\') {
            slashes++
            k--
        }
        return slashes % 2 == 1
    }

    private fun skipSpace(from: Int): Int {
        var i = from
        var newlines = 0
        while (i < src.length && (src[i] == ' ' || src[i] == '\t' || src[i] == '\n')) {
            if (src[i] == '\n' && ++newlines > 1) break
            i++
        }
        return i
    }

    private fun angle() {
        var j = pos + 1
        while (j < src.length && src[j] > ' ' && src[j] != '<' && src[j] != '>') j++
        if (j < src.length && src[j] == '>' && j > pos + 1) {
            val inner = src.substring(pos + 1, j)
            val uri = isUriAutolink(inner)
            if (uri || EMAIL_AUTOLINK.matches(inner)) {
                val link = Node(LINK)
                link.url = if (uri) inner else "mailto:$inner"
                val text = Node(TEXT)
                text.text = inner
                text.literal = true
                link.first = text
                link.last = text
                add(link)
                pos = j + 1
                return
            }
        }
        val end = htmlEnd(pos)
        if (end < 0) {
            plain('<')
            return
        }
        val raw = src.substring(pos, end)
        pos = end
        if (raw.startsWith("</") || raw[1].isLetter()) {
            val closing = raw.startsWith("</")
            var k = if (closing) 2 else 1
            val nameStart = k
            while (k < raw.length && (raw[k].isLetterOrDigit() || raw[k] == '-')) k++
            val name = raw.substring(nameStart, k).lowercase()
            if (name in STYLE_TAGS) {
                val tag = Node(TAG)
                tag.text = name
                tag.closing = closing
                add(tag)
            }
        }
    }

    // remembers misses: a needle absent after some position is absent after any later one
    private fun find(needle: String, from: Int): Int {
        val known = missing[needle]
        if (known != null && known <= from) return -1
        val found = src.indexOf(needle, from)
        if (found < 0) missing[needle] = if (known == null) from else minOf(known, from)
        return found
    }

    private fun isUriAutolink(inner: String): Boolean {
        val colon = inner.indexOf(':')
        if (colon < 2 || colon > 32 || !inner[0].isAsciiLetter()) return false
        for (i in 1 until colon) if (!(inner[i].isAsciiLetter() || inner[i].isDigit() || inner[i] in "+.-")) return false
        return inner.none { it <= ' ' || it == '<' || it == '>' }
    }

    private fun htmlEnd(start: Int): Int {
        val n = src.length
        if (src.startsWith("<!--", start)) return find("-->", start + 4).let { if (it < 0) -1 else it + 3 }
        if (src.startsWith("<?", start)) return find("?>", start + 2).let { if (it < 0) -1 else it + 2 }
        if (src.startsWith("<![CDATA[", start)) return find("]]>", start + 9).let { if (it < 0) -1 else it + 3 }
        if (src.startsWith("<!", start) && start + 2 < n && src[start + 2].isAsciiLetter()) return find(">", start).let { if (it < 0) -1 else it + 1 }
        var i = start + 1
        val closing = i < n && src[i] == '/'
        if (closing) i++
        if (i >= n || !src[i].isAsciiLetter()) return -1
        while (i < n && (src[i].isAsciiLetter() || src[i].isDigit() || src[i] == '-')) i++
        if (closing) {
            while (i < n && src[i].isWhitespace()) i++
            return if (i < n && src[i] == '>') i + 1 else -1
        }
        while (true) {
            val spaceStart = i
            while (i < n && src[i].isWhitespace()) i++
            if (i >= n) return -1
            if (src[i] == '>') return i + 1
            if (src[i] == '/') return if (i + 1 < n && src[i + 1] == '>') i + 2 else -1
            if (i == spaceStart) return -1
            if (!(src[i].isAsciiLetter() || src[i] == '_' || src[i] == ':')) return -1
            while (i < n && (src[i].isAsciiLetter() || src[i].isDigit() || src[i] in "_.:-")) i++
            var j = i
            while (j < n && src[j].isWhitespace()) j++
            if (j < n && src[j] == '=') {
                j++
                while (j < n && src[j].isWhitespace()) j++
                if (j >= n) return -1
                when (src[j]) {
                    '"', '\'' -> {
                        val close = find(src[j].toString(), j + 1)
                        if (close < 0) return -1
                        j = close + 1
                    }
                    else -> {
                        val valueStart = j
                        while (j < n && !src[j].isWhitespace() && src[j] !in "\"'=<>`") j++
                        if (j == valueStart) return -1
                    }
                }
                i = j
            }
        }
    }

    private fun entity() {
        var j = pos + 1
        while (j < src.length && j - pos < 34 && (src[j].isLetterOrDigit() || src[j] == '#')) j++
        if (j < src.length && src[j] == ';' && j > pos + 1) {
            val raw = src.substring(pos, j + 1)
            val decoded = HtmlEntities.decode(raw)
            if (decoded != raw) {
                flushText()
                pending.append(decoded)
                flushText(literal = true)
                pos = j + 1
                return
            }
        }
        plain('&')
    }

    private fun processEmphasis(bottom: Node?) {
        val openersBottom = HashMap<Int, Node?>()
        var closer = lastDelimiter
        while (closer != null && closer.prevDelimiter !== bottom) closer = closer.prevDelimiter
        if (closer === bottom) closer = null
        while (closer != null) {
            if (!closer.canClose) {
                closer = closer.nextDelimiter
                continue
            }
            val key = (closer.char.code * 2 + (if (closer.canOpen) 1 else 0)) * 3 + closer.original % 3
            val limit = if (openersBottom.containsKey(key)) openersBottom[key] else bottom
            var opener = closer.prevDelimiter
            while (opener != null && opener !== bottom && opener !== limit) {
                if (opener.char == closer.char && opener.canOpen && matches(opener, closer)) break
                opener = opener.prevDelimiter
            }
            if (opener != null && opener !== bottom && opener !== limit) {
                val use = if (closer.char == '~') closer.count else if (closer.count >= 2 && opener.count >= 2) 2 else 1
                val container = Node(if (closer.char == '~') STRIKE else if (use == 2) STRONG else EMPHASIS)
                opener.count -= use
                closer.count -= use
                val first = opener.next
                if (first != null && first !== closer) {
                    container.first = first
                    container.last = closer.prev
                    first.prev = null
                    closer.prev!!.next = null
                }
                opener.next = container
                container.prev = opener
                container.next = closer
                closer.prev = container
                opener.nextDelimiter = closer
                closer.prevDelimiter = opener
                if (opener.count == 0) {
                    remove(opener)
                    removeDelimiter(opener)
                }
                if (closer.count == 0) {
                    val next = closer.nextDelimiter
                    remove(closer)
                    removeDelimiter(closer)
                    closer = next
                }
            } else {
                openersBottom[key] = closer.prevDelimiter
                val next = closer.nextDelimiter
                if (!closer.canOpen) removeDelimiter(closer)
                closer = next
            }
        }
        while (lastDelimiter != null && lastDelimiter !== bottom) removeDelimiter(lastDelimiter!!)
    }

    private fun matches(opener: Node, closer: Node): Boolean {
        if (closer.char == '~') return opener.count == closer.count
        val odd = (closer.canOpen || opener.canClose) && (opener.original + closer.original) % 3 == 0 &&
            !(opener.original % 3 == 0 && closer.original % 3 == 0)
        return !odd
    }

    private fun remove(node: Node) {
        val p = node.prev
        val n = node.next
        if (p == null) head = n else p.next = n
        if (n == null) tail = p else n.prev = p
        node.prev = null
        node.next = null
    }

    private fun removeDelimiter(node: Node) {
        val p = node.prevDelimiter
        val n = node.nextDelimiter
        p?.nextDelimiter = n
        n?.prevDelimiter = p
        if (lastDelimiter === node) lastDelimiter = p
        node.prevDelimiter = null
        node.nextDelimiter = null
    }

    private fun emit(first: Node?, segments: Segments) {
        val tags = IntArray(8)
        val stack = ArrayList<Pair<Node?, Inline.Text>>()
        var node = first
        var style = PLAIN
        val inline = segments.inline
        while (true) {
            if (node == null) {
                if (stack.isEmpty()) return
                val (next, saved) = stack.removeAt(stack.lastIndex)
                node = next
                style = saved
                continue
            }
            val current: Node = node
            node = current.next
            when (current.kind) {
                TEXT -> text(current.text, withTags(style, tags), current.literal, segments)
                DELIMITER -> text(current.char.toString().repeat(current.count), withTags(style, tags), true, segments)
                CODE -> inline.preserved(current.text, withTags(style, tags).copy(code = true))
                SOFT_BREAK -> inline.collapsed(" ", withTags(style, tags))
                HARD_BREAK -> inline.lineBreak()
                EMPHASIS, STRONG, STRIKE, LINK -> {
                    stack.add(node to style)
                    style = when (current.kind) {
                        EMPHASIS -> style.copy(italic = true)
                        STRONG -> style.copy(bold = true)
                        STRIKE -> style.copy(strike = true)
                        else -> externalLink(current.url)?.let { style.copy(link = it) } ?: style
                    }
                    node = current.first
                }
                IMAGE -> image(current, withTags(style, tags), segments)
                TAG -> tag(current, tags, inline)
            }
        }
    }

    private fun tag(node: Node, tags: IntArray, inline: InlineBuilder) {
        val index = when (node.text) {
            "br" -> {
                if (!node.closing) inline.lineBreak()
                return
            }
            "b", "strong" -> 0
            "i", "em" -> 1
            "u", "ins" -> 2
            "s", "del", "strike" -> 3
            "code", "kbd", "samp", "tt" -> 4
            "sub" -> 5
            "sup" -> 6
            else -> return
        }
        tags[index] = maxOf(0, tags[index] + if (node.closing) -1 else 1)
    }

    private fun withTags(style: Inline.Text, tags: IntArray): Inline.Text {
        if (tags.all { it == 0 }) return style
        return style.copy(
            bold = style.bold || tags[0] > 0,
            italic = style.italic || tags[1] > 0,
            underline = style.underline || tags[2] > 0,
            strike = style.strike || tags[3] > 0,
            code = style.code || tags[4] > 0,
            script = if (tags[6] > 0) Script.SUPER else if (tags[5] > 0) Script.SUB else style.script,
        )
    }

    // GFM extended autolinks are found here, in finished text, so escaped or linked text never becomes a link
    private fun text(value: String, style: Inline.Text, literal: Boolean, segments: Segments) {
        val inline = segments.inline
        if (literal || style.link != null || (!value.contains("://") && !value.contains("www.", ignoreCase = true))) {
            inline.collapsed(value, style)
            return
        }
        var last = 0
        for (match in BARE_LINK.findAll(value)) {
            val start = match.range.first
            if (start > 0 && !value[start - 1].isWhitespace() && value[start - 1] !in "*_~(") continue
            var link = match.value
            link = trimAutolink(link)
            if (link.startsWith("www.", ignoreCase = true) && '.' !in link.substring(4)) continue
            if (!link.startsWith("www.", ignoreCase = true) && link.substringAfter("://").isEmpty()) continue
            inline.collapsed(value.substring(last, start), style)
            inline.collapsed(link, style.copy(link = if (link.startsWith("www.", ignoreCase = true)) "http://$link" else link))
            last = start + link.length
        }
        inline.collapsed(value.substring(last), style)
    }

    private fun trimAutolink(link: String): String {
        var end = link.length
        while (end > 0) {
            val c = link[end - 1]
            if (c in "?!.,:*_~'\"") {
                end--
                continue
            }
            if (c == ')') {
                val body = link.substring(0, end)
                if (body.count { it == '(' } < body.count { it == ')' }) {
                    end--
                    continue
                }
            }
            if (c == ';') {
                val amp = link.lastIndexOf('&', end - 1)
                if (amp >= 0 && end - 1 > amp + 1 && (amp + 1 until end - 1).all { link[it].isLetterOrDigit() }) {
                    end = amp
                    continue
                }
            }
            break
        }
        return link.substring(0, end)
    }

    private fun image(node: Node, style: Inline.Text, segments: Segments) {
        val alt = StringBuilder()
        collectAlt(node.first, alt)
        val url = node.url.trim()
        val bytes = if (url.startsWith("data:", ignoreCase = true)) decodeDataUri(url) else null
        if (bytes != null && bytes.isNotEmpty()) {
            segments.block(Block.Picture(bytes, alt = alt.toString().collapseSpaces()))
        } else if (alt.isNotBlank()) {
            segments.inline.collapsed(alt.toString(), style)
        }
    }

    private fun collectAlt(first: Node?, sb: StringBuilder) {
        val stack = ArrayList<Node?>()
        var node = first
        while (true) {
            if (node == null) {
                if (stack.isEmpty()) return
                node = stack.removeAt(stack.lastIndex)
                continue
            }
            val current: Node = node
            node = current.next
            when (current.kind) {
                TEXT, CODE -> sb.append(current.text)
                DELIMITER -> sb.append(current.char.toString().repeat(current.count))
                SOFT_BREAK, HARD_BREAK -> sb.append(' ')
                EMPHASIS, STRONG, STRIKE, LINK, IMAGE -> {
                    stack.add(node)
                    node = current.first
                }
            }
        }
    }
}

private fun Char.isAsciiLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'

internal fun isAsciiPunctuation(c: Char): Boolean = c in "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"

private fun isPunctuation(c: Char): Boolean {
    if (c.code < 128) return isAsciiPunctuation(c)
    return when (c.category) {
        CharCategory.CONNECTOR_PUNCTUATION, CharCategory.DASH_PUNCTUATION, CharCategory.START_PUNCTUATION, CharCategory.END_PUNCTUATION,
        CharCategory.INITIAL_QUOTE_PUNCTUATION, CharCategory.FINAL_QUOTE_PUNCTUATION, CharCategory.OTHER_PUNCTUATION,
        CharCategory.MATH_SYMBOL, CharCategory.CURRENCY_SYMBOL, CharCategory.MODIFIER_SYMBOL, CharCategory.OTHER_SYMBOL,
        -> true
        else -> false
    }
}
