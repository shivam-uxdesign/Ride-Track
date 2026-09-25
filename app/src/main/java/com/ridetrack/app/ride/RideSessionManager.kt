package com.ridetrack.app.ride

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.ridetrack.app.data.RideRepository
import com.ridetrack.app.data.SettingsRepository
import com.ridetrack.telemetry.demo.DemoTelemetrySource
import com.ridetrack.telemetry.model.Bike
import com.ridetrack.telemetry.model.DataSourceKind
import com.ridetrack.telemetry.model.MountCalibration
import com.ridetrack.telemetry.model.RideEvent
import com.ridetrack.telemetry.model.SensorAvailability
import com.ridetrack.telemetry.model.TelemetryFrame
import com.ridetrack.telemetry.model.TelemetrySample
import com.ridetrack.telemetry.processing.TelemetryPipeline
import com.ridetrack.telemetry.source.TelemetrySource
import com.ridetrack.telemetry.state.RideAction
import com.ridetrack.telemetry.state.RideError
import com.ridetrack.telemetry.state.RideState
import com.ridetrack.telemetry.state.reduce
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

/** The ride currently being recorded. */
data class ActiveRide(
    val rideId: String,
    val bikeId: String,
    val bikeName: String,
    val source: DataSourceKind,
    val startWallMillis: Long,
    val calibrated: Boolean,
    val sensors: SensorAvailability,
    /** Set when recent writes failed; data is being held in memory and retried. */
    val storageProblem: Boolean = false,
)

/**
 * Owns the ride lifecycle ([RideState]) and the recording loop. Lives for the whole
 * process so a ride survives activity recreation, rotation and backgrounding.
 *
 * Threading: all pipeline access happens on [recordingDispatcher] (single thread), so the
 * high-rate reading collector and the low-rate ticker never race.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RideSessionManager(
    private val context: Context,
    private val rides: RideRepository,
    private val settings: SettingsRepository,
    private val phoneSource: () -> TelemetrySource,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<RideState>(RideState.Idle)
    val state: StateFlow<RideState> = _state.asStateFlow()

    private val _frame = MutableStateFlow<TelemetryFrame?>(null)
    /** Low-rate (5 Hz) UI snapshot. */
    val frame: StateFlow<TelemetryFrame?> = _frame.asStateFlow()

    private val _active = MutableStateFlow<ActiveRide?>(null)
    val active: StateFlow<ActiveRide?> = _active.asStateFlow()

    private val autoPauseEnabled = settings.settings.map { it.autoPause }
        .stateIn(scope, SharingStarted.Eagerly, true)

    private val recordingDispatcher = Dispatchers.Default.limitedParallelism(1)
    private val lifecycleMutex = Mutex()
    private var recordingJob: Job? = null
    private var recorder: Recorder? = null

    private fun dispatch(action: RideAction): RideState = _state.updateAndGet { reduce(it, action) }

    fun beginPreRideCheck() {
        dispatch(RideAction.BeginCheck)
    }

    fun setChecksPassed(passed: Boolean) {
        dispatch(if (passed) RideAction.ChecksPassed else RideAction.ChecksFailed)
    }

    fun cancelPreRideCheck() {
        dispatch(RideAction.CancelCheck)
    }

    /** Starts recording. Only valid from [RideState.Ready]. Returns the ride id. */
    suspend fun start(bike: Bike): String? = lifecycleMutex.withLock {
        if (_state.value != RideState.Ready) return null
        val demo = settings.settings.first().demoMode
        val startNanos = SystemClock.elapsedRealtimeNanos()
        val startWall = System.currentTimeMillis()
        val rideId = UUID.randomUUID().toString()

        val source: TelemetrySource
        val calibration: MountCalibration?
        if (demo) {
            val demoSource = DemoTelemetrySource(clockNanos = SystemClock::elapsedRealtimeNanos)
            source = demoSource
            calibration = demoSource.calibration
        } else {
            source = phoneSource()
            calibration = bike.calibration
        }

        try {
            rides.create(rideId, bike.id, source.kind, startWall)
        } catch (e: Exception) {
            Log.e(TAG, "Could not create ride", e)
            dispatch(RideAction.Fail(RideError.STORAGE_FAILURE))
            return null
        }

        val pipeline = TelemetryPipeline(source.kind, calibration, source.sensors, startNanos, startWall)
        val rec = Recorder(rideId, pipeline).also { it.pendingEvents += pipeline.start() }
        recorder = rec
        _active.value = ActiveRide(
            rideId = rideId,
            bikeId = bike.id,
            bikeName = bike.displayName,
            source = source.kind,
            startWallMillis = startWall,
            calibrated = calibration != null,
            sensors = source.sensors,
        )
        _frame.value = pipeline.frame(startNanos).first
        dispatch(RideAction.Start(rideId))

        recordingJob = scope.launch(recordingDispatcher) {
            launch {
                try {
                    source.readings().collect { reading -> rec.pendingEvents += rec.pipeline.process(reading) }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e(TAG, "Telemetry source failed", e)
                }
            }
            tickLoop(rec)
        }
        if (source.kind == DataSourceKind.PHONE) RideRecordingService.start(context)
        rideId
    }

    private suspend fun CoroutineScope.tickLoop(rec: Recorder) {
        var tick = 0L
        while (isActive) {
            delay(TICK_MILLIS)
            tick++
            val now = SystemClock.elapsedRealtimeNanos()
            val (frame, events) = rec.pipeline.frame(now)
            rec.pendingEvents += events
            _frame.value = frame
            syncAutoPause(rec)
            if (tick % SAMPLE_EVERY_TICKS == 0L) rec.pendingSamples += rec.pipeline.sample(now)
            if (tick % FLUSH_EVERY_TICKS == 0L) flush(rec)
        }
    }

    private fun syncAutoPause(rec: Recorder) {
        val shouldPause = autoPauseEnabled.value && rec.pipeline.isPausedStop
        val s = _state.value
        if (shouldPause && s is RideState.Recording) dispatch(RideAction.AutoPause)
        if (!shouldPause && (s is RideState.Paused || (s is RideState.EndingRide && s.wasPaused))) {
            dispatch(RideAction.AutoResume)
        }
    }

    /** Writes buffered samples/events and the stats snapshot. Keeps data in memory on failure. */
    private suspend fun flush(rec: Recorder): Boolean {
        val samples = rec.pendingSamples.toList()
        val events = rec.pendingEvents.toList()
        return try {
            rides.append(rec.rideId, samples, events)
            rec.pendingSamples.subList(0, samples.size).clear()
            rec.pendingEvents.subList(0, events.size).clear()
            rides.snapshot(rec.rideId, rec.pipeline.stats, System.currentTimeMillis())
            if (_active.value?.storageProblem == true) _active.update { it?.copy(storageProblem = false) }
            true
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Flush failed; will retry", e)
            _active.update { it?.copy(storageProblem = true) }
            false
        }
    }

    fun requestEnd() {
        dispatch(RideAction.RequestEnd)
    }

    fun cancelEnd() {
        dispatch(RideAction.CancelEnd)
    }

    /** Stops recording and saves. Safe to call repeatedly; only the first call saves. */
    fun confirmEnd() {
        if (_state.value !is RideState.EndingRide) return
        dispatch(RideAction.ConfirmEnd)
        scope.launch {
            lifecycleMutex.withLock { finish() }
        }
    }

    private suspend fun finish() {
        val rec = recorder ?: return
        val saving = _state.value as? RideState.Saving ?: return
        withContext(NonCancellable) {
            recordingJob?.cancelAndJoin()
            recordingJob = null
            val ok = withContext(recordingDispatcher) {
                val now = SystemClock.elapsedRealtimeNanos()
                rec.pendingEvents += rec.pipeline.end(now)
                rec.pendingSamples += rec.pipeline.sample(now)
                var saved = false
                for (attempt in 1..3) {
                    if (flush(rec)) {
                        saved = true
                        break
                    }
                    delay(300L * attempt)
                }
                if (saved) {
                    val active = _active.value
                    val end = rec.pipeline.wallMillis(now)
                    val name = RideNames.forStart(active?.startWallMillis ?: end)
                    runCatching { rides.complete(rec.rideId, end, name) }.isSuccess
                } else {
                    false
                }
            }
            RideRecordingService.stop(context)
            recorder = null
            if (ok) {
                _active.value = null
                _frame.value = null
                dispatch(RideAction.Saved)
            } else {
                // The ride stays IN_PROGRESS in the database and is offered for recovery.
                _active.value = null
                _frame.value = null
                dispatch(RideAction.Fail(RideError.STORAGE_FAILURE))
            }
            if (saving.rideId != rec.rideId) Log.w(TAG, "State/recorder mismatch")
        }
    }

    /** Returns to idle after the summary or an error has been shown. */
    fun acknowledge() {
        dispatch(RideAction.Acknowledge)
    }

    private class Recorder(val rideId: String, val pipeline: TelemetryPipeline) {
        val pendingSamples = ArrayList<TelemetrySample>()
        val pendingEvents = ArrayList<RideEvent>()
    }

    companion object {
        private const val TAG = "RideSession"
        private const val TICK_MILLIS = 200L
        private const val SAMPLE_EVERY_TICKS = 5L // 1 Hz
        private const val FLUSH_EVERY_TICKS = 10L // every 2 s
    }
}
