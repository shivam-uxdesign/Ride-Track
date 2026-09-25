package com.ridetrack.telemetry.model

enum class FuelType { PETROL, DIESEL, ELECTRIC, OTHER }

/** How the phone is mounted. Informational; lean math relies on [MountCalibration] instead. */
enum class MountOrientation { PORTRAIT, LANDSCAPE }

data class Bike(
    val id: String,
    val make: String,
    val model: String,
    val year: Int?,
    val displacementCc: Int?,
    val weightKg: Int?,
    val fuelType: FuelType,
    val mountOrientation: MountOrientation,
    val calibration: MountCalibration?,
    val createdAtMillis: Long,
) {
    val displayName: String get() = listOf(make, model).filter { it.isNotBlank() }.joinToString(" ")
}
