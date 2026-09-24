package com.vasmarfas.card.core

import kotlin.math.abs
import kotlin.math.roundToInt

// ppi counts physical pixels, nativeScale is physical pixels per point at the default zoom, scale is
// the UIKit scale Compose draws with. A model with two screens has two entries
class AppleDisplay(
    val name: String,
    val diagonalInches: Double,
    val widthPx: Int,
    val heightPx: Int,
    val ppi: Int,
    val scale: Int,
    val nativeScale: Double,
    val identifiers: List<String>,
) {
    fun pxPerInch(density: Float, nativeScale: Double = this.nativeScale): Double = ppi * density / nativeScale

    fun sameSize(width: Int, height: Int): Boolean = minOf(width, height) == minOf(widthPx, heightPx) && maxOf(width, height) == maxOf(widthPx, heightPx)
}

class AppleScreen(val model: String?, val nativeScale: Double?)

// iOS reports no physical density, so the ruler takes it from here. Sizes and ppi from the
// support.apple.com tech specs, scales from the HIG device table, identifiers checked against
// api.ipsw.me and AppleDB
val appleDisplays: List<AppleDisplay> = listOf(
    AppleDisplay("iPhone XS", 5.8, 1125, 2436, 458, 3, 3.0, listOf("iPhone11,2")),
    AppleDisplay("iPhone XS Max", 6.5, 1242, 2688, 458, 3, 3.0, listOf("iPhone11,4", "iPhone11,6")),
    AppleDisplay("iPhone XR", 6.1, 828, 1792, 326, 2, 2.0, listOf("iPhone11,8")),
    AppleDisplay("iPhone 11", 6.1, 828, 1792, 326, 2, 2.0, listOf("iPhone12,1")),
    AppleDisplay("iPhone 11 Pro", 5.8, 1125, 2436, 458, 3, 3.0, listOf("iPhone12,3")),
    AppleDisplay("iPhone 11 Pro Max", 6.5, 1242, 2688, 458, 3, 3.0, listOf("iPhone12,5")),
    AppleDisplay("iPhone SE (2nd generation)", 4.7, 750, 1334, 326, 2, 2.0, listOf("iPhone12,8")),
    AppleDisplay("iPhone 12 mini", 5.4, 1080, 2340, 476, 3, 2.88, listOf("iPhone13,1")),
    AppleDisplay("iPhone 12", 6.1, 1170, 2532, 460, 3, 3.0, listOf("iPhone13,2")),
    AppleDisplay("iPhone 12 Pro", 6.1, 1170, 2532, 460, 3, 3.0, listOf("iPhone13,3")),
    AppleDisplay("iPhone 12 Pro Max", 6.7, 1284, 2778, 458, 3, 3.0, listOf("iPhone13,4")),
    AppleDisplay("iPhone 13 mini", 5.4, 1080, 2340, 476, 3, 2.88, listOf("iPhone14,4")),
    AppleDisplay("iPhone 13", 6.1, 1170, 2532, 460, 3, 3.0, listOf("iPhone14,5")),
    AppleDisplay("iPhone 13 Pro", 6.1, 1170, 2532, 460, 3, 3.0, listOf("iPhone14,2")),
    AppleDisplay("iPhone 13 Pro Max", 6.7, 1284, 2778, 458, 3, 3.0, listOf("iPhone14,3")),
    AppleDisplay("iPhone SE (3rd generation)", 4.7, 750, 1334, 326, 2, 2.0, listOf("iPhone14,6")),
    AppleDisplay("iPhone 14", 6.1, 1170, 2532, 460, 3, 3.0, listOf("iPhone14,7")),
    AppleDisplay("iPhone 14 Plus", 6.7, 1284, 2778, 458, 3, 3.0, listOf("iPhone14,8")),
    AppleDisplay("iPhone 14 Pro", 6.1, 1179, 2556, 460, 3, 3.0, listOf("iPhone15,2")),
    AppleDisplay("iPhone 14 Pro Max", 6.7, 1290, 2796, 460, 3, 3.0, listOf("iPhone15,3")),
    AppleDisplay("iPhone 15", 6.1, 1179, 2556, 460, 3, 3.0, listOf("iPhone15,4")),
    AppleDisplay("iPhone 15 Plus", 6.7, 1290, 2796, 460, 3, 3.0, listOf("iPhone15,5")),
    AppleDisplay("iPhone 15 Pro", 6.1, 1179, 2556, 460, 3, 3.0, listOf("iPhone16,1")),
    AppleDisplay("iPhone 15 Pro Max", 6.7, 1290, 2796, 460, 3, 3.0, listOf("iPhone16,2")),
    AppleDisplay("iPhone 16", 6.1, 1179, 2556, 460, 3, 3.0, listOf("iPhone17,3")),
    AppleDisplay("iPhone 16 Plus", 6.7, 1290, 2796, 460, 3, 3.0, listOf("iPhone17,4")),
    AppleDisplay("iPhone 16 Pro", 6.3, 1206, 2622, 460, 3, 3.0, listOf("iPhone17,1")),
    AppleDisplay("iPhone 16 Pro Max", 6.9, 1320, 2868, 460, 3, 3.0, listOf("iPhone17,2")),
    AppleDisplay("iPhone 16e", 6.1, 1170, 2532, 460, 3, 3.0, listOf("iPhone17,5")),
    AppleDisplay("iPhone 17", 6.3, 1206, 2622, 460, 3, 3.0, listOf("iPhone18,3")),
    AppleDisplay("iPhone 17 Pro", 6.3, 1206, 2622, 460, 3, 3.0, listOf("iPhone18,1")),
    AppleDisplay("iPhone 17 Pro Max", 6.9, 1320, 2868, 460, 3, 3.0, listOf("iPhone18,2")),
    AppleDisplay("iPhone Air", 6.5, 1260, 2736, 460, 3, 3.0, listOf("iPhone18,4")),
    AppleDisplay("iPhone 17e", 6.1, 1170, 2532, 460, 3, 3.0, listOf("iPhone18,5")),
    AppleDisplay("iPhone 18 Pro", 6.3, 1206, 2622, 460, 3, 3.0, listOf("iPhone19,2")),
    AppleDisplay("iPhone 18 Pro Max", 6.9, 1320, 2868, 460, 3, 3.0, listOf("iPhone19,3", "iPhone19,7")),
    AppleDisplay("iPhone Duo", 7.6, 1878, 2670, 430, 3, 3.0, listOf("iPhone19,4")),
    AppleDisplay("iPhone Duo", 5.4, 1398, 2034, 460, 3, 3.0, listOf("iPhone19,4")),
    AppleDisplay("iPad (7th generation)", 10.2, 1620, 2160, 264, 2, 2.0, listOf("iPad7,11", "iPad7,12")),
    AppleDisplay("iPad (8th generation)", 10.2, 1620, 2160, 264, 2, 2.0, listOf("iPad11,6", "iPad11,7")),
    AppleDisplay("iPad (9th generation)", 10.2, 1620, 2160, 264, 2, 2.0, listOf("iPad12,1", "iPad12,2")),
    AppleDisplay("iPad (10th generation)", 10.9, 1640, 2360, 264, 2, 2.0, listOf("iPad13,18", "iPad13,19")),
    AppleDisplay("iPad (A16)", 11.0, 1640, 2360, 264, 2, 2.0, listOf("iPad15,7", "iPad15,8")),
    AppleDisplay("iPad mini (5th generation)", 7.9, 1536, 2048, 326, 2, 2.0, listOf("iPad11,1", "iPad11,2")),
    AppleDisplay("iPad mini (6th generation)", 8.3, 1488, 2266, 326, 2, 2.0, listOf("iPad14,1", "iPad14,2")),
    AppleDisplay("iPad mini (A17 Pro)", 8.3, 1488, 2266, 326, 2, 2.0, listOf("iPad16,1", "iPad16,2")),
    AppleDisplay("iPad Air (3rd generation)", 10.5, 1668, 2224, 264, 2, 2.0, listOf("iPad11,3", "iPad11,4")),
    AppleDisplay("iPad Air (4th generation)", 10.9, 1640, 2360, 264, 2, 2.0, listOf("iPad13,1", "iPad13,2")),
    AppleDisplay("iPad Air (5th generation)", 10.9, 1640, 2360, 264, 2, 2.0, listOf("iPad13,16", "iPad13,17")),
    AppleDisplay("iPad Air 11-inch (M2)", 11.0, 1640, 2360, 264, 2, 2.0, listOf("iPad14,8", "iPad14,9")),
    AppleDisplay("iPad Air 13-inch (M2)", 13.0, 2048, 2732, 264, 2, 2.0, listOf("iPad14,10", "iPad14,11")),
    AppleDisplay("iPad Air 11-inch (M3)", 11.0, 1640, 2360, 264, 2, 2.0, listOf("iPad15,3", "iPad15,4")),
    AppleDisplay("iPad Air 13-inch (M3)", 13.0, 2048, 2732, 264, 2, 2.0, listOf("iPad15,5", "iPad15,6")),
    AppleDisplay("iPad Air 11-inch (M4)", 11.0, 1640, 2360, 264, 2, 2.0, listOf("iPad16,8", "iPad16,9")),
    AppleDisplay("iPad Air 13-inch (M4)", 13.0, 2048, 2732, 264, 2, 2.0, listOf("iPad16,10", "iPad16,11")),
    AppleDisplay("iPad Pro 11-inch (1st generation)", 11.0, 1668, 2388, 264, 2, 2.0, listOf("iPad8,1", "iPad8,2", "iPad8,3", "iPad8,4")),
    AppleDisplay("iPad Pro 12.9-inch (3rd generation)", 12.9, 2048, 2732, 264, 2, 2.0, listOf("iPad8,5", "iPad8,6", "iPad8,7", "iPad8,8")),
    AppleDisplay("iPad Pro 11-inch (2nd generation)", 11.0, 1668, 2388, 264, 2, 2.0, listOf("iPad8,9", "iPad8,10")),
    AppleDisplay("iPad Pro 12.9-inch (4th generation)", 12.9, 2048, 2732, 264, 2, 2.0, listOf("iPad8,11", "iPad8,12")),
    AppleDisplay("iPad Pro 11-inch (3rd generation)", 11.0, 1668, 2388, 264, 2, 2.0, listOf("iPad13,4", "iPad13,5", "iPad13,6", "iPad13,7")),
    AppleDisplay("iPad Pro 12.9-inch (5th generation)", 12.9, 2048, 2732, 264, 2, 2.0, listOf("iPad13,8", "iPad13,9", "iPad13,10", "iPad13,11")),
    AppleDisplay("iPad Pro 11-inch (4th generation)", 11.0, 1668, 2388, 264, 2, 2.0, listOf("iPad14,3", "iPad14,4")),
    AppleDisplay("iPad Pro 12.9-inch (6th generation)", 12.9, 2048, 2732, 264, 2, 2.0, listOf("iPad14,5", "iPad14,6")),
    AppleDisplay("iPad Pro 11-inch (M4)", 11.0, 1668, 2420, 264, 2, 2.0, listOf("iPad16,3", "iPad16,4")),
    AppleDisplay("iPad Pro 13-inch (M4)", 13.0, 2064, 2752, 264, 2, 2.0, listOf("iPad16,5", "iPad16,6")),
    AppleDisplay("iPad Pro 11-inch (M5)", 11.0, 1668, 2420, 264, 2, 2.0, listOf("iPad17,1", "iPad17,2")),
    AppleDisplay("iPad Pro 13-inch (M5)", 13.0, 2064, 2752, 264, 2, 2.0, listOf("iPad17,3", "iPad17,4")),
)

// Safari gives only the size in points and the pixel ratio, several models can share them
fun appleCandidates(screen: AppleScreen, pixels: Pair<Int, Int>?, density: Float): List<AppleDisplay> {
    screen.model?.let { model ->
        val named = appleDisplays.filter { model in it.identifiers }
        return named.filter { pixels != null && it.sameSize(pixels.first, pixels.second) }.ifEmpty { named }
    }
    if (pixels == null || density <= 0f) return emptyList()
    val short = minOf(pixels.first, pixels.second) / density
    val long = maxOf(pixels.first, pixels.second) / density
    return appleDisplays.filter {
        it.scale == density.roundToInt() &&
            abs(minOf(it.widthPx, it.heightPx) / it.nativeScale - short) < 1.5 &&
            abs(maxOf(it.widthPx, it.heightPx) / it.nativeScale - long) < 1.5
    }
}
