package com.example.orbit.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbit.R
import com.example.orbit.model.Task
import com.example.orbit.model.TaskCategory
import com.example.orbit.model.TaskDate
import com.example.orbit.model.TaskFilter
import com.example.orbit.model.TaskPriority
import com.example.orbit.ui.theme.OrbitCorners
import com.example.orbit.ui.theme.orbitColors

@Composable
fun priorityColor(priority: TaskPriority): Color {
    val colors = MaterialTheme.orbitColors
    return when (priority) {
        TaskPriority.HIGH -> colors.primary
        TaskPriority.MEDIUM -> colors.secondary
        TaskPriority.LOW -> colors.textMuted
    }
}

@Composable
fun categoryColor(category: TaskCategory): Color {
    val colors = MaterialTheme.orbitColors
    return when (category) {
        TaskCategory.STUDY -> colors.primary
        TaskCategory.WORK -> colors.secondary
        TaskCategory.LIFE -> colors.success
        TaskCategory.HEALTH -> colors.warning
        TaskCategory.OTHER -> colors.textSecondary
    }
}

@Composable
fun PriorityPill(
    priority: TaskPriority,
    modifier: Modifier = Modifier,
) {
    TagPill(
        text = stringResource(priority.labelRes),
        color = priorityColor(priority),
        modifier = modifier,
    )
}

@Composable
fun CategoryPill(
    category: TaskCategory,
    modifier: Modifier = Modifier,
) {
    TagPill(
        text = stringResource(category.labelRes),
        color = categoryColor(category),
        modifier = modifier,
    )
}

@Composable
fun DuePill(
    dueDate: Long?,
    completed: Boolean,
    modifier: Modifier = Modifier,
    today: Long = TaskDate.todayEpochDay(),
) {
    val colors = MaterialTheme.orbitColors
    val label = when (dueDate) {
        null -> stringResource(R.string.due_none)
        today -> stringResource(R.string.due_today)
        today + 1 -> stringResource(R.string.due_tomorrow)
        today - 1 -> stringResource(R.string.due_yesterday)
        else -> TaskDate.monthDayLabel(dueDate)
    }
    val color = when {
        dueDate == null -> colors.textMuted
        completed -> colors.textSecondary
        dueDate < today -> colors.warning
        dueDate == today -> colors.primary
        else -> colors.textSecondary
    }
    TagPill(text = label, color = color, modifier = modifier)
}

@Composable
fun TaskCompletionToggle(
    completed: Boolean,
    taskTitle: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    val description = stringResource(
        if (completed) R.string.tasks_toggle_undo else R.string.tasks_toggle_done,
        taskTitle,
    )
    val fill by animateColorAsState(
        targetValue = if (completed) colors.textPrimary else colors.card,
        animationSpec = tween(220),
        label = "toggleFill",
    )
    val outline by animateColorAsState(
        targetValue = if (completed) colors.textPrimary else colors.textMuted.copy(alpha = 0.55f),
        animationSpec = tween(220),
        label = "toggleOutline",
    )
    val checkAlpha by animateFloatAsState(
        targetValue = if (completed) 1f else 0f,
        animationSpec = tween(180),
        label = "checkAlpha",
    )
    val checkScale by animateFloatAsState(
        targetValue = if (completed) 1f else 0.55f,
        animationSpec = tween(220),
        label = "checkScale",
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .toggleable(
                value = completed,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(25.dp)) {
            drawCircle(color = fill, radius = size.minDimension / 2f)
            drawCircle(
                color = outline,
                radius = size.minDimension / 2f,
                style = Stroke(width = 1.3.dp.toPx()),
            )
        }
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = colors.backgroundTop,
            modifier = Modifier
                .size(15.dp)
                .alpha(checkAlpha)
                .scale(checkScale),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskMetaFlow(
    task: Task,
    modifier: Modifier = Modifier,
    today: Long = TaskDate.todayEpochDay(),
    goalTitle: String? = null,
    stageTitle: String? = null,
) {
    val colors = MaterialTheme.orbitColors
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PriorityPill(task.priority)
        CategoryPill(task.category)
        goalTitle?.let { title ->
            TagPill(
                text = stringResource(R.string.tasks_goal_label, title),
                color = colors.primary,
            )
        }
        stageTitle?.let { title ->
            TagPill(
                text = stringResource(R.string.tasks_stage_label, title),
                color = colors.secondary,
            )
        }
        DuePill(dueDate = task.dueDate, completed = task.isCompleted, today = today)
        TagPill(
            text = stringResource(
                if (task.isCompleted) R.string.status_completed else R.string.status_pending,
            ),
            color = if (task.isCompleted) colors.success else colors.textSecondary,
        )
    }
}

@Composable
fun CompactTaskRow(
    task: Task,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    today: Long = TaskDate.todayEpochDay(),
    goalTitle: String? = null,
    stageTitle: String? = null,
) {
    val colors = MaterialTheme.orbitColors
    val titleColor by animateColorAsState(
        targetValue = if (task.isCompleted) colors.textMuted else colors.textPrimary,
        animationSpec = tween(220),
        label = "compactTitleColor",
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onEdit)
            .padding(start = 2.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TaskCompletionToggle(
            completed = task.isCompleted,
            taskTitle = task.title,
            onToggle = onToggle,
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = titleColor,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    letterSpacing = 0.2.sp,
                ),
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (goalTitle != null || stageTitle != null) {
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    goalTitle?.let { title ->
                        Text(
                            text = stringResource(R.string.tasks_goal_label, title),
                            modifier = Modifier.weight(1f, fill = false),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                letterSpacing = 0.2.sp,
                            ),
                            color = colors.textMuted.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    stageTitle?.let { title ->
                        Text(
                            text = stringResource(R.string.tasks_stage_label, title),
                            modifier = Modifier.weight(1f, fill = false),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                letterSpacing = 0.2.sp,
                            ),
                            color = colors.textMuted.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskFilterTabs(
    selected: TaskFilter,
    countFor: (TaskFilter) -> Int,
    onSelect: (TaskFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TaskFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            val labelColor by animateColorAsState(
                targetValue = if (isSelected) colors.textPrimary else colors.textMuted,
                animationSpec = tween(180),
                label = "filterContent",
            )
            val indicatorAlpha by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = tween(180),
                label = "filterIndicator",
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(OrbitCorners.control))
                    .selectable(
                        selected = isSelected,
                        role = Role.Tab,
                        onClick = { onSelect(filter) },
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(filter.labelRes),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                        color = labelColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = countFor(filter).toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 0.sp,
                            fontFeatureSettings = "tnum",
                        ),
                        color = if (isSelected) colors.accentLine else colors.textMuted.copy(
                            alpha = 0.72f,
                        ),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(2.dp)
                        .alpha(indicatorAlpha)
                        .background(colors.accentLine),
                )
            }
        }
    }
}

@Composable
fun OrbitLoadingCard(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.orbitColors
    OrbitCard(
        modifier = modifier,
        padding = PaddingValues(horizontal = 18.dp, vertical = 22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = colors.primary,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.task_loading),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
fun OrbitAddTaskCard(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    OrbitCard(
        modifier = modifier,
        onClick = onClick,
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
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = colors.onAccent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = colors.textMuted,
                )
            }
            Spacer(Modifier.width(8.dp))
            Canvas(modifier = Modifier.size(14.dp)) {
                val stroke = 1.5.dp.toPx()
                drawLine(
                    color = colors.accentLine,
                    start = Offset(size.width * 0.10f, size.height * 0.50f),
                    end = Offset(size.width * 0.86f, size.height * 0.50f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = colors.accentLine,
                    start = Offset(size.width * 0.56f, size.height * 0.18f),
                    end = Offset(size.width * 0.86f, size.height * 0.50f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = colors.accentLine,
                    start = Offset(size.width * 0.56f, size.height * 0.82f),
                    end = Offset(size.width * 0.86f, size.height * 0.50f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
