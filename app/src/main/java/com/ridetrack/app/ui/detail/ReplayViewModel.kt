package com.ridetrack.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ridetrack.app.AppContainer
import com.ridetrack.app.ui.common.routePoints
import com.ridetrack.app.ui.components.GeoPoint
import com.ridetrack.telemetry.math.Geo
import com.ridetrack.telemetry.model.TelemetrySample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Replay data prepared once: samples, route and cumulative distance per sample. */
class ReplayTrack(val samples: List<TelemetrySample>) {
    val route: List<GeoPoint> = samples.routePoints()
    val startMillis: Long = samples.firstOrNull()?.timeMillis ?: 0L
    val durationMillis: Long = (samples.lastOrNull()?.timeMillis ?: 0L) - startMillis
    val cumulativeDistanceM: DoubleArray = DoubleArray(samples.size).also { out ->
        var total = 0.0
        var lastLat: Double? = null
        var lastLon: Double? = null
        samples.forEachIndexed { i, s ->
            val lat = s.latitude
            val lon = s.longitude
            if (lat != null && lon != null) {
                if (lastLat != null && lastLon != null) total += Geo.distanceM(lastLat!!, lastLon!!, lat, lon)
                lastLat = lat
                lastLon = lon
            }
            out[i] = total
        }
    }
}

data class ReplayState(
    val loading: Boolean = true,
    val track: ReplayTrack? = null,
    val positionMillis: Long = 0,
    val playing: Boolean = false,
    val speed: Float = 1f,
)

class ReplayViewModel(private val c: AppContainer, private val rideId: String) : ViewModel() {
    private val _state = MutableStateFlow(ReplayState())
    val state: StateFlow<ReplayState> = _state.asStateFlow()
    private var playJob: Job? = null

    init {
        viewModelScope.launch {
            val track = withContext(Dispatchers.Default) { ReplayTrack(c.rides.track(rideId).samples) }
            _state.update { it.copy(loading = false, track = track) }
        }
    }

    fun togglePlay() = if (_state.value.playing) pause() else play()

    fun play() {
        val track = _state.value.track ?: return
        if (track.durationMillis <= 0) return
        if (_state.value.positionMillis >= track.durationMillis) _state.update { it.copy(positionMillis = 0) }
        _state.update { it.copy(playing = true) }
        playJob?.cancel()
        playJob = viewModelScope.launch {
            var last = System.nanoTime()
            while (isActive) {
                delay(FRAME_MILLIS)
                val now = System.nanoTime()
                val realMillis = (now - last) / 1_000_000L
                last = now
                val advance = (realMillis * BASE_RATE * _state.value.speed).toLong()
                val next = (_state.value.positionMillis + advance).coerceAtMost(track.durationMillis)
                _state.update { it.copy(positionMillis = next) }
                if (next >= track.durationMillis) {
                    _state.update { it.copy(playing = false) }
                    break
                }
            }
        }
    }

    fun pause() {
        playJob?.cancel()
        playJob = null
        _state.update { it.copy(playing = false) }
    }

    fun setSpeed(speed: Float) = _state.update { it.copy(speed = speed) }

    fun seek(fraction: Float) {
        val track = _state.value.track ?: return
        _state.update { it.copy(positionMillis = (fraction.coerceIn(0f, 1f) * track.durationMillis).toLong()) }
    }

    companion object {
        private const val FRAME_MILLIS = 33L
        /** 1× replays 20 seconds of riding per second. */
        const val BASE_RATE = 20.0
        val SPEEDS = listOf(0.5f, 1f, 2f)
    }
}
