package com.ridetrack.telemetry.processing

import com.ridetrack.telemetry.math.Units
import com.ridetrack.telemetry.math.Vec3
import com.ridetrack.telemetry.model.MountCalibration
import com.ridetrack.telemetry.source.AccelReading
import com.ridetrack.telemetry.source.GyroReading
import kotlin.math.abs
import kotlin.math.sqrt

sealed interface CalibrationResult {
    data class Success(val calibration: MountCalibration) : CalibrationResult
    /** The bike/phone moved during the capture window. */
    data object TooMuchMotion : CalibrationResult
    /** Measured gravity is implausible (sensor fault or the phone was being handled). */
    data object ImplausibleGravity : CalibrationResult
    data object NotEnoughData : CalibrationResult
}

/**
 * Captures the phone mounting offset: the average gravity direction while the motorcycle
 * is held upright and stationary for [durationNanos].
 */
class CalibrationCollector(
    private val durationNanos: Long = 3_000_000_000L,
    private val maxAccelStdDev: Double = 0.35,
    private val maxMeanGyro: Double = 0.08,
) {
    private var firstNanos: Long? = null
    private var lastNanos: Long? = null
    private var n = 0
    private var sum = Vec3.ZERO
    private var sumMagSq = 0.0
    private var sumMag = 0.0
    private var gyroSum = 0.0
    private var gyroN = 0

    val progress: Double
        get() {
            val first = firstNanos ?: return 0.0
            val last = lastNanos ?: return 0.0
            return ((last - first).toDouble() / durationNanos).coerceIn(0.0, 1.0)
        }

    val isComplete: Boolean get() = progress >= 1.0

    fun onAccel(r: AccelReading) {
        if (firstNanos == null) firstNanos = r.timeNanos
        lastNanos = r.timeNanos
        val v = Vec3(r.x, r.y, r.z)
        sum += v
        val m = v.norm
        sumMag += m
        sumMagSq += m * m
        n++
    }

    fun onGyro(r: GyroReading) {
        gyroSum += Vec3(r.x, r.y, r.z).norm
        gyroN++
    }

    fun result(nowMillis: Long): CalibrationResult {
        if (n < 20 || !isComplete) return CalibrationResult.NotEnoughData
        val meanMag = sumMag / n
        val variance = (sumMagSq / n - meanMag * meanMag).coerceAtLeast(0.0)
        if (sqrt(variance) > maxAccelStdDev) return CalibrationResult.TooMuchMotion
        if (gyroN > 0 && gyroSum / gyroN > maxMeanGyro) return CalibrationResult.TooMuchMotion
        if (abs(meanMag - Units.STANDARD_GRAVITY) > 1.0) return CalibrationResult.ImplausibleGravity
        val mean = sum / n.toDouble()
        if (mean.norm < 1e-3) return CalibrationResult.ImplausibleGravity
        return CalibrationResult.Success(MountCalibration(up = mean.normalized(), createdAtMillis = nowMillis))
    }
}
