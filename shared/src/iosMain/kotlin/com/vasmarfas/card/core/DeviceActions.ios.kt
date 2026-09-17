package com.vasmarfas.card.core

import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.AudioToolbox.kSystemSoundID_Vibrate
import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.knownTimeZoneNames
import platform.Foundation.localTimeZone
import platform.Foundation.timeZoneWithName

actual fun playTone(frequencyHz: Double, durationMs: Int, volume: Float) {
    AudioServicesPlaySystemSound(1057u)
}

actual fun vibrate(durationMs: Int) {
    AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
}

actual fun timeZoneIds(): List<String> = NSTimeZone.knownTimeZoneNames.mapNotNull { it as? String }.sorted()

actual fun timeZoneOffsetSeconds(zoneId: String, epochSeconds: Long): Int? {
    val zone = NSTimeZone.timeZoneWithName(zoneId) ?: return null
    return zone.secondsFromGMTForDate(NSDate.dateWithTimeIntervalSince1970(epochSeconds.toDouble())).toInt()
}

actual fun systemTimeZoneId(): String = NSTimeZone.localTimeZone.name

// the status bar and the home indicator belong to the hosting SwiftUI scene, so the flag travels
// back to iOSApp rather than being applied here
object SystemBarsBridge {
    var onChange: ((Boolean) -> Unit)? = null
}

actual fun setSystemBarsHidden(hidden: Boolean) {
    SystemBarsBridge.onChange?.invoke(hidden)
}

