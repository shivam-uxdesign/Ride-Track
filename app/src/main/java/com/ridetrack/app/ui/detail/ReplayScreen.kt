package com.ridetrack.app.ui.detail

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ridetrack.app.ui.appViewModel
import com.ridetrack.app.ui.common.indexAt
import com.ridetrack.app.ui.common.positionAt
import com.ridetrack.app.ui.components.EmptyState
import com.ridetrack.app.ui.components.RouteMap
import com.ridetrack.app.ui.components.RtCard
import com.ridetrack.app.ui.components.ScreenHeader
import com.ridetrack.app.ui.components.StatBlock
import com.ridetrack.app.ui.components.leanColor
import com.ridetrack.app.ui.format.Format
import com.ridetrack.app.ui.theme.RtColors
import com.ridetrack.app.ui.theme.RtDimens
import com.ridetrack.app.ui.theme.RtType
import java.util.Locale

@Composable
fun ReplayScreen(rideId: String, onBack: () -> Unit) {
    val vm = appViewModel(key = "replay-$rideId") { ReplayViewModel(it, rideId) }
    val s by vm.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = RtDimens.screenPadding),
    ) {
        ScreenHeader("Replay", onBack = onBack)
        val track = s.track
        if (track == null) return@Column
        if (track.route.size < 2 || track.durationMillis <= 0) {
            EmptyState("Nothing to replay", "This ride has no recorded GPS route.")
            return@Column
        }
        val t = track.startMillis + s.positionMillis
        val idx = track.samples.indexAt(t)
        val sample = track.samples.getOrNull(idx)

        RouteMap(
            route = track.route,
            marker = track.samples.positionAt(t),
            interactive = true,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
        Spacer(Modifier.height(RtDimens.cardSpacing))
        RtCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatBlock("Speed", Format.speedKmh(sample?.speedMps), unit = "km/h", style = RtType.metricM)
                StatBlock("Lean", Format.lean(sample?.leanDeg), style = RtType.metricM, color = leanColor(sample?.leanDeg))
                StatBlock("G", Format.g(sample?.combinedG), style = RtType.metricM, color = RtColors.GForce)
            }
            Spacer(Modifier.height(RtDimens.md))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatBlock("Distance", Format.distanceValue(track.cumulativeDistanceM.getOrElse(idx) { 0.0 }), unit = "km", style = RtType.metricS)
                StatBlock("Elapsed", Format.clock(s.positionMillis), style = RtType.metricS, horizontalAlignment = Alignment.End)
            }
        }
        Spacer(Modifier.height(RtDimens.sm))
        Slider(
            value = s.positionMillis.toFloat() / track.durationMillis,
            onValueChange = { vm.pause(); vm.seek(it) },
            colors = SliderDefaults.colors(thumbColor = RtColors.Primary, activeTrackColor = RtColors.Primary, inactiveTrackColor = RtColors.Outline),
        )
        Row(Modifier.fillMaxWidth().padding(bottom = RtDimens.md), verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(
                onClick = vm::togglePlay,
                shape = CircleShape,
                modifier = Modifier.size(56.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = RtColors.Primary, contentColor = RtColors.OnPrimary),
            ) {
                Icon(
                    if (s.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (s.playing) "Pause replay" else "Play replay",
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(Modifier.width(RtDimens.md))
            ReplayViewModel.SPEEDS.forEach { speed ->
                FilterChip(
                    selected = s.speed == speed,
                    onClick = { vm.setSpeed(speed) },
                    label = { Text(if (speed == 0.5f) "0.5×" else String.format(Locale.US, "%.0f×", speed)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RtColors.Primary.copy(alpha = 0.18f),
                        selectedLabelColor = RtColors.Primary,
                        labelColor = RtColors.TextSecondary,
                    ),
                    modifier = Modifier.padding(end = RtDimens.xs),
                )
            }
            Spacer(Modifier.weight(1f))
        }
        Text(
            "1× plays 20 seconds of riding per second.",
            style = RtType.caption,
            color = RtColors.TextTertiary,
            modifier = Modifier.padding(bottom = RtDimens.sm),
        )
    }
}
