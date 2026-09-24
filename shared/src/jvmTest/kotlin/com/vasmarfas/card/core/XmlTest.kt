package com.vasmarfas.card.core

import org.w3c.dom.Attr
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class XmlTest {
    private val w = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
    private val r = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private val mc = "http://schemas.openxmlformats.org/markup-compatibility/2006"
    private val drawing = "http://schemas.openxmlformats.org/drawingml/2006/main"
    private val picture = "http://schemas.openxmlformats.org/drawingml/2006/picture"
    private val xmlNs = "http://www.w3.org/XML/1998/namespace"
    private val xmlnsNs = "http://www.w3.org/2000/xmlns/"
    private val xlink = "http://www.w3.org/1999/xlink"

    private fun events(xml: String): List<String> {
        val reader = XmlReader(xml)
        val out = ArrayList<String>()
        while (true) {
            val event = reader.next()
            out += when (event) {
                XmlEvent.START_ELEMENT -> "<${reader.name}@${reader.depth}"
                XmlEvent.END_ELEMENT -> "</${reader.name}@${reader.depth}"
                XmlEvent.TEXT -> "'${reader.text}'@${reader.depth}"
                XmlEvent.END_DOCUMENT -> "end@${reader.depth}"
            }
            if (event == XmlEvent.END_DOCUMENT) return out
        }
    }

    private fun javax(xml: String): Element =
        DocumentBuilderFactory.newInstance()
            .apply {
                isNamespaceAware = true
                isCoalescing = true
            }
            .newDocumentBuilder()
            .parse(ByteArrayInputStream(xml.encodeToByteArray()))
            .documentElement

    @Test
    fun readsOoxmlDocument() {
        val xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="$w" xmlns:r="$r" xmlns:mc="$mc" mc:Ignorable="w14">
  <w:body>
    <w:p w:rsidR="00A1B2C3" w:rsidRDefault='00D4E5F6'>
      <w:pPr><w:pStyle w:val="Heading1"/></w:pPr>
      <w:r><w:t xml:space="preserve">Hello, </w:t></w:r>
      <w:r><w:rPr><w:b/></w:rPr><w:t>world &amp; more</w:t></w:r>
      <w:hyperlink r:id="rId5" w:history="1"><w:r><w:t>link</w:t></w:r></w:hyperlink>
      <w:r><w:drawing><graphic xmlns="$drawing"><graphicData uri="$picture"><blip r:embed="rId7"/></graphicData></graphic></w:drawing></w:r>
    </w:p>
    <w:sectPr><w:pgSz w:w="11906" w:h="16838"/></w:sectPr>
  </w:body>
</w:document>"""
        val doc = parseXml(xml)
        assertEquals("w:document", doc.name)
        assertEquals("document", doc.localName)
        assertEquals(w, doc.namespace)
        assertEquals("w14", doc.attr("Ignorable", mc))
        assertEquals(w, doc.attributes.single { it.name == "xmlns:w" }.value)
        assertEquals(xmlnsNs, doc.attributes.single { it.name == "xmlns:w" }.namespace)
        val body = assertNotNull(doc.child("body", w))
        assertEquals(listOf("p", "sectPr"), body.elements.map { it.localName })
        val p = assertNotNull(body.child("p", w))
        assertEquals("00A1B2C3", p.attr("rsidR", w))
        assertEquals("00D4E5F6", p.attr("rsidRDefault"))
        assertNull(p.attr("rsidR", ""))
        val runs = p.children("r", w)
        assertEquals(3, runs.size)
        val graphic = assertNotNull(runs[2].child("drawing", w)?.child("graphic", drawing))
        assertEquals("graphic", graphic.name)
        val graphicData = assertNotNull(graphic.child("graphicData", drawing))
        assertEquals(picture, graphicData.attr("uri", ""))
        assertEquals("rId7", graphicData.child("blip", drawing)?.attr("embed", r))
        val first = assertNotNull(runs[0].child("t", w))
        assertEquals("Hello, ", first.text())
        assertEquals("preserve", first.attr("space", xmlNs))
        assertEquals("world & more", runs[1].child("t")?.text())
        val link = assertNotNull(p.child("hyperlink", w))
        assertEquals("rId5", link.attr("id", r))
        assertNull(link.attr("id", w))
        assertEquals("link", link.text())
        val size = assertNotNull(body.child("sectPr")?.child("pgSz"))
        assertEquals("11906" to "16838", size.attr("w", w) to size.attr("h", w))
        compare(doc, javax(xml))
    }

    @Test
    fun scopesNamespaces() {
        val xml = """<Types xmlns="urn:a" x="1"><Default Extension="xml"/><b xmlns="urn:b"><c/><d xmlns=""><e/></d></b><f/>""" +
            """<p:g xmlns:p="urn:1"><p:h xmlns:p="urn:2" p:k="v"/><p:i/></p:g><q:z/></Types>"""
        val reader = XmlReader(xml)
        val seen = ArrayList<String>()
        while (reader.next() != XmlEvent.END_DOCUMENT) {
            if (reader.name.isNotEmpty()) seen += "${reader.localName}=${reader.namespace}"
        }
        assertEquals(
            listOf(
                "Types=urn:a", "Default=urn:a", "Default=urn:a", "b=urn:b", "c=urn:b", "c=urn:b", "d=", "e=", "e=", "d=", "b=urn:b",
                "f=urn:a", "f=urn:a", "g=urn:1", "h=urn:2", "h=urn:2", "i=urn:1", "i=urn:1", "g=urn:1", "z=", "z=", "Types=urn:a",
            ),
            seen,
        )
        val root = parseXml(xml)
        assertEquals("", root.attributes.single { it.name == "x" }.namespace)
        assertEquals("urn:2", root.child("g")?.child("h")?.attributes?.single { it.localName == "k" }?.namespace)
        val bound = xml.replace("<q:z/>", "")
        compare(parseXml(bound), javax(bound))
    }

    @Test
    fun parsesAttributes() {
        val xml = "<a one=\"1\" two='2' gt=\"a > b\" mixed='say \"hi\"' ws=\"x\ty\nz\r\nw\" " +
            "refs=\"&#9;&#10;&amp;&lt;&quot;&#x1F600;\" empty=\"\" spaced = \"s\" />"
        val reader = XmlReader(xml)
        assertEquals(XmlEvent.START_ELEMENT, reader.next())
        assertEquals(listOf("one", "two", "gt", "mixed", "ws", "refs", "empty", "spaced"), (0 until reader.attributeCount).map { reader.attributeName(it) })
        assertEquals(
            listOf("1", "2", "a > b", "say \"hi\"", "x y z w", "\t\n&<\"\uD83D\uDE00", "", "s"),
            (0 until reader.attributeCount).map { reader.attributeValue(it) },
        )
        assertEquals("a > b", reader.attribute("gt"))
        assertEquals("a > b", reader.attribute("gt", ""))
        assertNull(reader.attribute("gt", "urn:x"))
        assertNull(reader.attribute("missing"))
        assertEquals(XmlEvent.END_ELEMENT, reader.next())
        assertEquals(0, reader.attributeCount)
        assertEquals("a", reader.name)
        assertEquals(XmlEvent.END_DOCUMENT, reader.next())
        assertEquals("x y z w", javax(xml).getAttribute("ws"))
        assertEquals("\t\n&<\"\uD83D\uDE00", javax(xml).getAttribute("refs"))
    }

    @Test
    fun decodesEntities() {
        val xml = "<p>&amp;&lt;&gt;&quot;&apos; &#65;&#x42;&#X43; &#x1F600;&#128512; " +
            "&nbsp;&copy;&reg;&trade;&hellip;&mdash;&ndash;&laquo;&raquo;&lsquo;&rsquo;&ldquo;&rdquo;&bdquo;&euro;&deg;&plusmn;&times;&divide;" +
            "&middot;&bull;&sect;&para;&shy;&iexcl;&iquest;&eacute;&Eacute;&agrave;&ccedil;&ntilde;&ouml;&szlig;&yuml;&thinsp; " +
            "&unknown; AT&T &#xZZ; &#0; &#xD800; &#x110000; &; &#; a&b;c &amp</p>"
        val expected = "&<>\"' ABC \uD83D\uDE00\uD83D\uDE00 " +
            "\u00A0©®™…—–«»‘’“”„€°±×÷·•§¶\u00AD¡¿éÉàçñößÿ\u2009 " +
            "&unknown; AT&T &#xZZ; &#0; &#xD800; &#x110000; &; &#; a&b;c &amp"
        assertEquals(expected, parseXml(xml).text())
        val xmlOnly = "<p>&amp;&lt;&gt;&quot;&apos;&#65;&#x42;&#x1F600;&#128512;</p>"
        assertEquals(javax(xmlOnly).textContent, parseXml(xmlOnly).text())
    }

    @Test
    fun mergesTextAndCdataAndDropsCommentsAndPis() {
        assertEquals(listOf("<a@1", "'x<y>&amp;z'@1", "</a@1", "end@0"), events("<a>x<![CDATA[<y>&amp;]]>z</a>"))
        assertEquals(listOf("<a@1", "'1 23'@1", "</a@1", "end@0"), events("<a>1<!-- c --> 2<?pi data?>3<![CDATA[]]></a>"))
        assertEquals("l1\nl2\nl3c1\nc2\n", parseXml("<a>l1\r\nl2\rl3<![CDATA[c1\r\nc2\r]]></a>").text())
        assertEquals(
            listOf("<a@1", "' '@1", "<b@2", "</b@2", "'\n'@1", "</a@1", "end@0"),
            events("<a> <b/>\n</a>"),
        )
    }

    @Test
    fun skipsPrologAndDoctypeWithInternalSubset() {
        val xml = "\uFEFF" + """<?xml version="1.0" encoding="UTF-8"?>
<!-- leading comment -->
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd" [
  <!ENTITY brackets "a > b ] c">
  <!-- comment with ] and > and ' -->
  <!ELEMENT note (#PCDATA)>
  <?pi ]> ?>
  <!ATTLIST note id CDATA '>'>
]>
<?xml-stylesheet href="style.css"?>
<html xmlns="http://www.w3.org/1999/xhtml"><body><p>&nbsp;x</p></body></html>
<!-- trailing comment -->
"""
        val root = parseXml(xml)
        assertEquals("html", root.localName)
        assertEquals("http://www.w3.org/1999/xhtml", root.namespace)
        assertEquals("\u00A0x", root.child("body")?.child("p")?.text())
        assertEquals("html", parseXml("<!doctype html>\n<html><body/></html>").name)
    }

    @Test
    fun skipElementConsumesSubtree() {
        val reader = XmlReader("<root><skip a='1'><x><y/>text</x><![CDATA[c]]></skip><keep>k</keep><empty/><after/></root>")
        assertEquals(XmlEvent.START_ELEMENT, reader.next())
        assertEquals(XmlEvent.START_ELEMENT, reader.next())
        assertEquals("skip", reader.name)
        reader.skipElement()
        assertEquals("skip", reader.name)
        assertEquals(2, reader.depth)
        assertEquals(XmlEvent.START_ELEMENT, reader.next())
        assertEquals("keep", reader.name)
        reader.skipElement()
        assertEquals(XmlEvent.START_ELEMENT, reader.next())
        assertEquals("empty", reader.name)
        reader.skipElement()
        assertEquals(XmlEvent.START_ELEMENT, reader.next())
        assertEquals("after", reader.name)
        assertEquals(XmlEvent.END_ELEMENT, reader.next())
        assertFailsWith<IllegalStateException> { reader.skipElement() }
        assertEquals(XmlEvent.END_ELEMENT, reader.next())
        assertEquals("root", reader.name)
        assertEquals(XmlEvent.END_DOCUMENT, reader.next())
        assertEquals(XmlEvent.END_DOCUMENT, reader.next())
    }

    @Test
    fun domHelpers() {
        val root = parseXml(
            """<FictionBook xmlns="http://www.gribuser.ru/xml/fictionbook/2.0" xmlns:l="$xlink"><body><section id="s1">""" +
                """<p>One <emphasis>two</emphasis> three</p><p>Four</p><a l:href="#n1" type="note">1</a></section><section id="s2"/></body>""" +
                """<binary id="img" content-type="image/png">AAAA</binary></FictionBook>""",
        )
        val fb = "http://www.gribuser.ru/xml/fictionbook/2.0"
        assertEquals(listOf("body", "binary"), root.elements.map { it.localName })
        val body = assertNotNull(root.child("body", fb))
        assertNull(root.child("body", "urn:other"))
        assertEquals(listOf("s1", "s2"), body.children("section").map { it.attr("id") })
        assertEquals(emptyList(), body.children("section", "urn:other"))
        val section = body.elements.first()
        assertEquals(listOf("One two three", "Four"), section.children("p", fb).map { it.text() })
        assertEquals(3, section.elements.first().children.size)
        assertEquals("One two threeFour1", section.text())
        val a = assertNotNull(section.child("a"))
        assertEquals("#n1", a.attr("href", xlink))
        assertEquals("#n1", a.attr("href"))
        assertNull(a.attr("href", ""))
        assertEquals("note", a.attr("type", ""))
        assertEquals("image/png", root.child("binary")?.attr("content-type"))
        assertEquals("One two threeFour1AAAA", root.text())
        assertEquals("", body.children("section")[1].text())
    }

    @Test
    fun closesOpenElementsAtEndOfInput() {
        assertEquals(listOf("<root@1", "<a@2", "'text'@2", "</a@2", "</root@1", "end@0"), events("<root><a>text"))
        assertEquals(listOf("<root@1", "</root@1", "end@0"), events("<root>"))
        val root = parseXml("<FictionBook><body><p>Обрыв</p><p>на середи")
        assertEquals("Обрывна середи", root.text())
        assertEquals(2, root.child("body")?.elements?.size)
    }

    @Test
    fun reportsErrorsWithOffsets() {
        fun error(xml: String): String = assertFailsWith<XmlException>(xml) { parseXml(xml) }.message.orEmpty()
        assertEquals("Mismatched end tag </a>, expected </b> at offset 6", error("<a><b></a>"))
        assertEquals("Mismatched end tag </b>, expected </a> at offset 3", error("<a></b >"))
        assertEquals("Unterminated tag <b at offset 3", error("<a><b"))
        assertEquals("Unterminated tag <b at offset 3", error("<a><b x='1'/"))
        assertEquals("Unterminated comment at offset 3", error("<a><!-- x"))
        assertEquals("Unterminated CDATA section at offset 3", error("<a><![CDATA[x"))
        assertEquals("Unterminated processing instruction at offset 3", error("<a><?pi"))
        assertEquals("Unterminated attribute value at offset 5", error("<a x='1></a>"))
        assertEquals("Value of attribute x is not quoted at offset 5", error("<a x=1/>"))
        assertEquals("Attribute x has no value at offset 4", error("<a x/>"))
        assertEquals("Content after the root element at offset 4", error("<a/><b/>"))
        assertEquals("Text before the root element at offset 0", error("text<a/>"))
        assertEquals("Text after the root element at offset 4", error("<a/>tail"))
        assertEquals("No root element at offset 0", error(""))
        assertEquals("No root element at offset 21", error("<?xml version='1.0'?>"))
        assertEquals("Unexpected end tag </a> at offset 0", error("</a>"))
        assertEquals("Invalid tag at offset 3", error("<a>< b/></a>"))
        assertEquals("Unterminated DOCTYPE at offset 0", error("<!DOCTYPE a [ <!ENTITY x 'y'>"))
        assertEquals("DOCTYPE after the root element at offset 3", error("<a><!DOCTYPE a></a>"))
        assertEquals("Unexpected markup at offset 3", error("<a><!ELEMENT x></a>"))
        assertEquals("CDATA outside the root element at offset 0", error("<![CDATA[x]]><a/>"))
    }

    @Test
    fun reportsDepthLikeXmlPullParser() {
        assertEquals(
            listOf("<root@1", "'t'@1", "<a@2", "<b@3", "</b@3", "'u'@2", "</a@2", "</root@1", "end@0"),
            events("<!-- c --><root>t<a><b/>u</a></root>  "),
        )
    }

    @Test
    fun builderOutputParsesBack() {
        val text = "Tom & Jerry <3 > \"quotes\" 'apos' \t tab\nnewline\r\ncrlf \u0001control \uD83D\uDE00 ]]> end"
        val attribute = "a&b<c\"d'e>f\tg\nh\ri"
        val xml = XmlBuilder()
            .start("w:document", "xmlns:w" to w, "xmlns:r" to r)
            .start("w:body")
            .start("w:p", "w:rsidR" to "001")
            .leaf("w:t", text, "xml:space" to "preserve")
            .leaf("w:br")
            .leaf("w:t", "", "a" to attribute, "r:id" to "rId1")
            .end()
            .end()
            .end()
            .toString()
        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><w:document "), xml)
        assertTrue("<w:br/>" in xml, xml)
        val expectedText = text.replace("\u0001", "")
        val t = parseXml(xml).child("body", w)?.child("p", w)?.children("t", w).orEmpty()
        assertEquals(expectedText, t[0].text())
        assertEquals("preserve", t[0].attr("space", xmlNs))
        assertEquals(attribute, t[1].attr("a"))
        assertEquals("rId1", t[1].attr("id", r))
        val dom = javax(xml)
        val javaxT = dom.getElementsByTagNameNS(w, "t")
        assertEquals(expectedText, javaxT.item(0).textContent)
        assertEquals("preserve", (javaxT.item(0) as Element).getAttributeNS(xmlNs, "space"))
        assertEquals(attribute, (javaxT.item(1) as Element).getAttribute("a"))
        compare(parseXml(xml), dom)
    }

    @Test
    fun builderClosesOpenElementsOnlyInItsOutput() {
        val builder = XmlBuilder(declaration = false).start("a").start("b", "k" to "v")
        assertEquals("<a><b k=\"v\"/></a>", builder.toString())
        builder.text("x")
        assertEquals("<a><b k=\"v\">x</b></a>", builder.toString())
        builder.end().leaf("c", null, "n" to "1").leaf("d", "text").end()
        assertEquals("<a><b k=\"v\">x</b><c n=\"1\"/><d>text</d></a>", builder.toString())
        assertFailsWith<IllegalStateException> { builder.end() }
    }

    @Test
    fun matchesJavaxOnGeneratedDocuments() {
        val random = Random(8)
        repeat(500) {
            val doc = randomDocument(random)
            compare(parseXml(doc), javax(doc))
        }
    }

    @Test
    fun handlesDeepNesting() {
        val depth = 100_000
        val root = parseXml("<a>".repeat(depth) + "x" + "</a>".repeat(depth))
        assertEquals("x", root.text())
        var element = root
        var levels = 1
        while (element.elements.isNotEmpty()) {
            element = element.elements[0]
            levels++
        }
        assertEquals(depth, levels)
    }

    @Test
    fun parsesTenMegabytesQuickly() {
        val words = Samples.get(SampleKind.RUSSIAN, 1 shl 20).decodeToString().split(' ', '\n').filter { it.isNotEmpty() && '\uFFFD' !in it }
        val sb = StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<FictionBook xmlns=\"http://www.gribuser.ru/xml/fictionbook/2.0\" xmlns:l=\"$xlink\"><body><section>\n")
        var i = 0
        var word = 0
        while (sb.length < 10 shl 20) {
            sb.append("<p id=\"p").append(i).append("\">")
            repeat(12) { sb.append(words[word++ % words.size]).append(' ') }
            sb.append("<emphasis>").append(words[word++ % words.size]).append("</emphasis> &amp; &#8212; <a l:href=\"#n").append(i).append("\" type=\"note\">[")
                .append(i).append("]</a></p>\n")
            i++
        }
        sb.append("</section></body></FictionBook>\n")
        val xml = sb.toString()
        val start = System.nanoTime()
        val root = parseXml(xml)
        val domMs = (System.nanoTime() - start) / 1e6
        val readerStart = System.nanoTime()
        val reader = XmlReader(xml)
        var elements = 0
        while (true) {
            val event = reader.next()
            if (event == XmlEvent.END_DOCUMENT) break
            if (event == XmlEvent.START_ELEMENT) elements++
        }
        val readerMs = (System.nanoTime() - readerStart) / 1e6
        println("10 MB XML: DOM %.0f ms, pull reader %.0f ms, %d elements".format(domMs, readerMs, elements))
        assertEquals(i, root.child("body")?.child("section")?.children("p")?.size)
        assertEquals(3 + 3 * i, elements)
        assertTrue(domMs < 5000 && readerMs < 5000)
    }

    private fun randomDocument(random: Random): String {
        val prefixes = listOf("", "a", "b")
        val names = listOf("item", "p", "span", "row", "cell", "x-y", "n.1", "_u")
        val values = listOf("plain", "x &amp; y", "tab\there", "line\nbreak", "q&quot;uote", "&#x1F600;", "'single'", "&lt;&gt;", "", "Кириллица")
        val texts = listOf(
            "plain", " spaced ", "&lt;tag&gt;", "&#169; 2024", "\r\nCRLF\r\n", "Кириллица", "<![CDATA[raw <b> & ]]]]>", "<!-- comment -->",
            "<?pi x?>", "&#x1F600;", "\n  ", "a]b", "&apos;&quot;",
        )
        val sb = StringBuilder("<?xml version=\"1.0\"?>\n<!-- generated -->\n")
        fun element(depth: Int) {
            val prefix = prefixes[random.nextInt(prefixes.size)]
            val name = (if (prefix.isEmpty()) "" else "$prefix:") + names[random.nextInt(names.size)]
            sb.append('<').append(name)
            if (depth == 0) {
                sb.append(" xmlns=\"urn:default\" xmlns:a=\"urn:a\" xmlns:b=\"urn:b\"")
            } else {
                if (random.nextInt(5) == 0) sb.append(" xmlns:a=\"urn:a").append(depth).append('"')
                if (random.nextInt(7) == 0) sb.append(" xmlns=\"\"")
            }
            val attributes = listOf("id", "a:ref", "b:kind", "xml:lang", "plain").shuffled(random).take(random.nextInt(4))
            for (attribute in attributes) {
                val quote = if (random.nextBoolean()) '"' else '\''
                val value = values[random.nextInt(values.size)].let { if (quote == '\'') it.replace("'", "&apos;") else it }
                sb.append(' ').append(attribute).append('=').append(quote).append(value).append(quote)
            }
            if (depth > 4 || random.nextInt(6) == 0) {
                sb.append("/>")
                return
            }
            sb.append('>')
            repeat(random.nextInt(5)) {
                if (random.nextInt(3) == 0) element(depth + 1) else sb.append(texts[random.nextInt(texts.size)])
            }
            sb.append("</").append(name).append('>')
        }
        element(0)
        sb.append("\n<!-- end -->\n")
        return sb.toString()
    }

    private fun compare(ours: XmlElement, theirs: Element) {
        assertEquals(theirs.tagName, ours.name)
        assertEquals(theirs.localName, ours.localName, ours.name)
        assertEquals(theirs.namespaceURI.orEmpty(), ours.namespace, ours.name)
        val theirAttributes = (0 until theirs.attributes.length).map { theirs.attributes.item(it) as Attr }
            .map { Triple(it.namespaceURI.orEmpty(), it.localName, it.value) }.toSet()
        assertEquals(theirAttributes, ours.attributes.map { Triple(it.namespace, it.localName, it.value) }.toSet(), ours.name)
        val theirChildren = ArrayList<Any>()
        val text = StringBuilder()
        for (i in 0 until theirs.childNodes.length) {
            val node = theirs.childNodes.item(i)
            when (node.nodeType) {
                Node.TEXT_NODE, Node.CDATA_SECTION_NODE -> text.append(node.nodeValue)
                Node.ELEMENT_NODE -> {
                    if (text.isNotEmpty()) theirChildren += text.toString()
                    text.clear()
                    theirChildren += node
                }
            }
        }
        if (text.isNotEmpty()) theirChildren += text.toString()
        assertEquals(theirChildren.size, ours.children.size, "children of ${ours.name}")
        for ((mine, other) in ours.children.zip(theirChildren)) {
            when (mine) {
                is XmlText -> assertEquals(other, mine.text)
                is XmlElement -> compare(mine, other as Element)
            }
        }
    }
}
