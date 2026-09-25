package com.ridetrack.app.ui.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ridetrack.app.AppContainer
import com.ridetrack.app.data.LiveMetric
import com.ridetrack.app.ride.ActiveRide
import com.ridetrack.app.sensors.BatteryState
import com.ridetrack.telemetry.model.TelemetryFrame
import com.ridetrack.telemetry.state.RideState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class LiveChrome(
    val rideState: RideState = RideState.Idle,
    val active: ActiveRide? = null,
    val metrics: List<LiveMetric> = emptyList(),
    val showGIndicator: Boolean = true,
    val autoPause: Boolean = true,
    val battery: BatteryState? = null,
)

class LiveRideViewModel(private val c: AppContainer) : ViewModel() {
    /** Changes rarely: state machine, settings, battery. */
    val chrome: StateFlow<LiveChrome> = combine(
        c.session.state,
        c.session.active,
        c.settings.settings,
        c.battery.observe(),
    ) { state, active, settings, battery ->
        LiveChrome(
            rideState = state,
            active = active,
            metrics = LiveMetric.entries.filter { it in settings.liveMetrics }.take(LiveMetric.MAX_VISIBLE),
            showGIndicator = settings.showGForceIndicator,
            autoPause = settings.autoPause,
            battery = battery,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LiveChrome(rideState = c.session.state.value, active = c.session.active.value))

    /** 5 Hz telemetry snapshot. */
    val frame: StateFlow<TelemetryFrame?> = c.session.frame

    fun requestEnd() = c.session.requestEnd()
    fun cancelEnd() = c.session.cancelEnd()
    fun confirmEnd() = c.session.confirmEnd()
    fun acknowledge() = c.session.acknowledge()
}
