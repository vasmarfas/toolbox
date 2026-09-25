package com.vasmarfas.card.core

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.AVAudioApplication
import platform.AVFAudio.AVAudioApplicationRecordPermissionDenied
import platform.AVFAudio.AVAudioApplicationRecordPermissionGranted
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType

actual suspend fun ensurePermission(permission: AppPermission): Boolean = when (permission) {
    AppPermission.MICROPHONE -> when (AVAudioApplication.sharedInstance().recordPermission) {
        AVAudioApplicationRecordPermissionGranted -> true
        AVAudioApplicationRecordPermissionDenied -> false
        else -> suspendCancellableCoroutine { continuation ->
            AVAudioApplication.requestRecordPermissionWithCompletionHandler { granted -> continuation.resume(granted) }
        }
    }
    AppPermission.CAMERA -> when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
        AVAuthorizationStatusAuthorized -> true
        AVAuthorizationStatusNotDetermined -> suspendCancellableCoroutine { continuation ->
            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted -> continuation.resume(granted) }
        }
        else -> false
    }
    else -> true
}

actual fun hasPermission(permission: AppPermission): Boolean = when (permission) {
    AppPermission.MICROPHONE -> AVAudioApplication.sharedInstance().recordPermission == AVAudioApplicationRecordPermissionGranted
    AppPermission.CAMERA -> AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) == AVAuthorizationStatusAuthorized
    else -> true
}
