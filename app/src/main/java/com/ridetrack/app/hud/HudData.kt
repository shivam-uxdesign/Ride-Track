package com.ridetrack.app.hud

import com.ridetrack.app.ride.ActiveRide
import com.ridetrack.telemetry.model.DataSourceKind
import com.ridetrack.telemetry.model.GpsQuality
import com.ridetrack.telemetry.model.TelemetryFrame

enum class HudStatus { RECORDING, STOPPED, GPS_LOST }

/** Everything the pop-up shows, already reduced to what's known. Null = unknown. */
data class HudData(
    val status: HudStatus,
    val stoppedForMillis: Long?,
    val speedMps: Double?,
    val leanDeg: Double?,
    /** Why lean is missing, when it is. */
    val leanNote: String?,
    val distanceM: Double?,
    val elapsedMillis: Long?,
    val avgSpeedMps: Double?,
    val maxSpeedMps: Double?,
    val longitudinalG: Double?,
    val combinedG: Double?,
    /** Signed max lean (+ right), the larger side. */
    val maxLeanDeg: Double?,
    val headingDeg: Double?,
    val demo: Boolean,
) {
    companion object {
        fun from(frame: TelemetryFrame?, active: ActiveRide?, paused: Boolean, stoppedForMillis: Long?): HudData {
            val stats = frame?.stats
            val left = stats?.maxLeftLeanDeg
            val right = stats?.maxRightLeanDeg
            val maxLean = when {
                left == null && right == null -> null
                (right ?: 0.0) >= (left ?: 0.0) -> right
                else -> left?.let { -it }
            }
            val gpsLost = active?.source == DataSourceKind.PHONE &&
                (frame?.gpsQuality == GpsQuality.LOST || frame?.gpsQuality == GpsQuality.UNAVAILABLE)
            return HudData(
                status = when {
                    paused -> HudStatus.STOPPED
                    gpsLost -> HudStatus.GPS_LOST
                    else -> HudStatus.RECORDING
                },
                stoppedForMillis = stoppedForMillis.takeIf { paused },
                speedMps = frame?.speedMps,
                leanDeg = frame?.leanDeg,
                leanNote = when {
                    active?.calibrated == false -> "Mount not calibrated"
                    active?.sensors?.canEstimateLean == false -> "No gyroscope"
                    else -> null
                },
                distanceM = stats?.distanceM,
                elapsedMillis = frame?.elapsedMillis,
                avgSpeedMps = stats?.avgSpeedMps,
                maxSpeedMps = stats?.maxSpeedMps,
                longitudinalG = frame?.longitudinalG,
                combinedG = frame?.combinedG,
                maxLeanDeg = maxLean,
                headingDeg = frame?.headingDeg,
                demo = active?.source == DataSourceKind.DEMO,
            )
        }
    }
}
