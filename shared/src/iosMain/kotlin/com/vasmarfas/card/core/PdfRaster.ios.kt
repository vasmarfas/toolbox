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
import platform.CoreGraphics.CGPDFPageRef
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.kCGBitmapByteOrder32Big
import platform.CoreGraphics.kCGPDFCropBox

@OptIn(ExperimentalForeignApi::class)
actual class PdfRaster private constructor(private val document: CGPDFDocumentRef) {
    private val gate = RenderGate { CGPDFDocumentRelease(document) }

    actual val pageCount: Int = CGPDFDocumentGetNumberOfPages(document).toInt()

    actual suspend fun render(page: Int, width: Int): ImageBitmap = gate.use {
        withContext(Dispatchers.Default) {
            val source = page(page)
            val (pageWidth, pageHeight) = size(source)
            val scale = width / pageWidth
            draw(source, scale, width, (pageHeight * scale).roundToInt().coerceAtLeast(1), 0.0, 0.0)
        }
    }

    actual suspend fun render(page: Int, pageWidth: Int, x: Int, y: Int, w: Int, h: Int): ImageBitmap = gate.use {
        withContext(Dispatchers.Default) {
            val source = page(page)
            val (points, pointsHigh) = size(source)
            val scale = pageWidth / points
            draw(source, scale, w, h, -x.toDouble(), -(pointsHigh * scale - y - h))
        }
    }

    private fun page(index: Int): CGPDFPageRef = CGPDFDocumentGetPage(document, (index + 1).convert()) ?: throw IllegalArgumentException("No page ${index + 1}")

    private fun size(source: CGPDFPageRef): Pair<Double, Double> {
        val (w, h) = CGPDFPageGetBoxRect(source, kCGPDFCropBox).useContents { size.width to size.height }
        return if (CGPDFPageGetRotationAngle(source) % 180 != 0) h to w else w to h
    }

    private fun draw(source: CGPDFPageRef, scale: Double, width: Int, height: Int, shiftX: Double, shiftY: Double): ImageBitmap {
        val (x, y) = CGPDFPageGetBoxRect(source, kCGPDFCropBox).useContents { origin.x to origin.y }
        val (pageWidth, pageHeight) = size(source)
        val angle = ((CGPDFPageGetRotationAngle(source) % 360) + 360) % 360
        val rgba = ByteArray(width * height * 4)
        val space = CGColorSpaceCreateDeviceRGB()
        rgba.usePinned { pinned ->
            val context = CGBitmapContextCreate(
                pinned.addressOf(0), width.convert(), height.convert(), 8u, (width * 4).convert(), space,
                CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value or kCGBitmapByteOrder32Big,
            )
            CGContextSetRGBFillColor(context, 1.0, 1.0, 1.0, 1.0)
            CGContextFillRect(context, CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()))
            CGContextTranslateCTM(context, shiftX, shiftY)
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
        return imageBitmapOf(pixels, width, height)
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
