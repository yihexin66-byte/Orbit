package com.example.orbit.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbit.R
import com.example.orbit.model.Goal
import com.example.orbit.model.GoalProgress
import com.example.orbit.model.GoalStage
import com.example.orbit.model.GoalStatus
import com.example.orbit.model.Task
import com.example.orbit.model.TaskDate
import com.example.orbit.ui.GoalDeletionState
import com.example.orbit.ui.GoalUiState
import com.example.orbit.ui.components.CompactTaskRow
import com.example.orbit.ui.components.GlassDivider
import com.example.orbit.ui.components.OrbitCard
import com.example.orbit.ui.components.OrbitEmptyState
import com.example.orbit.ui.components.OrbitIconButton
import com.example.orbit.ui.components.OrbitLazyScreen
import com.example.orbit.ui.components.OrbitLoadingCard
import com.example.orbit.ui.components.OrbitNote
import com.example.orbit.ui.components.OrbitProgressBar
import com.example.orbit.ui.components.OrbitSectionTitle
import com.example.orbit.ui.components.OrbitTextLink
import com.example.orbit.ui.theme.OrbitCorners
import com.example.orbit.ui.theme.orbitColors
import kotlin.math.roundToInt

@Composable
fun GoalDetailScreen(
    uiState: GoalUiState,
    goalId: Long,
    deletion: GoalDeletionState?,
    onBack: () -> Unit,
    onEditGoal: (Long) -> Unit,
    onAddStage: (Long) -> Unit,
    onEditStage: (Long) -> Unit,
    onToggleStage: (Long, Boolean) -> Unit,
    onAddTask: (Long, Long?) -> Unit,
    onToggleTask: (Long) -> Unit,
    onEditTask: (Long) -> Unit,
    onStatusChange: (Long, GoalStatus) -> Unit,
    onDeleteGoal: (Long) -> Unit,
    onDeleteDismiss: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    val goal = uiState.goalById(goalId)

    OrbitLazyScreen(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = {
            item(key = "nav") {
                DetailNavRow(
                    onBack = onBack,
                    onEdit = { goal?.let { onEditGoal(it.id) } },
                    editEnabled = goal != null,
                )
            }

            when {
                goal == null && uiState.isLoading -> {
                    item(key = "loading") {
                        OrbitLoadingCard(modifier = Modifier.padding(top = 6.dp))
                    }
                }

                goal == null && uiState.loadFailed -> {
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

                goal == null -> {
                    item(key = "missing") {
                        OrbitEmptyState(
                            modifier = Modifier.padding(top = 6.dp),
                            title = stringResource(R.string.goal_detail_missing_title),
                            description = stringResource(R.string.goal_detail_missing_desc),
                            actionLabel = stringResource(R.string.goal_detail_back),
                            onAction = onBack,
                        )
                    }
                }

                else -> {
                    val stages = uiState.stagesFor(goal.id)
                    val goalLevelTasks = uiState.goalLevelTasks(goal.id)
                    val progress = uiState.progressFor(goal)
                    val currentStageIndex = stages.indexOfFirst { !it.isCompleted }

                    item(key = "hero") {
                        GoalDetailHero(
                            goal = goal,
                            progress = progress,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }

                    item(key = "stages-header") {
                        OrbitSectionTitle(
                            title = stringResource(R.string.goal_detail_stages),
                            modifier = Modifier.padding(top = 8.dp),
                            trailing = {
                                Text(
                                    text = stringResource(
                                        R.string.goal_detail_stage_count,
                                        stages.size,
                                    ),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum",
                                    ),
                                    color = colors.accentLine,
                                )
                                if (stages.isNotEmpty()) {
                                    Spacer(Modifier.width(4.dp))
                                    OrbitTextLink(
                                        text = stringResource(R.string.goal_detail_add_stage),
                                        onClick = { onAddStage(goal.id) },
                                    )
                                }
                            },
                        )
                    }

                    if (stages.isEmpty()) {
                        item(key = "stages-empty") {
                            OrbitEmptyState(
                                title = stringResource(R.string.goal_detail_stage_empty_title),
                                description = stringResource(R.string.goal_detail_stage_empty_desc),
                                actionLabel = stringResource(R.string.goal_detail_add_stage),
                                onAction = { onAddStage(goal.id) },
                            )
                        }
                    } else {
                        items(
                            count = stages.size,
                            key = { index -> "stage-${stages[index].id}" },
                        ) { index ->
                            val stage = stages[index]
                            StageTimelineItem(
                                stage = stage,
                                progress = uiState.stageProgress(stage),
                                tasks = uiState.tasksForStage(stage.id),
                                isCurrent = index == currentStageIndex,
                                isLast = index == stages.lastIndex,
                                onToggle = {
                                    onToggleStage(stage.id, !stage.isCompleted)
                                },
                                onEdit = { onEditStage(stage.id) },
                                onAddTask = { onAddTask(goal.id, stage.id) },
                                onToggleTask = onToggleTask,
                                onEditTask = onEditTask,
                            )
                        }
                    }

                    item(key = "goal-tasks-header") {
                        OrbitSectionTitle(
                            title = stringResource(R.string.goal_detail_goal_tasks),
                            modifier = Modifier.padding(top = 10.dp),
                            trailing = {
                                Text(
                                    text = goalLevelTasks.size.toString().padStart(2, '0'),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum",
                                    ),
                                    color = colors.textMuted,
                                )
                                Spacer(Modifier.width(4.dp))
                                OrbitTextLink(
                                    text = stringResource(R.string.goal_detail_add_goal_task),
                                    onClick = { onAddTask(goal.id, null) },
                                )
                            },
                        )
                    }

                    if (goalLevelTasks.isEmpty()) {
                        item(key = "goal-tasks-empty") {
                            OrbitCard(
                                padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.goal_detail_goal_tasks_empty),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                    ),
                                    color = colors.textMuted,
                                )
                            }
                        }
                    } else {
                        item(key = "goal-tasks") {
                            OrbitCard(
                                padding = PaddingValues(vertical = 4.dp),
                            ) {
                                goalLevelTasks.forEachIndexed { index, task ->
                                    CompactTaskRow(
                                        task = task,
                                        today = TaskDate.todayEpochDay(),
                                        onToggle = { onToggleTask(task.id) },
                                        onEdit = { onEditTask(task.id) },
                                    )
                                    if (index != goalLevelTasks.lastIndex) {
                                        GlassDivider(
                                            startPadding = 56.dp,
                                            endPadding = 16.dp,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item(key = "progress-note") {
                        OrbitNote(
                            text = stringResource(
                                if (progress.hasStages) {
                                    R.string.goal_detail_note_staged
                                } else {
                                    R.string.goal_detail_note_tasks
                                },
                            ),
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }

                    item(key = "goal-actions") {
                        GoalActionsCard(
                            status = goal.status,
                            onChange = { status -> onStatusChange(goal.id, status) },
                            onDelete = { onDeleteGoal(goal.id) },
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        },
    )

    if (deletion != null) {
        GoalDeleteDialog(
            deletion = deletion,
            onDismiss = onDeleteDismiss,
            onConfirm = onDeleteConfirm,
        )
    }
}

@Composable
private fun DetailNavRow(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    editEnabled: Boolean,
) {
    val colors = MaterialTheme.orbitColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OrbitIconButton(
            onClick = onBack,
            contentDescription = stringResource(R.string.goal_detail_back),
            icon = Icons.AutoMirrored.Filled.ArrowBack,
        )
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(colors.primary),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.goal_detail_eyebrow),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.8.sp),
            color = colors.textSecondary,
        )
        Spacer(Modifier.weight(1f))
        if (editEnabled) {
            OrbitIconButton(
                onClick = onEdit,
                contentDescription = stringResource(R.string.goal_detail_edit),
                icon = Icons.Filled.Edit,
            )
        }
    }
}

@Composable
private fun GoalDetailHero(
    goal: Goal,
    progress: GoalProgress,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    val surface = if (colors.isDark) colors.cardElevated else colors.textPrimary
    val onSurface = if (colors.isDark) colors.textPrimary else colors.backgroundTop
    val percent = (progress.value * 100f).roundToInt()
    val statusLabel = stringResource(goal.status.labelRes)
    val shape = RoundedCornerShape(OrbitCorners.card)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(surface)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        if (goal.status == GoalStatus.ACTIVE) {
                            colors.primary
                        } else {
                            onSurface.copy(alpha = 0.28f)
                        },
                    ),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = onSurface.copy(alpha = 0.64f),
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

        Spacer(Modifier.height(14.dp))
        Text(
            text = goal.title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 30.sp,
                lineHeight = 36.sp,
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
                color = onSurface.copy(alpha = 0.58f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = stringResource(R.string.goals_progress, percent),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 52.sp,
                    lineHeight = 52.sp,
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
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(
                R.string.goal_detail_meta,
                progress.completedStageCount,
                progress.stageCount,
                progress.completedTaskCount,
                progress.taskCount,
            ),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontFeatureSettings = "tnum",
            ),
            color = onSurface.copy(alpha = 0.58f),
        )
        Spacer(Modifier.height(12.dp))
        OrbitProgressBar(
            progress = progress.value,
            height = 5.dp,
            color = colors.primary,
            trackColor = onSurface.copy(alpha = 0.18f),
        )
    }
}

@Composable
private fun StageTimelineItem(
    stage: GoalStage,
    progress: Float,
    tasks: List<Task>,
    isCurrent: Boolean,
    isLast: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onAddTask: () -> Unit,
    onToggleTask: (Long) -> Unit,
    onEditTask: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    val completed = stage.isCompleted
    val percent = (progress.coerceIn(0f, 1f) * 100f).roundToInt()
    val completedTasks = tasks.count { it.isCompleted }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        StageRail(
            completed = completed,
            isCurrent = isCurrent,
            isLast = isLast,
            modifier = Modifier
                .width(22.dp)
                .fillMaxHeight(),
        )
        Spacer(Modifier.width(10.dp))
        OrbitCard(
            modifier = Modifier.weight(1f),
            padding = PaddingValues(
                start = 12.dp,
                end = 6.dp,
                top = 10.dp,
                bottom = 8.dp,
            ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StageCompletionToggle(
                    completed = completed,
                    stageTitle = stage.title,
                    onToggle = onToggle,
                )
                Spacer(Modifier.width(2.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isCurrent && !completed) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(colors.primary),
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = stage.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 16.sp,
                                lineHeight = 21.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = if (completed) colors.textMuted else colors.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(
                                R.string.goal_detail_stage_tasks,
                                tasks.size,
                                completedTasks,
                            ),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                fontFeatureSettings = "tnum",
                            ),
                            color = colors.textMuted,
                        )
                        stage.targetDate?.let { target ->
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = TaskDate.monthDayLabel(target),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = colors.textMuted,
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = stringResource(R.string.goals_progress, percent),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum",
                            ),
                            color = if (completed) colors.textMuted else colors.accentLine,
                        )
                    }
                }
                Spacer(Modifier.width(2.dp))
                FlatIconAction(
                    onClick = onEdit,
                    contentDescription = stringResource(
                        R.string.stage_edit_action,
                        stage.title,
                    ),
                    icon = Icons.Filled.Edit,
                )
            }

            Spacer(Modifier.height(2.dp))
            OrbitProgressBar(
                progress = progress,
                height = 4.dp,
                color = if (completed) colors.primary else colors.accentLine,
                trackColor = colors.track,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            if (tasks.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                tasks.forEachIndexed { index, task ->
                    CompactTaskRow(
                        task = task,
                        today = TaskDate.todayEpochDay(),
                        onToggle = { onToggleTask(task.id) },
                        onEdit = { onEditTask(task.id) },
                    )
                    if (index != tasks.lastIndex) {
                        GlassDivider(startPadding = 52.dp, endPadding = 8.dp)
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.goal_detail_stage_tasks_empty),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = colors.textMuted,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                OrbitTextLink(
                    text = stringResource(R.string.goal_detail_add_stage_task),
                    onClick = onAddTask,
                )
            }
        }
    }
}

@Composable
private fun StageRail(
    completed: Boolean,
    isCurrent: Boolean,
    isLast: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val nodeCenterY = 24.dp.toPx()
            if (!isLast) {
                drawLine(
                    color = colors.cardBorder,
                    start = Offset(center.x, nodeCenterY + 9.dp.toPx()),
                    end = Offset(center.x, size.height),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            when {
                completed -> {
                    drawRect(
                        color = colors.accentLine,
                        topLeft = Offset(center.x - 4.dp.toPx(), nodeCenterY - 4.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(8.dp.toPx(), 8.dp.toPx()),
                    )
                }

                isCurrent -> {
                    val outer = 5.5.dp.toPx()
                    drawRect(
                        color = colors.accentLine,
                        topLeft = Offset(center.x - outer, nodeCenterY - outer),
                        size = androidx.compose.ui.geometry.Size(outer * 2f, outer * 2f),
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                    drawRect(
                        color = colors.accentLine,
                        topLeft = Offset(center.x - 2.dp.toPx(), nodeCenterY - 2.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(4.dp.toPx(), 4.dp.toPx()),
                    )
                }

                else -> {
                    drawRect(
                        color = colors.track,
                        topLeft = Offset(center.x - 4.dp.toPx(), nodeCenterY - 4.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(8.dp.toPx(), 8.dp.toPx()),
                    )
                }
            }
        }
    }
}

@Composable
private fun StageCompletionToggle(
    completed: Boolean,
    stageTitle: String,
    onToggle: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    val label = stringResource(
        if (completed) R.string.stage_toggle_reopen else R.string.stage_toggle_complete,
        stageTitle,
    )
    val stateLabel = stringResource(
        if (completed) R.string.stage_state_completed else R.string.stage_state_pending,
    )
    val shape = RoundedCornerShape(9.dp)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .semantics {
                contentDescription = label
                role = Role.Checkbox
                stateDescription = stateLabel
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(shape)
                .background(if (completed) colors.primary else Color.Transparent)
                .border(
                    width = 1.5.dp,
                    color = if (completed) colors.primary else colors.textMuted.copy(alpha = 0.55f),
                    shape = shape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (completed) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = colors.onAccent,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun FlatIconAction(
    onClick: () -> Unit,
    contentDescription: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    val colors = MaterialTheme.orbitColors
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalActionsCard(
    status: GoalStatus,
    onChange: (GoalStatus) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    OrbitCard(
        modifier = modifier,
        padding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.goal_detail_status_actions),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 1.4.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = colors.textMuted,
        )
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (status) {
                GoalStatus.ACTIVE -> {
                    GoalActionButton(
                        text = stringResource(R.string.goal_detail_pause),
                        onClick = { onChange(GoalStatus.PAUSED) },
                    )
                    GoalActionButton(
                        text = stringResource(R.string.goal_detail_complete_action),
                        onClick = { onChange(GoalStatus.COMPLETED) },
                        emphasized = true,
                    )
                    GoalActionButton(
                        text = stringResource(R.string.goal_detail_archive),
                        onClick = { onChange(GoalStatus.ARCHIVED) },
                    )
                }

                GoalStatus.PAUSED -> {
                    GoalActionButton(
                        text = stringResource(R.string.goal_detail_resume),
                        onClick = { onChange(GoalStatus.ACTIVE) },
                        emphasized = true,
                    )
                    GoalActionButton(
                        text = stringResource(R.string.goal_detail_complete_action),
                        onClick = { onChange(GoalStatus.COMPLETED) },
                    )
                    GoalActionButton(
                        text = stringResource(R.string.goal_detail_archive),
                        onClick = { onChange(GoalStatus.ARCHIVED) },
                    )
                }

                GoalStatus.COMPLETED -> {
                    GoalActionButton(
                        text = stringResource(R.string.goal_detail_reactivate),
                        onClick = { onChange(GoalStatus.ACTIVE) },
                        emphasized = true,
                    )
                    GoalActionButton(
                        text = stringResource(R.string.goal_detail_archive),
                        onClick = { onChange(GoalStatus.ARCHIVED) },
                    )
                }

                GoalStatus.ARCHIVED -> {
                    GoalActionButton(
                        text = stringResource(R.string.goal_detail_restore),
                        onClick = { onChange(GoalStatus.ACTIVE) },
                        emphasized = true,
                    )
                }
            }
            GoalActionButton(
                text = stringResource(R.string.goal_detail_delete),
                onClick = onDelete,
                danger = true,
            )
        }
    }
}

@Composable
private fun GoalActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    danger: Boolean = false,
) {
    val colors = MaterialTheme.orbitColors
    val shape = RoundedCornerShape(OrbitCorners.control)
    val fill = when {
        emphasized -> colors.primary
        else -> Color.Transparent
    }
    val content = when {
        emphasized -> colors.onAccent
        danger -> colors.danger
        else -> colors.textPrimary
    }
    val borderColor = when {
        emphasized -> colors.primary
        danger -> colors.danger.copy(alpha = 0.38f)
        else -> colors.cardBorder
    }
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(fill)
            .border(1.dp, borderColor, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = content,
            maxLines = 1,
        )
    }
}

@Composable
private fun GoalDeleteDialog(
    deletion: GoalDeletionState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    val info = deletion.info
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.cardElevated,
        shape = RoundedCornerShape(OrbitCorners.card),
        tonalElevation = 0.dp,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        title = {
            Text(
                text = stringResource(R.string.goal_delete_confirm_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Text(
                text = stringResource(
                    R.string.goal_delete_confirm_message,
                    info.goalTitle,
                    info.stageCount,
                    info.taskCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !deletion.isDeleting,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colors.danger,
                    disabledContentColor = colors.textMuted,
                ),
            ) {
                Text(
                    stringResource(
                        if (deletion.isDeleting) {
                            R.string.goal_detail_deleting
                        } else {
                            R.string.delete_confirm
                        },
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !deletion.isDeleting,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.textSecondary),
            ) {
                Text(stringResource(R.string.delete_cancel))
            }
        },
    )
}
