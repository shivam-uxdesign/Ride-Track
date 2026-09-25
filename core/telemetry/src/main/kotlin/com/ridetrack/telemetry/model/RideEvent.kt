package com.ridetrack.telemetry.model

enum class RideEventType {
    START,
    STOP,
    END,
    HARD_BRAKE,
    STRONG_ACCELERATION,
    SIGNIFICANT_LEAN,
    LEFT_TURN,
    RIGHT_TURN,
    SHARP_DIRECTION_CHANGE,
    GPS_SIGNAL_LOST,
    GPS_SIGNAL_RESTORED,
    SENSOR_DEGRADED,
}

/**
 * A detected ride event. [value] meaning depends on [type]: G for brake/accel, signed
 * degrees for lean (+ right) and turns (heading change, + clockwise), seconds for stops.
 */
data class RideEvent(
    val type: RideEventType,
    val timeMillis: Long,
    val latitude: Double?,
    val longitude: Double?,
    val speedMps: Double?,
    val value: Double? = null,
)
