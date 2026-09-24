package com.vasmarfas.card.core

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.AVAudioApplication
import platform.AVFAudio.AVAudioApplicationRecordPermissionDenied
import platform.AVFAudio.AVAudioApplicationRecordPermissionGranted

actual suspend fun ensurePermission(permission: AppPermission): Boolean {
    if (permission != AppPermission.MICROPHONE) return true
    return when (AVAudioApplication.sharedInstance().recordPermission) {
        AVAudioApplicationRecordPermissionGranted -> true
        AVAudioApplicationRecordPermissionDenied -> false
        else -> suspendCancellableCoroutine { continuation ->
            AVAudioApplication.requestRecordPermissionWithCompletionHandler { granted -> continuation.resume(granted) }
        }
    }
}

actual fun hasPermission(permission: AppPermission): Boolean =
    permission != AppPermission.MICROPHONE || AVAudioApplication.sharedInstance().recordPermission == AVAudioApplicationRecordPermissionGranted
