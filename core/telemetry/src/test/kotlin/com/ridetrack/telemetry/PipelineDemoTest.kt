package com.ridetrack.telemetry

import com.ridetrack.telemetry.demo.DemoRideModel
import com.ridetrack.telemetry.model.DataSourceKind
import com.ridetrack.telemetry.model.LeanConfidence
import com.ridetrack.telemetry.model.RideEvent
import com.ridetrack.telemetry.model.RideEventType
import com.ridetrack.telemetry.model.SensorAvailability
import com.ridetrack.telemetry.model.TelemetryFrame
import com.ridetrack.telemetry.processing.TelemetryPipeline
import com.ridetrack.telemetry.source.GyroReading
import com.ridetrack.telemetry.source.LocationReading
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Runs the simulated ride through the real pipeline, end to end. */
class PipelineDemoTest {

    private val allSensors = SensorAvailability(accelerometer = true, gyroscope = true, magnetometer = false)

    private class Run(val frames: List<Pair<Double, TelemetryFrame>>, val events: List<RideEvent>, val pipeline: TelemetryPipeline)

    private fun simulate(seconds: Double, sensors: SensorAvailability = allSensors, calibrated: Boolean = true): Run {
        val model = DemoRideModel()
        val pipeline = TelemetryPipeline(
            DataSourceKind.DEMO, model.calibration.takeIf { calibrated }, sensors, startNanos = 0, startWallMillis = 1_000_000,
        )
        val events = mutableListOf(pipeline.start())
        val frames = mutableListOf<Pair<Double, TelemetryFrame>>()
        var t = 0L
        var step = 0
        while (t < seconds * 1e9) {
            for (r in model.step(t)) {
                if (!sensors.gyroscope && r is GyroReading) continue
                events += pipeline.process(r)
            }
            if (step % 10 == 0) {
                val (frame, e) = pipeline.frame(t)
                frames += t / 1e9 to frame
                events += e
            }
            t += model.stepNanos
            step++
        }
        events += pipeline.end(t)
        return Run(frames, events, pipeline)
    }

    @Test
    fun `steady corner lean matches the physical lean`() {
        val run = simulate(60.0)
        // Middle of the right sweeper (script: 24 s .. 33 s), speed ~17 m/s, 12°/s.
        val frame = run.frames.first { it.first >= 31.0 }.second
        val speed = frame.speedMps!!
        val expected = Math.toDegrees(kotlin.math.atan(speed * Math.toRadians(12.0) / 9.80665))
        assertEquals(LeanConfidence.GOOD, frame.leanConfidence)
        assertTrue(frame.leanDeg!! > 0, "right lean should be positive")
        assertEquals(expected, frame.leanDeg!!, 3.0)
        // Left sweeper (37 s .. 46 s) leans left.
        assertTrue(run.frames.first { it.first >= 44.0 }.second.leanDeg!! < -10.0)
    }

    @Test
    fun `full loop produces sensible stats and events`() {
        val run = simulate(108.0)
        val stats = run.pipeline.stats
        val types = run.events.map { it.type }

        assertEquals(RideEventType.START, types.first())
        assertEquals(RideEventType.END, types.last())
        assertTrue(RideEventType.HARD_BRAKE in types, "events: $types")
        assertEquals(1, stats.stopCount, "events: $types")
        assertTrue(stats.rightTurns >= 1 && stats.leftTurns >= 1, "events: $types")
        assertTrue(RideEventType.SIGNIFICANT_LEAN in types)

        assertTrue(stats.distanceM in 1_000.0..2_000.0, "distance ${stats.distanceM}")
        assertTrue(stats.maxSpeedMps!! in 18.0..22.0)
        assertTrue(stats.maxBrakeG!! < -0.35)
        assertTrue(stats.maxRightLeanDeg!! > 20.0 && stats.maxLeftLeanDeg!! > 15.0)
        assertTrue(stats.stoppedMillis in 5_000..20_000, "stopped ${stats.stoppedMillis}")
        assertTrue(abs(stats.movingMillis + stats.stoppedMillis - 108_000) < 100)

        val brake = run.events.first { it.type == RideEventType.HARD_BRAKE }
        assertNotNull(brake.latitude)
        assertTrue(brake.speedMps!! > 10.0)
    }

    @Test
    fun `lean is unavailable without calibration or gyroscope`() {
        val uncalibrated = simulate(40.0, calibrated = false)
        assertTrue(uncalibrated.frames.all { it.second.leanDeg == null && it.second.leanConfidence == LeanConfidence.UNAVAILABLE })
        assertNull(uncalibrated.pipeline.stats.maxRightLeanDeg)

        val noGyro = simulate(40.0, sensors = allSensors.copy(gyroscope = false))
        assertTrue(noGyro.frames.all { it.second.leanDeg == null })
    }

    @Test
    fun `gps loss is reported and speed becomes unknown`() {
        val model = DemoRideModel()
        val p = TelemetryPipeline(DataSourceKind.DEMO, model.calibration, allSensors, 0, 0)
        var t = 0L
        val events = mutableListOf<RideEvent>()
        while (t < 20_000_000_000L) {
            model.step(t).filter { it !is LocationReading || t < 12_000_000_000L }.forEach { events += p.process(it) }
            events += p.frame(t).second
            t += model.stepNanos
        }
        assertTrue(events.any { it.type == RideEventType.GPS_SIGNAL_LOST })
        val frame = p.frame(t).first
        assertNull(frame.speedMps)
        assertNull(frame.latitude)
    }
}
