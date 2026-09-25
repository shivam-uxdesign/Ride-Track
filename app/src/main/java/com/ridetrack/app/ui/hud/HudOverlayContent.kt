package com.ridetrack.app.ui.hud

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.ridetrack.app.data.HudLayout
import com.ridetrack.app.data.HudSettings
import com.ridetrack.app.data.HudSize
import com.ridetrack.app.hud.HudData
import com.ridetrack.app.ui.theme.RideTrackTheme

/** Callbacks from the pop-up's quick controls. */
interface HudControlActions {
    fun setLayout(layout: HudLayout)
    fun setSize(size: HudSize)
    fun setOpacity(percent: Int)
    fun hideForRide()
    fun openApp()
    fun closeControls()
}

@Composable
fun HudOverlayContent(
    data: HudData,
    settings: HudSettings,
    collapsed: Boolean,
    controlsOpen: Boolean,
    actions: HudControlActions,
) {
    RideTrackTheme {
        Column(horizontalAlignment = Alignment.Start) {
            ScaledBy(settings.size.scale) {
                if (collapsed && !controlsOpen) HudBubble(data, settings) else HudCard(data, settings)
            }
            if (controlsOpen) {
                Spacer(Modifier.height(8.dp))
                HudControls(
                    settings = settings,
                    onLayout = actions::setLayout,
                    onSize = actions::setSize,
                    onOpacity = actions::setOpacity,
                    onHide = actions::hideForRide,
                    onOpenApp = actions::openApp,
                    onClose = actions::closeControls,
                )
            }
        }
    }
}

/** Scales dp and sp together so the card's layout size changes, not just its drawing. */
@Composable
fun ScaledBy(scale: Float, content: @Composable () -> Unit) {
    val d = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(d.density * scale, d.fontScale), content = content)
}
