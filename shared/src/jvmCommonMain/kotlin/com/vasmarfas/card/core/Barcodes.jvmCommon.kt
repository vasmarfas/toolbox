package com.vasmarfas.card.core

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.GenericMultipleBarcodeReader

private val pictureHints = mapOf(DecodeHintType.TRY_HARDER to true)

internal fun Result.toScanned(): ScannedCode = ScannedCode(text, label(barcodeFormat))

internal fun readLuminance(source: LuminanceSource): List<ScannedCode> =
    runCatching { GenericMultipleBarcodeReader(MultiFormatReader()).decodeMultiple(BinaryBitmap(HybridBinarizer(source)), pictureHints) }
        .getOrNull().orEmpty().distinctBy { it.text }.map { it.toScanned() }

internal actual fun scanPixels(pixels: IntArray, width: Int, height: Int): List<ScannedCode> =
    readLuminance(RGBLuminanceSource(width, height, pixels))

private fun label(format: BarcodeFormat): String = when (format) {
    BarcodeFormat.QR_CODE -> "QR"
    BarcodeFormat.EAN_13 -> "EAN-13"
    BarcodeFormat.EAN_8 -> "EAN-8"
    BarcodeFormat.UPC_A -> "UPC-A"
    BarcodeFormat.UPC_E -> "UPC-E"
    BarcodeFormat.CODE_128 -> "Code 128"
    BarcodeFormat.CODE_39 -> "Code 39"
    BarcodeFormat.CODE_93 -> "Code 93"
    BarcodeFormat.DATA_MATRIX -> "Data Matrix"
    BarcodeFormat.PDF_417 -> "PDF417"
    BarcodeFormat.RSS_14 -> "GS1 DataBar"
    BarcodeFormat.RSS_EXPANDED -> "GS1 DataBar Expanded"
    BarcodeFormat.AZTEC -> "Aztec"
    BarcodeFormat.CODABAR -> "Codabar"
    BarcodeFormat.ITF -> "ITF"
    BarcodeFormat.MAXICODE -> "MaxiCode"
    else -> format.name
}
