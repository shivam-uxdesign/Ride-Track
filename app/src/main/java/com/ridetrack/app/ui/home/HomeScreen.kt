package com.ridetrack.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ridetrack.app.ui.appViewModel
import com.ridetrack.app.ui.common.RideRow
import com.ridetrack.app.ui.components.Chip
import com.ridetrack.app.ui.components.DemoBadge
import com.ridetrack.app.ui.components.EmptyState
import com.ridetrack.app.ui.components.Label
import com.ridetrack.app.ui.components.PrimaryButton
import com.ridetrack.app.ui.components.RtCard
import com.ridetrack.app.ui.components.SecondaryButton
import com.ridetrack.app.ui.components.SectionHeader
import com.ridetrack.app.ui.components.StatTile
import com.ridetrack.app.ui.components.StatusIndicator
import com.ridetrack.app.ui.components.StatusLevel
import com.ridetrack.app.ui.components.TwoColumn
import com.ridetrack.app.ui.format.Format
import com.ridetrack.app.ui.theme.RtColors
import com.ridetrack.app.ui.theme.RtDimens
import com.ridetrack.app.ui.theme.RtType
import com.ridetrack.telemetry.model.Ride

@Composable
fun HomeScreen(
    onStartRide: () -> Unit,
    onReturnToRide: () -> Unit,
    onOpenRide: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenBikes: () -> Unit,
    onAddBike: () -> Unit,
    onSeeAllRides: () -> Unit,
) {
    val vm = appViewModel { HomeViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshEnvironment() }
    var confirmDiscard by remember { mutableStateOf<Ride?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = RtDimens.screenPadding),
    ) {
        // Header
        Row(Modifier.fillMaxWidth().padding(top = RtDimens.xs), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("RIDE TRACK", style = RtType.label.copy(letterSpacing = RtType.label.letterSpacing * 2), color = RtColors.Primary)
                Text(
                    s.bike?.displayName ?: "No bike yet",
                    style = RtType.caption,
                    color = RtColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (s.demoMode) DemoBadge(Modifier.padding(end = RtDimens.xs))
            IconButton(onClick = onOpenProfile) {
                Icon(Icons.Outlined.AccountCircle, contentDescription = "Profile and settings", tint = RtColors.TextSecondary)
            }
        }
        Spacer(Modifier.height(RtDimens.md))

        s.unfinished?.let { ride ->
            UnfinishedRideCard(ride, onSave = { vm.saveUnfinished(ride) }, onDiscard = { confirmDiscard = ride })
            Spacer(Modifier.height(RtDimens.cardSpacing))
        }

        ReadyCard(s, onStartRide, onReturnToRide, onAddBike, onOpenBikes)

        if (!s.loading) {
            if (s.totals != null) {
                val t = s.totals!!
                SectionHeader("Your riding")
                TwoColumn(
                    left = { StatTile("Total distance", Format.distanceValue(t.distanceM), it, unit = "km") },
                    right = { StatTile("Total rides", t.rideCount.toString(), it) },
                )
                Spacer(Modifier.height(RtDimens.cardSpacing))
                TwoColumn(
                    left = { StatTile("Riding time", Format.duration(t.movingMillis), it) },
                    right = { StatTile("Longest ride", t.longestRideM?.let(Format::distanceValue) ?: Format.DASH, it, unit = "km") },
                )
            }
            if (s.recent.isNotEmpty()) {
                SectionHeader("Recent rides") {
                    TextButton(onClick = onSeeAllRides) { Text("See all", style = RtType.caption, color = RtColors.Primary) }
                }
                s.recent.forEach { ride ->
                    RideRow(ride, bikeName = null, onClick = { onOpenRide(ride.id) }, compact = true)
                    Spacer(Modifier.height(RtDimens.cardSpacing))
                }
            } else if (s.hasBikes && s.totals == null) {
                Spacer(Modifier.height(RtDimens.lg))
                EmptyState(
                    title = "No rides yet",
                    message = "Start your first ride to begin building your riding history.",
                    icon = Icons.Outlined.Route,
                )
            }
        }
        Spacer(Modifier.height(RtDimens.lg))
    }

    confirmDiscard?.let { ride ->
        AlertDialog(
            onDismissRequest = { confirmDiscard = null },
            title = { Text("Discard unfinished ride?") },
            text = { Text("The recorded route and telemetry for this ride will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = { vm.discardUnfinished(ride); confirmDiscard = null }) { Text("Discard", color = RtColors.Error) }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = null }) { Text("Keep") } },
            containerColor = RtColors.SurfaceRaised,
        )
    }
}

@Composable
private fun ReadyCard(
    s: HomeUiState,
    onStartRide: () -> Unit,
    onReturnToRide: () -> Unit,
    onAddBike: () -> Unit,
    onOpenBikes: () -> Unit,
) {
    RtCard {
        if (s.rideState.isActive) {
            Chip("Ride in progress", RtColors.Ok)
            Spacer(Modifier.height(RtDimens.md))
            Text(s.bike?.displayName ?: "", style = RtType.headline, color = RtColors.TextPrimary)
            Spacer(Modifier.height(RtDimens.lg))
            PrimaryButton("Return to ride", onReturnToRide, large = true)
            return@RtCard
        }
        if (!s.loading && !s.hasBikes) {
            Label("Your bike")
            Spacer(Modifier.height(RtDimens.xs))
            Text("Add your motorcycle", style = RtType.headline, color = RtColors.TextPrimary)
            Spacer(Modifier.height(RtDimens.xs))
            Text(
                "Every ride is saved against a bike. Add yours to start recording.",
                style = RtType.body,
                color = RtColors.TextSecondary,
            )
            Spacer(Modifier.height(RtDimens.lg))
            PrimaryButton("Add bike", onAddBike, large = true, icon = Icons.Outlined.TwoWheeler)
            return@RtCard
        }

        Label("Your bike")
        Spacer(Modifier.height(RtDimens.xxs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                s.bike?.displayName ?: " ",
                style = RtType.title,
                color = RtColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onOpenBikes) { Text("Change", style = RtType.caption, color = RtColors.Primary) }
        }
        Spacer(Modifier.height(RtDimens.xl))
        Text("READY TO RIDE", style = RtType.label, color = RtColors.Primary)
        Spacer(Modifier.height(RtDimens.sm))
        PrimaryButton("Start ride", onStartRide, large = true, icon = Icons.Rounded.PlayArrow, haptic = true, enabled = s.bike != null)
        Spacer(Modifier.height(RtDimens.md))
        Row(horizontalArrangement = Arrangement.spacedBy(RtDimens.md)) {
            if (s.demoMode) {
                StatusIndicator("GPS", "Simulated", StatusLevel.WARNING)
            } else {
                val (text, level) = when (s.gps) {
                    GpsReadiness.READY -> "Ready" to StatusLevel.OK
                    GpsReadiness.PERMISSION_NEEDED -> "Permission needed" to StatusLevel.WARNING
                    GpsReadiness.DISABLED -> "Off" to StatusLevel.ERROR
                    GpsReadiness.NO_HARDWARE -> "Unavailable" to StatusLevel.ERROR
                }
                StatusIndicator("GPS", text, level)
            }
            val (motionText, motionLevel) = when {
                s.demoMode -> "Simulated" to StatusLevel.WARNING
                s.sensors.canEstimateLean -> "Ready" to StatusLevel.OK
                s.sensors.accelerometer -> "No gyroscope" to StatusLevel.WARNING
                else -> "Unavailable" to StatusLevel.ERROR
            }
            StatusIndicator("Motion", motionText, motionLevel)
        }
        if (!s.demoMode && s.bike != null && s.bike.calibration == null) {
            Spacer(Modifier.height(RtDimens.sm))
            Text(
                "Phone mount not calibrated — lean angle will be unavailable until you calibrate.",
                style = RtType.caption,
                color = RtColors.Warning,
            )
        }
    }
}

@Composable
private fun UnfinishedRideCard(ride: Ride, onSave: () -> Unit, onDiscard: () -> Unit) {
    RtCard(color = RtColors.SurfaceRaised) {
        Chip("Unfinished ride", RtColors.Warning)
        Spacer(Modifier.height(RtDimens.sm))
        Text(
            "A ride started ${Format.rideDate(ride.startTimeMillis)} was interrupted before it was saved. " +
                "${Format.distance(ride.stats.distanceM)} was recorded.",
            style = RtType.body,
            color = RtColors.TextPrimary,
        )
        Spacer(Modifier.height(RtDimens.md))
        Row {
            SecondaryButton("Discard", onDiscard, Modifier.weight(1f), contentColor = RtColors.Error)
            Spacer(Modifier.width(RtDimens.sm))
            PrimaryButton("Save ride", onSave, Modifier.weight(1f))
        }
    }
}
