package com.ridetrack.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ridetrack.app.ui.appViewModel
import com.ridetrack.app.ui.components.Chip
import com.ridetrack.app.ui.components.DemoBadge
import com.ridetrack.app.ui.components.EmptyState
import com.ridetrack.app.ui.components.GeoPoint
import com.ridetrack.app.ui.components.Label
import com.ridetrack.app.ui.components.PrimaryButton
import com.ridetrack.app.ui.components.RouteThumbnail
import com.ridetrack.app.ui.components.SecondaryButton
import com.ridetrack.app.ui.components.Stat
import com.ridetrack.app.ui.components.StatRow
import com.ridetrack.app.ui.components.StatusIndicator
import com.ridetrack.app.ui.components.StatusLevel
import com.ridetrack.app.ui.components.breathingGlow
import com.ridetrack.app.ui.components.riseIn
import com.ridetrack.app.ui.format.Format
import com.ridetrack.app.ui.theme.RtColors
import com.ridetrack.app.ui.theme.RtDimens
import com.ridetrack.app.ui.theme.RtType
import com.ridetrack.app.ui.theme.pressScale
import com.ridetrack.telemetry.model.DataSourceKind
import com.ridetrack.telemetry.model.Ride
import java.time.LocalTime

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
            .padding(horizontal = RtDimens.screenPaddingWide),
    ) {
        Header(s, onOpenProfile, onOpenBikes)
        Spacer(Modifier.height(28.dp))

        s.unfinished?.let { ride ->
            UnfinishedRideCard(ride, onSave = { vm.saveUnfinished(ride) }, onDiscard = { confirmDiscard = ride })
            Spacer(Modifier.height(RtDimens.md))
        }

        ReadyCard(s, onStartRide, onReturnToRide, onAddBike, Modifier.riseIn(0))

        if (!s.loading) {
            val t = s.totals
            if (t != null) {
                Spacer(Modifier.height(32.dp))
                Label("This month", Modifier.riseIn(1))
                Spacer(Modifier.height(14.dp))
                StatRow(
                    listOf(
                        Stat("Distance", Format.distanceValue(t.distanceThisMonthM), "km"),
                        Stat("Rides", t.ridesThisMonth.toString()),
                        Stat("Moving", Format.duration(t.movingThisMonthMillis)),
                    ),
                    modifier = Modifier.riseIn(1),
                    style = RtType.metricL,
                )
            }
            if (s.recent.isNotEmpty()) {
                Spacer(Modifier.height(32.dp))
                Row(Modifier.riseIn(2), verticalAlignment = Alignment.CenterVertically) {
                    Label("Recent", Modifier.weight(1f))
                    TextButton(onClick = onSeeAllRides) { Text("See all", style = RtType.body, color = RtColors.Primary) }
                }
                s.recent.forEach { ride ->
                    RecentRideRow(ride, s.thumbnails[ride.id], onClick = { onOpenRide(ride.id) }, modifier = Modifier.riseIn(2))
                }
            } else if (s.hasBikes && s.totals == null) {
                EmptyState(
                    title = "No rides yet",
                    message = "Start your first ride to begin building your riding history.",
                    icon = Icons.Outlined.Route,
                    modifier = Modifier.padding(top = RtDimens.lg),
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

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Good night"
}

@Composable
private fun Header(s: HomeUiState, onOpenProfile: () -> Unit, onOpenBikes: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(
            Modifier
                .weight(1f)
                .clickable(onClick = onOpenBikes, role = Role.Button),
        ) {
            Text(greeting(), style = RtType.title.copy(fontSize = RtType.headline.fontSize * 1.18f), color = RtColors.TextPrimary)
            val bike = s.bike
            val sub = when {
                bike == null -> "No bike yet"
                s.demoMode -> "${bike.displayName} · demo mode"
                bike.calibration != null -> "${bike.displayName} · mount calibrated"
                else -> "${bike.displayName} · mount not calibrated"
            }
            Text(sub, style = RtType.body, color = RtColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        }
        if (s.demoMode) DemoBadge(Modifier.padding(end = RtDimens.xs))
        val interaction = remember { MutableInteractionSource() }
        Box(
            Modifier
                .size(44.dp)
                .pressScale(interaction)
                .clip(CircleShape)
                .background(RtColors.Surface)
                .border(1.dp, RtColors.Hairline, CircleShape)
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onOpenProfile)
                .semantics { contentDescription = "Profile and settings" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = RtColors.TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ReadyCard(s: HomeUiState, onStartRide: () -> Unit, onReturnToRide: () -> Unit, onAddBike: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(RtDimens.heroRadius)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(RtColors.Surface)
            .border(1.dp, RtColors.Hairline, shape)
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        when {
            s.rideState.isActive -> {
                Chip("Ride in progress", RtColors.Ok)
                Text("Recording", style = RtType.hero, color = RtColors.TextPrimary)
                PrimaryButton("Return to ride", onReturnToRide, large = true)
            }
            !s.loading && !s.hasBikes -> {
                Column {
                    Text("Add your motorcycle", style = RtType.hero, color = RtColors.TextPrimary)
                    Text(
                        "Every ride is saved against a bike. Add yours to start recording.",
                        style = RtType.body,
                        color = RtColors.TextSecondary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                PrimaryButton("Add bike", onAddBike, large = true, icon = Icons.Outlined.TwoWheeler)
            }
            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    if (s.demoMode) {
                        StatusIndicator("GPS", "simulated", StatusLevel.WARNING)
                    } else {
                        val (text, level) = when (s.gps) {
                            GpsReadiness.READY -> "ready" to StatusLevel.OK
                            GpsReadiness.PERMISSION_NEEDED -> "needs permission" to StatusLevel.WARNING
                            GpsReadiness.DISABLED -> "off" to StatusLevel.ERROR
                            GpsReadiness.NO_HARDWARE -> "unavailable" to StatusLevel.ERROR
                        }
                        StatusIndicator("GPS", text, level)
                    }
                    val (motion, motionLevel) = when {
                        s.demoMode -> "simulated" to StatusLevel.WARNING
                        s.sensors.canEstimateLean -> "ready" to StatusLevel.OK
                        s.sensors.accelerometer -> "no gyroscope" to StatusLevel.WARNING
                        else -> "unavailable" to StatusLevel.ERROR
                    }
                    StatusIndicator("Sensors", motion, motionLevel)
                }
                Column {
                    Text("Ready to ride", style = RtType.hero, color = RtColors.TextPrimary)
                    Text(
                        if (!s.demoMode && s.bike?.calibration == null) "Calibrate the phone mount in Bike to see lean angle."
                        else "GPS and sensors are checked again when you start.",
                        style = RtType.body,
                        color = RtColors.TextSecondary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                PrimaryButton(
                    "Start ride", onStartRide,
                    modifier = Modifier.breathingGlow(RtColors.Primary, enabled = s.bike != null),
                    large = true, icon = Icons.Rounded.PlayArrow, haptic = true, enabled = s.bike != null,
                )
            }
        }
    }
}

@Composable
private fun RecentRideRow(ride: Ride, route: List<GeoPoint>?, onClick: () -> Unit, modifier: Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .clip(RoundedCornerShape(18.dp))
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(RtColors.Surface),
            contentAlignment = Alignment.Center,
        ) {
            RouteThumbnail(route.orEmpty(), Modifier.size(40.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ride.name, style = RtType.bodyStrong, color = RtColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (ride.source == DataSourceKind.DEMO) {
                    Spacer(Modifier.width(6.dp))
                    Text("DEMO", style = RtType.label, color = RtColors.Warning)
                }
            }
            Text(
                "${Format.rideDate(ride.startTimeMillis).substringBefore(" ·")} · ${Format.duration(ride.durationMillis)}",
                style = RtType.caption,
                color = RtColors.TextSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(Format.distanceValue(ride.stats.distanceM), style = RtType.metricS, color = RtColors.TextPrimary)
        Text(" km", style = RtType.caption, color = RtColors.TextSecondary)
    }
}

@Composable
private fun UnfinishedRideCard(ride: Ride, onSave: () -> Unit, onDiscard: () -> Unit) {
    val shape = RoundedCornerShape(RtDimens.cardRadius)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(RtColors.SurfaceRaised)
            .border(1.dp, RtColors.Warning.copy(alpha = 0.3f), shape)
            .padding(20.dp),
    ) {
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
