package com.ridetrack.telemetry.math

import kotlin.math.exp

/**
 * Time-constant based exponential moving average; robust to irregular sample intervals.
 */
class Ema(private val timeConstantSec: Double) {
    var value: Double? = null
        private set

    fun update(sample: Double, dtSec: Double): Double {
        val current = value
        val next = if (current == null || dtSec <= 0.0) {
            current ?: sample
        } else {
            val alpha = 1.0 - exp(-dtSec / timeConstantSec)
            current + alpha * (sample - current)
        }
        value = next
        return next
    }

    fun reset() {
        value = null
    }
}
