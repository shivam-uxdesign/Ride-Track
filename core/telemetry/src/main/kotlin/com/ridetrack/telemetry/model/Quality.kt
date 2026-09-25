package com.ridetrack.telemetry.model

enum class GpsQuality {
    /** No location permission / provider or no fix yet. */
    UNAVAILABLE,
    /** Had a fix but none for several seconds. */
    LOST,
    LOW_ACCURACY,
    GOOD,
    EXCELLENT;

    val hasFix: Boolean get() = this == LOW_ACCURACY || this == GOOD || this == EXCELLENT

    companion object {
        fun fromAccuracy(accuracyM: Double?): GpsQuality = when {
            accuracyM == null -> LOW_ACCURACY
            accuracyM <= 8.0 -> EXCELLENT
            accuracyM <= 20.0 -> GOOD
            else -> LOW_ACCURACY
        }
    }
}

enum class LeanConfidence {
    GOOD,
    /** Estimated but degraded (no speed reference, unreliable sensor accuracy...). */
    LOW,
    /** No gyroscope, no calibration, or no data yet. */
    UNAVAILABLE,
}

data class SensorAvailability(
    val accelerometer: Boolean,
    val gyroscope: Boolean,
    val magnetometer: Boolean,
) {
    val canEstimateLean: Boolean get() = accelerometer && gyroscope
}
