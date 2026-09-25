package com.ridetrack.telemetry.state

enum class RideError {
    LOCATION_PERMISSION_REVOKED,
    STORAGE_FAILURE,
    SOURCE_FAILURE,
}

/**
 * The single source of truth for the ride lifecycle. Use [reduce] to move between states;
 * invalid transitions are ignored, so impossible UI states cannot occur.
 */
sealed interface RideState {
    data object Idle : RideState
    data object PreRideCheck : RideState
    data object Ready : RideState
    data class Recording(val rideId: String) : RideState
    data class Paused(val rideId: String) : RideState
    data class EndingRide(val rideId: String, val wasPaused: Boolean) : RideState
    data class Saving(val rideId: String) : RideState
    data class RideComplete(val rideId: String) : RideState
    data class Error(val error: RideError, val rideId: String?) : RideState

    /** A ride exists and telemetry is being collected. */
    val isActive: Boolean
        get() = this is Recording || this is Paused || this is EndingRide
}

sealed interface RideAction {
    data object BeginCheck : RideAction
    data object ChecksPassed : RideAction
    data object ChecksFailed : RideAction
    data object CancelCheck : RideAction
    data class Start(val rideId: String) : RideAction
    data object AutoPause : RideAction
    data object AutoResume : RideAction
    data object RequestEnd : RideAction
    data object CancelEnd : RideAction
    data object ConfirmEnd : RideAction
    data object Saved : RideAction
    data class Fail(val error: RideError) : RideAction
    data object Acknowledge : RideAction
}

fun reduce(state: RideState, action: RideAction): RideState = when (action) {
    RideAction.BeginCheck -> when (state) {
        RideState.Idle, is RideState.RideComplete, is RideState.Error -> RideState.PreRideCheck
        else -> state
    }
    RideAction.ChecksPassed -> if (state == RideState.PreRideCheck) RideState.Ready else state
    RideAction.ChecksFailed -> if (state == RideState.Ready) RideState.PreRideCheck else state
    RideAction.CancelCheck -> when (state) {
        RideState.PreRideCheck, RideState.Ready -> RideState.Idle
        else -> state
    }
    is RideAction.Start -> if (state == RideState.Ready) RideState.Recording(action.rideId) else state
    RideAction.AutoPause -> if (state is RideState.Recording) RideState.Paused(state.rideId) else state
    RideAction.AutoResume -> when (state) {
        is RideState.Paused -> RideState.Recording(state.rideId)
        is RideState.EndingRide -> state.copy(wasPaused = false)
        else -> state
    }
    RideAction.RequestEnd -> when (state) {
        is RideState.Recording -> RideState.EndingRide(state.rideId, wasPaused = false)
        is RideState.Paused -> RideState.EndingRide(state.rideId, wasPaused = true)
        else -> state
    }
    RideAction.CancelEnd -> if (state is RideState.EndingRide) {
        if (state.wasPaused) RideState.Paused(state.rideId) else RideState.Recording(state.rideId)
    } else {
        state
    }
    RideAction.ConfirmEnd -> if (state is RideState.EndingRide) RideState.Saving(state.rideId) else state
    RideAction.Saved -> if (state is RideState.Saving) RideState.RideComplete(state.rideId) else state
    is RideAction.Fail -> RideState.Error(action.error, rideIdOf(state))
    RideAction.Acknowledge -> when (state) {
        is RideState.RideComplete, is RideState.Error -> RideState.Idle
        else -> state
    }
}

private fun rideIdOf(state: RideState): String? = when (state) {
    is RideState.Recording -> state.rideId
    is RideState.Paused -> state.rideId
    is RideState.EndingRide -> state.rideId
    is RideState.Saving -> state.rideId
    is RideState.RideComplete -> state.rideId
    is RideState.Error -> state.rideId
    else -> null
}
