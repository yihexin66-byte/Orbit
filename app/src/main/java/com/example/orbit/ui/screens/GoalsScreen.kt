package com.example.orbit.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbit.R
import com.example.orbit.model.Goal
import com.example.orbit.model.GoalProgress
import com.example.orbit.model.GoalStage
import com.example.orbit.model.GoalStatus
import com.example.orbit.model.Task
import com.example.orbit.model.TaskCategory
import com.example.orbit.model.TaskDate
import com.example.orbit.model.TaskPriority
import com.example.orbit.ui.GoalLoadStatus
import com.example.orbit.ui.GoalUiState
import com.example.orbit.ui.PlannerSegment
import com.example.orbit.ui.components.OrbitAddTaskCard
import com.example.orbit.ui.components.OrbitCard
import com.example.orbit.ui.components.OrbitEmptyState
import com.example.orbit.ui.components.OrbitLazyScreen
import com.example.orbit.ui.components.OrbitLoadingCard
import com.example.orbit.ui.components.OrbitNote
import com.example.orbit.ui.components.OrbitProgressBar
import com.example.orbit.ui.components.OrbitSectionTitle
import com.example.orbit.ui.components.PlannerHeader
import com.example.orbit.ui.components.TagPill
import com.example.orbit.ui.theme.OrbitCorners
import com.example.orbit.ui.theme.OrbitTheme
import com.example.orbit.ui.theme.orbitColors
import kotlin.math.roundToInt

private const val MAX_PATH_STAGES = 4
private const val MAX_SEGMENT_STAGES = 8

@Composable
fun GoalsScreen(
    uiState: GoalUiState,
    onAddGoal: () -> Unit,
    onOpenGoal: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    segment: PlannerSegment = PlannerSegment.GOALS,
    onSegmentChange: (PlannerSegment) -> Unit = {},
    taskCount: Int = 0,
    habitCount: Int = 0,
    cloudSignedIn: Boolean = false,
    onOpenAiPlan: () -> Unit = {},
) {
    val colors = MaterialTheme.orbitColors
    val goals = uiState.orderedGoals
    val focusGoal = uiState.activeGoals.firstOrNull()

    OrbitLazyScreen(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = {
            item(key = "header") {
                PlannerHeader(
                    segment = segment,
                    subtitle = if (goals.isEmpty()) {
                        stringResource(R.string.goals_subtitle_empty)
                    } else {
                        stringResource(
                            R.string.goals_subtitle,
                            goals.size,
                            uiState.stageCount,
                        )
                    },
                    goalCount = goals.size,
                    taskCount = taskCount,
                    habitCount = habitCount,
                    onSegmentChange = onSegmentChange,
                    onAdd = onAddGoal,
                    addContentDescription = stringResource(R.string.goals_add_fab),
                    cloudSignedIn = cloudSignedIn,
                )
            }

            item(key = "ai-plan") {
                AiPlanEntryCard(onClick = onOpenAiPlan)
            }

            when {
                uiState.isLoading -> {
                    item(key = "loading") {
                        OrbitLoadingCard(modifier = Modifier.padding(top = 6.dp))
                    }
                }

                uiState.loadFailed -> {
                    item(key = "load-error") {
                        OrbitEmptyState(
                            modifier = Modifier.padding(top = 6.dp),
                            title = stringResource(R.string.task_load_failed_title),
                            description = stringResource(R.string.task_load_failed_desc),
                            actionLabel = stringResource(R.string.task_retry),
                            onAction = onRetry,
                        )
                    }
                }

                goals.isEmpty() -> {
                    item(key = "empty") {
                        OrbitEmptyState(
                            modifier = Modifier.padding(top = 6.dp),
                            title = stringResource(R.string.goals_galaxy_empty_title),
                            description = stringResource(R.string.goals_galaxy_empty_desc),
                            actionLabel = stringResource(R.string.goals_add_card_title),
                            onAction = onAddGoal,
                        )
                    }
                }

                else -> {
                    if (focusGoal != null) {
                        item(key = "focus") {
                            GoalFocusCard(
                                goal = focusGoal,
                                progress = uiState.progressFor(focusGoal),
                                stages = uiState.stagesFor(focusGoal.id),
                                onOpen = { onOpenGoal(focusGoal.id) },
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }

                    item(key = "list-header") {
                        OrbitSectionTitle(
                            title = stringResource(R.string.goals_list_title),
                            modifier = Modifier.padding(top = 8.dp),
                            trailing = {
                                Text(
                                    text = goals.size.toString().padStart(2, '0'),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum",
                                    ),
                                    color = colors.accentLine,
                                )
                            },
                        )
                    }

                    items(
                        count = goals.size,
                        key = { index -> "goal-${goals[index].id}" },
                    ) { index ->
                        val goal = goals[index]
                        GoalCard(
                            goal = goal,
                            progress = uiState.progressFor(goal),
                            stages = uiState.stagesFor(goal.id),
                            stageProgress = uiState::stageProgress,
                            onOpen = { onOpenGoal(goal.id) },
                        )
                    }

                    item(key = "add-card") {
                        OrbitAddTaskCard(
                            title = stringResource(R.string.goals_add_card_title),
                            description = stringResource(R.string.goals_add_card_desc),
                            onClick = onAddGoal,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            item(key = "goal-note") {
                OrbitNote(
                    text = stringResource(R.string.goal_editor_note),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        },
    )
}

/**
 * Entry to the AI planning flow. The spark tile marks it as the only
 * generative action on this tab while keeping the same card rhythm.
 */
@Composable
private fun AiPlanEntryCard(onClick: () -> Unit) {
    val colors = MaterialTheme.orbitColors
    OrbitCard(
        onClick = onClick,
        accent = colors.primary,
        padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                SparkGlyph(
                    color = colors.onAccent,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.goals_ai_card_title),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = colors.textPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                    TagPill(text = "AI", color = colors.primary)
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.goals_ai_card_desc),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = colors.textMuted,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = colors.accentLine,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun SparkGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension * 0.46f
        val waist = radius * 0.18f
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx, cy - radius)
            quadraticBezierTo(cx + waist, cy - waist, cx + radius, cy)
            quadraticBezierTo(cx + waist, cy + waist, cx, cy + radius)
            quadraticBezierTo(cx - waist, cy + waist, cx - radius, cy)
            quadraticBezierTo(cx - waist, cy - waist, cx, cy - radius)
            close()
        }
        drawPath(path, color = color)
        drawCircle(
            color = color,
            radius = radius * 0.16f,
            center = Offset(size.width * 0.83f, size.height * 0.18f),
        )
    }
}

/**
 * One hero for the goal that deserves attention. It is also the only node on
 * this screen that exposes the full progress description, so screen readers
 * hear the summary once and the list stays a quiet index.
 */
@Composable
private fun GoalFocusCard(
    goal: Goal,
    progress: GoalProgress,
    stages: List<GoalStage>,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    val surface = if (colors.isDark) colors.cardElevated else colors.textPrimary
    val onSurface = if (colors.isDark) colors.textPrimary else colors.backgroundTop
    val statusLabel = stringResource(goal.status.labelRes)
    val percent = (progress.value * 100f).roundToInt()
    val description = stringResource(
        R.string.goals_galaxy_node_desc,
        goal.title,
        percent,
        statusLabel,
    )
    val meta = goalMeta(progress)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.99f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "goalFocusPress",
    )
    val shape = RoundedCornerShape(OrbitCorners.card)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(surface)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onOpen,
            )
            .semantics { contentDescription = description },
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(colors.primary),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.goals_focus_label),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        letterSpacing = 1.6.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = onSurface.copy(alpha = 0.62f),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = onSurface.copy(alpha = 0.66f),
                )
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = goal.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 26.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (!goal.description.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = goal.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    ),
                    color = onSurface.copy(alpha = 0.60f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = stringResource(R.string.goals_progress, percent),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 48.sp,
                        lineHeight = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFeatureSettings = "tnum",
                    ),
                    color = colors.primary,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.goal_detail_complete_label),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        letterSpacing = 1.4.sp,
                    ),
                    color = onSurface.copy(alpha = 0.48f),
                    modifier = Modifier.padding(bottom = 7.dp),
                )
                Spacer(Modifier.weight(1f))
                goal.targetDate?.let { target ->
                    Text(
                        text = stringResource(
                            R.string.goals_target_date,
                            TaskDate.monthDayLabel(target),
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = onSurface.copy(alpha = 0.72f),
                        modifier = Modifier
                            .padding(bottom = 3.dp)
                            .clip(RoundedCornerShape(OrbitCorners.pill))
                            .border(
                                width = 1.dp,
                                color = onSurface.copy(alpha = 0.20f),
                                shape = RoundedCornerShape(OrbitCorners.pill),
                            )
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            if (stages.isNotEmpty()) {
                GoalStagePath(
                    stages = stages,
                    accent = colors.primary,
                    track = onSurface.copy(alpha = 0.20f),
                    labelColor = onSurface,
                )
            } else {
                OrbitProgressBar(
                    progress = progress.value,
                    height = 5.dp,
                    color = colors.primary,
                    trackColor = onSurface.copy(alpha = 0.18f),
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        lineHeight = 15.sp,
                        fontFeatureSettings = "tnum",
                    ),
                    color = onSurface.copy(alpha = 0.56f),
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(colors.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = colors.onAccent,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalStagePath(
    stages: List<GoalStage>,
    accent: Color,
    track: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
) {
    val shown = stages.take(MAX_PATH_STAGES)
    val overflow = stages.size - shown.size
    val stepCount = shown.size + if (overflow > 0) 1 else 0
    val currentIndex = stages.indexOfFirst { !it.isCompleted }
    val activeIndex = when {
        currentIndex < 0 -> -1
        currentIndex < shown.size -> currentIndex
        else -> shown.size
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp),
        ) {
            val slot = size.width / stepCount
            val centerY = size.height / 2f
            val startX = slot / 2f
            val endX = startX + slot * (stepCount - 1)
            val lineWidth = 2.dp.toPx()

            drawLine(
                color = track,
                start = Offset(startX, centerY),
                end = Offset(endX, centerY),
                strokeWidth = lineWidth,
                cap = StrokeCap.Round,
            )
            val highlightedEnd = if (activeIndex < 0) {
                endX
            } else {
                startX + slot * activeIndex
            }
            if (highlightedEnd > startX) {
                drawLine(
                    color = accent,
                    start = Offset(startX, centerY),
                    end = Offset(highlightedEnd, centerY),
                    strokeWidth = lineWidth,
                    cap = StrokeCap.Round,
                )
            }

            repeat(stepCount) { index ->
                val cx = startX + slot * index
                when {
                    index == activeIndex -> {
                        val outer = 6.5.dp.toPx()
                        val inner = 2.5.dp.toPx()
                        drawRect(
                            color = accent,
                            topLeft = Offset(cx - outer, centerY - outer),
                            size = Size(outer * 2f, outer * 2f),
                            style = Stroke(width = 1.5.dp.toPx()),
                        )
                        drawRect(
                            color = accent,
                            topLeft = Offset(cx - inner, centerY - inner),
                            size = Size(inner * 2f, inner * 2f),
                        )
                    }

                    activeIndex < 0 || index < activeIndex -> {
                        val radius = 3.5.dp.toPx()
                        drawRect(
                            color = accent,
                            topLeft = Offset(cx - radius, centerY - radius),
                            size = Size(radius * 2f, radius * 2f),
                        )
                    }

                    else -> {
                        val radius = 3.5.dp.toPx()
                        drawRect(
                            color = track,
                            topLeft = Offset(cx - radius, centerY - radius),
                            size = Size(radius * 2f, radius * 2f),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            shown.forEachIndexed { index, stage ->
                val isCurrent = index == activeIndex
                Text(
                    text = stage.title.ifBlank { "—" },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        lineHeight = 12.sp,
                        letterSpacing = 0.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    ),
                    color = when {
                        isCurrent -> accent
                        stage.isCompleted -> labelColor.copy(alpha = 0.68f)
                        else -> labelColor.copy(alpha = 0.40f)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp),
                )
            }
            if (overflow > 0) {
                Text(
                    text = stringResource(R.string.goals_stage_more, overflow),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        lineHeight = 12.sp,
                        letterSpacing = 0.sp,
                        fontWeight = if (activeIndex == shown.size) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Medium
                        },
                    ),
                    color = if (activeIndex == shown.size) {
                        accent
                    } else {
                        labelColor.copy(alpha = 0.40f)
                    },
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun GoalCard(
    goal: Goal,
    progress: GoalProgress,
    stages: List<GoalStage>,
    stageProgress: (GoalStage) -> Float,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    val percent = (progress.value * 100f).roundToInt()
    val marker = when (goal.status) {
        GoalStatus.ACTIVE -> colors.accentLine
        GoalStatus.PAUSED -> colors.textSecondary
        GoalStatus.COMPLETED -> colors.success
        GoalStatus.ARCHIVED -> colors.textMuted
    }

    OrbitCard(
        modifier = modifier,
        onClick = onOpen,
        padding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(marker),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = stringResource(goal.status.labelRes),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 1.3.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = colors.textMuted,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.goals_progress, percent),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 24.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFeatureSettings = "tnum",
                ),
                color = colors.accentLine,
            )
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = goal.title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 19.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!goal.description.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = goal.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                ),
                color = colors.textMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(14.dp))
        if (stages.isEmpty()) {
            OrbitProgressBar(
                progress = progress.value,
                height = 4.dp,
                color = colors.accentLine,
                trackColor = colors.track,
            )
        } else if (stages.size <= MAX_SEGMENT_STAGES) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                stages.forEach { stage ->
                    val fraction = stageProgress(stage).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.track),
                    ) {
                        if (fraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .fillMaxHeight()
                                    .background(colors.accentLine),
                            )
                        }
                    }
                }
            }
        } else {
            OrbitProgressBar(
                progress = progress.value,
                height = 4.dp,
                color = colors.accentLine,
                trackColor = colors.track,
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = goalMeta(progress),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    fontFeatureSettings = "tnum",
                ),
                color = colors.textMuted,
            )
            Spacer(Modifier.weight(1f))
            goal.targetDate?.let { target ->
                Text(
                    text = stringResource(
                        R.string.goals_target_date,
                        TaskDate.monthDayLabel(target),
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        fontFeatureSettings = "tnum",
                    ),
                    color = colors.textMuted,
                )
            }
        }
    }
}

@Composable
private fun goalMeta(progress: GoalProgress): String = when {
    progress.hasStages -> stringResource(
        R.string.goals_meta_stages,
        progress.stageCount,
        progress.completedStageCount,
    )

    progress.taskCount > 0 -> stringResource(
        R.string.goals_meta_tasks,
        progress.taskCount,
        progress.completedTaskCount,
    )

    else -> stringResource(R.string.goals_meta_undecomposed)
}

@Composable
internal fun goalStatusColor(status: GoalStatus): Color {
    val colors = MaterialTheme.orbitColors
    return when (status) {
        GoalStatus.ACTIVE -> colors.primary
        GoalStatus.PAUSED -> colors.warning
        GoalStatus.COMPLETED -> colors.success
        GoalStatus.ARCHIVED -> colors.textMuted
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF101010, widthDp = 393, heightDp = 852)
@Composable
private fun GoalsScreenPreview() {
    val today = TaskDate.todayEpochDay()
    val goals = listOf(
        Goal(
            id = 1,
            title = "完成个人作品集",
            description = "把三个项目整理成可投递的案例",
            status = GoalStatus.ACTIVE,
            targetDate = today + 40,
            createdAt = 1L,
            sortOrder = 0,
        ),
        Goal(
            id = 2,
            title = "保持每周运动",
            description = "每周至少三次",
            status = GoalStatus.PAUSED,
            createdAt = 2L,
            sortOrder = 1,
        ),
        Goal(
            id = 3,
            title = "读完十二本书",
            status = GoalStatus.ACTIVE,
            createdAt = 3L,
            sortOrder = 2,
        ),
    )
    val stages = listOf(
        GoalStage(id = 1, goalId = 1, title = "梳理案例", isCompleted = true),
        GoalStage(id = 2, goalId = 1, title = "完成视觉稿"),
        GoalStage(id = 3, goalId = 1, title = "上线部署"),
        GoalStage(id = 4, goalId = 2, title = "八月运动周"),
    )
    val tasks = listOf(
        Task(
            id = 1,
            title = "整理项目截图",
            priority = TaskPriority.HIGH,
            category = TaskCategory.WORK,
            goalId = 1,
            stageId = 2,
            isCompleted = true,
            createdAt = 1L,
        ),
        Task(
            id = 2,
            title = "写项目总结",
            priority = TaskPriority.MEDIUM,
            category = TaskCategory.WORK,
            goalId = 1,
            stageId = 2,
            createdAt = 2L,
        ),
    )
    OrbitTheme {
        GoalsScreen(
            uiState = GoalUiState(
                goals = goals,
                stages = stages,
                tasks = tasks,
                loadStatus = GoalLoadStatus.READY,
            ),
            onAddGoal = {},
            onOpenGoal = {},
            onRetry = {},
        )
    }
}
