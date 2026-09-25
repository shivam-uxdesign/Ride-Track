package com.ridetrack.telemetry

import com.ridetrack.telemetry.math.Geo
import com.ridetrack.telemetry.math.Vec3
import com.ridetrack.telemetry.model.MountCalibration
import com.ridetrack.telemetry.processing.CalibrationCollector
import com.ridetrack.telemetry.processing.CalibrationResult
import com.ridetrack.telemetry.source.AccelReading
import com.ridetrack.telemetry.source.GyroReading
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GeoAndCalibrationTest {

    @Test
    fun `one degree of latitude is about 111 km`() {
        assertEquals(111_195.0, Geo.distanceM(0.0, 0.0, 1.0, 0.0), 50.0)
    }

    @Test
    fun `heading delta wraps around north`() {
        assertEquals(20.0, Geo.headingDeltaDeg(350.0, 10.0), 1e-9)
        assertEquals(-20.0, Geo.headingDeltaDeg(10.0, 350.0), 1e-9)
    }

    @Test
    fun `destination round trips with distance and bearing`() {
        val (lat, lon) = Geo.destination(12.97, 77.59, 45.0, 1000.0)
        assertEquals(1000.0, Geo.distanceM(12.97, 77.59, lat, lon), 0.5)
        assertEquals(45.0, Geo.bearingDeg(12.97, 77.59, lat, lon), 0.1)
    }

    @Test
    fun `portrait mount derives forward and right axes`() {
        val cal = MountCalibration(up = Vec3.Y, createdAtMillis = 0)
        assertVec(Vec3(0.0, 0.0, -1.0), cal.forward)
        assertVec(Vec3.X, cal.right)
    }

    @Test
    fun `landscape mount derives forward and right axes`() {
        // Phone rotated 90° counter-clockwise: its +X points down... here +X points up.
        val cal = MountCalibration(up = Vec3.X, createdAtMillis = 0)
        assertVec(Vec3(0.0, 0.0, -1.0), cal.forward)
        assertVec(Vec3(0.0, -1.0, 0.0), cal.right)
    }

    @Test
    fun `stationary capture produces calibration`() {
        val c = CalibrationCollector()
        var t = 0L
        while (!c.isComplete) {
            c.onAccel(AccelReading(t, 0.2, 9.78, 0.4))
            c.onGyro(GyroReading(t, 0.001, 0.0, 0.002))
            t += 20_000_000L
        }
        val r = c.result(123)
        assertIs<CalibrationResult.Success>(r)
        assertTrue(r.calibration.up.y > 0.99)
    }

    @Test
    fun `movement during capture is rejected`() {
        val c = CalibrationCollector()
        var t = 0L
        var i = 0
        while (!c.isComplete) {
            val wobble = if (i++ % 2 == 0) 2.0 else -2.0
            c.onAccel(AccelReading(t, 0.0, 9.8 + wobble, 0.0))
            t += 20_000_000L
        }
        assertEquals(CalibrationResult.TooMuchMotion, c.result(0))
    }

    private fun assertVec(expected: Vec3, actual: Vec3) {
        assertTrue(abs(expected.x - actual.x) < 1e-9 && abs(expected.y - actual.y) < 1e-9 && abs(expected.z - actual.z) < 1e-9, "expected $expected but was $actual")
    }
}
