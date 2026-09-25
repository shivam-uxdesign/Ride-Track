package com.ridetrack.app.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ridetrack.app.ui.appViewModel
import com.ridetrack.app.ui.common.Maneuvers
import com.ridetrack.app.ui.common.RideDynamics
import com.ridetrack.app.ui.common.isTurn
import com.ridetrack.app.ui.common.positionAt
import com.ridetrack.app.ui.common.presentation
import com.ridetrack.app.ui.components.ChartSeries
import com.ridetrack.app.ui.components.DemoBadge
import com.ridetrack.app.ui.components.EmptyState
import com.ridetrack.app.ui.components.Label
import com.ridetrack.app.ui.components.LineChart
import com.ridetrack.app.ui.components.PrimaryButton
import com.ridetrack.app.ui.components.RouteMap
import com.ridetrack.app.ui.components.RtCard
import com.ridetrack.app.ui.components.ScreenHeader
import com.ridetrack.app.ui.components.SectionHeader
import com.ridetrack.app.ui.components.StatBlock
import com.ridetrack.app.ui.components.StatTile
import com.ridetrack.app.ui.components.TwoColumn
import com.ridetrack.app.ui.components.leanColor
import com.ridetrack.app.ui.format.Format
import com.ridetrack.app.ui.theme.RtColors
import com.ridetrack.app.ui.theme.RtDimens
import com.ridetrack.app.ui.theme.RtType
import com.ridetrack.telemetry.model.DataSourceKind
import com.ridetrack.telemetry.model.Ride
import com.ridetrack.telemetry.model.TelemetrySample

@Composable
fun RideDetailScreen(rideId: String, onBack: () -> Unit, onReplay: () -> Unit) {
    val vm = appViewModel(key = "detail-$rideId") { RideDetailViewModel(it, rideId) }
    val s by vm.state.collectAsStateWithLifecycle()
    val scrub by vm.scrub.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showTurns by remember { mutableStateOf(false) }

    val ride = s.ride
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = RtDimens.screenPadding),
    ) {
        ScreenHeader(
            title = ride?.name ?: "Ride",
            subtitle = ride?.let { listOfNotNull(Format.rideDate(it.startTimeMillis), s.bikeName).joinToString(" · ") },
            onBack = onBack,
        ) {
            if (ride != null) {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Ride options", tint = RtColors.TextPrimary)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = RtColors.SurfaceRaised) {
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { menuOpen = false; renaming = true })
                    DropdownMenuItem(text = { Text("Delete", color = RtColors.Error) }, onClick = { menuOpen = false; confirmDelete = true })
                }
            }
        }
        if (ride == null) {
            if (!s.loading) EmptyState("Ride not found", "This ride may have been deleted.")
            return@Column
        }
        Column(Modifier.verticalScroll(rememberScrollState())) {
            if (ride.source == DataSourceKind.DEMO) {
                DemoBadge(Modifier.padding(bottom = RtDimens.sm))
            }
            SummaryGrid(ride)

            val data = s.data
            SectionHeader("Map")
            val samples = data?.samples.orEmpty()
            val scrubSample = scrub?.let { f -> samples.getOrNull((f * (samples.size - 1)).toInt()) }
            RouteMap(
                route = data?.route.orEmpty(),
                marker = scrubSample?.let { samples.positionAt(it.timeMillis) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
            )
            Spacer(Modifier.height(RtDimens.cardSpacing))
            PrimaryButton("Play ride", onReplay, icon = Icons.Rounded.PlayArrow, enabled = (data?.route?.size ?: 0) >= 2)

            SectionHeader("Telemetry")
            if (data == null) {
                Text("Loading telemetry…", style = RtType.caption, color = RtColors.TextSecondary)
            } else if (samples.size < 2) {
                Text("Not enough telemetry was recorded for charts.", style = RtType.caption, color = RtColors.TextSecondary)
            } else {
                ScrubReadout(ride, scrubSample, onClear = vm::clearScrub)
                Spacer(Modifier.height(RtDimens.cardSpacing))
                RtCard {
                    ChartBlock("Speed", Format.speedWithUnit(scrubSample?.speedMps), data.speed, RtColors.Primary, scrub, vm::scrubTo)
                    ChartDivider()
                    LineChart(
                        "Lean angle", Format.lean(scrubSample?.leanDeg), data.lean, RtColors.Right, scrub, vm::scrubTo,
                        negativeColor = RtColors.Left,
                        unavailableText = "Lean was unavailable for this ride (phone mount not calibrated or no gyroscope).",
                    )
                    ChartDivider()
                    ChartBlock("G-force", Format.g(scrubSample?.combinedG), data.gForce, RtColors.GForce, scrub, vm::scrubTo)
                    ChartDivider()
                    ChartBlock("Elevation", Format.altitude(scrubSample?.altitudeM), data.elevation, RtColors.Left, scrub, vm::scrubTo)
                }
                Spacer(Modifier.height(RtDimens.xs))
                Text("Tap or drag a chart to scrub through the ride.", style = RtType.caption, color = RtColors.TextTertiary)
            }

            RideDynamics(ride)
            Maneuvers(ride)

            val events = data?.track?.events.orEmpty().filter { showTurns || !it.type.isTurn }
            SectionHeader("Events") {
                if (data?.track?.events?.any { it.type.isTurn } == true) {
                    TextButton(onClick = { showTurns = !showTurns }) {
                        Text(if (showTurns) "Hide turns" else "Show turns", style = RtType.caption, color = RtColors.Primary)
                    }
                }
            }
            RtCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp, horizontal = 20.dp)) {
                if (events.isEmpty()) {
                    Text("No events recorded.", style = RtType.body, color = RtColors.TextSecondary, modifier = Modifier.padding(vertical = 12.dp))
                }
                events.forEachIndexed { i, e ->
                    val p = e.presentation()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { vm.scrubToTime(e.timeMillis) }
                            .padding(vertical = 12.dp)
                            .semantics(mergeDescendants = true) {},
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(p.icon, contentDescription = null, tint = p.color, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(RtDimens.sm))
                        Column(Modifier.weight(1f)) {
                            Text(p.title, style = RtType.bodyStrong.copy(fontSize = RtType.body.fontSize), color = RtColors.TextPrimary)
                            if (!p.detail.isNullOrBlank()) Text(p.detail, style = RtType.caption, color = RtColors.TextSecondary)
                        }
                        Text(Format.timeOfDay(e.timeMillis), style = RtType.caption, color = RtColors.TextSecondary)
                    }
                    if (i < events.lastIndex) HorizontalDivider(color = RtColors.Outline.copy(alpha = 0.5f))
                }
            }
            Spacer(Modifier.height(RtDimens.lg))
        }
    }

    if (renaming && ride != null) {
        var name by remember { mutableStateOf(ride.name) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text("Rename ride") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it.take(60) }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = { vm.rename(name); renaming = false }, enabled = name.isNotBlank()) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Cancel") } },
            containerColor = RtColors.SurfaceRaised,
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ride?") },
            text = { Text("This ride and all of its telemetry will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.delete(onBack) }) { Text("Delete", color = RtColors.Error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
            containerColor = RtColors.SurfaceRaised,
        )
    }
}

@Composable
private fun SummaryGrid(ride: Ride) {
    TwoColumn(
        left = { StatTile("Distance", Format.distanceValue(ride.stats.distanceM), it, unit = "km") },
        right = { StatTile("Duration", Format.duration(ride.durationMillis), it) },
    )
    Spacer(Modifier.height(RtDimens.cardSpacing))
    TwoColumn(
        left = { StatTile("Moving time", Format.duration(ride.stats.movingMillis), it) },
        right = { StatTile("Stopped time", Format.duration(ride.stats.stoppedMillis), it) },
    )
    Spacer(Modifier.height(RtDimens.cardSpacing))
    TwoColumn(
        left = { StatTile("Avg speed", Format.speedKmh(ride.stats.avgSpeedMps), it, unit = "km/h") },
        right = { StatTile("Max speed", Format.speedKmh(ride.stats.maxSpeedMps), it, unit = "km/h") },
    )
}

@Composable
private fun ScrubReadout(ride: Ride, sample: TelemetrySample?, onClear: () -> Unit) {
    RtCard(color = RtColors.SurfaceRaised) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Label(
                if (sample == null) "Timeline" else "At ${Format.clock(sample.timeMillis - ride.startTimeMillis)} · ${Format.timeOfDay(sample.timeMillis)}",
                Modifier.weight(1f),
            )
            if (sample != null) {
                Text("Clear", style = RtType.caption, color = RtColors.Primary, modifier = Modifier.clickable(onClick = onClear).padding(4.dp))
            }
        }
        Spacer(Modifier.height(RtDimens.sm))
        if (sample == null) {
            Text("Scrub a chart to replay the ride moment by moment.", style = RtType.body, color = RtColors.TextSecondary)
        } else {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                StatBlock("Speed", Format.speedKmh(sample.speedMps), unit = "km/h", style = RtType.metricS)
                StatBlock("Lean", Format.lean(sample.leanDeg), style = RtType.metricS, color = leanColor(sample.leanDeg))
                StatBlock("G", Format.g(sample.combinedG), style = RtType.metricS, color = RtColors.GForce)
                StatBlock("Elev.", Format.altitude(sample.altitudeM), style = RtType.metricS)
            }
        }
    }
}

@Composable
private fun ChartBlock(title: String, readout: String, series: ChartSeries, color: androidx.compose.ui.graphics.Color, scrub: Float?, onScrub: (Float) -> Unit) {
    LineChart(title, readout, series, color, scrub, onScrub)
}

@Composable
private fun ChartDivider() {
    Spacer(Modifier.height(RtDimens.md))
    HorizontalDivider(color = RtColors.Outline.copy(alpha = 0.5f))
    Spacer(Modifier.height(RtDimens.md))
}
