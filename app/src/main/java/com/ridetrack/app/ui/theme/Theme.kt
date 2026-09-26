package com.ridetrack.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Premium Minimal palette: near-black canvas, one soft cyan accent, hairlines instead of
 * heavy cards. Semantic colours only where they carry meaning (lean side, braking, G).
 */
object RtColors {
    val Background = Color(0xFF0A0A0B)
    /** Live ride screen: pure black for maximum contrast and OLED power saving. */
    val LiveBackground = Color(0xFF000000)
    val Surface = Color(0xFF141416)
    val SurfaceRaised = Color(0xFF1C1C1F)
    /** Tracks, chart grids, inactive controls. */
    val Outline = Color(0xFF2A2A2E)
    /** 1dp dividers and card edges. */
    val Hairline = Color(0x14FFFFFF)
    val TextPrimary = Color(0xFFF4F4F5)
    val TextSecondary = Color(0xFF8B8B93)
    val TextTertiary = Color(0xFF5E5E66)

    val Primary = Color(0xFF69C8CB)
    val OnPrimary = Color(0xFF04292A)
    /** High-emphasis secondary action (e.g. "View details"). */
    val Inverse = Color(0xFFF4F4F5)
    val OnInverse = Color(0xFF0A0A0B)

    /** Left lean / left-side telemetry. */
    val Left = Color(0xFFA5A1FF)
    /** Right lean / right-side telemetry. */
    val Right = Color(0xFFFB7185)
    val Accel = Color(0xFF4ADE80)
    val Brake = Color(0xFFFB7185)
    val GForce = Color(0xFFFBBF24)
    val Paused = Color(0xFFA5A1FF)
    val Warning = Color(0xFFFBBF24)
    val Error = Color(0xFFF43F5E)
    val Ok = Color(0xFF4ADE80)
}

object RtDimens {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp

    val screenPadding = 16.dp
    val cardPadding = 20.dp
    val cardRadius = 24.dp
    val heroRadius = 28.dp
    val cardSpacing = 12.dp
    val buttonHeight = 56.dp
    val primaryButtonHeight = 64.dp
    val screenPaddingWide = 20.dp
    val iconSize = 20.dp
    val minTouch = 48.dp
}

private val colorScheme = darkColorScheme(
    primary = RtColors.Primary,
    onPrimary = RtColors.OnPrimary,
    primaryContainer = Color(0xFF1E3E40),
    onPrimaryContainer = RtColors.Primary,
    secondary = RtColors.Left,
    inverseSurface = RtColors.Inverse,
    inverseOnSurface = RtColors.OnInverse,
    onSecondary = Color.Black,
    background = RtColors.Background,
    onBackground = RtColors.TextPrimary,
    surface = RtColors.Background,
    onSurface = RtColors.TextPrimary,
    surfaceVariant = RtColors.Surface,
    onSurfaceVariant = RtColors.TextSecondary,
    surfaceContainerLowest = RtColors.Background,
    surfaceContainerLow = RtColors.Surface,
    surfaceContainer = RtColors.Surface,
    surfaceContainerHigh = RtColors.SurfaceRaised,
    surfaceContainerHighest = RtColors.SurfaceRaised,
    outline = RtColors.Outline,
    outlineVariant = RtColors.Outline,
    error = RtColors.Error,
    onError = Color.Black,
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun RideTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography = RtType.material,
        shapes = shapes,
        content = content,
    )
}
