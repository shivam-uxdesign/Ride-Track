package com.ridetrack.telemetry.model

/** Derived, UI-facing snapshot of the ride. Emitted at a low, fixed rate. */
data class TelemetryFrame(
    val timeMillis: Long,
    val elapsedMillis: Long,
    val stats: RideStats,
    val speedMps: Double?,
    val leanDeg: Double?,
    val leanConfidence: LeanConfidence,
    val longitudinalG: Double?,
    val lateralG: Double?,
    val headingDeg: Double?,
    val altitudeM: Double?,
    val gpsQuality: GpsQuality,
    val gpsAccuracyM: Double?,
    val isStopped: Boolean,
    val latitude: Double?,
    val longitude: Double?,
    val source: DataSourceKind,
) {
    val combinedG: Double?
        get() = if (longitudinalG != null && lateralG != null) kotlin.math.hypot(longitudinalG, lateralG) else null
}
