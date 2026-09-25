package com.ridetrack.telemetry.processing

import com.ridetrack.telemetry.math.Geo
import com.ridetrack.telemetry.model.GpsQuality
import com.ridetrack.telemetry.source.LocationReading

/**
 * Turns raw location fixes into speed, heading, distance and a quality state.
 *
 * - Fixes worse than [maxAccuracyForDistanceM] never contribute distance.
 * - Stationary jitter (small moves within the accuracy radius at walking pace) is ignored.
 * - Physically implausible jumps are rejected.
 * - No fix for [lossTimeoutNanos] marks the signal [GpsQuality.LOST].
 */
class GpsProcessor(
    private val maxAccuracyForDistanceM: Double = 25.0,
    private val lossTimeoutNanos: Long = 5_000_000_000L,
    private val maxPlausibleSpeedMps: Double = 90.0,
) {
    var quality: GpsQuality = GpsQuality.UNAVAILABLE
        private set
    var speedMps: Double? = null
        private set
    var headingDeg: Double? = null
        private set
    /** Clockwise heading change rate derived from consecutive fixes, deg/s. */
    var headingRateDegPerSec: Double? = null
        private set
    var altitudeM: Double? = null
        private set
    var accuracyM: Double? = null
        private set
    var latitude: Double? = null
        private set
    var longitude: Double? = null
        private set
    /** Longitudinal acceleration derived from consecutive GPS speeds, m/s². */
    var accelMps2: Double? = null
        private set
    var hasEverHadFix: Boolean = false
        private set

    private var lastFixNanos: Long? = null
    private var anchor: LocationReading? = null
    private var previous: LocationReading? = null
    private var previousSpeed: Double? = null
    private var previousHeading: Double? = null
    private var previousHeadingNanos: Long? = null

    /** Returns the distance (m) this fix adds to the ride. */
    fun onLocation(r: LocationReading): Double {
        val prev = previous
        val dtSec = prev?.let { (r.timeNanos - it.timeNanos) / 1e9 }
        if (dtSec != null && dtSec <= 0.0) return 0.0 // out-of-order or duplicate fix

        lastFixNanos = r.timeNanos
        hasEverHadFix = true
        quality = GpsQuality.fromAccuracy(r.horizontalAccuracyM)
        accuracyM = r.horizontalAccuracyM
        latitude = r.latitude
        longitude = r.longitude
        if (r.altitudeM != null) altitudeM = r.altitudeM

        val derivedSpeed = if (prev != null && dtSec != null && dtSec < 5.0) {
            Geo.distanceM(prev.latitude, prev.longitude, r.latitude, r.longitude) / dtSec
        } else {
            null
        }
        val speed = r.speedMps ?: derivedSpeed
        speedMps = speed

        val prevSpeed = previousSpeed
        accelMps2 = if (speed != null && prevSpeed != null && dtSec != null && dtSec in 0.2..3.0) {
            (speed - prevSpeed) / dtSec
        } else {
            null
        }
        previousSpeed = speed

        updateHeading(r, prev, speed)
        previous = r
        return accumulateDistance(r, speed)
    }

    private fun updateHeading(r: LocationReading, prev: LocationReading?, speed: Double?) {
        if (speed == null || speed < 2.0) {
            headingRateDegPerSec = if (speed != null) 0.0 else null
            return
        }
        val heading = r.bearingDeg
            ?: prev?.let { Geo.bearingDeg(it.latitude, it.longitude, r.latitude, r.longitude) }
            ?: return
        val lastHeading = previousHeading
        val lastNanos = previousHeadingNanos
        headingRateDegPerSec = if (lastHeading != null && lastNanos != null) {
            val dt = (r.timeNanos - lastNanos) / 1e9
            if (dt in 0.2..3.0) Geo.headingDeltaDeg(lastHeading, heading) / dt else null
        } else {
            null
        }
        headingDeg = heading
        previousHeading = heading
        previousHeadingNanos = r.timeNanos
    }

    private fun accumulateDistance(r: LocationReading, speed: Double?): Double {
        val acc = r.horizontalAccuracyM
        if (acc != null && acc > maxAccuracyForDistanceM) return 0.0
        val a = anchor
        if (a == null) {
            anchor = r
            return 0.0
        }
        val d = Geo.distanceM(a.latitude, a.longitude, r.latitude, r.longitude)
        val dt = (r.timeNanos - a.timeNanos) / 1e9
        if (dt <= 0) return 0.0
        if (d / dt > maxPlausibleSpeedMps) return 0.0 // glitch; keep the old anchor
        val jitterRadius = maxOf(acc ?: 10.0, 4.0)
        if ((speed == null || speed < 1.0) && d < jitterRadius) return 0.0
        anchor = r
        return d
    }

    /** Call periodically. Returns true when the signal has just been declared lost. */
    fun checkTimeout(nowNanos: Long): Boolean {
        val last = lastFixNanos ?: return false
        if (quality.hasFix && nowNanos - last > lossTimeoutNanos) {
            markLost()
            return true
        }
        return false
    }

    fun onProviderDisabled() {
        markLost()
        quality = GpsQuality.UNAVAILABLE
    }

    private fun markLost() {
        quality = GpsQuality.LOST
        speedMps = null
        accelMps2 = null
        headingRateDegPerSec = null
        previousSpeed = null
        previousHeading = null
        previousHeadingNanos = null
    }
}
