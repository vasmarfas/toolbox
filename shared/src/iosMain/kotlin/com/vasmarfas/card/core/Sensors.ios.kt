package com.vasmarfas.card.core

import kotlin.math.PI
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.hasTorch
import platform.AVFoundation.isTorchAvailable
import platform.AVFoundation.setTorchMode
import platform.CoreGraphics.CGRectGetHeight
import platform.CoreGraphics.CGRectGetWidth
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreMotion.CMAltimeter
import platform.CoreMotion.CMAttitudeReferenceFrameXMagneticNorthZVertical
import platform.CoreMotion.CMMotionManager
import platform.CoreMotion.CMPedometer
import platform.Foundation.NSDate
import platform.Foundation.NSOperationQueue
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceBatteryState
import platform.UIKit.UIScreen
import platform.darwin.NSObject

private val motionManager = CMMotionManager()

private const val GRAVITY = 9.80665

@OptIn(ExperimentalForeignApi::class)
actual fun availableSensors(): Set<SensorType> = buildSet {
    if (motionManager.accelerometerAvailable) add(SensorType.ACCELEROMETER)
    if (motionManager.gyroAvailable) add(SensorType.GYROSCOPE)
    if (motionManager.magnetometerAvailable) add(SensorType.MAGNETOMETER)
    if (motionManager.deviceMotionAvailable) {
        add(SensorType.ORIENTATION)
        add(SensorType.GRAVITY)
    }
    if (CMAltimeter.isRelativeAltitudeAvailable()) add(SensorType.PRESSURE)
    if (CMPedometer.isStepCountingAvailable()) add(SensorType.STEP_COUNTER)
}

@OptIn(ExperimentalForeignApi::class)
actual fun sensorFlow(type: SensorType): Flow<SensorReading> = callbackFlow {
    val queue = NSOperationQueue.mainQueue
    when (type) {
        SensorType.ACCELEROMETER -> {
            if (!motionManager.accelerometerAvailable) {
                close()
                return@callbackFlow
            }
            motionManager.accelerometerUpdateInterval = 0.05
            motionManager.startAccelerometerUpdatesToQueue(queue) { data, _ ->
                data?.acceleration?.useContents {
                    trySend(SensorReading(floatArrayOf((x * GRAVITY).toFloat(), (y * GRAVITY).toFloat(), (z * GRAVITY).toFloat()), currentEpochMillis()))
                }
            }
            awaitClose { motionManager.stopAccelerometerUpdates() }
        }

        SensorType.GYROSCOPE -> {
            if (!motionManager.gyroAvailable) {
                close()
                return@callbackFlow
            }
            motionManager.gyroUpdateInterval = 0.05
            motionManager.startGyroUpdatesToQueue(queue) { data, _ ->
                data?.rotationRate?.useContents {
                    trySend(SensorReading(floatArrayOf(x.toFloat(), y.toFloat(), z.toFloat()), currentEpochMillis()))
                }
            }
            awaitClose { motionManager.stopGyroUpdates() }
        }

        SensorType.MAGNETOMETER -> {
            if (!motionManager.magnetometerAvailable) {
                close()
                return@callbackFlow
            }
            motionManager.magnetometerUpdateInterval = 0.1
            motionManager.startMagnetometerUpdatesToQueue(queue) { data, _ ->
                data?.magneticField?.useContents {
                    trySend(SensorReading(floatArrayOf(x.toFloat(), y.toFloat(), z.toFloat()), currentEpochMillis()))
                }
            }
            awaitClose { motionManager.stopMagnetometerUpdates() }
        }

        SensorType.ORIENTATION, SensorType.GRAVITY -> {
            if (!motionManager.deviceMotionAvailable) {
                close()
                return@callbackFlow
            }
            motionManager.deviceMotionUpdateInterval = 0.05
            motionManager.startDeviceMotionUpdatesUsingReferenceFrame(
                CMAttitudeReferenceFrameXMagneticNorthZVertical,
                queue,
            ) { motion, _ ->
                if (motion == null) return@startDeviceMotionUpdatesUsingReferenceFrame
                if (type == SensorType.GRAVITY) {
                    motion.gravity.useContents {
                        trySend(SensorReading(floatArrayOf((x * GRAVITY).toFloat(), (y * GRAVITY).toFloat(), (z * GRAVITY).toFloat()), currentEpochMillis()))
                    }
                } else {
                    val attitude = motion.attitude
                    if (attitude != null) {
                        val azimuth = ((-attitude.yaw * 180.0 / PI) + 360.0) % 360.0
                        val pitch = attitude.pitch * 180.0 / PI
                        val roll = attitude.roll * 180.0 / PI
                        trySend(SensorReading(floatArrayOf(azimuth.toFloat(), pitch.toFloat(), roll.toFloat()), currentEpochMillis()))
                    }
                }
            }
            awaitClose { motionManager.stopDeviceMotionUpdates() }
        }

        SensorType.PRESSURE -> {
            if (!CMAltimeter.isRelativeAltitudeAvailable()) {
                close()
                return@callbackFlow
            }
            val altimeter = CMAltimeter()
            altimeter.startRelativeAltitudeUpdatesToQueue(queue) { data, _ ->
                val kpa = data?.pressure?.doubleValue
                if (kpa != null) trySend(SensorReading(floatArrayOf((kpa * 10.0).toFloat()), currentEpochMillis()))
            }
            awaitClose { altimeter.stopRelativeAltitudeUpdates() }
        }

        SensorType.STEP_COUNTER -> {
            if (!CMPedometer.isStepCountingAvailable()) {
                close()
                return@callbackFlow
            }
            val pedometer = CMPedometer()
            pedometer.startPedometerUpdatesFromDate(NSDate()) { data, _ ->
                val steps = data?.numberOfSteps?.floatValue
                if (steps != null) trySend(SensorReading(floatArrayOf(steps), currentEpochMillis()))
            }
            awaitClose { pedometer.stopPedometerUpdates() }
        }

        else -> close()
    }
}

actual suspend fun requestMotionAccess(): Boolean = true

private class LocationDelegate(private val onFix: (LocationFix) -> Unit) : NSObject(), CLLocationManagerDelegateProtocol {
    @OptIn(ExperimentalForeignApi::class)
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val location = didUpdateLocations.lastOrNull() as? CLLocation ?: return
        location.coordinate.useContents {
            onFix(
                LocationFix(
                    latitude = latitude,
                    longitude = longitude,
                    altitude = location.altitude.takeIf { location.verticalAccuracy >= 0 },
                    accuracy = location.horizontalAccuracy.takeIf { it >= 0 },
                    speed = location.speed.takeIf { it >= 0 },
                    bearing = location.course.takeIf { it >= 0 },
                    timestampMs = (location.timestamp.timeIntervalSince1970 * 1000).toLong(),
                    provider = "CoreLocation",
                ),
            )
        }
    }
}

actual fun locationSupported(): Boolean = CLLocationManager.locationServicesEnabled()

actual fun locationFlow(): Flow<LocationFix> = callbackFlow {
    val manager = CLLocationManager()
    val delegate = LocationDelegate { trySend(it) }
    manager.delegate = delegate
    manager.requestWhenInUseAuthorization()
    manager.startUpdatingLocation()
    awaitClose {
        manager.stopUpdatingLocation()
        manager.delegate = null
    }
}

actual suspend fun batteryInfo(): BatteryInfo? {
    val device = UIDevice.currentDevice
    device.batteryMonitoringEnabled = true
    val level = device.batteryLevel
    if (level < 0) return null
    val state = device.batteryState
    val charging = state == UIDeviceBatteryState.UIDeviceBatteryStateCharging || state == UIDeviceBatteryState.UIDeviceBatteryStateFull
    val status = when (state) {
        UIDeviceBatteryState.UIDeviceBatteryStateCharging -> "charging"
        UIDeviceBatteryState.UIDeviceBatteryStateFull -> "full"
        UIDeviceBatteryState.UIDeviceBatteryStateUnplugged -> "discharging"
        else -> "unknown"
    }
    return BatteryInfo((level * 100).toInt(), charging, listOf("Status" to status))
}

private fun torchDevice(): AVCaptureDevice? =
    AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)?.takeIf { it.hasTorch }

actual fun torchSupported(): Boolean = torchDevice() != null

@OptIn(ExperimentalForeignApi::class)
actual fun setTorch(on: Boolean): Boolean {
    val device = torchDevice() ?: return false
    if (!device.isTorchAvailable()) return false
    if (!device.lockForConfiguration(null)) return false
    device.setTorchMode(if (on) AVCaptureTorchModeOn else AVCaptureTorchModeOff)
    device.unlockForConfiguration()
    return true
}

@OptIn(ExperimentalForeignApi::class)
actual fun displayExtras(): List<Pair<String, String>> {
    val screen = UIScreen.mainScreen
    val bounds = screen.bounds
    return buildList {
        add("Scale" to "${screen.scale}x")
        add("Brightness" to "${(screen.brightness * 100).toInt()}%")
        bounds.useContents {
            add("Points" to "${size.width.toInt()} × ${size.height.toInt()}")
            add("Pixels" to "${(size.width * screen.scale).toInt()} × ${(size.height * screen.scale).toInt()}")
        }
    }
}

actual fun screenDpi(): Float? = (UIScreen.mainScreen.scale * 163.0).toFloat()

@OptIn(ExperimentalForeignApi::class)
actual fun screenPixels(): Pair<Int, Int>? = CGRectGetWidth(UIScreen.mainScreen.nativeBounds).toInt() to
    CGRectGetHeight(UIScreen.mainScreen.nativeBounds).toInt()

actual fun microphoneSupported(): Boolean = false

actual fun microphoneLevelFlow(): Flow<Double> = emptyFlow()

actual fun setScreenBrightness(value: Float?) {
    if (value != null) UIScreen.mainScreen.brightness = value.toDouble()
}
