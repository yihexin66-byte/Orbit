package com.example.orbit.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbit.R
import com.example.orbit.ui.PlannerSegment
import com.example.orbit.ui.theme.OrbitCorners
import com.example.orbit.ui.theme.orbitColors

/**
 * Shared chrome for the merged 目标 page: one screen title, one segmented
 * switch, one add action. Goals, tasks and habits sit under the same header so
 * switching feels like changing lens rather than changing pages.
 */
@Composable
fun PlannerHeader(
    segment: PlannerSegment,
    subtitle: String,
    goalCount: Int,
    taskCount: Int,
    habitCount: Int,
    onSegmentChange: (PlannerSegment) -> Unit,
    onAdd: () -> Unit,
    addContentDescription: String,
    modifier: Modifier = Modifier,
    cloudSignedIn: Boolean = false,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OrbitScreenHeader(
            eyebrow = stringResource(R.string.planner_eyebrow),
            title = stringResource(R.string.planner_title),
            subtitle = subtitle,
            trailing = {
                Column(horizontalAlignment = Alignment.End) {
                    OrbitBadge(
                        text = stringResource(
                            if (cloudSignedIn) R.string.cloud_synced else R.string.local_data,
                        ),
                    )
                    Spacer(Modifier.height(10.dp))
                    OrbitIconButton(
                        onClick = onAdd,
                        contentDescription = addContentDescription,
                        filled = true,
                        size = 52.dp,
                    )
                }
            },
        )
        Spacer(Modifier.height(18.dp))
        PlannerSegmentTabs(
            selected = segment,
            goalCount = goalCount,
            taskCount = taskCount,
            habitCount = habitCount,
            onSelect = onSegmentChange,
        )
    }
}

/**
 * Three-way lens switch. The selected third is a solid volt pill, so the active
 * list is legible at a glance without a second accent border around the track.
 */
@Composable
fun PlannerSegmentTabs(
    selected: PlannerSegment,
    goalCount: Int,
    taskCount: Int,
    habitCount: Int,
    onSelect: (PlannerSegment) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    val shape = RoundedCornerShape(OrbitCorners.pill)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.cardBorder, shape)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlannerSegment.entries.forEach { segment ->
            val isSelected = segment == selected
            val background by animateColorAsState(
                targetValue = if (isSelected) colors.primary else colors.card,
                animationSpec = tween(180),
                label = "plannerSegmentBackground",
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) colors.onAccent else colors.textSecondary,
                animationSpec = tween(180),
                label = "plannerSegmentContent",
            )
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val pressScale by animateFloatAsState(
                targetValue = if (pressed) 0.98f else 1f,
                animationSpec = tween(110),
                label = "plannerSegmentPress",
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .graphicsLayer {
                        scaleX = pressScale
                        scaleY = pressScale
                    }
                    .clip(RoundedCornerShape(OrbitCorners.pill))
                    .background(background)
                    .selectable(
                        selected = isSelected,
                        role = Role.Tab,
                        interactionSource = interaction,
                        indication = LocalIndication.current,
                        onClick = { onSelect(segment) },
                    )
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(segment.labelRes),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                    color = contentColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when (segment) {
                        PlannerSegment.GOALS -> goalCount
                        PlannerSegment.TASKS -> taskCount
                        PlannerSegment.HABITS -> habitCount
                    }.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 0.sp,
                        fontFeatureSettings = "tnum",
                    ),
                    color = contentColor.copy(alpha = 0.62f),
                )
            }
        }
    }
}
