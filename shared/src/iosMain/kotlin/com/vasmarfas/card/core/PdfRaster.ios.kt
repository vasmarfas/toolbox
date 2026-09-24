package com.vasmarfas.card.core

import androidx.compose.ui.graphics.ImageBitmap
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFRelease
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawPDFPage
import platform.CoreGraphics.CGContextFillRect
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGContextRotateCTM
import platform.CoreGraphics.CGContextScaleCTM
import platform.CoreGraphics.CGContextSetRGBFillColor
import platform.CoreGraphics.CGContextTranslateCTM
import platform.CoreGraphics.CGDataProviderCreateWithCFData
import platform.CoreGraphics.CGDataProviderRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGPDFDocumentCreateWithProvider
import platform.CoreGraphics.CGPDFDocumentGetNumberOfPages
import platform.CoreGraphics.CGPDFDocumentGetPage
import platform.CoreGraphics.CGPDFDocumentRef
import platform.CoreGraphics.CGPDFDocumentRelease
import platform.CoreGraphics.CGPDFPageGetBoxRect
import platform.CoreGraphics.CGPDFPageGetRotationAngle
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.kCGBitmapByteOrder32Big
import platform.CoreGraphics.kCGPDFCropBox

@OptIn(ExperimentalForeignApi::class)
actual class PdfRaster private constructor(private val document: CGPDFDocumentRef) {
    private val gate = RenderGate { CGPDFDocumentRelease(document) }

    actual val pageCount: Int = CGPDFDocumentGetNumberOfPages(document).toInt()

    actual suspend fun render(page: Int, width: Int): ImageBitmap = gate.use {
        withContext(Dispatchers.Default) {
            val source = CGPDFDocumentGetPage(document, (page + 1).convert()) ?: throw IllegalArgumentException("No page ${page + 1}")
            val (x, y, w, h) = CGPDFPageGetBoxRect(source, kCGPDFCropBox).useContents { listOf(origin.x, origin.y, size.width, size.height) }
            val angle = ((CGPDFPageGetRotationAngle(source) % 360) + 360) % 360
            val turned = angle % 180 != 0
            val pageWidth = if (turned) h else w
            val pageHeight = if (turned) w else h
            val scale = width / pageWidth
            val height = (pageHeight * scale).roundToInt().coerceAtLeast(1)
            val rgba = ByteArray(width * height * 4)
            val space = CGColorSpaceCreateDeviceRGB()
            rgba.usePinned { pinned ->
                val context = CGBitmapContextCreate(
                    pinned.addressOf(0), width.convert(), height.convert(), 8u, (width * 4).convert(), space,
                    CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value or kCGBitmapByteOrder32Big,
                )
                CGContextSetRGBFillColor(context, 1.0, 1.0, 1.0, 1.0)
                CGContextFillRect(context, CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()))
                CGContextScaleCTM(context, scale, scale)
                when (angle) {
                    90 -> {
                        CGContextTranslateCTM(context, 0.0, pageHeight)
                        CGContextRotateCTM(context, -PI / 2)
                    }
                    180 -> {
                        CGContextTranslateCTM(context, pageWidth, pageHeight)
                        CGContextRotateCTM(context, PI)
                    }
                    270 -> {
                        CGContextTranslateCTM(context, pageWidth, 0.0)
                        CGContextRotateCTM(context, PI / 2)
                    }
                }
                CGContextTranslateCTM(context, -x, -y)
                CGContextDrawPDFPage(context, source)
                CGContextRelease(context)
            }
            CGColorSpaceRelease(space)
            val pixels = IntArray(width * height) {
                val o = it * 4
                (0xFF shl 24) or ((rgba[o].toInt() and 0xFF) shl 16) or ((rgba[o + 1].toInt() and 0xFF) shl 8) or (rgba[o + 2].toInt() and 0xFF)
            }
            imageBitmapOf(pixels, width, height)
        }
    }

    actual fun close() {
        gate.close()
    }

    actual companion object {
        actual suspend fun open(bytes: ByteArray): PdfRaster {
            val data = bytes.usePinned { CFDataCreate(null, it.addressOf(0).reinterpret<UByteVar>(), bytes.size.convert()) }
            val provider = CGDataProviderCreateWithCFData(data)
            val document = CGPDFDocumentCreateWithProvider(provider)
            CGDataProviderRelease(provider)
            CFRelease(data)
            return PdfRaster(document ?: throw IllegalArgumentException("Not a PDF"))
        }
    }
}
