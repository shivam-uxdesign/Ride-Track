package com.ridetrack.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ridetrack.app.R

/** Geist (SIL Open Font License, bundled). */
val Geist = FontFamily(
    Font(R.font.geist_light, FontWeight.Light),
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
)

/**
 * Type scale. Big numbers are light and tightly tracked; telemetry uses tabular figures so
 * digits never jitter as values change.
 */
object RtType {
    private const val TABULAR = "tnum"
    private fun style(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0, tabular: Boolean = false) = TextStyle(
        fontFamily = Geist,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = tracking.em,
        fontFeatureSettings = if (tabular) TABULAR else null,
    )

    val speedHero = style(148, 140, FontWeight.Light, -0.04, tabular = true)
    val display = style(88, 84, FontWeight.Light, -0.04, tabular = true)
    val metricXL = style(40, 44, FontWeight.Normal, -0.03, tabular = true)
    val metricL = style(30, 34, FontWeight.Normal, -0.03, tabular = true)
    val metricM = style(24, 28, FontWeight.Normal, -0.03, tabular = true)
    val metricS = style(18, 22, FontWeight.Normal, -0.02, tabular = true)
    val unit = style(14, 18, FontWeight.Normal)

    val hero = style(34, 38, FontWeight.Medium, -0.03)
    val title = style(28, 34, FontWeight.Medium, -0.03)
    val headline = style(22, 28, FontWeight.Medium, -0.02)
    val bodyStrong = style(16, 22, FontWeight.Medium)
    val body = style(15, 21, FontWeight.Normal)
    val caption = style(13, 18, FontWeight.Normal)
    /** Small, muted, uppercase labels above values. */
    val label = style(11, 14, FontWeight.Medium, 0.08)
    val button = style(16, 20, FontWeight.SemiBold)

    val material = Typography(
        displayLarge = display,
        headlineLarge = title,
        headlineMedium = headline,
        titleLarge = headline,
        titleMedium = bodyStrong,
        titleSmall = bodyStrong,
        bodyLarge = body,
        bodyMedium = body,
        bodySmall = caption,
        labelLarge = button,
        labelMedium = label,
        labelSmall = caption,
    )
}
