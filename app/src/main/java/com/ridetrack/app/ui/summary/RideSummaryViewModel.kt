package com.ridetrack.app.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ridetrack.app.AppContainer
import com.ridetrack.app.ui.common.routePoints
import com.ridetrack.app.ui.components.GeoPoint
import com.ridetrack.telemetry.model.Ride
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SummaryUiState(val loading: Boolean = true, val ride: Ride? = null, val bikeName: String? = null, val route: List<GeoPoint> = emptyList())

class RideSummaryViewModel(c: AppContainer, rideId: String) : ViewModel() {
    private val route = MutableStateFlow<List<GeoPoint>>(emptyList())

    val state: StateFlow<SummaryUiState> = combine(c.rides.observeRide(rideId), c.bikes.observeBikes(), route) { ride, bikes, r ->
        SummaryUiState(loading = false, ride = ride, bikeName = bikes.firstOrNull { it.id == ride?.bikeId }?.displayName, route = r)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SummaryUiState())

    init {
        viewModelScope.launch { route.value = c.rides.track(rideId).samples.routePoints() }
    }
}
