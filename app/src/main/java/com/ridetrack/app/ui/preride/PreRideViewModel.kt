package com.ridetrack.app.ui.preride

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ridetrack.app.AppContainer
import com.ridetrack.app.sensors.BatteryState
import com.ridetrack.app.sensors.Permissions
import com.ridetrack.telemetry.model.Bike
import com.ridetrack.telemetry.model.GpsQuality
import com.ridetrack.telemetry.model.SensorAvailability
import com.ridetrack.telemetry.source.LocationReading
import com.ridetrack.telemetry.state.RideState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GpsCheck { PERMISSION_NEEDED, DISABLED, NO_HARDWARE, SEARCHING, LOW_ACCURACY, GOOD, EXCELLENT, SIMULATED }

data class PreRideUiState(
    val loading: Boolean = true,
    val bike: Bike? = null,
    val demoMode: Boolean = false,
    val gps: GpsCheck = GpsCheck.SEARCHING,
    val gpsAccuracyM: Double? = null,
    val sensors: SensorAvailability = SensorAvailability(false, false, false),
    val battery: BatteryState? = null,
    val notificationsGranted: Boolean = true,
    val starting: Boolean = false,
) {
    /** GPS must be usable (a fix may still be on its way); demo mode needs nothing. */
    val canStart: Boolean
        get() = bike != null && !starting && (demoMode || gps !in setOf(GpsCheck.PERMISSION_NEEDED, GpsCheck.DISABLED, GpsCheck.NO_HARDWARE))
}

class PreRideViewModel(private val c: AppContainer) : ViewModel() {
    private val env = MutableStateFlow(Env())
    private var probeJob: Job? = null

    private data class Env(
        val gps: GpsCheck = GpsCheck.SEARCHING,
        val accuracy: Double? = null,
        val sensors: SensorAvailability = SensorAvailability(false, false, false),
        val battery: BatteryState? = null,
        val notifications: Boolean = true,
        val starting: Boolean = false,
    )

    val state: StateFlow<PreRideUiState> = combine(c.bikes.observeBikes(), c.settings.settings, env) { bikes, settings, e ->
        PreRideUiState(
            loading = false,
            bike = bikes.firstOrNull { it.id == settings.selectedBikeId } ?: bikes.firstOrNull(),
            demoMode = settings.demoMode,
            gps = if (settings.demoMode) GpsCheck.SIMULATED else e.gps,
            gpsAccuracyM = e.accuracy.takeIf { !settings.demoMode },
            sensors = e.sensors,
            battery = e.battery,
            notificationsGranted = e.notifications,
            starting = e.starting,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PreRideUiState())

    init {
        c.session.beginPreRideCheck()
        viewModelScope.launch {
            // Keep the ride state machine in sync with the checks.
            state.collect { s -> if (!s.loading && !s.starting) c.session.setChecksPassed(s.canStart) }
        }
        refresh()
    }

    /** Re-evaluates permissions, GPS and sensors (after a permission result or returning from Settings). */
    fun refresh() {
        val inv = c.sensorInventory
        val gps = when {
            !inv.hasGpsHardware() -> GpsCheck.NO_HARDWARE
            !Permissions.hasFineLocation(c.appContext) -> GpsCheck.PERMISSION_NEEDED
            !inv.isGpsEnabled() -> GpsCheck.DISABLED
            else -> env.value.gps.takeIf { it in setOf(GpsCheck.LOW_ACCURACY, GpsCheck.GOOD, GpsCheck.EXCELLENT) } ?: GpsCheck.SEARCHING
        }
        env.update {
            it.copy(
                gps = gps,
                accuracy = it.accuracy.takeIf { gps != GpsCheck.SEARCHING },
                sensors = inv.availability(),
                battery = c.battery.current(),
                notifications = Permissions.hasNotifications(c.appContext),
            )
        }
        if (gps == GpsCheck.SEARCHING || gps.hasFix()) startGpsProbe() else stopGpsProbe()
    }

    private fun GpsCheck.hasFix() = this == GpsCheck.LOW_ACCURACY || this == GpsCheck.GOOD || this == GpsCheck.EXCELLENT

    private fun startGpsProbe() {
        if (probeJob?.isActive == true) return
        probeJob = viewModelScope.launch {
            c.phoneSource().locationReadings().filterIsInstance<LocationReading>().collect { fix ->
                val check = when (GpsQuality.fromAccuracy(fix.horizontalAccuracyM)) {
                    GpsQuality.EXCELLENT -> GpsCheck.EXCELLENT
                    GpsQuality.GOOD -> GpsCheck.GOOD
                    else -> GpsCheck.LOW_ACCURACY
                }
                env.update { it.copy(gps = check, accuracy = fix.horizontalAccuracyM) }
            }
        }
    }

    private fun stopGpsProbe() {
        probeJob?.cancel()
        probeJob = null
    }

    fun start(onStarted: () -> Unit) {
        val s = state.value
        val bike = s.bike ?: return
        if (!s.canStart) return
        env.update { it.copy(starting = true) }
        viewModelScope.launch {
            stopGpsProbe()
            // Make sure the state machine has seen the latest check result.
            c.session.setChecksPassed(true)
            val id = c.session.start(bike)
            if (id != null) {
                onStarted()
            } else {
                env.update { it.copy(starting = false) }
                refresh()
            }
        }
    }

    fun cancel() {
        stopGpsProbe()
        c.session.cancelPreRideCheck()
    }

    override fun onCleared() {
        stopGpsProbe()
        val s = c.session.state.value
        if (s == RideState.PreRideCheck || s == RideState.Ready) c.session.cancelPreRideCheck()
    }

}
