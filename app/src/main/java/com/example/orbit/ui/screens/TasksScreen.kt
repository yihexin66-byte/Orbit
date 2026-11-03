package com.example.orbit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbit.R
import com.example.orbit.model.Task
import com.example.orbit.model.TaskCategory
import com.example.orbit.model.TaskDate
import com.example.orbit.model.TaskFilter
import com.example.orbit.model.TaskPriority
import com.example.orbit.ui.PlannerSegment
import com.example.orbit.ui.TaskLoadStatus
import com.example.orbit.ui.TaskUiState
import com.example.orbit.ui.components.CompactTaskRow
import com.example.orbit.ui.components.GlassDivider
import com.example.orbit.ui.components.OrbitAddTaskCard
import com.example.orbit.ui.components.OrbitCard
import com.example.orbit.ui.components.OrbitEmptyState
import com.example.orbit.ui.components.OrbitLazyScreen
import com.example.orbit.ui.components.OrbitLoadingCard
import com.example.orbit.ui.components.OrbitNote
import com.example.orbit.ui.components.OrbitSectionTitle
import com.example.orbit.ui.components.PlannerHeader
import com.example.orbit.ui.components.TaskFilterTabs
import com.example.orbit.ui.theme.OrbitCorners
import com.example.orbit.ui.theme.OrbitTheme
import com.example.orbit.ui.theme.orbitColors

@Composable
fun TasksScreen(
    uiState: TaskUiState,
    onFilterChange: (TaskFilter) -> Unit,
    onToggleTask: (Long) -> Unit,
    onEditTask: (Long) -> Unit,
    onAddTask: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    goalLabels: Map<Long, String> = emptyMap(),
    stageLabels: Map<Long, String> = emptyMap(),
    segment: PlannerSegment = PlannerSegment.TASKS,
    onSegmentChange: (PlannerSegment) -> Unit = {},
    goalCount: Int = 0,
    habitCount: Int = 0,
    cloudSignedIn: Boolean = false,
) {
    val visibleTasks = uiState.filteredTasks
    val pendingTasks = visibleTasks.filterNot { it.isCompleted }
    val completedTasks = visibleTasks.filter { it.isCompleted }
    val showPending = uiState.filter != TaskFilter.COMPLETED
    val showCompleted = uiState.filter != TaskFilter.PENDING
    val featuredTask = pendingTasks.firstOrNull()
    val compactPending = pendingTasks.drop(1)

    OrbitLazyScreen(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = {
            item(key = "header") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    PlannerHeader(
                        segment = segment,
                        subtitle = stringResource(
                            R.string.tasks_subtitle,
                            uiState.tasks.size,
                            uiState.pendingCount,
                        ),
                        taskCount = uiState.pendingCount,
                        goalCount = goalCount,
                        habitCount = habitCount,
                        onSegmentChange = onSegmentChange,
                        onAdd = onAddTask,
                        addContentDescription = stringResource(R.string.content_desc_add),
                        cloudSignedIn = cloudSignedIn,
                    )
                    Spacer(Modifier.height(16.dp))
                    TaskFilterTabs(
                        selected = uiState.filter,
                        countFor = uiState::countFor,
                        onSelect = onFilterChange,
                    )
                }
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

                visibleTasks.isEmpty() -> {
                    item(key = "empty") {
                        EmptyTasksState(
                            filter = uiState.filter,
                            onAddTask = onAddTask,
                        )
                    }
                }

                else -> {
                    if (showPending && pendingTasks.isNotEmpty()) {
                        item(key = "pending-header") {
                            TaskSectionHeader(
                                title = stringResource(R.string.tasks_section_pending),
                                count = pendingTasks.size,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                        featuredTask?.let { task ->
                            item(key = "featured-${task.id}") {
                                FeaturedTaskCard(
                                    task = task,
                                    today = uiState.todayEpochDay,
                                    goalTitle = task.goalId?.let(goalLabels::get),
                                    stageTitle = task.stageId?.let(stageLabels::get),
                                    onToggle = { onToggleTask(task.id) },
                                    onEdit = { onEditTask(task.id) },
                                )
                            }
                        }
                        if (compactPending.isNotEmpty()) {
                            item(key = "pending-list") {
                                OrbitCard(padding = PaddingValues(vertical = 6.dp)) {
                                    compactPending.forEachIndexed { index, task ->
                                        CompactTaskRow(
                                            task = task,
                                            onToggle = { onToggleTask(task.id) },
                                            onEdit = { onEditTask(task.id) },
                                            today = uiState.todayEpochDay,
                                            goalTitle = task.goalId?.let(goalLabels::get),
                                            stageTitle = task.stageId?.let(stageLabels::get),
                                        )
                                        if (index != compactPending.lastIndex) {
                                            GlassDivider(
                                                startPadding = 58.dp,
                                                endPadding = 10.dp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showCompleted && completedTasks.isNotEmpty()) {
                        item(key = "completed-header") {
                            TaskSectionHeader(
                                title = stringResource(R.string.tasks_section_completed),
                                count = completedTasks.size,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                        item(key = "completed-list") {
                            OrbitCard(padding = PaddingValues(vertical = 6.dp)) {
                                completedTasks.forEachIndexed { index, task ->
                                    CompactTaskRow(
                                        task = task,
                                        onToggle = { onToggleTask(task.id) },
                                        onEdit = { onEditTask(task.id) },
                                        today = uiState.todayEpochDay,
                                        goalTitle = task.goalId?.let(goalLabels::get),
                                        stageTitle = task.stageId?.let(stageLabels::get),
                                    )
                                    if (index != completedTasks.lastIndex) {
                                        GlassDivider(
                                            startPadding = 58.dp,
                                            endPadding = 10.dp,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item(key = "add-card") {
                        OrbitAddTaskCard(
                            title = stringResource(R.string.tasks_add_card_title),
                            description = stringResource(R.string.tasks_add_card_desc),
                            onClick = onAddTask,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }

            item(key = "storage-note") {
                OrbitNote(
                    text = stringResource(R.string.local_notice_tasks),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        },
    )
}

@Composable
private fun FeaturedTaskCard(
    task: Task,
    today: Long,
    goalTitle: String?,
    stageTitle: String?,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    // Same inversion rule as the other hero modules: near-black in light,
    // raised graphite in dark, so both themes read the same hierarchy.
    val surface = if (colors.isDark) colors.cardElevated else colors.textPrimary
    val onSurface = if (colors.isDark) colors.textPrimary else colors.backgroundTop
    val muted = onSurface.copy(alpha = 0.62f)
    val toggleDescription = stringResource(
        if (task.isCompleted) R.string.tasks_toggle_undo else R.string.tasks_toggle_done,
        task.title,
    )
    val meta = buildList {
        add(stringResource(R.string.tasks_priority_label, stringResource(task.priority.labelRes)))
        add(stringResource(task.category.labelRes))
        val due = task.dueDate
        if (due != null && due != today) {
            add(
                when (due) {
                    today + 1 -> stringResource(R.string.due_tomorrow)
                    today - 1 -> stringResource(R.string.due_yesterday)
                    else -> TaskDate.monthDayLabel(due)
                },
            )
        }
    }.joinToString("  ·  ")

    CompositionLocalProvider(LocalContentColor provides onSurface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(OrbitCorners.card))
                .background(surface)
                .clickable(role = Role.Button, onClick = onEdit)
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(colors.primary),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.tasks_next_label),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        letterSpacing = 1.6.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = muted,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.home_focus_edit_hint),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        letterSpacing = 0.6.sp,
                    ),
                    color = muted.copy(alpha = 0.72f),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = task.title,
                style = MaterialTheme.typography.headlineSmall,
                color = onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (!task.description.isNullOrBlank()) {
                Spacer(Modifier.height(7.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                    ),
                    color = muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 0.2.sp,
                ),
                color = muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (goalTitle != null || stageTitle != null) {
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    goalTitle?.let { title ->
                        Text(
                            text = stringResource(R.string.tasks_goal_label, title),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 0.2.sp,
                            ),
                            color = muted.copy(alpha = 0.82f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    stageTitle?.let { title ->
                        Text(
                            text = stringResource(R.string.tasks_stage_label, title),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 0.2.sp,
                            ),
                            color = muted.copy(alpha = 0.82f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(OrbitCorners.control))
                    .background(colors.primary)
                    .clickable(role = Role.Button, onClick = onToggle)
                    .semantics { contentDescription = toggleDescription }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = colors.onAccent,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = stringResource(R.string.home_focus_done),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = colors.onAccent,
                )
            }
        }
    }
}

@Composable
private fun EmptyTasksState(
    filter: TaskFilter,
    onAddTask: () -> Unit,
) {
    val title = when (filter) {
        TaskFilter.ALL -> stringResource(R.string.tasks_empty_title)
        TaskFilter.PENDING -> stringResource(R.string.tasks_empty_pending_title)
        TaskFilter.COMPLETED -> stringResource(R.string.tasks_empty_completed_title)
    }
    val description = when (filter) {
        TaskFilter.ALL -> stringResource(R.string.tasks_empty_desc)
        TaskFilter.PENDING -> stringResource(R.string.tasks_empty_pending_desc)
        TaskFilter.COMPLETED -> stringResource(R.string.tasks_empty_completed_desc)
    }
    OrbitEmptyState(
        modifier = Modifier.padding(top = 6.dp),
        title = title,
        description = description,
        actionLabel = stringResource(R.string.tasks_add_card_title),
        onAction = onAddTask,
    )
}

@Composable
private fun TaskSectionHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    OrbitSectionTitle(
        title = title,
        modifier = modifier.fillMaxWidth(),
        trailing = {
            Text(
                text = count.toString().padStart(2, '0'),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                    fontFeatureSettings = "tnum",
                ),
                color = colors.accentLine,
            )
        },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF101010, widthDp = 393, heightDp = 852)
@Composable
private fun TasksScreenPreview() {
    val today = TaskDate.todayEpochDay()
    OrbitTheme {
        TasksScreen(
            uiState = TaskUiState(
                tasks = listOf(
                    Task(
                        id = 1,
                        title = "完成作品集任务页数据层",
                        description = "Room + Repository + 状态流",
                        priority = TaskPriority.HIGH,
                        category = TaskCategory.STUDY,
                        dueDate = today,
                        createdAt = 1L,
                    ),
                    Task(
                        id = 2,
                        title = "整理本周投递记录",
                        priority = TaskPriority.MEDIUM,
                        category = TaskCategory.WORK,
                        dueDate = today + 1,
                        createdAt = 2L,
                    ),
                    Task(
                        id = 3,
                        title = "跑步 5 公里",
                        priority = TaskPriority.LOW,
                        category = TaskCategory.HEALTH,
                        dueDate = today,
                        isCompleted = true,
                        createdAt = 3L,
                        completedAt = TaskDate.datePickerMillis(today) + 12 * 60 * 60 * 1000L,
                    ),
                ),
                loadStatus = TaskLoadStatus.READY,
                todayEpochDay = today,
            ),
            onFilterChange = {},
            onToggleTask = {},
            onEditTask = {},
            onAddTask = {},
            onRetry = {},
        )
    }
}
