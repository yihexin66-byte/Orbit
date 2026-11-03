package com.example.orbit.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbit.R
import com.example.orbit.model.HabitDay
import com.example.orbit.model.TaskDate
import com.example.orbit.ui.theme.orbitColors

/**
 * 48dp check target shared by the planner cards and the home summary. The
 * whole circle toggles today's check-in and announces both the action and the
 * current state for accessibility services.
 */
@Composable
fun HabitCheckButton(
    checked: Boolean,
    actionDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.orbitColors.primary,
    size: Dp = 48.dp,
) {
    val colors = MaterialTheme.orbitColors
    val stateLabel = stringResource(
        if (checked) R.string.habits_checked_today else R.string.habits_pending_today,
    )
    val checkContent = if (colors.isDark) colors.onAccent else colors.backgroundTop
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = { onClick() },
            )
            .semantics {
                contentDescription = actionDescription
                stateDescription = stateLabel
            },
        contentAlignment = Alignment.Center,
    ) {
        // The touch target stays 48dp while the visible disc is slightly
        // smaller, keeping habit rows as light as the task rows.
        Box(
            modifier = Modifier
                .size(size - 8.dp)
                .clip(CircleShape)
                .background(if (checked) accent else colors.card)
                .border(
                    width = 1.dp,
                    color = if (checked) accent else colors.cardBorder,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = checkContent,
                    modifier = Modifier.size(size * 0.36f),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(size * 0.18f)
                        .clip(CircleShape)
                        .background(accent),
                )
            }
        }
    }
}

/** Compact seven-day strip used inside habit cards and the home summary. */
@Composable
fun HabitWeekDots(
    days: List<HabitDay>,
    accent: Color,
    description: String,
    modifier: Modifier = Modifier,
    dotSize: Dp = 12.dp,
    showLabels: Boolean = true,
) {
    val colors = MaterialTheme.orbitColors
    Row(
        modifier = modifier
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        days.forEach { day ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = if (showLabels) Modifier.weight(1f) else Modifier,
            ) {
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(
                            when {
                                day.checked -> accent
                                day.isFuture -> colors.track.copy(alpha = 0.55f)
                                else -> colors.track
                            },
                        )
                        .then(
                            if (day.isToday) {
                                Modifier.border(1.dp, accent.copy(alpha = 0.8f), CircleShape)
                            } else {
                                Modifier
                            },
                        ),
                )
                if (showLabels) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = TaskDate.weekdayLabel(day.epochDay).takeLast(1),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            lineHeight = 10.sp,
                            letterSpacing = 0.sp,
                        ),
                        color = if (day.isToday) accent else colors.textMuted,
                    )
                }
            }
            if (!showLabels && day != days.last()) {
                Spacer(Modifier.width(4.dp))
            }
        }
    }
}

/**
 * Monday-first 12-week heatmap. Cells are drawn in one cached canvas pass, so
 * five habits on screen still cost far less than 420 composables.
 */
@Composable
fun HabitHeatmap(
    weeks: List<List<HabitDay>>,
    accent: Color,
    description: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    val gap = 3.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
    ) {
        // Fill the card width on phones; the cap only guards very wide windows.
        val cell = ((maxWidth - gap * 11) / 12f)
            .coerceAtLeast(4.dp)
            .coerceAtMost(24.dp)
        val width = cell * 12 + gap * 11
        val height = cell * 7 + gap * 6
        // Future days stay quieter than past unchecked days in both themes;
        // a plain alpha override would make white tracks glow in dark mode.
        val futureTrack = colors.track.copy(alpha = colors.track.alpha * 0.45f)
        Canvas(modifier = Modifier.size(width = width, height = height)) {
            val cellPx = cell.toPx()
            val gapPx = gap.toPx()
            val radius = CornerRadius(cellPx * 0.28f)
            weeks.forEachIndexed { weekIndex, week ->
                week.forEachIndexed { dayIndex, day ->
                    val topLeft = Offset(
                        x = weekIndex * (cellPx + gapPx),
                        y = dayIndex * (cellPx + gapPx),
                    )
                    val fill = when {
                        day.checked -> accent
                        day.isFuture -> futureTrack
                        else -> colors.track
                    }
                    drawRoundRect(
                        color = fill,
                        topLeft = topLeft,
                        size = Size(cellPx, cellPx),
                        cornerRadius = radius,
                    )
                    if (day.isToday) {
                        drawRoundRect(
                            color = colors.textPrimary.copy(alpha = 0.8f),
                            topLeft = topLeft,
                            size = Size(cellPx, cellPx),
                            cornerRadius = radius,
                            style = Stroke(width = 1.dp.toPx()),
                        )
                    }
                }
            }
        }
    }
}
