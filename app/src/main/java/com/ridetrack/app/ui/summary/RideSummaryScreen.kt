package com.ridetrack.app.ui.summary

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ridetrack.app.ui.appViewModel
import com.ridetrack.app.ui.common.HeadlineStats
import com.ridetrack.app.ui.common.Maneuvers
import com.ridetrack.app.ui.common.RideDynamics
import com.ridetrack.app.ui.common.TimeBreakdown
import com.ridetrack.app.ui.components.DemoBadge
import com.ridetrack.app.ui.components.EmptyState
import com.ridetrack.app.ui.components.PrimaryButton
import com.ridetrack.app.ui.components.RouteMap
import com.ridetrack.app.ui.components.SecondaryButton
import com.ridetrack.app.ui.components.SectionHeader
import com.ridetrack.app.ui.format.Format
import com.ridetrack.app.ui.theme.RtColors
import com.ridetrack.app.ui.theme.RtDimens
import com.ridetrack.app.ui.theme.RtType
import com.ridetrack.telemetry.model.DataSourceKind

@Composable
fun RideSummaryScreen(rideId: String, onDone: () -> Unit, onOpenDetail: () -> Unit) {
    val vm = appViewModel(key = "summary-$rideId") { RideSummaryViewModel(it, rideId) }
    val s by vm.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onDone)
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = RtDimens.screenPadding),
    ) {
        val ride = s.ride
        Spacer(Modifier.height(RtDimens.lg))
        Text("RIDE COMPLETE", style = RtType.label, color = RtColors.Primary)
        Spacer(Modifier.height(RtDimens.xs))
        if (ride == null) {
            if (!s.loading) EmptyState("Ride not found", "This ride may have been deleted.", action = { PrimaryButton("Done", onDone) })
            return@Column
        }
        Text(ride.name, style = RtType.title, color = RtColors.TextPrimary)
        Text(
            listOfNotNull(Format.rideDate(ride.startTimeMillis), s.bikeName).joinToString(" · "),
            style = RtType.caption,
            color = RtColors.TextSecondary,
        )
        if (ride.source == DataSourceKind.DEMO) {
            Spacer(Modifier.height(RtDimens.xs))
            DemoBadge()
        }
        Spacer(Modifier.height(RtDimens.lg))

        AnimatedVisibility(visible, enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 8 }) {
            Column {
                HeadlineStats(ride)
                RideDynamics(ride)
                Maneuvers(ride)
                TimeBreakdown(ride)
                SectionHeader("Map")
                RouteMap(
                    s.route,
                    Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                )
            }
        }
        Spacer(Modifier.height(RtDimens.lg))
        PrimaryButton("Done", onDone, large = true)
        Spacer(Modifier.height(RtDimens.sm))
        SecondaryButton("View detailed analysis", onOpenDetail)
        Spacer(Modifier.height(RtDimens.lg))
    }
}
