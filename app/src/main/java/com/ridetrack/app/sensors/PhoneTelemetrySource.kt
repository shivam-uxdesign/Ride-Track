package com.ridetrack.app.sensors

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import androidx.core.content.getSystemService
import com.ridetrack.telemetry.model.DataSourceKind
import com.ridetrack.telemetry.model.SensorAvailability
import com.ridetrack.telemetry.source.AccelReading
import com.ridetrack.telemetry.source.GyroReading
import com.ridetrack.telemetry.source.LocationReading
import com.ridetrack.telemetry.source.RawReading
import com.ridetrack.telemetry.source.SourceSignal
import com.ridetrack.telemetry.source.SourceStatusReading
import com.ridetrack.telemetry.source.TelemetrySource
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.merge

/**
 * Phone GPS + IMU. Sensors are registered only while the flow is collected, on a
 * background thread, and unregistered as soon as collection stops.
 */
class PhoneTelemetrySource(
    private val context: Context,
    private val inventory: SensorInventory,
    private val includeGps: Boolean = true,
) : TelemetrySource {
    override val kind = DataSourceKind.PHONE
    override val sensors: SensorAvailability = inventory.availability()

    override fun readings(): Flow<RawReading> =
        merge(motionReadings(), if (includeGps) locationReadings() else emptyFlow())

    /** Accelerometer + gyroscope at ~50 Hz. */
    fun motionReadings(): Flow<RawReading> = callbackFlow {
        val sm = context.getSystemService<SensorManager>()
        if (sm == null) {
            close()
            return@callbackFlow
        }
        val thread = HandlerThread("ride-sensors").apply { start() }
        val handler = Handler(thread.looper)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val v = e.values
                val reading = when (e.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> AccelReading(e.timestamp, v[0].toDouble(), v[1].toDouble(), v[2].toDouble())
                    Sensor.TYPE_GYROSCOPE -> GyroReading(e.timestamp, v[0].toDouble(), v[1].toDouble(), v[2].toDouble())
                    else -> return
                }
                trySend(reading)
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                val signal = if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
                    SourceSignal.MOTION_SENSOR_UNRELIABLE
                } else {
                    SourceSignal.MOTION_SENSOR_RELIABLE
                }
                trySend(SourceStatusReading(SystemClock.elapsedRealtimeNanos(), signal))
            }
        }
        listOf(Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE).forEach { type ->
            sm.getDefaultSensor(type)?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME, handler) }
        }
        awaitClose {
            sm.unregisterListener(listener)
            thread.quitSafely()
        }
    }.buffer(capacity = 512, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** GPS fixes at 1 Hz. Requires location permission (checked by the caller). */
    @SuppressLint("MissingPermission")
    fun locationReadings(): Flow<RawReading> = callbackFlow {
        val lm = context.getSystemService<LocationManager>()
        if (lm == null || !Permissions.hasFineLocation(context)) {
            trySend(SourceStatusReading(SystemClock.elapsedRealtimeNanos(), SourceSignal.GPS_PROVIDER_DISABLED))
            awaitClose { }
            return@callbackFlow
        }
        val thread = HandlerThread("ride-gps").apply { start() }
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location.toReading())
            }

            override fun onProviderDisabled(provider: String) {
                trySend(SourceStatusReading(SystemClock.elapsedRealtimeNanos(), SourceSignal.GPS_PROVIDER_DISABLED))
            }

            override fun onProviderEnabled(provider: String) {
                trySend(SourceStatusReading(SystemClock.elapsedRealtimeNanos(), SourceSignal.GPS_PROVIDER_ENABLED))
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
        }
        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener, thread.looper)
        } catch (e: SecurityException) {
            trySend(SourceStatusReading(SystemClock.elapsedRealtimeNanos(), SourceSignal.GPS_PROVIDER_DISABLED))
        } catch (e: IllegalArgumentException) {
            // No GPS provider on this device.
            trySend(SourceStatusReading(SystemClock.elapsedRealtimeNanos(), SourceSignal.GPS_PROVIDER_DISABLED))
        }
        awaitClose {
            lm.removeUpdates(listener)
            thread.quitSafely()
        }
    }.buffer(capacity = 64)
}

fun Location.toReading() = LocationReading(
    timeNanos = elapsedRealtimeNanos,
    latitude = latitude,
    longitude = longitude,
    altitudeM = if (hasAltitude()) altitude else null,
    speedMps = if (hasSpeed()) speed.toDouble() else null,
    bearingDeg = if (hasBearing()) bearing.toDouble() else null,
    horizontalAccuracyM = if (hasAccuracy()) accuracy.toDouble() else null,
)
