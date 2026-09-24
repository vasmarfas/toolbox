package com.vasmarfas.card.tools.documents.pdf

import kotlin.math.abs
import kotlin.math.floor

data class PageRef(val document: PdfDocument, val pageIndex: Int, val extraRotation: Int = 0)

object PdfAssembler {
    const val PRODUCER = "vasmarfas Toolbox"

    // the page tree is flat, so every page gets its inherited resources, boxes and rotation in its own
    // dictionary. Objects shared between pages of one source are copied once
    fun assemble(pages: List<PageRef>, info: Map<String, String> = emptyMap()): ByteArray {
        require(pages.isNotEmpty()) { "No pages to assemble" }
        for (page in pages) require(page.extraRotation % 90 == 0) { "Rotation must be a multiple of 90, got ${page.extraRotation}" }
        val writer = PdfWriter()
        val catalog = writer.reserve()
        val tree = writer.reserve()
        val copier = PageCopier(writer)
        val sources = pages.map { it.document.page(it.pageIndex) }
        val targets = sources.map { writer.reserve() }
        for (i in sources.indices) copier.register(sources[i], targets[i])
        for (i in sources.indices) writer[targets[i]] = copier.copyPage(sources[i], tree, targets[i], pages[i].extraRotation)
        writer[tree] = PdfDict("Type" to PdfName("Pages"), "Kids" to PdfArray(targets.toMutableList<PdfObject>()), "Count" to PdfInt.of(targets.size))
        writer[catalog] = PdfDict("Type" to PdfName("Catalog"), "Pages" to tree)
        val infoDict = PdfDict()
        for ((key, value) in info) infoDict[key] = PdfString.ofText(value)
        if ("Producer" !in infoDict) infoDict["Producer"] = PdfString.ofText(PRODUCER)
        return writer.toByteArray(catalog, writer.add(infoDict))
    }
}

private class PageCopier(private val writer: PdfWriter) {
    private class Pending(val document: PdfDocument, val source: PdfObject, val target: PdfRef)

    private class Annotation(val dict: PdfDict, val target: PdfRef)

    private val copies = HashMap<PdfDocument, HashMap<Int, PdfRef>>()
    private val inherited = HashMap<PdfObject, PdfRef>()
    private val pageTargets = HashMap<PdfDocument, HashMap<Int, PdfRef>>()
    private val namedDestinations = HashMap<PdfDocument, Map<PdfString, PdfObject>>()
    private val pending = ArrayDeque<Pending>()
    private var annotationDocument: PdfDocument? = null
    private var annotationCopies: Map<Int, PdfRef> = emptyMap()

    fun register(page: PdfPage, target: PdfRef) {
        val ref = page.ref ?: return
        pageTargets.getOrPut(page.document) { HashMap() }.getOrPut(ref.number) { target }
    }

    fun copyPage(page: PdfPage, parent: PdfRef, self: PdfRef, extraRotation: Int): PdfDict {
        val doc = page.document
        val out = PdfDict()
        out["Type"] = PdfName("Page")
        out["Parent"] = parent
        out["MediaBox"] = box(page.mediaBox)
        if (page.cropBox != page.mediaBox) out["CropBox"] = box(page.cropBox)
        val rotation = ((page.rotation + extraRotation) % 360 + 360) % 360
        if (rotation != 0) out["Rotate"] = PdfInt.of(rotation)
        out["Resources"] = resources(page)
        for ((key, value) in page.dict.entries) {
            if (key !in skippedPageKeys) put(out, key, copy(doc, value))
        }
        annotations(page, self)?.let { out["Annots"] = it }
        drain()
        return out
    }

    private fun resources(page: PdfPage): PdfObject {
        val doc = page.document
        val entry = page.resourcesEntry
        if (entry == null || doc.resolve(entry) !is PdfDict) return PdfDict()
        if (entry is PdfRef || page.dict["Resources"] === entry) return copy(doc, entry)
        inherited[entry]?.let { return it }
        val target = writer.reserve()
        inherited[entry] = target
        pending.addLast(Pending(doc, entry, target))
        return target
    }

    private fun annotations(page: PdfPage, self: PdfRef): PdfArray? {
        val doc = page.document
        val list = page.dict.array("Annots", doc) ?: return null
        val kept = ArrayList<Annotation>()
        val local = HashMap<Int, PdfRef>()
        for (item in list.items) {
            val annotation = doc.resolve(item) as? PdfDict ?: continue
            if (!keep(doc, annotation)) continue
            val target = writer.reserve()
            if (item is PdfRef) local[item.number] = target
            kept.add(Annotation(annotation, target))
        }
        if (kept.isEmpty()) return null
        annotationDocument = doc
        annotationCopies = local
        val result = PdfArray()
        for (annotation in kept) {
            writer[annotation.target] = copyAnnotation(doc, annotation.dict, self)
            result.add(annotation.target)
        }
        drain()
        annotationDocument = null
        annotationCopies = emptyMap()
        return result
    }

    private fun keep(doc: PdfDocument, annotation: PdfDict): Boolean {
        if (annotation.name("Subtype", doc) != "Link") return true
        annotation["Dest"]?.let { return destination(doc, it) != null }
        val action = annotation.dict("A", doc) ?: return true
        if (action.name("S", doc) != "GoTo") return true
        return destination(doc, action["D"] ?: return false) != null
    }

    // widgets lose /Parent: the output has no AcroForm, and the field tree would drag in widgets of other pages
    private fun copyAnnotation(doc: PdfDocument, annotation: PdfDict, self: PdfRef): PdfDict {
        val out = PdfDict()
        val widget = annotation.name("Subtype", doc) == "Widget"
        for ((key, value) in annotation.entries) {
            when (key) {
                "P", "StructParent" -> Unit
                "Parent" -> if (!widget) put(out, key, copy(doc, value))
                "Dest" -> destination(doc, value)?.let { out[key] = it }
                "A" -> action(doc, value)?.let { out[key] = it }
                else -> put(out, key, copy(doc, value))
            }
        }
        out["P"] = self
        return out
    }

    private fun action(doc: PdfDocument, value: PdfObject): PdfObject? {
        val dict = doc.resolve(value) as? PdfDict ?: return null
        if (dict.name("S", doc) != "GoTo") return copy(doc, value)
        val out = PdfDict()
        for ((key, entry) in dict.entries) {
            if (key == "D") {
                out["D"] = destination(doc, entry) ?: return null
            } else {
                put(out, key, copy(doc, entry))
            }
        }
        return out
    }

    private fun destination(doc: PdfDocument, dest: PdfObject): PdfArray? {
        val array = explicitDestination(doc, dest, 0) ?: return null
        val number = when (val first = array.items.firstOrNull()) {
            is PdfRef -> first.number
            is PdfInt -> doc.pages.getOrNull(first.value.toInt())?.ref?.number
            else -> null
        } ?: return null
        val target = pageTargets[doc]?.get(number) ?: return null
        val out = PdfArray(target)
        for (i in 1 until array.size) out.add(copy(doc, array[i]))
        return out
    }

    private fun explicitDestination(doc: PdfDocument, dest: PdfObject, depth: Int): PdfArray? {
        if (depth > 8) return null
        val next = when (val value = doc.resolve(dest)) {
            is PdfArray -> return value
            is PdfDict -> value["D"]
            is PdfName -> doc.catalog.dict("Dests", doc)?.get(value.name)
            is PdfString -> named(doc)[value] ?: doc.catalog.dict("Dests", doc)?.get(latin1(value.bytes))
            else -> null
        }
        return next?.let { explicitDestination(doc, it, depth + 1) }
    }

    private fun named(doc: PdfDocument): Map<PdfString, PdfObject> = namedDestinations.getOrPut(doc) {
        val result = HashMap<PdfString, PdfObject>()
        val root = doc.catalog.dict("Names", doc)?.dict("Dests", doc)
        val stack = ArrayDeque<PdfDict>()
        val seen = HashSet<PdfDict>()
        if (root != null) stack.addLast(root)
        while (stack.isNotEmpty()) {
            val node = stack.removeLast()
            if (!seen.add(node) || seen.size > 100_000) continue
            node.array("Names", doc)?.let { names ->
                for (i in 0 until names.size - 1 step 2) {
                    val key = names.resolved(i, doc) as? PdfString ?: continue
                    result.getOrPut(key) { names[i + 1] }
                }
            }
            node.array("Kids", doc)?.items?.forEach { kid -> (doc.resolve(kid) as? PdfDict)?.let { stack.addLast(it) } }
        }
        result
    }

    private fun copy(doc: PdfDocument, obj: PdfObject): PdfObject = when (obj) {
        is PdfRef -> reference(doc, obj)
        is PdfArray -> PdfArray(obj.items.mapTo(ArrayList(obj.size)) { copy(doc, it) })
        is PdfDict -> copyDict(doc, obj, skipLength = false)
        is PdfStream -> PdfStream(copyDict(doc, obj.dict, skipLength = true), obj.data)
        else -> obj
    }

    private fun copyDict(doc: PdfDocument, dict: PdfDict, skipLength: Boolean): PdfDict {
        val out = PdfDict()
        for ((key, value) in dict.entries) {
            if (skipLength && key == "Length") continue
            put(out, key, copy(doc, value))
        }
        return out
    }

    // the page tree, catalog and structure tree are never followed
    private fun reference(doc: PdfDocument, ref: PdfRef): PdfObject {
        if (doc === annotationDocument) annotationCopies[ref.number]?.let { return it }
        pageTargets[doc]?.get(ref.number)?.let { return it }
        val known = copies.getOrPut(doc) { HashMap() }
        known[ref.number]?.let { return it }
        if (doc.isPageTreeNode(ref.number)) return PdfNull
        val source = doc.resolve(ref)
        if (source is PdfNull || (source is PdfDict && source.name("Type") in structuralTypes)) return PdfNull
        val target = writer.reserve()
        known[ref.number] = target
        pending.addLast(Pending(doc, source, target))
        return target
    }

    private fun drain() {
        while (pending.isNotEmpty()) {
            val next = pending.removeFirst()
            writer[next.target] = copy(next.document, next.source)
        }
    }

    private fun put(dict: PdfDict, key: String, value: PdfObject) {
        if (value !is PdfNull) dict[key] = value
    }

    private fun box(rect: PdfRect) = PdfArray(number(rect.left), number(rect.bottom), number(rect.right), number(rect.top))

    private fun number(value: Double): PdfObject = if (value == floor(value) && abs(value) < 1e15) PdfInt.of(value.toLong()) else PdfReal(value)

    companion object {
        private val skippedPageKeys = setOf("Type", "Parent", "MediaBox", "CropBox", "Rotate", "Resources", "Annots", "StructParents", "B")
        private val structuralTypes = setOf("Catalog", "Pages", "Page", "StructTreeRoot", "StructElem", "Outlines", "OBJR", "MCR")
    }
}
