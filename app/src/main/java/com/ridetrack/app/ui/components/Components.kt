package com.ridetrack.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ridetrack.app.ui.theme.RtColors
import com.ridetrack.app.ui.theme.RtDimens
import com.ridetrack.app.ui.theme.RtType

@Composable
fun RtCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(RtDimens.cardPadding),
    color: Color = RtColors.Surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(RtDimens.cardRadius)
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, color = color) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(modifier = modifier.fillMaxWidth(), shape = shape, color = color) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    large: Boolean = false,
    icon: ImageVector? = null,
    color: Color = RtColors.Primary,
    contentColor: Color = RtColors.OnPrimary,
    haptic: Boolean = false,
) {
    val haptics = LocalHapticFeedback.current
    Button(
        onClick = {
            if (haptic) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(if (large) RtDimens.primaryButtonHeight else RtDimens.buttonHeight),
        shape = RoundedCornerShape(if (large) 20.dp else 16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = contentColor,
            disabledContainerColor = RtColors.SurfaceRaised,
            disabledContentColor = RtColors.TextTertiary,
        ),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(RtDimens.xs))
        }
        Text(text.uppercase(), style = RtType.button)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    contentColor: Color = RtColors.TextPrimary,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(RtDimens.buttonHeight),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, RtColors.Outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor, disabledContentColor = RtColors.TextTertiary),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(RtDimens.iconSize))
            Spacer(Modifier.width(RtDimens.xs))
        }
        Text(text.uppercase(), style = RtType.button.copy(fontSize = RtType.button.fontSize * 0.9f))
    }
}

/** Small, muted, uppercase label. */
@Composable
fun Label(text: String, modifier: Modifier = Modifier, color: Color = RtColors.TextSecondary, textAlign: TextAlign? = null) {
    Text(text.uppercase(), modifier = modifier, style = RtType.label, color = color, textAlign = textAlign, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = RtDimens.lg, bottom = RtDimens.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text.uppercase(),
            style = RtType.label,
            color = RtColors.TextSecondary,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        trailing()
    }
}

/** A value with an optional unit, e.g. "24.6 km". */
@Composable
fun MetricValue(
    value: String,
    unit: String? = null,
    style: TextStyle = RtType.metricM,
    color: Color = RtColors.TextPrimary,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Text(value, style = style, color = color, maxLines = 1)
        if (unit != null && value != com.ridetrack.app.ui.format.Format.DASH) {
            Spacer(Modifier.width(4.dp))
            Text(unit, style = RtType.unit, color = RtColors.TextSecondary, modifier = Modifier.padding(bottom = 3.dp), maxLines = 1)
        }
    }
}

/** Label above a value. */
@Composable
fun StatBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    style: TextStyle = RtType.metricM,
    color: Color = RtColors.TextPrimary,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "$label ${value}${unit?.let { " $it" } ?: ""}"
        },
        horizontalAlignment = horizontalAlignment,
    ) {
        Label(label)
        Spacer(Modifier.height(RtDimens.xxs))
        MetricValue(value, unit, style, color)
    }
}

/** Stat in its own card; used in 2-column grids. */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, unit: String? = null, color: Color = RtColors.TextPrimary) {
    RtCard(modifier = modifier, contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)) {
        StatBlock(label, value, unit = unit, color = color)
    }
}

@Composable
fun TwoColumn(modifier: Modifier = Modifier, left: @Composable (Modifier) -> Unit, right: @Composable (Modifier) -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RtDimens.cardSpacing)) {
        left(Modifier.weight(1f))
        right(Modifier.weight(1f))
    }
}

enum class StatusLevel(val color: Color) {
    OK(RtColors.Ok),
    WARNING(RtColors.Warning),
    ERROR(RtColors.Error),
    INACTIVE(RtColors.TextTertiary),
}

/** Dot + text; state is always spelled out, never colour-only. */
@Composable
fun StatusIndicator(label: String, status: String, level: StatusLevel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "$label: $status" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = RtType.caption, color = RtColors.TextSecondary)
        Spacer(Modifier.width(RtDimens.xs))
        Box(
            Modifier
                .size(8.dp)
                .background(level.color, CircleShape),
        )
        Spacer(Modifier.width(6.dp))
        Text(status, style = RtType.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = RtColors.TextPrimary)
    }
}

@Composable
fun Chip(text: String, color: Color, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(50))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text.uppercase(), style = RtType.label.copy(fontSize = RtType.label.fontSize * 0.9f), color = color, maxLines = 1)
    }
}

@Composable
fun DemoBadge(modifier: Modifier = Modifier) {
    Chip("Demo mode", RtColors.Warning, modifier)
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    action: (@Composable () -> Unit)? = null,
) {
    RtCard(modifier = modifier) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = RtColors.TextSecondary, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(RtDimens.sm))
            }
            Text(title, style = RtType.bodyStrong, color = RtColors.TextPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(RtDimens.xs))
            Text(message, style = RtType.body, color = RtColors.TextSecondary, textAlign = TextAlign.Center)
            if (action != null) {
                Spacer(Modifier.height(RtDimens.md))
                action()
            }
        }
    }
}

/** Screen title row with an optional back button and trailing actions. */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = RtDimens.xs, bottom = RtDimens.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = RtColors.TextPrimary)
            }
            Spacer(Modifier.width(RtDimens.xxs))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = RtType.title,
                color = RtColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                Text(subtitle, style = RtType.caption, color = RtColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        actions()
    }
}

@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = RtColors.TextPrimary) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = RtType.body, color = RtColors.TextSecondary, modifier = Modifier.weight(1f))
        Text(value, style = RtType.body.copy(fontFeatureSettings = "tnum"), color = valueColor, textAlign = TextAlign.End)
    }
}
