package com.ridetrack.app.ui.bike

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ridetrack.app.AppContainer
import com.ridetrack.telemetry.processing.CalibrationCollector
import com.ridetrack.telemetry.processing.CalibrationResult
import com.ridetrack.telemetry.source.AccelReading
import com.ridetrack.telemetry.source.GyroReading
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

sealed interface CalibrationPhase {
    data object Instructions : CalibrationPhase
    data class Settling(val secondsLeft: Int) : CalibrationPhase
    data class Capturing(val progress: Float) : CalibrationPhase
    data object Success : CalibrationPhase
    data class Failed(val reason: String) : CalibrationPhase
    data object Unsupported : CalibrationPhase
    data object RideActive : CalibrationPhase
}

class CalibrationViewModel(private val c: AppContainer, private val bikeId: String) : ViewModel() {
    private val _phase = MutableStateFlow<CalibrationPhase>(
        when {
            c.session.state.value.isActive -> CalibrationPhase.RideActive
            !c.sensorInventory.availability().accelerometer -> CalibrationPhase.Unsupported
            else -> CalibrationPhase.Instructions
        },
    )
    val phase: StateFlow<CalibrationPhase> = _phase.asStateFlow()
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            // A short countdown so tapping the button doesn't shake the capture.
            for (s in 2 downTo 1) {
                _phase.value = CalibrationPhase.Settling(s)
                delay(1_000)
            }
            val collector = CalibrationCollector()
            _phase.value = CalibrationPhase.Capturing(0f)
            val completed = withTimeoutOrNull(8_000) {
                c.phoneSource(includeGps = false).motionReadings()
                    .takeWhile { !collector.isComplete }
                    .collect { r ->
                        when (r) {
                            is AccelReading -> collector.onAccel(r)
                            is GyroReading -> collector.onGyro(r)
                            else -> Unit
                        }
                        _phase.value = CalibrationPhase.Capturing(collector.progress.toFloat())
                    }
                true
            } ?: false
            _phase.value = when (val result = if (completed) collector.result(System.currentTimeMillis()) else CalibrationResult.NotEnoughData) {
                is CalibrationResult.Success -> {
                    c.bikes.setCalibration(bikeId, result.calibration)
                    CalibrationPhase.Success
                }
                CalibrationResult.TooMuchMotion -> CalibrationPhase.Failed("The phone moved during calibration. Keep the motorcycle upright and completely still, then try again.")
                CalibrationResult.ImplausibleGravity -> CalibrationPhase.Failed("Sensor readings looked wrong. Make sure the phone is firmly mounted and try again.")
                CalibrationResult.NotEnoughData -> CalibrationPhase.Failed("Not enough sensor data was received. Try again.")
            }
        }
    }

    fun reset() {
        job?.cancel()
        _phase.value = CalibrationPhase.Instructions
    }
}
