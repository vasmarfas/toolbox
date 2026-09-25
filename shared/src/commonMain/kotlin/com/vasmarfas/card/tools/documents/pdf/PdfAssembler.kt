package com.vasmarfas.card.tools.documents.pdf

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
