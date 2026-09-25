package com.ridetrack.app.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.LocationManager
import androidx.core.content.getSystemService
import androidx.core.location.LocationManagerCompat
import com.ridetrack.telemetry.model.SensorAvailability

class SensorInventory(private val context: Context) {
    private val sensorManager = context.getSystemService<SensorManager>()
    private val locationManager = context.getSystemService<LocationManager>()

    fun availability(): SensorAvailability = SensorAvailability(
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null,
        gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null,
        magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null,
    )

    fun hasGpsHardware(): Boolean = locationManager?.allProviders?.contains(LocationManager.GPS_PROVIDER) == true

    fun isGpsEnabled(): Boolean {
        val lm = locationManager ?: return false
        return LocationManagerCompat.isLocationEnabled(lm) && lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }
}
