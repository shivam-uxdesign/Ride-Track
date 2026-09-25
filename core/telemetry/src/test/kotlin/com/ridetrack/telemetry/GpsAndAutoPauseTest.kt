package com.ridetrack.telemetry

import com.ridetrack.telemetry.math.Geo
import com.ridetrack.telemetry.model.GpsQuality
import com.ridetrack.telemetry.processing.AutoPauseDetector
import com.ridetrack.telemetry.processing.GpsProcessor
import com.ridetrack.telemetry.source.LocationReading
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val SEC = 1_000_000_000L

class GpsAndAutoPauseTest {

    private fun fix(t: Long, lat: Double, lon: Double, speed: Double?, acc: Double? = 4.0) =
        LocationReading(t, lat, lon, null, speed, 0.0, acc)

    @Test
    fun `straight line distance accumulates`() {
        val gps = GpsProcessor()
        var total = 0.0
        var lat = 12.0
        for (i in 0..10) {
            total += gps.onLocation(fix(i * SEC, lat, 77.0, 10.0))
            lat = Geo.destination(lat, 77.0, 0.0, 10.0).first
        }
        assertEquals(100.0, total, 0.5)
        assertEquals(GpsQuality.EXCELLENT, gps.quality)
    }

    @Test
    fun `low accuracy fixes add no distance`() {
        val gps = GpsProcessor()
        gps.onLocation(fix(0, 12.0, 77.0, 10.0, acc = 50.0))
        val d = gps.onLocation(fix(SEC, 12.0001, 77.0, 10.0, acc = 60.0))
        assertEquals(0.0, d)
        assertEquals(GpsQuality.LOW_ACCURACY, gps.quality)
    }

    @Test
    fun `stationary jitter is ignored`() {
        val gps = GpsProcessor()
        var total = 0.0
        for (i in 0..30) {
            val jitter = if (i % 2 == 0) 0.00002 else -0.00002 // ~2 m
            total += gps.onLocation(fix(i * SEC, 12.0 + jitter, 77.0, 0.1, acc = 5.0))
        }
        assertEquals(0.0, total)
    }

    @Test
    fun `impossible jump is rejected`() {
        val gps = GpsProcessor()
        gps.onLocation(fix(0, 12.0, 77.0, 10.0))
        assertEquals(0.0, gps.onLocation(fix(SEC, 12.01, 77.0, 10.0)))
    }

    @Test
    fun `signal is declared lost after timeout and speed becomes unknown`() {
        val gps = GpsProcessor()
        gps.onLocation(fix(0, 12.0, 77.0, 10.0))
        assertFalse(gps.checkTimeout(3 * SEC))
        assertTrue(gps.checkTimeout(6 * SEC))
        assertEquals(GpsQuality.LOST, gps.quality)
        assertNull(gps.speedMps)
    }

    @Test
    fun `missing accuracy is never reported as excellent`() {
        val gps = GpsProcessor()
        gps.onLocation(fix(0, 12.0, 77.0, 10.0, acc = null))
        assertEquals(GpsQuality.LOW_ACCURACY, gps.quality)
        assertNull(gps.accuracyM)
    }

    @Test
    fun `auto pause uses hysteresis and ignores the initial wait`() {
        val ap = AutoPauseDetector()
        // Waiting at the start: stopped, but not a counted stop.
        for (i in 0..6) ap.update(i * SEC, 0.0)
        assertTrue(ap.isStopped)
        assertFalse(ap.isCountedStop)
        // Pull away.
        ap.update(7 * SEC, 5.0)
        assertEquals(AutoPauseDetector.Transition.RESUMED, ap.update(8 * SEC, 6.0))
        // Brief slow-down (3 s) does not count as a stop.
        for (i in 9..11) ap.update(i * SEC, 0.2)
        ap.update(12 * SEC, 8.0)
        assertFalse(ap.isStopped)
        // Real stop.
        var transition: AutoPauseDetector.Transition? = null
        for (i in 13..19) transition = ap.update(i * SEC, 0.0) ?: transition
        assertEquals(AutoPauseDetector.Transition.STOPPED, transition)
        assertTrue(ap.isCountedStop)
        // Unknown speed never changes state.
        assertNull(ap.update(30 * SEC, null))
        assertTrue(ap.isStopped)
    }
}
