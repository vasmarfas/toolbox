package com.vasmarfas.card.tools.media

class IcoImage(val width: Int, val height: Int, val png: ByteArray)

object Ico {
    fun encode(images: List<IcoImage>): ByteArray {
        require(images.isNotEmpty()) { "an icon needs at least one image" }
        require(images.size <= 65535) { "too many images for one icon" }
        for (image in images) require(image.width in 1..256 && image.height in 1..256) { "icon images must be 1..256 pixels on each side" }
        val out = ByteSink(6 + images.size * 16 + images.sumOf { it.png.size })
        out.shortLE(0)
        out.shortLE(1)
        out.shortLE(images.size)
        var offset = 6 + images.size * 16
        for (image in images) {
            out.byte(image.width and 0xFF)
            out.byte(image.height and 0xFF)
            out.byte(0)
            out.byte(0)
            out.shortLE(1)
            out.shortLE(32)
            out.intLE(image.png.size)
            out.intLE(offset)
            offset += image.png.size
        }
        for (image in images) out.bytes(image.png)
        return out.toByteArray()
    }
}
