package com.ridetrack.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ridetrack.app.ui.components.DemoBadge
import com.ridetrack.app.ui.components.MetricValue
import com.ridetrack.app.ui.components.RtCard
import com.ridetrack.app.ui.format.Format
import com.ridetrack.app.ui.theme.RtColors
import com.ridetrack.app.ui.theme.RtType
import com.ridetrack.telemetry.model.DataSourceKind
import com.ridetrack.telemetry.model.Ride

/** Ride history card: name, date, distance, duration, avg/max speed. */
@Composable
fun RideRow(ride: Ride, bikeName: String?, onClick: () -> Unit, modifier: Modifier = Modifier, compact: Boolean = false) {
    RtCard(modifier = modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(ride.name, style = RtType.bodyStrong, color = RtColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(Format.rideDate(ride.startTimeMillis), bikeName.takeIf { !compact }).joinToString(" · "),
                    style = RtType.caption,
                    color = RtColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (ride.source == DataSourceKind.DEMO) DemoBadge()
        }
        Spacer(Modifier.height(if (compact) 10.dp else 14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.Bottom) {
            MetricValue(Format.distanceValue(ride.stats.distanceM), "km", RtType.metricM)
            MetricValue(Format.duration(ride.durationMillis), null, RtType.metricM)
        }
        if (!compact) {
            Spacer(Modifier.height(10.dp))
            Row {
                Text("Avg ${Format.speedWithUnit(ride.stats.avgSpeedMps)}", style = RtType.caption, color = RtColors.TextSecondary)
                Spacer(Modifier.width(16.dp))
                Text("Max ${Format.speedWithUnit(ride.stats.maxSpeedMps)}", style = RtType.caption, color = RtColors.TextSecondary)
            }
        }
    }
}
