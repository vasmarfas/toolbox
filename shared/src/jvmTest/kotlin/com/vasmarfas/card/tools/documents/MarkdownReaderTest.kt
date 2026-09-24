package com.vasmarfas.card.tools.documents

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.TimeSource

class MarkdownReaderTest {
    private fun md(text: String): String = dumpBlocks(MarkdownReader.read(text).blocks)

    private fun inline(text: String): String = md(text).removePrefix("P ").trimEnd('\n')

    @Test
    fun headings() {
        assertEquals(
            "H1 \"Один\"\nH2 \"Два\"\nH3 \"Три\"\nH1 \"Setext один\"\nH2 \"Setext два\"\nP \"#без пробела\"\nH6 \"C#\"\n",
            md("# Один\n## Два ##\n###   Три   ###\nSetext один\n===\nSetext два\n---\n#без пробела\n###### C#\n"),
        )
    }

    @Test
    fun emphasis() {
        assertEquals("{i}\"a\" \" \" {b}\"b\" \" \" {b,i}\"c\" \" \" {s}\"d\" \" \" {i}\"e\" \" \" {b}\"f\"", inline("*a* **b** ***c*** ~~d~~ _e_ __f__"))
        assertEquals("\"snake_case_name и 2 * 3 * 4\"", inline("snake_case_name и 2 * 3 * 4"))
        assertEquals("\"2\" {i}\"3\" \"4\"", inline("2*3*4"))
        assertEquals("\"внутри\" {b}\"слова\" \"тоже\"", inline("внутри**слова**тоже"))
        assertEquals("\"_внутри_слова работает только звёздочкой\"", inline("_внутри_слова работает только звёздочкой"))
        assertEquals("{i}\"foo \" {b,i}\"bar\"", inline("*foo **bar***"))
        assertEquals("{i}\"foo\" {b,i}\"bar\"", inline("*foo**bar***"))
        assertEquals("\"** не жирный **\"", inline("** не жирный **"))
        assertEquals("\"**(скобки)**текст\"", inline("**(скобки)**текст"))
        assertEquals("{b}\"(скобки)\" \" текст\"", inline("**(скобки)** текст"))
        assertEquals("\"а ~~~ три тильды ~~~\"", inline("а ~~~ три тильды ~~~"))
    }

    @Test
    fun codeSpansAndEscapes() {
        assertEquals("{c}\"код\" \" \" {c}\"с ` внутри\" \" \" {c}\"``\"", inline("`код` `` с ` внутри `` ` `` `"))
        assertEquals("\"`не код` *не курсив* [не ссылка] 1. # \\\\\\\\\"", inline("\\`не код\\` \\*не курсив\\* \\[не ссылка\\] 1\\. \\# \\\\\\\\"))
        assertEquals("{c}\"без пары и \" \"пара`\"", inline("`без пары и `пара`"))
        assertEquals("\"``не закрыт \" {c}\"a\"", inline("``не закрыт `a`"))
        assertEquals("\"& © < \" {c}\"&amp;\"", inline("&amp; &copy; &lt; `&amp;`"))
    }

    @Test
    fun links() {
        val text = """
            [inline](https://example.com/a "title") [с <угловыми>](<https://example.com/b c>) [пустая]()
            [ref] [full][Ref] [collapsed][] [missing][nope] <https://auto.example> <mail@example.com>
            голая https://bare.example/path?x=1, и www.site.example. [внутренний](#anchor) [отн](page.md)

            [ref]: https://ref.example/ "Title"
            [collapsed]: <https://collapsed.example>
        """.trimIndent()
        assertEquals(
            "P {link=https://example.com/a}\"inline\" \" \" {link=https://example.com/b c}\"с <угловыми>\" \" пустая \" " +
                "{link=https://ref.example/}\"ref\" \" \" {link=https://ref.example/}\"full\" \" \" {link=https://collapsed.example}\"collapsed\" " +
                "\" [missing][nope] \" {link=https://auto.example}\"https://auto.example\" \" \" {link=mailto:mail@example.com}\"mail@example.com\" " +
                "\" голая \" {link=https://bare.example/path?x=1}\"https://bare.example/path?x=1\" \", и \" {link=http://www.site.example}\"www.site.example\" " +
                "\". внутренний отн\"\n",
            md(text),
        )
    }

    @Test
    fun linkLabelLimits() {
        val short = "a".repeat(999)
        val long = "a".repeat(1000)
        assertEquals("P {link=https://x.example}\"$short\"\n", md("[$short]\n\n[$short]: https://x.example"))
        assertEquals("P \"[$long]\"\nP \"[$long]: \" {link=https://x.example}\"https://x.example\"\n", md("[$long]\n\n[$long]: https://x.example"))
        assertEquals("P \"[[[foo]]]\"\nP \"[[[foo]]]: \" {link=https://x.example}\"https://x.example\"\n", md("[[[foo]]]\n\n[[[foo]]]: https://x.example"))
        assertEquals("P \"[foo \" {link=https://x.example}\"bar\" \"]\"\n", md("[foo [bar]]\n\n[bar]: https://x.example"))
    }

    @Test
    fun imagesAndHtml() {
        val png = TestImages.png(2, 2)
        val doc = MarkdownReader.read("до ![картинка](data:image/png;base64,${encodeBase64(png)}) после ![внешняя](https://example.com/x.png)\n\n<div align=\"center\">\n<b>жирный тег</b> и <u>подчёркнутый</u>, H<sub>2</sub>O<br>строка\n</div>\n\n<!--\nскрыто\n-->\n\n<script>\nalert(1)\n</script>\n\n<div style=\"page-break-after: always;\"></div>\n")
        assertEquals(5, doc.blocks.size)
        assertEquals("P \"до\"\n", dumpBlocks(doc.blocks.subList(0, 1)))
        assertTrue((doc.blocks[1] as Block.Picture).bytes.contentEquals(png))
        assertEquals("картинка", (doc.blocks[1] as Block.Picture).alt)
        assertEquals("P \"после внешняя\"\n", dumpBlocks(doc.blocks.subList(2, 3)))
        assertEquals("P {b}\"жирный тег\" \" и \" {u}\"подчёркнутый\" \", H\" {sub}\"2\" \"O\" \\n \"строка\"\n", dumpBlocks(doc.blocks.subList(3, 4)))
        assertEquals(Block.PageBreak, doc.blocks[4])
    }

    @Test
    fun hardBreaks() {
        assertEquals("P \"два пробела\" \\n \"обратная черта\" \\n \"мягкий перенос\"\n", md("два пробела  \nобратная черта\\\nмягкий\nперенос"))
    }

    @Test
    fun lists() {
        val text = """
            - один
            - два
              продолжение

              второй абзац
              - вложенный
                1. глубже
            * другая звёздочка — новый список

            3. три
            4) новая скобка — новый список

            -
              с пустой первой строкой
            - [x] задача
        """.trimIndent()
        assertEquals(
            """
            UL
              ITEM
                P "один"
              ITEM
                P "два продолжение"
                P "второй абзац"
                UL
                  ITEM
                    P "вложенный"
                    OL start=1
                      ITEM
                        P "глубже"
            UL
              ITEM
                P "другая звёздочка — новый список"
            OL start=3
              ITEM
                P "три"
            OL start=4
              ITEM
                P "новая скобка — новый список"
            UL
              ITEM
                P "с пустой первой строкой"
              ITEM
                P "[x] задача"

            """.trimIndent(),
            md(text),
        )
    }

    @Test
    fun listsVersusRulesAndParagraphs() {
        assertEquals("RULE\nRULE\nP \"текст 2. не пункт\"\nP \"текст\"\nOL start=1\n  ITEM\n    P \"пункт\"\n", md("* * *\n- - -\nтекст\n2. не пункт\n\nтекст\n1. пункт"))
    }

    @Test
    fun quotes() {
        assertEquals(
            """
            QUOTE
              P "цитата ленивое продолжение"
              QUOTE
                P "вложенная"
              UL
                ITEM
                  P "список в цитате"
            P "после"

            """.trimIndent(),
            md("> цитата\nленивое продолжение\n>\n>> вложенная\n>\n> - список в цитате\n\nпосле"),
        )
    }

    @Test
    fun code() {
        assertEquals(
            "CODE \"fenced\\n  indented\\n\\ttab\"\nCODE \"tilde ``` inside\"\nCODE \"indented code\\n\\nafter blank\"\nP \"text\"\n",
            md("```kotlin\nfenced\n  indented\n\ttab\n```\n~~~~\ntilde ``` inside\n~~~~\n\n    indented code\n\n    after blank\ntext\n"),
        )
        assertEquals("CODE \"unclosed\\nto the end\"\n", md("```\nunclosed\nto the end"))
        assertEquals("P \"текст\"\nCODE \"внутри\"\n", md("текст\n```\nвнутри\n```"))
    }

    @Test
    fun tables() {
        val text = """
            | Слева | Центр | Справа |
            |:------|:-----:|-------:|
            | a \| b | `c` | **d** |
            | короткая |
            без рамки | тоже
        """.trimIndent()
        assertEquals(
            """
            TABLE header=1
              ROW
                CELL
                  P "Слева"
                CELL
                  P[center] "Центр"
                CELL
                  P[end] "Справа"
              ROW
                CELL
                  P "a | b"
                CELL
                  P[center] {c}"c"
                CELL
                  P[end] {b}"d"
              ROW
                CELL
                  P "короткая"
                CELL
                CELL
              ROW
                CELL
                  P "без рамки"
                CELL
                  P[center] "тоже"
                CELL

            """.trimIndent(),
            md(text),
        )
        assertEquals("TABLE header=1\n  ROW\n    CELL\n      P \"a\"\n    CELL\n      P \"b\"\n", md("a | b\n- | -"))
        assertEquals("P \"a | b | c --- | ---\"\n", md("a | b | c\n--- | ---"))
    }

    @Test
    fun frontMatter() {
        val doc = MarkdownReader.read("---\ntitle: \"Заголовок: с двоеточием\"\nauthor: Автор\nlang: ru\n---\n\n# Текст\n")
        assertEquals("Заголовок: с двоеточием", doc.title)
        assertEquals("Автор", doc.author)
        assertEquals("ru", doc.language)
        assertEquals("H1 \"Текст\"\n", dumpBlocks(doc.blocks))
        assertEquals("RULE\nP \"не метаданные\"\nRULE\n", md("---\n\nне метаданные\n\n---\n"))
    }

    // from the cmark pathological suite: a quadratic pass takes seconds here, a linear one milliseconds
    @Test
    fun pathologicalInputsStayLinear() {
        val inputs = listOf(
            "*a ".repeat(30_000),
            "*a **a ".repeat(20_000) + "b" + " a** a*".repeat(20_000),
            "[".repeat(50_000) + "x" + "]".repeat(50_000),
            "[ (](".repeat(30_000),
            "[a](b".repeat(30_000),
            "`".repeat(5_000) + " x " + "``".repeat(2_000),
            ">".repeat(30_000) + " глубоко",
            "<a href=\"".repeat(10_000),
            "x <!--".repeat(30_000),
            "- ".repeat(5_000) + "x",
        )
        for (text in inputs) {
            val mark = TimeSource.Monotonic.markNow()
            val doc = MarkdownReader.read(text)
            val elapsed = mark.elapsedNow().inWholeMilliseconds
            assertTrue(doc.blocks.isNotEmpty())
            assertTrue(elapsed < 2000, "${text.take(12)}... took $elapsed ms")
        }
    }
}
