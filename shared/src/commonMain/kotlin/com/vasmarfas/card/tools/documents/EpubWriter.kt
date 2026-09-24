package com.vasmarfas.card.tools.documents

import com.vasmarfas.card.core.XmlBuilder
import com.vasmarfas.card.core.XmlException
import com.vasmarfas.card.core.ZipWriter
import com.vasmarfas.card.core.parseXml
import com.vasmarfas.card.tools.developer.Sha256
import com.vasmarfas.card.tools.developer.toHex

private const val XHTML_PROLOG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE html>\n"
private const val XHTML_NS = "http://www.w3.org/1999/xhtml"
private const val OPS_NS = "http://www.idpf.org/2007/ops"

// the model has no date, a fixed one keeps equal documents byte-equal
private const val MODIFIED = "2026-01-01T00:00:00Z"

private val EPUB_IMAGE_TYPES = mapOf("image/jpeg" to "jpg", "image/png" to "png", "image/gif" to "gif", "image/svg+xml" to "svg")

// EPUB 3 plus an NCX for EPUB 2 reading systems
internal class EpubWriter(private val doc: Doc) {
    private class TocEntry(val level: Int, val label: String, val href: String) {
        val children = ArrayList<TocEntry>()
    }

    private class Image(val path: String, val type: String, val bytes: ByteArray)

    private val images = ArrayList<Image>()
    private val imageByContent = HashMap<Int, MutableList<Image>>()
    private val toc = ArrayList<TocEntry>()
    private var headingCount = 0
    private val language = languageOf(doc)
    private val title = documentTitle(doc)

    fun write(): ByteArray {
        val chapters = chapters(doc.blocks)
        val files = chapters.mapIndexed { i, blocks -> "chapter-${i + 1}.xhtml" to chapter(blocks, "chapter-${i + 1}.xhtml") }
        if (toc.isEmpty()) {
            files.forEachIndexed { i, (name, _) -> toc.add(TocEntry(1, fallbackLabel(chapters[i], i), name)) }
        }
        val uid = uid(files.map { it.second })
        val zip = ZipWriter()
        zip.add("mimetype", "application/epub+zip".encodeToByteArray(), compress = false)
        zip.add("META-INF/container.xml", container().encodeToByteArray())
        zip.add("OEBPS/content.opf", opf(uid, files.map { it.first }).encodeToByteArray())
        zip.add("OEBPS/nav.xhtml", nav().encodeToByteArray())
        zip.add("OEBPS/toc.ncx", ncx(uid).encodeToByteArray())
        zip.add("OEBPS/style.css", BASE_CSS.encodeToByteArray())
        for ((name, content) in files) zip.add("OEBPS/$name", content.encodeToByteArray())
        for (image in images) zip.add("OEBPS/${image.path}", image.bytes)
        return zip.toByteArray()
    }

    private fun chapters(blocks: List<Block>): List<List<Block>> {
        val result = ArrayList<List<Block>>()
        var current = ArrayList<Block>()
        for (block in blocks) {
            val split = block == Block.PageBreak || (block is Block.Heading && block.level == 1)
            if (split && current.isNotEmpty()) {
                result.add(current)
                current = ArrayList()
            }
            if (block != Block.PageBreak) current.add(block)
        }
        if (current.isNotEmpty() || result.isEmpty()) result.add(current)
        return result
    }

    private fun chapter(blocks: List<Block>, file: String): String {
        val xml = XmlBuilder(declaration = false)
        xml.start("html", "xmlns" to XHTML_NS, "xmlns:epub" to OPS_NS, "lang" to language, "xml:lang" to language).text("\n")
        xml.start("head").text("\n")
        xml.leaf("title", chapterTitle(blocks)).text("\n")
        xml.leaf("link", null, "rel" to "stylesheet", "type" to "text/css", "href" to "style.css").text("\n")
        xml.end().text("\n").start("body").text("\n")
        HtmlBody(
            xml,
            image = ::imagePath,
            headingId = { heading ->
                val id = "h${++headingCount}"
                heading.content.plainText().collapseSpaces().takeIf { it.isNotEmpty() }?.let { addToc(heading.level, it, "$file#$id") }
                id
            },
            link = { externalLink(it)?.let(::uriSafe) },
        ).blocks(blocks)
        xml.end().text("\n").end()
        return "$XHTML_PROLOG$xml\n"
    }

    private fun chapterTitle(blocks: List<Block>): String =
        blocks.firstNotNullOfOrNull { (it as? Block.Heading)?.content?.plainText()?.collapseSpaces()?.takeIf(String::isNotEmpty) } ?: title

    private fun fallbackLabel(blocks: List<Block>, index: Int): String {
        if (index == 0 && doc.title != null) return title
        val text = blocksText(blocks, 200).collapseSpaces()
        return if (text.isEmpty()) "${index + 1}" else if (text.length <= 60) text else text.take(60).substringBeforeLast(' ') + "…"
    }

    private fun addToc(level: Int, label: String, href: String) {
        val entry = TocEntry(level.coerceIn(1, 6), label, href)
        var siblings = toc
        while (siblings.isNotEmpty() && siblings.last().level < entry.level) siblings = siblings.last().children
        siblings.add(entry)
    }

    private fun imagePath(picture: Block.Picture): String? {
        val type = ImageHeader.mimeType(picture.bytes) ?: return null
        val extension = EPUB_IMAGE_TYPES[type] ?: return null
        if (type == "image/svg+xml" && !isWellFormed(picture.bytes)) return null
        val key = picture.bytes.contentHashCode()
        imageByContent[key]?.firstOrNull { it.bytes.contentEquals(picture.bytes) }?.let { return it.path }
        val image = Image("images/image-${images.size + 1}.$extension", type, picture.bytes)
        images.add(image)
        imageByContent.getOrPut(key) { ArrayList() }.add(image)
        return image.path
    }

    private fun isWellFormed(bytes: ByteArray): Boolean = try {
        parseXml(bytes.decodeToString())
        true
    } catch (e: XmlException) {
        false
    }

    // RFC 9562 version 8 UUID from SHA-256, so the identifier depends on the content only
    private fun uid(chapters: List<String>): String {
        val sb = StringBuilder()
        sb.append(title).append('\u0000').append(doc.author.orEmpty()).append('\u0000').append(language)
        for (c in chapters) sb.append('\u0000').append(c)
        for (image in images) sb.append('\u0000').append(image.bytes.size).append(':').append(image.bytes.contentHashCode())
        val hash = Sha256.digest(sb.toString().encodeToByteArray()).copyOf(16)
        hash[6] = ((hash[6].toInt() and 0x0F) or 0x80).toByte()
        hash[8] = ((hash[8].toInt() and 0x3F) or 0x80).toByte()
        val hex = hash.toHex()
        return "urn:uuid:${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20)}"
    }

    private fun container(): String = XmlBuilder()
        .start("container", "version" to "1.0", "xmlns" to "urn:oasis:names:tc:opendocument:xmlns:container")
        .start("rootfiles")
        .leaf("rootfile", null, "full-path" to "OEBPS/content.opf", "media-type" to "application/oebps-package+xml")
        .end()
        .end()
        .toString()

    private fun opf(uid: String, chapters: List<String>): String {
        val xml = XmlBuilder()
        xml.start("package", "xmlns" to "http://www.idpf.org/2007/opf", "version" to "3.0", "unique-identifier" to "uid", "xml:lang" to language)
        xml.start("metadata", "xmlns:dc" to "http://purl.org/dc/elements/1.1/")
        xml.leaf("dc:identifier", uid, "id" to "uid")
        xml.leaf("dc:title", title)
        doc.author?.takeIf { it.isNotBlank() }?.let { xml.leaf("dc:creator", it) }
        xml.leaf("dc:language", language)
        xml.leaf("meta", MODIFIED, "property" to "dcterms:modified")
        xml.end()
        xml.start("manifest")
        xml.leaf("item", null, "id" to "nav", "href" to "nav.xhtml", "media-type" to "application/xhtml+xml", "properties" to "nav")
        xml.leaf("item", null, "id" to "ncx", "href" to "toc.ncx", "media-type" to "application/x-dtbncx+xml")
        xml.leaf("item", null, "id" to "css", "href" to "style.css", "media-type" to "text/css")
        chapters.forEachIndexed { i, name -> xml.leaf("item", null, "id" to "c${i + 1}", "href" to name, "media-type" to "application/xhtml+xml") }
        images.forEachIndexed { i, image -> xml.leaf("item", null, "id" to "img${i + 1}", "href" to image.path, "media-type" to image.type) }
        xml.end()
        xml.start("spine", "toc" to "ncx")
        chapters.indices.forEach { xml.leaf("itemref", null, "idref" to "c${it + 1}") }
        xml.end()
        xml.end()
        return xml.toString()
    }

    private fun nav(): String {
        val xml = XmlBuilder(declaration = false)
        xml.start("html", "xmlns" to XHTML_NS, "xmlns:epub" to OPS_NS, "lang" to language, "xml:lang" to language).text("\n")
        xml.start("head").leaf("title", title).end().text("\n")
        xml.start("body").text("\n").start("nav", "epub:type" to "toc", "id" to "toc").text("\n")
        navList(xml, toc)
        xml.end().text("\n").end().text("\n").end()
        return "$XHTML_PROLOG$xml\n"
    }

    private fun navList(xml: XmlBuilder, entries: List<TocEntry>) {
        xml.start("ol")
        for (entry in entries) {
            xml.start("li").leaf("a", entry.label, "href" to entry.href)
            if (entry.children.isNotEmpty()) navList(xml, entry.children)
            xml.end()
        }
        xml.end()
    }

    private fun ncx(uid: String): String {
        val xml = XmlBuilder()
        xml.start("ncx", "xmlns" to "http://www.daisy.org/z3986/2005/ncx/", "version" to "2005-1", "xml:lang" to language)
        xml.start("head")
        xml.leaf("meta", null, "name" to "dtb:uid", "content" to uid)
        xml.leaf("meta", null, "name" to "dtb:depth", "content" to depth(toc).toString())
        xml.leaf("meta", null, "name" to "dtb:totalPageCount", "content" to "0")
        xml.leaf("meta", null, "name" to "dtb:maxPageNumber", "content" to "0")
        xml.end()
        xml.start("docTitle").leaf("text", title).end()
        doc.author?.takeIf { it.isNotBlank() }?.let { xml.start("docAuthor").leaf("text", it).end() }
        xml.start("navMap")
        var order = 0
        fun points(entries: List<TocEntry>) {
            for (entry in entries) {
                order++
                xml.start("navPoint", "id" to "np$order", "playOrder" to order.toString())
                xml.start("navLabel").leaf("text", entry.label).end()
                xml.leaf("content", null, "src" to entry.href)
                points(entry.children)
                xml.end()
            }
        }
        points(toc)
        xml.end()
        xml.end()
        return xml.toString()
    }

    private fun depth(entries: List<TocEntry>): Int = if (entries.isEmpty()) 0 else 1 + entries.maxOf { depth(it.children) }
}
