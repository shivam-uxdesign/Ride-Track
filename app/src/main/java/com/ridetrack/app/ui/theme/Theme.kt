package com.ridetrack.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Brand palette. Values tuned from the spec palette for contrast on near-black. */
object RtColors {
    val Background = Color(0xFF000000)
    val Surface = Color(0xFF1D1B20)
    val SurfaceRaised = Color(0xFF26242A)
    val Outline = Color(0xFF38363C)
    val TextPrimary = Color(0xFFF5F5F7)
    val TextSecondary = Color(0xFFA7A5AB)
    val TextTertiary = Color(0xFF77757B)

    val Primary = Color(0xFF69C8CB)
    val OnPrimary = Color(0xFF00282A)

    /** Left lean / left-side telemetry; lightened from #625CE6 for text contrast. */
    val Left = Color(0xFF8C87FF)
    /** Right lean / right-side telemetry. */
    val Right = Color(0xFFFF2F5B)
    val Accel = Color(0xFF32D65B)
    val Brake = Color(0xFFFF2F5B)
    val GForce = Color(0xFFFFAA00)
    val Paused = Color(0xFF8C87FF)
    val Warning = Color(0xFFFFAA00)
    val Error = Color(0xFFFF2F5B)
    val Ok = Color(0xFF32D65B)
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
    val cardSpacing = 12.dp
    val buttonHeight = 56.dp
    val primaryButtonHeight = 64.dp
    val iconSize = 20.dp
    val minTouch = 48.dp
}

private val colorScheme = darkColorScheme(
    primary = RtColors.Primary,
    onPrimary = RtColors.OnPrimary,
    primaryContainer = Color(0xFF1E3E40),
    onPrimaryContainer = RtColors.Primary,
    secondary = RtColors.Left,
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
