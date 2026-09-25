package com.vasmarfas.card.tools.documents.editor

import com.vasmarfas.card.tools.documents.pdf.PageCopier
import com.vasmarfas.card.tools.documents.pdf.PdfArray
import com.vasmarfas.card.tools.documents.pdf.PdfDict
import com.vasmarfas.card.tools.documents.pdf.PdfDocument
import com.vasmarfas.card.tools.documents.pdf.PdfInt
import com.vasmarfas.card.tools.documents.pdf.PdfName
import com.vasmarfas.card.tools.documents.pdf.PdfNull
import com.vasmarfas.card.tools.documents.pdf.PdfObject
import com.vasmarfas.card.tools.documents.pdf.PdfReal
import com.vasmarfas.card.tools.documents.pdf.PdfRef
import com.vasmarfas.card.tools.documents.pdf.PdfString
import com.vasmarfas.card.tools.documents.pdf.PdfWriter
import com.vasmarfas.card.tools.documents.pdf.asDouble

object PdfOutline {
    fun read(doc: PdfDocument, pageId: (Int) -> Long?): List<OutlineItem> {
        val root = doc.catalog.dict("Outlines", doc) ?: return emptyList()
        val copier = PageCopier(PdfWriter())
        val pageIndex = doc.pages.mapNotNull { page -> page.ref?.number?.let { it to page.index } }.toMap()
        val seen = HashSet<PdfDict>()

        fun target(item: PdfDict): Pair<Long?, Double?> {
            val dest = item["Dest"] ?: item.dict("A", doc)?.takeIf { it.name("S", doc) == "GoTo" }?.get("D") ?: return null to null
            val array = copier.explicitDestination(doc, dest) ?: return null to null
            val index = when (val first = array.items.firstOrNull()) {
                is PdfRef -> pageIndex[first.number]
                is PdfInt -> first.value.toInt()
                else -> null
            }
            val top = when (array.resolved(1, doc).let { (it as? PdfName)?.name }) {
                "XYZ" -> array.resolved(3, doc).asDouble()
                "FitH", "FitBH" -> array.resolved(2, doc).asDouble()
                else -> null
            }
            return index?.let(pageId) to top
        }

        fun level(first: PdfObject?, depth: Int): List<OutlineItem> {
            if (depth > 16) return emptyList()
            val out = ArrayList<OutlineItem>()
            var current = doc.resolve(first) as? PdfDict
            while (current != null && seen.add(current) && seen.size < 20_000) {
                val (page, top) = target(current)
                out += OutlineItem(
                    title = current.text("Title", doc).orEmpty(),
                    pageId = page,
                    top = top,
                    children = level(current["First"], depth + 1),
                    open = (current.int("Count", doc) ?: 0) > 0,
                )
                current = doc.resolve(current["Next"]) as? PdfDict
            }
            return out
        }
        return level(root["First"], 0)
    }

    internal fun write(writer: PdfWriter, items: List<OutlineItem>, pageTarget: (Long) -> PdfRef?): PdfRef? {
        if (items.isEmpty()) return null
        val root = writer.reserve()

        fun visible(list: List<OutlineItem>): Int = list.size + list.sumOf { if (it.open) visible(it.children) else 0 }

        fun level(list: List<OutlineItem>, parent: PdfRef): Pair<PdfRef, PdfRef> {
            val refs = list.map { writer.reserve() }
            list.forEachIndexed { i, item ->
                val dict = PdfDict("Title" to PdfString.ofText(item.title.ifEmpty { "…" }), "Parent" to parent)
                item.pageId?.let(pageTarget)?.let { page ->
                    dict["Dest"] = PdfArray(page, PdfName("XYZ"), PdfNull, item.top?.let { PdfReal(it) } ?: PdfNull, PdfNull)
                }
                if (i > 0) dict["Prev"] = refs[i - 1]
                if (i + 1 < refs.size) dict["Next"] = refs[i + 1]
                if (item.children.isNotEmpty()) {
                    val (first, last) = level(item.children, refs[i])
                    dict["First"] = first
                    dict["Last"] = last
                    val count = visible(item.children)
                    dict["Count"] = PdfInt.of(if (item.open) count else -count)
                }
                writer[refs[i]] = dict
            }
            return refs.first() to refs.last()
        }

        val (first, last) = level(items, root)
        writer[root] = PdfDict("Type" to PdfName("Outlines"), "First" to first, "Last" to last, "Count" to PdfInt.of(visible(items)))
        return root
    }
}
