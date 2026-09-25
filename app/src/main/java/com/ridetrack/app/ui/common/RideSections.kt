package com.ridetrack.app.ui.common

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ridetrack.app.ui.components.InfoRow
import com.ridetrack.app.ui.components.RtCard
import com.ridetrack.app.ui.components.SectionHeader
import com.ridetrack.app.ui.components.StatTile
import com.ridetrack.app.ui.components.TwoColumn
import com.ridetrack.app.ui.format.Format
import com.ridetrack.app.ui.theme.RtColors
import com.ridetrack.app.ui.theme.RtDimens
import com.ridetrack.app.ui.theme.RtType
import com.ridetrack.telemetry.model.Ride

@Composable
fun HeadlineStats(ride: Ride) {
    TwoColumn(
        left = { StatTile("Distance", Format.distanceValue(ride.stats.distanceM), it, unit = "km") },
        right = { StatTile("Duration", Format.duration(ride.durationMillis), it) },
    )
    Spacer(Modifier.height(RtDimens.cardSpacing))
    TwoColumn(
        left = { StatTile("Avg speed", Format.speedKmh(ride.stats.avgSpeedMps), it, unit = "km/h") },
        right = { StatTile("Max speed", Format.speedKmh(ride.stats.maxSpeedMps), it, unit = "km/h") },
    )
}

@Composable
fun TimeBreakdown(ride: Ride) {
    SectionHeader("Time")
    RtCard {
        InfoRow("Moving time", Format.duration(ride.stats.movingMillis))
        Line()
        InfoRow("Stopped time", Format.duration(ride.stats.stoppedMillis))
        Line()
        InfoRow("Stops", ride.stats.stopCount.toString())
        Line()
        InfoRow("Average stop", Format.duration(ride.stats.avgStopMillis))
    }
}

@Composable
fun RideDynamics(ride: Ride) {
    val s = ride.stats
    SectionHeader("Ride dynamics")
    RtCard {
        InfoRow("Maximum left lean", Format.leanMagnitude(s.maxLeftLeanDeg), valueColor = RtColors.Left)
        Line()
        InfoRow("Maximum right lean", Format.leanMagnitude(s.maxRightLeanDeg), valueColor = RtColors.Right)
        Line()
        InfoRow("Average lean", Format.leanMagnitude(s.avgLeanDeg))
        Line()
        InfoRow("Maximum acceleration", Format.gSigned(s.maxAccelG), valueColor = RtColors.Accel)
        Line()
        InfoRow("Maximum braking", Format.gSigned(s.maxBrakeG), valueColor = RtColors.Brake)
        Line()
        InfoRow("Peak G-force", Format.g(s.peakG), valueColor = RtColors.GForce)
    }
    Spacer(Modifier.height(RtDimens.xs))
    Text(
        "Lean and G-force are estimated from phone sensors and are not certified measurements.",
        style = RtType.caption,
        color = RtColors.TextTertiary,
    )
}

@Composable
fun Maneuvers(ride: Ride) {
    val s = ride.stats
    SectionHeader("Maneuvers")
    TwoColumn(
        left = { StatTile("Left turns", s.leftTurns.toString(), it, color = RtColors.Left) },
        right = { StatTile("Right turns", s.rightTurns.toString(), it, color = RtColors.Right) },
    )
    Spacer(Modifier.height(RtDimens.cardSpacing))
    TwoColumn(
        left = { StatTile("Brake events", s.brakeEvents.toString(), it, color = RtColors.Brake) },
        right = { StatTile("Lean events", s.leanEvents.toString(), it) },
    )
}

@Composable
private fun Line() = HorizontalDivider(color = RtColors.Outline.copy(alpha = 0.6f))
