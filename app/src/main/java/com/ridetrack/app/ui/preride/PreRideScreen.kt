package com.ridetrack.app.ui.preride

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ridetrack.app.sensors.Permissions
import com.ridetrack.app.ui.appViewModel
import com.ridetrack.app.ui.components.DemoBadge
import com.ridetrack.app.ui.components.PrimaryButton
import com.ridetrack.app.ui.components.RtCard
import com.ridetrack.app.ui.components.ScreenHeader
import com.ridetrack.app.ui.components.SecondaryButton
import com.ridetrack.app.ui.components.StatusLevel
import com.ridetrack.app.ui.format.Format
import com.ridetrack.app.ui.theme.RtColors
import com.ridetrack.app.ui.theme.RtDimens
import com.ridetrack.app.ui.theme.RtType

@Composable
fun PreRideScreen(onBack: () -> Unit, onStarted: () -> Unit, onAddBike: () -> Unit, onCalibrate: (String) -> Unit) {
    val vm = appViewModel { PreRideViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var permissionDenied by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permissionDenied = !Permissions.hasFineLocation(context)
        vm.refresh()
    }
    fun requestPermissions() {
        val perms = buildList {
            addAll(Permissions.location)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }
    val canAskAgain = (context as? Activity)?.let {
        ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION)
    } ?: false

    val back = {
        vm.cancel()
        onBack()
    }
    BackHandler(onBack = back)

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = RtDimens.screenPadding),
    ) {
        ScreenHeader("Ready", subtitle = "Pre-ride check", onBack = back) {
            if (s.demoMode) DemoBadge()
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(RtDimens.xs))

            if (s.gps == GpsCheck.PERMISSION_NEEDED && !s.demoMode) {
                PermissionCard(
                    denied = permissionDenied,
                    permanentlyDenied = permissionDenied && !canAskAgain,
                    onRequest = ::requestPermissions,
                    onOpenSettings = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                        )
                    },
                )
                Spacer(Modifier.height(RtDimens.cardSpacing))
            }

            RtCard {
                val gpsRow = gpsRow(s)
                CheckRow("GPS", gpsRow.first, gpsRow.second)
                if (s.gps == GpsCheck.DISABLED) {
                    TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }) {
                        Text("Turn on location", color = RtColors.Primary)
                    }
                }
                Divider()
                val (motion, motionLevel) = when {
                    s.demoMode -> "Simulated" to StatusLevel.WARNING
                    s.sensors.canEstimateLean -> "Ready" to StatusLevel.OK
                    s.sensors.accelerometer -> "No gyroscope · lean unavailable" to StatusLevel.WARNING
                    else -> "Unavailable" to StatusLevel.ERROR
                }
                CheckRow("Motion sensors", motion, motionLevel)
                Divider()
                CheckRow(
                    "Compass",
                    if (s.demoMode) "Not used" else if (s.sensors.magnetometer) "Available" else "Not available",
                    if (s.sensors.magnetometer || s.demoMode) StatusLevel.OK else StatusLevel.INACTIVE,
                )
                Divider()
                val bike = s.bike
                val calibrated = s.demoMode || bike?.calibration != null
                CheckRow(
                    "Phone mount",
                    when {
                        s.demoMode -> "Simulated"
                        calibrated -> "Calibrated"
                        else -> "Not calibrated · lean unavailable"
                    },
                    if (calibrated) StatusLevel.OK else StatusLevel.WARNING,
                )
                if (!calibrated && bike != null) {
                    TextButton(onClick = { onCalibrate(bike.id) }) { Text("Calibrate now", color = RtColors.Primary) }
                }
                Divider()
                val battery = s.battery
                CheckRow(
                    "Battery",
                    battery?.let { "${it.percent}%" + if (it.charging) " · charging" else "" } ?: "Unavailable",
                    when {
                        battery == null -> StatusLevel.INACTIVE
                        battery.isLow -> StatusLevel.WARNING
                        else -> StatusLevel.OK
                    },
                )
                Divider()
                CheckRow("Bike", bike?.displayName ?: "No bike added", if (bike != null) StatusLevel.OK else StatusLevel.ERROR)
                if (bike == null && !s.loading) {
                    TextButton(onClick = onAddBike) { Text("Add bike", color = RtColors.Primary) }
                }
            }

            if (s.gps == GpsCheck.SEARCHING && !s.demoMode) {
                Spacer(Modifier.height(RtDimens.sm))
                Text(
                    "You can start now. Distance and route begin recording once GPS has a fix.",
                    style = RtType.caption,
                    color = RtColors.TextSecondary,
                )
            }
            if (s.battery?.isLow == true) {
                Spacer(Modifier.height(RtDimens.sm))
                Text("Battery is low. Consider charging while you ride.", style = RtType.caption, color = RtColors.Warning)
            }
            if (s.demoMode) {
                Spacer(Modifier.height(RtDimens.sm))
                Text(
                    "Demo mode is on: this ride will use simulated telemetry and be labelled DEMO. Turn it off in Profile.",
                    style = RtType.caption,
                    color = RtColors.Warning,
                )
            }
            Spacer(Modifier.height(RtDimens.lg))
        }
        PrimaryButton(
            text = if (s.starting) "Starting…" else "Start ride",
            onClick = { vm.start(onStarted) },
            enabled = s.canStart,
            large = true,
            icon = Icons.Rounded.PlayArrow,
            haptic = true,
        )
        Spacer(Modifier.height(RtDimens.md))
    }
}

private fun gpsRow(s: PreRideUiState): Pair<String, StatusLevel> = when (s.gps) {
    GpsCheck.SIMULATED -> "Simulated" to StatusLevel.WARNING
    GpsCheck.PERMISSION_NEEDED -> "Permission needed" to StatusLevel.ERROR
    GpsCheck.DISABLED -> "Location is off" to StatusLevel.ERROR
    GpsCheck.NO_HARDWARE -> "Unavailable" to StatusLevel.ERROR
    GpsCheck.SEARCHING -> "Searching…" to StatusLevel.WARNING
    GpsCheck.LOW_ACCURACY -> "Low accuracy · ${Format.accuracy(s.gpsAccuracyM)}" to StatusLevel.WARNING
    GpsCheck.GOOD -> "Good · ${Format.accuracy(s.gpsAccuracyM)}" to StatusLevel.OK
    GpsCheck.EXCELLENT -> "Excellent · ${Format.accuracy(s.gpsAccuracyM)}" to StatusLevel.OK
}

@Composable
private fun Divider() = HorizontalDivider(color = RtColors.Outline.copy(alpha = 0.6f))

@Composable
private fun CheckRow(label: String, value: String, level: StatusLevel) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$label: $value" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = RtType.body, color = RtColors.TextSecondary, modifier = Modifier.weight(1f))
        Box(
            Modifier
                .padding(end = 8.dp)
                .size(8.dp)
                .background(level.color, CircleShape),
        )
        Text(value, style = RtType.bodyStrong.copy(fontSize = RtType.body.fontSize), color = RtColors.TextPrimary)
    }
}

@Composable
private fun PermissionCard(denied: Boolean, permanentlyDenied: Boolean, onRequest: () -> Unit, onOpenSettings: () -> Unit) {
    RtCard(color = RtColors.SurfaceRaised) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = RtColors.Primary)
            Spacer(Modifier.width(8.dp))
            Text("Location access", style = RtType.bodyStrong, color = RtColors.TextPrimary)
        }
        Spacer(Modifier.height(RtDimens.xs))
        Text(
            "Location access is required to record your route, speed and distance. " +
                "It is only used while a ride is being recorded, and your rides stay on this phone. " +
                "You'll also be asked to allow a notification that shows while recording.",
            style = RtType.body,
            color = RtColors.TextSecondary,
        )
        if (denied) {
            Spacer(Modifier.height(RtDimens.xs))
            Text(
                if (permanentlyDenied) "Location was denied. Enable it in App settings → Permissions → Location." else "Location was denied. Rides can't be recorded without it.",
                style = RtType.caption,
                color = RtColors.Warning,
            )
        }
        Spacer(Modifier.height(RtDimens.md))
        if (permanentlyDenied) {
            SecondaryButton("Open app settings", onOpenSettings)
        } else {
            PrimaryButton("Allow location", onRequest)
        }
    }
}
