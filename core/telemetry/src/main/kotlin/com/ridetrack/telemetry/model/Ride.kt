package com.ridetrack.telemetry.model

enum class RideStatus { IN_PROGRESS, COMPLETED }

data class Ride(
    val id: String,
    val bikeId: String,
    val name: String,
    val status: RideStatus,
    val source: DataSourceKind,
    val startTimeMillis: Long,
    val endTimeMillis: Long?,
    val stats: RideStats,
) {
    val durationMillis: Long? get() = endTimeMillis?.let { it - startTimeMillis }
}
