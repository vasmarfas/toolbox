package com.vasmarfas.card.tools.documents

internal fun t(text: String) = Inline.Text(text)

internal fun b(text: String) = Inline.Text(text, bold = true)

internal fun i(text: String) = Inline.Text(text, italic = true)

internal fun u(text: String) = Inline.Text(text, underline = true)

internal fun s(text: String) = Inline.Text(text, strike = true)

internal fun c(text: String) = Inline.Text(text, code = true)

internal fun sup(text: String) = Inline.Text(text, script = Script.SUPER)

internal fun sub(text: String) = Inline.Text(text, script = Script.SUB)

internal fun a(text: String, href: String) = Inline.Text(text, link = href)

internal val br = Inline.LineBreak

internal fun p(vararg content: Inline, align: Align = Align.START) = Block.Paragraph(content.toList(), align)

internal fun p(text: String, align: Align = Align.START) = Block.Paragraph(listOf(Inline.Text(text)), align)

internal fun h(level: Int, text: String) = Block.Heading(level, listOf(Inline.Text(text)))

internal fun ul(vararg items: List<Block>) = Block.ListBlock(false, items.toList())

internal fun ol(start: Int, vararg items: List<Block>) = Block.ListBlock(true, items.toList(), start)

internal fun item(vararg blocks: Block) = blocks.toList()

internal fun cell(vararg blocks: Block, span: Int = 1) = Cell(blocks.toList(), span)

internal fun table(header: Int, vararg rows: List<Cell>) = Block.Table(rows.toList(), header)

internal fun row(vararg cells: Cell) = cells.toList()

internal object Samples {
    val png: ByteArray = TestImages.png(40, 30)
    val secondPng: ByteArray = TestImages.png(8, 16, seed = 5)

    fun rich(): Doc = Doc(
        blocks = listOf(
            h(1, "Первая глава"),
            p(
                t("Обычный текст, "), b("жирный"), t(", "), i("курсив"), t(", "), u("подчёркнутый"), t(", "), s("зачёркнутый"),
                t(", "), c("код()"), t(", H"), sub("2"), t("O и E=mc"), sup("2"), t(", "), a("ссылка", "https://example.com/путь?q=1&x=2"),
                t(" и "), Inline.Text("всё сразу", bold = true, italic = true), t("."),
            ),
            p("Текст по центру", Align.CENTER),
            p("Текст справа", Align.END),
            p("Текст по ширине страницы, достаточно длинный, чтобы выравнивание было заметно.", Align.JUSTIFY),
            p(t("Первая строка"), br, t("вторая строка")),
            h(2, "Списки"),
            ul(
                item(p("пункт один")),
                item(p("пункт два"), ol(3, item(p("вложенный три")), item(p("вложенный четыре")))),
                item(p("пункт три")),
            ),
            ol(5, item(p("пять")), item(p("шесть"))),
            h(3, "Таблица"),
            table(
                1,
                row(cell(p("Имя")), cell(p("Значение")), cell(p("Ещё"))),
                row(cell(p("объединено"), span = 2), cell(p("x"))),
                row(cell(p("1")), cell(p("2")), cell(p("3"))),
            ),
            Block.Quote(listOf(p("Цитата в две строки"), p("вторая часть цитаты"))),
            Block.Code("fun main() {\n    println(\"<Привет> & пока\")\n}"),
            Block.Picture(png, 30f, 22.5f, "Картинка"),
            Block.Rule,
            Block.PageBreak,
            h(1, "Вторая глава"),
            p("Спецсимволы & < > \" ' * _ ` # [ ] | ~ и эмодзи 😀, буква ё."),
        ),
        title = "Проверочный документ",
        author = "Иван Петров",
        language = "ru",
    )
}
