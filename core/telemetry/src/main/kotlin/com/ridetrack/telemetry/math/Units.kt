package com.ridetrack.telemetry.math

object Units {
    const val STANDARD_GRAVITY = 9.80665

    fun mpsToKmh(mps: Double): Double = mps * 3.6
    fun kmhToMps(kmh: Double): Double = kmh / 3.6
    fun mps2ToG(a: Double): Double = a / STANDARD_GRAVITY
    fun nanosToSeconds(nanos: Long): Double = nanos / 1_000_000_000.0
    fun nanosToMillis(nanos: Long): Long = nanos / 1_000_000L
}
