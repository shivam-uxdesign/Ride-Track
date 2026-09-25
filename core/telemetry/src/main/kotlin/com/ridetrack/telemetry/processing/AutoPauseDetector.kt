package com.ridetrack.telemetry.processing

/**
 * Detects the stopped state from GPS speed with hysteresis. Never ends a ride.
 * The initial stationary period before the bike first moves is not counted as a stop.
 */
class AutoPauseDetector(
    private val stopSpeedMps: Double = 0.8,
    private val resumeSpeedMps: Double = 1.6,
    private val stopDelayNanos: Long = 5_000_000_000L,
    private val resumeDelayNanos: Long = 1_000_000_000L,
) {
    enum class Transition { STOPPED, RESUMED }

    var isStopped: Boolean = false
        private set
    var hasMoved: Boolean = false
        private set
    /** True while stopped *after* having moved — i.e. a stop that counts. */
    var isCountedStop: Boolean = false
        private set

    private var belowSince: Long? = null
    private var aboveSince: Long? = null

    fun update(timeNanos: Long, speedMps: Double?): Transition? {
        if (speedMps == null) {
            belowSince = null
            aboveSince = null
            return null
        }
        if (!isStopped) {
            if (speedMps >= resumeSpeedMps) hasMoved = true
            if (speedMps < stopSpeedMps) {
                val since = belowSince ?: timeNanos.also { belowSince = it }
                if (timeNanos - since >= stopDelayNanos) {
                    isStopped = true
                    isCountedStop = hasMoved
                    belowSince = null
                    return Transition.STOPPED
                }
            } else {
                belowSince = null
            }
        } else {
            if (speedMps >= resumeSpeedMps) {
                val since = aboveSince ?: timeNanos.also { aboveSince = it }
                if (timeNanos - since >= resumeDelayNanos) {
                    isStopped = false
                    isCountedStop = false
                    hasMoved = true
                    aboveSince = null
                    return Transition.RESUMED
                }
            } else {
                aboveSince = null
            }
        }
        return null
    }
}
