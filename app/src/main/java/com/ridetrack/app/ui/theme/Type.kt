package com.ridetrack.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Type scale. Telemetry numbers use tabular figures so they don't jitter as they change. */
object RtType {
    private const val TABULAR = "tnum"

    val speedHero = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 112.sp, lineHeight = 112.sp, letterSpacing = (-4).sp, fontFeatureSettings = TABULAR)
    val metricXL = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 48.sp, lineHeight = 52.sp, letterSpacing = (-1.5).sp, fontFeatureSettings = TABULAR)
    val metricL = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.8).sp, fontFeatureSettings = TABULAR)
    val metricM = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.4).sp, fontFeatureSettings = TABULAR)
    val metricS = TextStyle(fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 22.sp, fontFeatureSettings = TABULAR)
    val unit = TextStyle(fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 18.sp)

    val title = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp)
    val headline = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp)
    val bodyStrong = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp)
    val body = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp)
    val caption = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp)
    /** Small, muted, uppercase labels above values. */
    val label = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 1.2.sp)
    val button = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = 1.sp)

    val material = Typography(
        headlineLarge = title,
        headlineMedium = headline,
        titleLarge = headline,
        titleMedium = bodyStrong,
        bodyLarge = body,
        bodyMedium = body,
        bodySmall = caption,
        labelLarge = button,
        labelMedium = label,
        labelSmall = caption,
    )
}
