package com.ridetrack.app.ui.bike

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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ridetrack.app.ui.appViewModel
import com.ridetrack.app.ui.components.PrimaryButton
import com.ridetrack.app.ui.components.RtCard
import com.ridetrack.app.ui.components.ScreenHeader
import com.ridetrack.app.ui.components.SecondaryButton
import com.ridetrack.app.ui.theme.RtColors
import com.ridetrack.app.ui.theme.RtDimens
import com.ridetrack.app.ui.theme.RtType

@Composable
fun CalibrationScreen(bikeId: String, onDone: () -> Unit) {
    val vm = appViewModel(key = "calibrate-$bikeId") { CalibrationViewModel(it, bikeId) }
    val phase by vm.phase.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = RtDimens.screenPadding),
    ) {
        ScreenHeader("Calibrate phone", subtitle = "Phone mounting offset", onBack = onDone)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            RtCard {
                Step(1, "Mount the phone normally, screen facing you.")
                Step(2, "Hold the motorcycle upright — on the paddock stand or with a helper, not the side stand.")
                Step(3, "Press Calibrate.")
                Step(4, "Keep everything completely still for 3 seconds.")
            }
            Spacer(Modifier.height(RtDimens.cardSpacing))
            RtCard(color = RtColors.SurfaceRaised) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = RtColors.Warning)
                    Spacer(Modifier.width(RtDimens.sm))
                    Text("Keep the motorcycle upright and stationary.", style = RtType.bodyStrong, color = RtColors.TextPrimary)
                }
            }
            Spacer(Modifier.height(RtDimens.lg))
            PhaseStatus(phase)
            Spacer(Modifier.height(RtDimens.md))
            Text(
                "Lean angle is estimated from phone sensors and is not a certified measurement.",
                style = RtType.caption,
                color = RtColors.TextTertiary,
            )
        }
        when (phase) {
            CalibrationPhase.Instructions -> PrimaryButton("Calibrate", vm::start, large = true, haptic = true)
            is CalibrationPhase.Failed -> PrimaryButton("Try again", vm::start, large = true)
            CalibrationPhase.Success -> PrimaryButton("Done", onDone, large = true)
            CalibrationPhase.Unsupported, CalibrationPhase.RideActive -> SecondaryButton("Back", onDone)
            is CalibrationPhase.Settling, is CalibrationPhase.Capturing -> PrimaryButton("Calibrating…", {}, enabled = false, large = true)
        }
        Spacer(Modifier.height(RtDimens.md))
    }
}

@Composable
private fun PhaseStatus(phase: CalibrationPhase) {
    Column(
        Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (phase) {
            CalibrationPhase.Instructions -> Unit
            is CalibrationPhase.Settling -> {
                Text("${phase.secondsLeft}", style = RtType.metricXL, color = RtColors.TextPrimary)
                Text("Get ready — don't touch the bike", style = RtType.body, color = RtColors.TextSecondary)
            }
            is CalibrationPhase.Capturing -> {
                Text("Hold still…", style = RtType.headline, color = RtColors.TextPrimary)
                Spacer(Modifier.height(RtDimens.md))
                LinearProgressIndicator(
                    progress = { phase.progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = RtColors.Primary,
                    trackColor = RtColors.Outline,
                )
            }
            CalibrationPhase.Success -> Result(Icons.Outlined.CheckCircle, RtColors.Ok, "Calibrated", "Mounting offset saved. Lean angle is now available for this bike.")
            is CalibrationPhase.Failed -> Result(Icons.Outlined.ErrorOutline, RtColors.Error, "Calibration failed", phase.reason)
            CalibrationPhase.Unsupported -> Result(Icons.Outlined.ErrorOutline, RtColors.Error, "Not supported", "This phone has no accelerometer, so the mount can't be calibrated.")
            CalibrationPhase.RideActive -> Result(Icons.Outlined.ErrorOutline, RtColors.Warning, "Ride in progress", "Finish the current ride before calibrating.")
        }
    }
}

@Composable
private fun Result(icon: androidx.compose.ui.graphics.vector.ImageVector, color: androidx.compose.ui.graphics.Color, title: String, message: String) {
    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(40.dp))
    Spacer(Modifier.height(RtDimens.sm))
    Text(title, style = RtType.headline, color = RtColors.TextPrimary)
    Spacer(Modifier.height(RtDimens.xs))
    Text(message, style = RtType.body, color = RtColors.TextSecondary, textAlign = TextAlign.Center)
}

@Composable
private fun Step(n: Int, text: String) {
    Row(Modifier.padding(vertical = 6.dp)) {
        Text("$n", style = RtType.bodyStrong, color = RtColors.Primary, modifier = Modifier.width(24.dp))
        Text(text, style = RtType.body, color = RtColors.TextPrimary)
    }
}
