package com.ridetrack.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ridetrack.app.ui.format.Format
import com.ridetrack.app.ui.theme.RtColors
import com.ridetrack.app.ui.theme.RtType
import kotlin.math.cos
import kotlin.math.sin

fun leanColor(deg: Double?) = when (Format.leanSide(deg)) {
    Format.LeanSide.LEFT -> RtColors.Left
    Format.LeanSide.RIGHT -> RtColors.Right
    else -> RtColors.TextPrimary
}

/**
 * Upper half-circle lean gauge (±60°). The arc fills from centre toward the lean side.
 * Hidden needle when lean is unavailable.
 */
@Composable
fun LeanArc(leanDeg: Double?, modifier: Modifier = Modifier, maxLeft: Double? = null, maxRight: Double? = null) {
    val animated by animateFloatAsState(
        targetValue = (leanDeg ?: 0.0).toFloat().coerceIn(-60f, 60f),
        animationSpec = tween(150),
        label = "lean",
    )
    val color = leanColor(leanDeg)
    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(2.6f)
            .semantics { contentDescription = "Lean ${Format.lean(leanDeg)}" },
    ) {
        val stroke = 6.dp.toPx()
        val radius = minOf(size.width / 2f, size.height) - stroke
        val center = Offset(size.width / 2f, size.height - stroke / 2)
        val topLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2, radius * 2)
        // Track covers -60°..+60° around vertical: 210°..330° in canvas angles.
        drawArc(RtColors.Outline, startAngle = 210f, sweepAngle = 120f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        fun tick(deg: Double, tickColor: androidx.compose.ui.graphics.Color) {
            val a = Math.toRadians(270.0 + deg)
            val inner = radius - 14.dp.toPx()
            drawLine(
                tickColor,
                Offset(center.x + (inner * cos(a)).toFloat(), center.y + (inner * sin(a)).toFloat()),
                Offset(center.x + (radius * cos(a)).toFloat(), center.y + (radius * sin(a)).toFloat()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        maxLeft?.let { tick(-it.coerceAtMost(60.0), RtColors.Left.copy(alpha = 0.7f)) }
        maxRight?.let { tick(it.coerceAtMost(60.0), RtColors.Right.copy(alpha = 0.7f)) }
        if (leanDeg != null) {
            drawArc(color, startAngle = 270f, sweepAngle = animated, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            val a = Math.toRadians(270.0 + animated)
            drawCircle(color, radius = 7.dp.toPx(), center = Offset(center.x + (radius * cos(a)).toFloat(), center.y + (radius * sin(a)).toFloat()))
        }
    }
}

/**
 * Friction-circle style G indicator: braking up, acceleration down, cornering left/right.
 * The outer ring is 1 G.
 */
@Composable
fun GForceIndicator(longitudinalG: Double?, lateralG: Double?, modifier: Modifier = Modifier) {
    val x by animateFloatAsState((lateralG ?: 0.0).toFloat().coerceIn(-1f, 1f), tween(150), label = "gx")
    val y by animateFloatAsState((longitudinalG ?: 0.0).toFloat().coerceIn(-1f, 1f), tween(150), label = "gy")
    val available = longitudinalG != null || lateralG != null
    Box(
        modifier.semantics {
            contentDescription = "G-force: longitudinal ${Format.gSigned(longitudinalG)}, lateral ${Format.gSigned(lateralG)}"
        },
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(18.dp),
        ) {
            val r = size.minDimension / 2f
            val c = center
            drawCircle(RtColors.Outline, radius = r, center = c, style = Stroke(1.5.dp.toPx()))
            drawCircle(RtColors.Outline.copy(alpha = 0.6f), radius = r / 2f, center = c, style = Stroke(1.dp.toPx()))
            drawLine(RtColors.Outline, Offset(c.x - r, c.y), Offset(c.x + r, c.y), 1.dp.toPx())
            drawLine(RtColors.Outline, Offset(c.x, c.y - r), Offset(c.x, c.y + r), 1.dp.toPx())
            if (available) {
                // Braking (negative longitudinal) moves the dot up; acceleration down.
                val dot = Offset(c.x + x * r, c.y + y * r)
                drawCircle(RtColors.GForce.copy(alpha = 0.25f), radius = 14.dp.toPx(), center = dot)
                drawCircle(RtColors.GForce, radius = 7.dp.toPx(), center = dot)
            }
        }
        Text("BRAKE", style = RtType.label, color = RtColors.TextTertiary, modifier = Modifier.align(Alignment.TopCenter))
        Text("ACCEL", style = RtType.label, color = RtColors.TextTertiary, modifier = Modifier.align(Alignment.BottomCenter))
        Text("L", style = RtType.label, color = RtColors.TextTertiary, modifier = Modifier.align(Alignment.CenterStart))
        Text("R", style = RtType.label, color = RtColors.TextTertiary, modifier = Modifier.align(Alignment.CenterEnd))
    }
}
