package com.vasmarfas.card.tools.media

object Bmp {
    private const val PIXELS_PER_METER = 2835

    // 24-bit BI_RGB when every pixel is opaque, otherwise 32-bit BI_BITFIELDS with an alpha mask in
    // a BITMAPV4HEADER
    fun encode(pixels: IntArray, width: Int, height: Int): ByteArray {
        require(width > 0 && height > 0 && pixels.size == width * height) { "image must have width × height pixels" }
        val opaque = pixels.all { it ushr 24 == 255 }
        val infoSize = if (opaque) 40 else 108
        val rowSize = if (opaque) (width * 3 + 3) and 3.inv() else width * 4
        val imageSize = rowSize * height
        val offset = 14 + infoSize
        val out = ByteSink(offset + imageSize)
        out.ascii("BM")
        out.intLE(offset + imageSize)
        out.intLE(0)
        out.intLE(offset)
        out.intLE(infoSize)
        out.intLE(width)
        out.intLE(height)
        out.shortLE(1)
        out.shortLE(if (opaque) 24 else 32)
        out.intLE(if (opaque) 0 else 3)
        out.intLE(imageSize)
        out.intLE(PIXELS_PER_METER)
        out.intLE(PIXELS_PER_METER)
        out.intLE(0)
        out.intLE(0)
        if (!opaque) {
            out.intLE(0x00FF0000)
            out.intLE(0x0000FF00)
            out.intLE(0x000000FF)
            out.intLE(-0x1000000)
            out.ascii("BGRs")
            repeat(12) { out.intLE(0) }
        }
        val padding = rowSize - width * (if (opaque) 3 else 4)
        for (y in height - 1 downTo 0) {
            for (x in y * width until (y + 1) * width) {
                val p = pixels[x]
                if (opaque) {
                    out.byte(p)
                    out.byte(p shr 8)
                    out.byte(p shr 16)
                } else {
                    out.intLE(p)
                }
            }
            repeat(padding) { out.byte(0) }
        }
        return out.toByteArray()
    }
}
