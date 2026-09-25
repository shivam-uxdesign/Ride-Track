package com.ridetrack.telemetry.processing

import com.ridetrack.telemetry.model.DataSourceKind
import com.ridetrack.telemetry.model.GpsQuality
import com.ridetrack.telemetry.model.LeanConfidence
import com.ridetrack.telemetry.model.MountCalibration
import com.ridetrack.telemetry.model.RideEvent
import com.ridetrack.telemetry.model.RideEventType
import com.ridetrack.telemetry.model.RideStats
import com.ridetrack.telemetry.model.SensorAvailability
import com.ridetrack.telemetry.model.TelemetryFrame
import com.ridetrack.telemetry.model.TelemetrySample
import com.ridetrack.telemetry.source.AccelReading
import com.ridetrack.telemetry.source.GyroReading
import com.ridetrack.telemetry.source.LocationReading
import com.ridetrack.telemetry.source.RawReading
import com.ridetrack.telemetry.source.SourceSignal
import com.ridetrack.telemetry.source.SourceStatusReading

/**
 * Raw readings in, derived telemetry out. Not thread-safe: feed it from one coroutine.
 *
 * High-rate readings go to [process]; the UI polls [frame] at a low rate and persistence
 * polls [sample] at ~1 Hz, so the UI never sees sensor-rate updates.
 */
class TelemetryPipeline(
    private val sourceKind: DataSourceKind,
    calibration: MountCalibration?,
    sensors: SensorAvailability,
    private val startNanos: Long,
    private val startWallMillis: Long,
    thresholds: EventThresholds = EventThresholds(),
) {
    private val gps = GpsProcessor()
    private val lean = LeanEstimator(calibration, sensors)
    private val dynamics = DynamicsProcessor(calibration, sensors)
    private val autoPause = AutoPauseDetector()
    private val detector = EventDetector(thresholds)
    private val accumulator = RideStatsAccumulator()

    private var lastAccountedNanos = startNanos
    private var lastGpsNanos: Long? = null
    private var lastDegradedEventMillis: Long? = null
    private var gpsLostReported = false

    val stats: RideStats get() = accumulator.stats
    val isStopped: Boolean get() = autoPause.isStopped
    /** Stopped after moving; the ride can be shown as paused. */
    val isPausedStop: Boolean get() = autoPause.isCountedStop

    fun wallMillis(nanos: Long): Long = startWallMillis + (nanos - startNanos) / 1_000_000L

    fun start(): RideEvent = record(RideEvent(RideEventType.START, startWallMillis, gps.latitude, gps.longitude, null))

    fun end(nowNanos: Long): List<RideEvent> {
        accountTime(nowNanos)
        val out = detector.flush().map(::record).toMutableList()
        out += record(RideEvent(RideEventType.END, wallMillis(nowNanos), gps.latitude, gps.longitude, currentSpeed()))
        return out
    }

    fun process(reading: RawReading): List<RideEvent> = when (reading) {
        is LocationReading -> onLocation(reading)
        is AccelReading -> onAccel(reading)
        is GyroReading -> {
            lean.onGyro(reading)
            emptyList()
        }
        is SourceStatusReading -> onStatus(reading)
    }

    private fun onLocation(r: LocationReading): List<RideEvent> {
        val out = ArrayList<RideEvent>()
        val wasLost = gpsLostReported
        accumulator.addDistance(gps.onLocation(r))
        val ctx = context(r.timeNanos)
        if (wasLost && gps.quality.hasFix) {
            gpsLostReported = false
            out += RideEvent(RideEventType.GPS_SIGNAL_RESTORED, ctx.timeMillis, ctx.latitude, ctx.longitude, ctx.speedMps)
        }

        val speed = gps.speedMps
        lean.speedMps = speed
        val dt = lastGpsNanos?.let { (r.timeNanos - it) / 1e9 } ?: 1.0
        lastGpsNanos = r.timeNanos
        dynamics.onGpsAccel(gps.accelMps2, dt)
        if (speed != null && (gps.quality == GpsQuality.GOOD || gps.quality == GpsQuality.EXCELLENT)) {
            accumulator.onReliableSpeed(speed)
        }

        when (autoPause.update(r.timeNanos, speed)) {
            AutoPauseDetector.Transition.STOPPED -> {
                out += detector.flush()
                if (autoPause.isCountedStop) {
                    out += RideEvent(RideEventType.STOP, ctx.timeMillis, ctx.latitude, ctx.longitude, speed)
                }
            }
            AutoPauseDetector.Transition.RESUMED, null -> Unit
        }
        if (!autoPause.isStopped) out += detector.onHeading(ctx, gps.headingDeg)
        return out.map(::record)
    }

    private fun onAccel(r: AccelReading): List<RideEvent> {
        lean.onAccel(r)
        dynamics.onAccel(r)
        val speed = gps.speedMps
        val moving = !autoPause.isStopped && speed != null && speed >= 2.0
        if (!moving) return emptyList()

        val leanDeg = lean.leanDeg?.takeIf { lean.confidence == LeanConfidence.GOOD }
        val longG = dynamics.longitudinalG
        accumulator.onDynamics(longG, lateralG())
        leanDeg?.let(accumulator::onLean)
        return detector.onDynamics(context(r.timeNanos), longG, leanDeg).map(::record)
    }

    private fun onStatus(r: SourceStatusReading): List<RideEvent> {
        val ctx = context(r.timeNanos)
        return when (r.signal) {
            SourceSignal.GPS_PROVIDER_DISABLED -> {
                gps.onProviderDisabled()
                lean.speedMps = null
                reportGpsLost(ctx)
            }
            SourceSignal.GPS_PROVIDER_ENABLED -> emptyList()
            SourceSignal.MOTION_SENSOR_UNRELIABLE -> {
                lean.sensorUnreliable = true
                val last = lastDegradedEventMillis
                if (last == null || ctx.timeMillis - last > 60_000) {
                    lastDegradedEventMillis = ctx.timeMillis
                    listOf(RideEvent(RideEventType.SENSOR_DEGRADED, ctx.timeMillis, ctx.latitude, ctx.longitude, ctx.speedMps))
                } else {
                    emptyList()
                }
            }
            SourceSignal.MOTION_SENSOR_RELIABLE -> {
                lean.sensorUnreliable = false
                emptyList()
            }
        }.map(::record)
    }

    private fun reportGpsLost(ctx: EventContext): List<RideEvent> {
        if (gpsLostReported || !gps.hasEverHadFix) return emptyList()
        gpsLostReported = true
        return listOf(RideEvent(RideEventType.GPS_SIGNAL_LOST, ctx.timeMillis, ctx.latitude, ctx.longitude, null))
    }

    /** Housekeeping (time accounting, GPS timeout) + a UI snapshot. Call at a low rate. */
    fun frame(nowNanos: Long): Pair<TelemetryFrame, List<RideEvent>> {
        val events = ArrayList<RideEvent>()
        if (gps.checkTimeout(nowNanos)) {
            lean.speedMps = null
            events += reportGpsLost(context(nowNanos)).map(::record)
        }
        accountTime(nowNanos)
        val frame = TelemetryFrame(
            timeMillis = wallMillis(nowNanos),
            elapsedMillis = (nowNanos - startNanos) / 1_000_000L,
            stats = accumulator.stats,
            speedMps = currentSpeed(),
            leanDeg = lean.leanDeg,
            leanConfidence = lean.confidence,
            longitudinalG = dynamics.longitudinalG,
            lateralG = lateralG(),
            headingDeg = gps.headingDeg.takeIf { gps.quality.hasFix },
            altitudeM = gps.altitudeM.takeIf { gps.quality.hasFix },
            gpsQuality = gps.quality,
            gpsAccuracyM = gps.accuracyM.takeIf { gps.quality.hasFix },
            isStopped = autoPause.isStopped,
            latitude = gps.latitude.takeIf { gps.quality.hasFix },
            longitude = gps.longitude.takeIf { gps.quality.hasFix },
            source = sourceKind,
        )
        return frame to events
    }

    fun sample(nowNanos: Long): TelemetrySample {
        val fix = gps.quality.hasFix
        val leanDeg = lean.leanDeg?.takeIf { lean.confidence != LeanConfidence.UNAVAILABLE }
        return TelemetrySample(
            timeMillis = wallMillis(nowNanos),
            latitude = gps.latitude.takeIf { fix },
            longitude = gps.longitude.takeIf { fix },
            speedMps = currentSpeed(),
            altitudeM = gps.altitudeM.takeIf { fix },
            headingDeg = gps.headingDeg.takeIf { fix },
            longitudinalG = dynamics.longitudinalG,
            lateralG = lateralG(),
            leanDeg = leanDeg,
            gpsAccuracyM = gps.accuracyM.takeIf { fix },
        )
    }

    private fun accountTime(nowNanos: Long) {
        val dtMillis = (nowNanos - lastAccountedNanos) / 1_000_000L
        if (dtMillis > 0) {
            accumulator.addTime(dtMillis, autoPause.isStopped)
            lastAccountedNanos += dtMillis * 1_000_000L
        }
    }

    /** Speed with sub-walking-pace GPS noise shown as a true standstill. */
    private fun currentSpeed(): Double? {
        if (!gps.quality.hasFix) return null
        val s = gps.speedMps ?: return null
        return if (s < 0.5) 0.0 else s
    }

    private fun lateralG(): Double? =
        dynamics.lateralG(currentSpeed(), lean.yawRateRadPerSec, gps.headingRateDegPerSec)

    private fun context(nanos: Long) = EventContext(
        timeMillis = wallMillis(nanos),
        latitude = gps.latitude,
        longitude = gps.longitude,
        speedMps = gps.speedMps,
    )

    private fun record(e: RideEvent): RideEvent {
        accumulator.onEvent(e)
        return e
    }
}
