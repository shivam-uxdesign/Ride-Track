package com.ridetrack.telemetry.source

import com.ridetrack.telemetry.model.DataSourceKind
import com.ridetrack.telemetry.model.SensorAvailability
import kotlinx.coroutines.flow.Flow

/**
 * Common abstraction for anything that produces telemetry. The processing pipeline and UI
 * never depend on a concrete source, so future sources (Bluetooth OBD, external GPS/IMU,
 * TPMS) plug in here.
 */
interface TelemetrySource {
    val kind: DataSourceKind
    val sensors: SensorAvailability

    /** Cold flow of readings; collection starts the hardware, cancellation stops it. */
    fun readings(): Flow<RawReading>
}
