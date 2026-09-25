package com.ridetrack.app.ui.live

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.GpsOff
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ridetrack.app.data.LiveMetric
import com.ridetrack.app.ui.appViewModel
import com.ridetrack.app.ui.components.Chip
import com.ridetrack.app.ui.components.DemoBadge
import com.ridetrack.app.ui.components.GForceIndicator
import com.ridetrack.app.ui.components.Label
import com.ridetrack.app.ui.components.LeanArc
import com.ridetrack.app.ui.components.PrimaryButton
import com.ridetrack.app.ui.components.RtCard
import com.ridetrack.app.ui.components.StatBlock
import com.ridetrack.app.ui.components.leanColor
import com.ridetrack.app.ui.format.Format
import com.ridetrack.app.ui.theme.RtColors
import com.ridetrack.app.ui.theme.RtDimens
import com.ridetrack.app.ui.theme.RtType
import com.ridetrack.telemetry.model.DataSourceKind
import com.ridetrack.telemetry.model.GpsQuality
import com.ridetrack.telemetry.model.LeanConfidence
import com.ridetrack.telemetry.model.TelemetryFrame
import com.ridetrack.telemetry.state.RideError
import com.ridetrack.telemetry.state.RideState

@Composable
fun LiveRideScreen(onRideSaved: (String) -> Unit, onExit: () -> Unit) {
    val vm = appViewModel { LiveRideViewModel(it) }
    val chrome by vm.chrome.collectAsStateWithLifecycle()
    val frame by vm.frame.collectAsStateWithLifecycle()
    val state = chrome.rideState

    KeepScreenOn()
    // Back leaves the screen but never ends the ride.
    BackHandler { onExit() }

    LaunchedEffect(state) {
        when (state) {
            is RideState.RideComplete -> {
                vm.acknowledge()
                onRideSaved(state.rideId)
            }
            RideState.Idle, RideState.PreRideCheck, RideState.Ready -> onExit()
            else -> Unit
        }
    }

    if (state is RideState.Error) {
        ErrorContent(state.error, onDone = { vm.acknowledge(); onExit() })
        return
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = RtDimens.screenPadding),
    ) {
        val landscape = maxWidth > maxHeight && maxWidth > 560.dp
        if (landscape) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(RtDimens.lg)) {
                Column(Modifier.weight(1f).fillMaxSize(), verticalArrangement = Arrangement.Center) {
                    StatusBar(chrome, frame)
                    Spacer(Modifier.height(RtDimens.md))
                    SpeedAndLean(frame, chrome)
                }
                Column(Modifier.weight(1f).fillMaxSize()) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
                        Spacer(Modifier.height(RtDimens.md))
                        PrimaryMetrics(frame)
                        SecondaryMetrics(frame, chrome)
                    }
                    EndButton(state, vm::requestEnd)
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                StatusBar(chrome, frame)
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Spacer(Modifier.height(RtDimens.lg))
                    SpeedAndLean(frame, chrome)
                    Spacer(Modifier.height(RtDimens.lg))
                    PrimaryMetrics(frame)
                    SecondaryMetrics(frame, chrome)
                    Spacer(Modifier.height(RtDimens.md))
                }
                EndButton(state, vm::requestEnd)
            }
        }
    }

    if (state is RideState.EndingRide) {
        AlertDialog(
            onDismissRequest = vm::cancelEnd,
            title = { Text("End ride?", style = RtType.headline) },
            text = { Text("Recording will stop and the ride will be saved.", style = RtType.body) },
            confirmButton = { TextButton(onClick = vm::confirmEnd) { Text("END & SAVE", style = RtType.button, color = RtColors.Brake) } },
            dismissButton = { TextButton(onClick = vm::cancelEnd) { Text("CANCEL", style = RtType.button, color = RtColors.TextPrimary) } },
            containerColor = RtColors.SurfaceRaised,
        )
    }
    if (state is RideState.Saving) SavingOverlay()
}

@Composable
private fun KeepScreenOn() {
    val activity = LocalContext.current as? android.app.Activity
    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}

@Composable
private fun StatusBar(chrome: LiveChrome, frame: TelemetryFrame?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = RtDimens.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RtDimens.xs),
    ) {
        val paused = chrome.rideState is RideState.Paused || (chrome.rideState as? RideState.EndingRide)?.wasPaused == true
        Box(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
            if (paused) Chip("Paused · stopped", RtColors.Paused) else Chip("Ride in progress", RtColors.Ok)
        }
        Spacer(Modifier.weight(1f))
        if (chrome.active?.source == DataSourceKind.DEMO) DemoBadge()
        val gps = frame?.gpsQuality
        if (chrome.active?.source == DataSourceKind.PHONE && gps != null) {
            when (gps) {
                GpsQuality.LOST, GpsQuality.UNAVAILABLE -> Chip("GPS lost", RtColors.Error, icon = Icons.Outlined.GpsOff)
                GpsQuality.LOW_ACCURACY -> Chip("GPS low accuracy", RtColors.Warning)
                else -> Unit
            }
        }
        chrome.battery?.takeIf { it.isLow }?.let { Chip("${it.percent}%", RtColors.Warning, icon = Icons.Outlined.BatteryAlert) }
    }
    if (chrome.active?.storageProblem == true) {
        Text(
            "Storage is having trouble. Your ride is kept in memory and will be saved when possible.",
            style = RtType.caption,
            color = RtColors.Warning,
            modifier = Modifier.padding(top = RtDimens.xs),
        )
    }
}

@Composable
private fun SpeedAndLean(frame: TelemetryFrame?, chrome: LiveChrome) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        val speed = frame?.speedMps
        Text(
            Format.speedKmh(speed),
            style = RtType.speedHero,
            color = if (speed == null) RtColors.TextTertiary else RtColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { contentDescription = "Speed ${Format.speedWithUnit(speed)}" },
        )
        Text("km/h", style = RtType.unit, color = RtColors.TextSecondary)
        if (speed == null) {
            Spacer(Modifier.height(RtDimens.xxs))
            val msg = when (frame?.gpsQuality) {
                GpsQuality.LOST -> "GPS signal lost"
                GpsQuality.UNAVAILABLE, null -> "Waiting for GPS"
                else -> "Speed unavailable"
            }
            Text(msg, style = RtType.caption, color = RtColors.Warning)
        }

        Spacer(Modifier.height(RtDimens.lg))
        val lean = frame?.leanDeg
        LeanArc(
            lean,
            Modifier.widthIn(max = 320.dp),
            maxLeft = frame?.stats?.maxLeftLeanDeg,
            maxRight = frame?.stats?.maxRightLeanDeg,
        )
        Spacer(Modifier.height(RtDimens.xs))
        Text(Format.lean(lean), style = RtType.metricXL, color = if (lean == null) RtColors.TextTertiary else leanColor(lean))
        Label("Lean")
        val note = when {
            chrome.active?.calibrated == false -> "Unavailable · phone mount not calibrated"
            chrome.active?.sensors?.canEstimateLean == false -> "Unavailable · no gyroscope"
            frame?.leanConfidence == LeanConfidence.LOW -> "Estimate may be inaccurate"
            else -> null
        }
        if (note != null) {
            Spacer(Modifier.height(RtDimens.xxs))
            Text(note, style = RtType.caption, color = RtColors.TextSecondary)
        }
    }
}

@Composable
private fun PrimaryMetrics(frame: TelemetryFrame?) {
    val stats = frame?.stats
    Row(Modifier.fillMaxWidth()) {
        StatBlock("Distance", stats?.let { Format.distanceValue(it.distanceM) } ?: Format.DASH, Modifier.weight(1f), unit = "km", style = RtType.metricL)
        StatBlock(
            "Duration",
            frame?.let { Format.clock(it.elapsedMillis) } ?: Format.DASH,
            Modifier.weight(1f),
            style = RtType.metricL,
            horizontalAlignment = Alignment.End,
        )
    }
    Spacer(Modifier.height(RtDimens.lg))
    Row(Modifier.fillMaxWidth()) {
        StatBlock("Avg speed", Format.speedKmh(stats?.avgSpeedMps), Modifier.weight(1f), unit = "km/h")
        StatBlock("Max speed", Format.speedKmh(stats?.maxSpeedMps), Modifier.weight(1f), unit = "km/h", horizontalAlignment = Alignment.End)
    }
}

@Composable
private fun SecondaryMetrics(frame: TelemetryFrame?, chrome: LiveChrome) {
    if (chrome.metrics.isEmpty() && !chrome.showGIndicator) return
    Spacer(Modifier.height(RtDimens.lg))
    RtCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(RtDimens.md)) {
                chrome.metrics.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth()) {
                        pair.forEach { metric -> MetricCell(metric, frame, Modifier.weight(1f)) }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            if (chrome.showGIndicator) {
                Spacer(Modifier.width(RtDimens.sm))
                GForceIndicator(frame?.longitudinalG, frame?.lateralG, Modifier.width(if (chrome.metrics.isEmpty()) 180.dp else 124.dp))
            }
        }
    }
}

@Composable
private fun MetricCell(metric: LiveMetric, frame: TelemetryFrame?, modifier: Modifier) {
    val long = frame?.longitudinalG
    when (metric) {
        LiveMetric.ACCELERATION -> StatBlock(
            "Acceleration",
            Format.gSigned(long?.takeIf { it > 0 } ?: long?.let { 0.0 }),
            modifier,
            color = RtColors.Accel,
        )
        LiveMetric.BRAKING -> StatBlock(
            "Braking",
            Format.gSigned(long?.takeIf { it < 0 } ?: long?.let { 0.0 }),
            modifier,
            color = RtColors.Brake,
        )
        LiveMetric.G_FORCE -> StatBlock("G-force", Format.g(frame?.combinedG), modifier, color = RtColors.GForce)
        LiveMetric.MAX_LEAN -> {
            val s = frame?.stats
            val left = s?.maxLeftLeanDeg
            val right = s?.maxRightLeanDeg
            val value = when {
                left == null && right == null -> Format.DASH
                (right ?: 0.0) >= (left ?: 0.0) -> Format.lean(right)
                else -> Format.lean(left?.let { -it })
            }
            StatBlock("Max lean", value, modifier)
        }
        LiveMetric.HEADING -> StatBlock("Heading", Format.heading(frame?.headingDeg), modifier)
        LiveMetric.ALTITUDE -> StatBlock("Altitude", Format.altitude(frame?.altitudeM), modifier)
    }
}

@Composable
private fun EndButton(state: RideState, onEnd: () -> Unit) {
    Spacer(Modifier.height(RtDimens.sm))
    PrimaryButton(
        "End ride",
        onEnd,
        large = true,
        enabled = state is RideState.Recording || state is RideState.Paused,
        icon = Icons.Rounded.Stop,
        color = RtColors.SurfaceRaised,
        contentColor = RtColors.Brake,
        haptic = true,
    )
    Spacer(Modifier.height(RtDimens.md))
}

@Composable
private fun SavingOverlay() {
    Box(
        Modifier
            .fillMaxSize()
            .padding(RtDimens.xl),
        contentAlignment = Alignment.Center,
    ) {
        RtCard(color = RtColors.SurfaceRaised, modifier = Modifier.widthIn(max = 320.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = RtColors.Primary, strokeWidth = 3.dp, modifier = Modifier.width(28.dp).height(28.dp))
                Spacer(Modifier.width(RtDimens.md))
                Text("Saving ride…", style = RtType.bodyStrong, color = RtColors.TextPrimary)
            }
        }
    }
}

@Composable
private fun ErrorContent(error: RideError, onDone: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(RtDimens.screenPadding),
        verticalArrangement = Arrangement.Center,
    ) {
        RtCard {
            Chip("Problem", RtColors.Error)
            Spacer(Modifier.height(RtDimens.md))
            val (title, message) = when (error) {
                RideError.STORAGE_FAILURE -> "Ride not saved yet" to
                    "The ride couldn't be written to storage. Anything already recorded is kept and will be offered for recovery on the Home screen."
                RideError.LOCATION_PERMISSION_REVOKED -> "Location access removed" to
                    "Recording stopped because location permission was revoked. Recorded data is kept."
                RideError.SOURCE_FAILURE -> "Sensors stopped" to
                    "The phone stopped delivering sensor data. Recorded data is kept."
            }
            Text(title, style = RtType.headline, color = RtColors.TextPrimary)
            Spacer(Modifier.height(RtDimens.xs))
            Text(message, style = RtType.body, color = RtColors.TextSecondary)
            Spacer(Modifier.height(RtDimens.lg))
            PrimaryButton("Back to home", onDone)
        }
    }
}
