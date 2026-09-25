package com.vasmarfas.card.tools.documents.editor

import com.vasmarfas.card.tools.documents.Align
import com.vasmarfas.card.tools.documents.Line
import com.vasmarfas.card.tools.documents.LineBreaker
import com.vasmarfas.card.tools.documents.PieceBuilder
import com.vasmarfas.card.tools.documents.RunStyle
import com.vasmarfas.card.tools.documents.hex4
import com.vasmarfas.card.tools.documents.pdf.PdfArray
import com.vasmarfas.card.tools.documents.pdf.PdfDict
import com.vasmarfas.card.tools.documents.pdf.PdfDocument
import com.vasmarfas.card.tools.documents.pdf.PdfInt
import com.vasmarfas.card.tools.documents.pdf.PdfName
import com.vasmarfas.card.tools.documents.pdf.PdfObject
import com.vasmarfas.card.tools.documents.pdf.PdfReal
import com.vasmarfas.card.tools.documents.pdf.PdfRect
import com.vasmarfas.card.tools.documents.pdf.PdfRef
import com.vasmarfas.card.tools.documents.pdf.PdfString
import com.vasmarfas.card.tools.documents.pdf.PdfWriter
import com.vasmarfas.card.tools.documents.pdf.formatReal
import com.vasmarfas.card.tools.documents.pdf.rectOf
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

enum class FieldKind { TEXT, CHECKBOX, RADIO, COMBO, LIST, BUTTON, SIGNATURE }

class FieldOption(val export: String, val label: String)

class FormWidget internal constructor(
    val pageIndex: Int,
    val rect: PdfRect,
    val onState: String?,
    internal val ref: PdfRef?,
    internal val dict: PdfDict,
    internal val rotation: Int,
)

class FormField internal constructor(
    val name: String,
    val kind: FieldKind,
    val value: FieldValue?,
    val options: List<FieldOption>,
    val flags: Int,
    val maxLength: Int,
    val alignment: Int,
    val fontSize: Float,
    val textColor: Int,
    val widgets: List<FormWidget>,
    val tooltip: String?,
    internal val node: PdfDict,
    internal val nodeRef: PdfRef?,
) {
    val readOnly: Boolean get() = flags and 1 != 0
    val required: Boolean get() = flags and 2 != 0
    val multiline: Boolean get() = kind == FieldKind.TEXT && flags and (1 shl 12) != 0
    val password: Boolean get() = kind == FieldKind.TEXT && flags and (1 shl 13) != 0
    val comb: Boolean get() = kind == FieldKind.TEXT && flags and (1 shl 24) != 0 && maxLength > 0
    val multiSelect: Boolean get() = kind == FieldKind.LIST && flags and (1 shl 21) != 0

    internal val merged: Boolean get() = widgets.size == 1 && widgets[0].dict === node

    fun display(value: FieldValue?): String = when (value) {
        is FieldValue.Text -> value.value
        is FieldValue.Choice -> value.values.joinToString(", ") { v -> options.firstOrNull { it.export == v }?.label ?: v }
        is FieldValue.Check -> value.state.orEmpty()
        null -> ""
    }
}

object PdfForms {
    fun read(doc: PdfDocument): List<FormField> {
        val acro = doc.catalog.dict("AcroForm", doc) ?: return emptyList()
        val fields = acro.array("Fields", doc) ?: return emptyList()
        val pageOf = HashMap<Int, Int>()
        for (page in doc.pages) {
            page.dict.array("Annots", doc)?.items?.forEach { (it as? PdfRef)?.let { ref -> pageOf.getOrPut(ref.number) { page.index } } }
        }
        val pageNumbers = doc.pages.mapNotNull { page -> page.ref?.number?.let { it to page.index } }.toMap()
        val out = ArrayList<FormField>()
        val seen = HashSet<Int>()
        val defaults = Inherited(null, null, null, acro.text("DA", doc), acro.int("Q", doc))
        for (item in fields.items) walk(doc, item, "", defaults, 0, pageOf, pageNumbers, out, seen)
        return out
    }

    private class Inherited(val type: String?, val flags: Int?, val value: PdfObject?, val appearance: String?, val alignment: Int?)

    private fun walk(
        doc: PdfDocument,
        item: PdfObject,
        prefix: String,
        parent: Inherited,
        depth: Int,
        pageOf: Map<Int, Int>,
        pageNumbers: Map<Int, Int>,
        out: MutableList<FormField>,
        seen: MutableSet<Int>,
    ) {
        if (depth > 32) return
        val ref = item as? PdfRef
        if (ref != null && !seen.add(ref.number)) return
        val node = doc.resolve(item) as? PdfDict ?: return
        val partial = node.text("T", doc)
        val name = when {
            partial == null -> prefix
            prefix.isEmpty() -> partial
            else -> "$prefix.$partial"
        }
        val here = Inherited(
            node.name("FT", doc) ?: parent.type,
            node.int("Ff", doc) ?: parent.flags,
            node["V"]?.let { doc.resolve(it) } ?: parent.value,
            node.text("DA", doc) ?: parent.appearance,
            node.int("Q", doc) ?: parent.alignment,
        )
        val kids = node.array("Kids", doc)?.items.orEmpty()
        val fieldKids = kids.filter { (doc.resolve(it) as? PdfDict)?.let { kid -> kid["T"] != null || kid.name("Subtype", doc) != "Widget" && kid["Kids"] != null } == true }
        if (fieldKids.isNotEmpty()) {
            for (kid in fieldKids) walk(doc, kid, name, here, depth + 1, pageOf, pageNumbers, out, seen)
            return
        }
        val widgetItems = if (kids.isEmpty()) listOf(item) else kids
        val widgets = widgetItems.mapNotNull { widget(doc, it, pageOf, pageNumbers) }
        if (widgets.isEmpty() || name.isEmpty()) return
        val flags = here.flags ?: 0
        val kind = when (here.type) {
            "Tx" -> FieldKind.TEXT
            "Ch" -> if (flags and (1 shl 17) != 0) FieldKind.COMBO else FieldKind.LIST
            "Sig" -> FieldKind.SIGNATURE
            "Btn" -> when {
                flags and (1 shl 16) != 0 -> FieldKind.BUTTON
                flags and (1 shl 15) != 0 -> FieldKind.RADIO
                else -> FieldKind.CHECKBOX
            }
            else -> return
        }
        val options = node.array("Opt", doc)?.items.orEmpty().mapNotNull { option ->
            when (val v = doc.resolve(option)) {
                is PdfString -> FieldOption(v.text(), v.text())
                is PdfArray -> {
                    val export = (v.resolved(0, doc) as? PdfString)?.text()
                    val label = (v.resolved(1, doc) as? PdfString)?.text()
                    if (export != null) FieldOption(export, label ?: export) else null
                }
                else -> null
            }
        }
        val (size, color) = parseAppearance(here.appearance)
        out += FormField(
            name = name,
            kind = kind,
            value = value(kind, here.value, doc),
            options = options,
            flags = flags,
            maxLength = node.int("MaxLen", doc) ?: 0,
            alignment = here.alignment ?: 0,
            fontSize = size,
            textColor = color,
            widgets = widgets,
            tooltip = node.text("TU", doc)?.takeIf { it.isNotBlank() },
            node = node,
            nodeRef = ref,
        )
    }

    private fun widget(doc: PdfDocument, item: PdfObject, pageOf: Map<Int, Int>, pageNumbers: Map<Int, Int>): FormWidget? {
        val dict = doc.resolve(item) as? PdfDict ?: return null
        val rect = rectOf(dict.array("Rect", doc), doc) ?: return null
        val ref = item as? PdfRef
        val page = ref?.let { pageOf[it.number] } ?: (dict["P"] as? PdfRef)?.let { pageNumbers[it.number] } ?: return null
        val states = dict.dict("AP", doc)?.dict("N", doc)?.keys.orEmpty()
        val onState = states.firstOrNull { it != "Off" }
        val rotation = dict.dict("MK", doc)?.int("R", doc)?.let { ((it % 360) + 360) % 360 } ?: 0
        return FormWidget(page, rect, onState, ref, dict, rotation)
    }

    private fun value(kind: FieldKind, value: PdfObject?, doc: PdfDocument): FieldValue? = when (kind) {
        FieldKind.TEXT -> (value as? PdfString)?.let { FieldValue.Text(it.text()) }
        FieldKind.COMBO, FieldKind.LIST -> when (value) {
            is PdfString -> FieldValue.Choice(listOf(value.text()))
            is PdfArray -> FieldValue.Choice(value.items.mapNotNull { (doc.resolve(it) as? PdfString)?.text() })
            else -> null
        }
        FieldKind.CHECKBOX, FieldKind.RADIO -> (value as? PdfName)?.let { FieldValue.Check(it.name.takeIf { name -> name != "Off" }) }
        FieldKind.BUTTON, FieldKind.SIGNATURE -> null
    }

    internal fun parseAppearance(da: String?): Pair<Float, Int> {
        if (da == null) return 0f to 0xFF000000.toInt()
        val tokens = da.split(' ', '\n', '\r', '\t').filter { it.isNotEmpty() }
        var size = 0f
        var color = 0xFF000000.toInt()
        for ((i, token) in tokens.withIndex()) {
            fun num(back: Int) = tokens.getOrNull(i - back)?.toFloatOrNull() ?: 0f
            when (token) {
                "Tf" -> size = num(1)
                "g" -> color = argb(num(1), num(1), num(1))
                "rg" -> color = argb(num(3), num(2), num(1))
                "k" -> {
                    val c = num(4)
                    val m = num(3)
                    val y = num(2)
                    val k = num(1)
                    color = argb((1 - c) * (1 - k), (1 - m) * (1 - k), (1 - y) * (1 - k))
                }
            }
        }
        return size to color
    }

    private fun argb(r: Float, g: Float, b: Float): Int =
        (0xFF shl 24) or ((r.coerceIn(0f, 1f) * 255).toInt() shl 16) or ((g.coerceIn(0f, 1f) * 255).toInt() shl 8) or (b.coerceIn(0f, 1f) * 255).toInt()
}

internal class FieldAppearance(private val writer: PdfWriter, private val painter: MarkPainter) {
    fun form(field: FormField, widget: FormWidget, value: FieldValue?, doc: PdfDocument): PdfRef? = when (field.kind) {
        FieldKind.TEXT -> text(field, widget, (value as? FieldValue.Text)?.value.orEmpty(), doc)
        FieldKind.COMBO -> text(field, widget, field.display(value), doc)
        FieldKind.LIST -> list(field, widget, (value as? FieldValue.Choice)?.values.orEmpty(), doc)
        FieldKind.CHECKBOX, FieldKind.RADIO -> null
        FieldKind.BUTTON, FieldKind.SIGNATURE -> null
    }

    fun checkStates(widget: FormWidget, color: Int): PdfDict {
        val (w, h) = frameSize(widget)
        val on = StringBuilder()
        val size = min(w, h)
        on.append("q ").append(rgb(color)).append(" RG ").append(n(max(1.0, size * 0.1))).append(" w 1 J 1 j ")
            .append(n(w * 0.2)).append(' ').append(n(h * 0.52)).append(" m ")
            .append(n(w * 0.42)).append(' ').append(n(h * 0.26)).append(" l ")
            .append(n(w * 0.82)).append(' ').append(n(h * 0.78)).append(" l S Q")
        return PdfDict(
            (widget.onState ?: "Yes") to formStream(widget, on.toString(), PdfDict()),
            "Off" to formStream(widget, "", PdfDict()),
        )
    }

    private fun text(field: FormField, widget: FormWidget, raw: String, doc: PdfDocument): PdfRef {
        val (w, h) = frameSize(widget)
        val shown = if (field.password) "•".repeat(raw.length) else raw
        val canvas = StringBuilder()
        val fonts = PdfDict()
        background(widget, doc, canvas, w, h)
        canvas.append("/Tx BMC q 1 1 ").append(n(w - 2)).append(' ').append(n(h - 2)).append(" re W n ")
        canvas.append(rgb(field.textColor)).append(" rg\n")
        val family = painter.fonts.family(MarkFont.SANS)
        if (field.comb) {
            val cell = w / field.maxLength
            val size = autoSize(field.fontSize, h, cell * 1.6)
            var i = 0
            for (ch in shown.take(field.maxLength)) {
                val line = LineBreaker.lines(PieceBuilder(painter.faces).apply { text(ch.toString(), RunStyle(family, size)) }.finish(), Float.MAX_VALUE / 4, Align.START, 1f, size).first()
                val width = lineWidth(line)
                val baseline = (h - size * 0.72) / 2
                emit(canvas, fonts, line, cell * i + (cell - width) / 2, baseline)
                i++
            }
        } else if (field.multiline) {
            val size = if (field.fontSize > 0) field.fontSize else 10f
            val lines = LineBreaker.lines(PieceBuilder(painter.faces).apply { text(shown, RunStyle(family, size)) }.finish(), (w - 4).toFloat(), align(field.alignment), 1.15f, size)
            var top = h - 2
            for (line in lines) {
                emit(canvas, fonts, line, 2.0, top - line.baseline)
                top -= line.height
            }
        } else {
            var size = autoSize(field.fontSize, h, Double.MAX_VALUE)
            var line = LineBreaker.lines(PieceBuilder(painter.faces).apply { text(shown.replace('\n', ' '), RunStyle(family, size)) }.finish(), Float.MAX_VALUE / 4, Align.START, 1f, size).first()
            if (field.fontSize <= 0f && lineWidth(line) > w - 4 && lineWidth(line) > 0) {
                size = max(4f, (size * (w - 4) / lineWidth(line)).toFloat())
                line = LineBreaker.lines(PieceBuilder(painter.faces).apply { text(shown.replace('\n', ' '), RunStyle(family, size)) }.finish(), Float.MAX_VALUE / 4, Align.START, 1f, size).first()
            }
            val width = lineWidth(line)
            val x = when (field.alignment) {
                1 -> (w - width) / 2
                2 -> w - 2 - width
                else -> 2.0
            }
            emit(canvas, fonts, line, x, (h - size * 0.72) / 2)
        }
        canvas.append("Q EMC")
        return formStream(widget, canvas.toString(), PdfDict("Font" to fonts))
    }

    private fun list(field: FormField, widget: FormWidget, selected: List<String>, doc: PdfDocument): PdfRef {
        val (w, h) = frameSize(widget)
        val size = if (field.fontSize > 0) field.fontSize else 10f
        val canvas = StringBuilder()
        val fonts = PdfDict()
        background(widget, doc, canvas, w, h)
        canvas.append("/Tx BMC q 1 1 ").append(n(w - 2)).append(' ').append(n(h - 2)).append(" re W n\n")
        val family = painter.fonts.family(MarkFont.SANS)
        var top = h - 1
        for (option in field.options) {
            val line = LineBreaker.lines(PieceBuilder(painter.faces).apply { text(option.label, RunStyle(family, size)) }.finish(), Float.MAX_VALUE / 4, Align.START, 1.15f, size).first()
            if (option.export in selected) canvas.append("0.6 0.75 0.93 rg 1 ").append(n(top - line.height)).append(' ').append(n(w - 2)).append(' ').append(n(line.height.toDouble())).append(" re f\n")
            canvas.append(rgb(field.textColor)).append(" rg\n")
            emit(canvas, fonts, line, 2.0, top - line.baseline)
            top -= line.height
            if (top < 0) break
        }
        canvas.append("Q EMC")
        return formStream(widget, canvas.toString(), PdfDict("Font" to fonts))
    }

    private fun background(widget: FormWidget, doc: PdfDocument, canvas: StringBuilder, w: Double, h: Double) {
        val mk = widget.dict.dict("MK", doc) ?: return
        colour(mk.array("BG", doc), doc)?.let { canvas.append(it).append(" rg 0 0 ").append(n(w)).append(' ').append(n(h)).append(" re f\n") }
        colour(mk.array("BC", doc), doc)?.let { canvas.append(it).append(" RG 1 w 0.5 0.5 ").append(n(w - 1)).append(' ').append(n(h - 1)).append(" re S\n") }
    }

    private fun colour(array: PdfArray?, doc: PdfDocument): String? {
        val v = array?.numbers(doc) ?: return null
        return when (v.size) {
            1 -> "${n(v[0])} ${n(v[0])} ${n(v[0])}"
            3 -> "${n(v[0])} ${n(v[1])} ${n(v[2])}"
            4 -> "${n((1 - v[0]) * (1 - v[3]))} ${n((1 - v[1]) * (1 - v[3]))} ${n((1 - v[2]) * (1 - v[3]))}"
            else -> null
        }
    }

    private fun emit(canvas: StringBuilder, fonts: PdfDict, line: Line, x: Double, baseline: Double) {
        for (k in line.boxes.indices) {
            val box = line.boxes[k]
            fonts[box.face.resource] = box.face.ref
            canvas.append("BT /").append(box.face.resource).append(' ').append(n(box.style.size.toDouble())).append(" Tf ")
                .append(n(x + line.xs[k])).append(' ').append(n(baseline)).append(" Td <")
            for (g in box.glyphs) canvas.append(hex4(g))
            canvas.append("> Tj ET\n")
        }
    }

    private fun autoSize(declared: Float, height: Double, width: Double): Float = if (declared > 0f) declared else min(12.0, min((height - 2) / 1.2, width)).toFloat().coerceAtLeast(4f)

    private fun align(q: Int) = when (q) {
        1 -> Align.CENTER
        2 -> Align.END
        else -> Align.START
    }

    private fun frameSize(widget: FormWidget): Pair<Double, Double> =
        if (widget.rotation % 180 != 0) widget.rect.height to widget.rect.width else widget.rect.width to widget.rect.height

    private fun formStream(widget: FormWidget, content: String, resources: PdfDict): PdfRef {
        val (w, h) = frameSize(widget)
        val dict = PdfDict(
            "Type" to PdfName("XObject"),
            "Subtype" to PdfName("Form"),
            "BBox" to PdfArray(PdfInt.of(0), PdfInt.of(0), PdfReal(w), PdfReal(h)),
            "Resources" to resources,
        )
        when (widget.rotation) {
            90 -> dict["Matrix"] = PdfArray(PdfInt.of(0), PdfInt.of(1), PdfInt.of(-1), PdfInt.of(0), PdfReal(h), PdfInt.of(0))
            180 -> dict["Matrix"] = PdfArray(PdfInt.of(-1), PdfInt.of(0), PdfInt.of(0), PdfInt.of(-1), PdfReal(w), PdfReal(h))
            270 -> dict["Matrix"] = PdfArray(PdfInt.of(0), PdfInt.of(-1), PdfInt.of(1), PdfInt.of(0), PdfInt.of(0), PdfReal(w))
        }
        return writer.stream(dict, content.encodeToByteArray())
    }

    private fun rgb(argb: Int): String = "${n(((argb shr 16) and 0xFF) / 255.0)} ${n(((argb shr 8) and 0xFF) / 255.0)} ${n((argb and 0xFF) / 255.0)}"
}

private fun n(value: Double): String = formatReal((value * 1000).roundToLong() / 1000.0)
