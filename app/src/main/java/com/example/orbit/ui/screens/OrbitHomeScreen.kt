package com.example.orbit.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbit.R
import com.example.orbit.learning.LearningBrowse
import com.example.orbit.learning.LearningLibrary
import com.example.orbit.learning.QuoteLibrary
import com.example.orbit.model.Habit
import com.example.orbit.model.HabitSummary
import com.example.orbit.model.Task
import com.example.orbit.model.TaskCategory
import com.example.orbit.model.TaskDate
import com.example.orbit.model.TaskPriority
import com.example.orbit.model.TaskSummary
import com.example.orbit.ui.HabitUiState
import com.example.orbit.ui.TaskLoadStatus
import com.example.orbit.ui.TaskUiState
import com.example.orbit.ui.components.AiAssistantFab
import com.example.orbit.ui.components.AiAssistantFabVisibility
import com.example.orbit.ui.components.CompactTaskRow
import com.example.orbit.ui.components.GlassDivider
import com.example.orbit.ui.components.HabitCheckButton
import com.example.orbit.ui.components.HabitWeekDots
import com.example.orbit.ui.components.OrbitBadge
import com.example.orbit.ui.components.OrbitCard
import com.example.orbit.ui.components.OrbitEmptyState
import com.example.orbit.ui.components.OrbitIconButton
import com.example.orbit.ui.components.OrbitLazyScreen
import com.example.orbit.ui.components.OrbitLoadingCard
import com.example.orbit.ui.components.OrbitMark
import com.example.orbit.ui.components.OrbitNote
import com.example.orbit.ui.components.OrbitSectionTitle
import com.example.orbit.ui.components.OrbitTaskDetail
import com.example.orbit.ui.components.OrbitTextLink
import com.example.orbit.ui.components.SpaceBackdrop
import com.example.orbit.ui.components.TaskOrbit
import com.example.orbit.ui.orbit.OrbitLayout
import com.example.orbit.ui.theme.OrbitCorners
import com.example.orbit.ui.theme.OrbitTheme
import com.example.orbit.ui.theme.accentPreviewColors
import com.example.orbit.ui.theme.orbitColors
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

internal const val HomeGreetingTag = "home_greeting"

/** Holds the detail content while its exit animation is still on screen. */
private class DetailTaskHolder(var task: Task? = null)

@Composable
fun OrbitHomeScreen(
    uiState: TaskUiState,
    onSelectTask: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onToggleTask: (Long) -> Unit,
    onEditTask: (Long) -> Unit,
    onAddTask: () -> Unit,
    onViewAllTasks: () -> Unit,
    onRetryLoad: () -> Unit,
    modifier: Modifier = Modifier,
    goalLabels: Map<Long, String> = emptyMap(),
    stageLabels: Map<Long, String> = emptyMap(),
    habitState: HabitUiState? = null,
    onToggleHabit: (Long) -> Unit = {},
    onEditHabit: (Long) -> Unit = {},
    onAddHabit: () -> Unit = {},
    onViewAllHabits: () -> Unit = {},
    cloudSignedIn: Boolean = false,
    displayName: String = "宇航员",
    dailyPoemId: String? = null,
    dailyWordId: String? = null,
    onOpenLearning: () -> Unit = {},
    onOpenAssistant: () -> Unit = {},
) {
    val today = uiState.todayEpochDay
    // Keyed on the snapshot day so an overnight refresh cannot leave a stale
    // date or greeting on screen.
    val dateText = remember(today) {
        "${TaskDate.monthDayLabel(today)} · ${TaskDate.weekdayLabel(today)}"
    }
    // One snapshot feeds the orbit, the metric strip and the section copy.
    val summary = uiState.todaySummary
    val selectedTask = uiState.selectedTask
    val todayTasks = uiState.todayTasks
    // Today's most important pending task always gets the large focus panel.
    // It is removed from the compact list below so each task exposes exactly
    // one toggle target, and the first screen answers "what should I do now?".
    val focusTask = remember(todayTasks) {
        todayTasks.firstOrNull { !it.isCompleted }
    }
    val listedTasks = remember(todayTasks, focusTask) {
        focusTask?.let { focus -> todayTasks.filterNot { it.id == focus.id } } ?: todayTasks
    }

    // Keeps the last task through the shrink animation after the detail closes.
    val detailHolder = remember { DetailTaskHolder() }
    if (selectedTask != null) detailHolder.task = selectedTask
    // The detail item only exists while it is opening, open or closing. That
    // avoids an empty LazyColumn slot leaving a double gap once it is hidden.
    val detailVisible = remember { MutableTransitionState(false) }
    LaunchedEffect(selectedTask != null) {
        detailVisible.targetState = selectedTask != null
    }

    // A node near the bottom of the orbit opens a detail card below the fold.
    // Once it has expanded, let the list reveal the card while keeping as much
    // of the orbit (and the selected node) on screen as possible.
    val detailBringIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(selectedTask?.id) {
        if (selectedTask != null) {
            delay(320)
            detailBringIntoView.bringIntoView()
        }
    }

    // The assistant disc is free to sit anywhere inside the content area. The
    // offset is display-only and remembered across configuration changes.
    var fabX by rememberSaveable { mutableFloatStateOf(-1f) }
    var fabY by rememberSaveable { mutableFloatStateOf(-1f) }
    var fabDragging by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val haptics = LocalHapticFeedback.current
        val fabSizePx = with(density) { 58.dp.toPx() }
        val fabMarginPx = with(density) { 18.dp.toPx() }
        val maxFabX = with(density) {
            (maxWidth.toPx() - fabSizePx - fabMarginPx).coerceAtLeast(0f)
        }
        val maxFabY = with(density) {
            (maxHeight.toPx() - fabSizePx - fabMarginPx).coerceAtLeast(0f)
        }
        val displayFabX = if (fabX < 0f) maxFabX else fabX.coerceIn(0f, maxFabX)
        val displayFabY = if (fabY < 0f) maxFabY else fabY.coerceIn(0f, maxFabY)
        val defaultTolerancePx = with(density) { 12.dp.toPx() }
        val edgeThresholdPx = with(density) { 36.dp.toPx() }
        val fabMoved = abs(displayFabX - maxFabX) > defaultTolerancePx ||
            abs(displayFabY - maxFabY) > defaultTolerancePx
        val distanceToEdgePx = minOf(
            displayFabX,
            displayFabY,
            (maxFabX - displayFabX).coerceAtLeast(0f),
            (maxFabY - displayFabY).coerceAtLeast(0f),
        )
        val fabIdleAlpha = AiAssistantFabVisibility.restingAlpha(
            moved = fabMoved,
            nearEdge = distanceToEdgePx <= edgeThresholdPx,
        )
        OrbitLazyScreen(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = 16.dp,
                // Keeps the last card clear of the docked assistant capsule.
                bottom = 108.dp,
            ),
            content = {
            item {
                HomeHeader(
                    dateText = dateText,
                    onAddTask = onAddTask,
                    cloudSignedIn = cloudSignedIn,
                    displayName = displayName,
                )
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TaskOrbit(
                        tasks = todayTasks,
                        selectedTaskId = uiState.selectedTaskId,
                        progress = summary.progress,
                        completed = summary.completed,
                        planned = summary.planned,
                        isLoading = uiState.isLoading,
                        loadFailed = uiState.loadFailed,
                        onSelectTask = onSelectTask,
                        onAddTask = onAddTask,
                        onCompleteTask = onToggleTask,
                    )
                    Spacer(Modifier.height(10.dp))
                    OrbitFootnote(
                        tasks = todayTasks,
                        isLoading = uiState.isLoading,
                    )
                }
            }
            if (detailVisible.currentState || detailVisible.targetState) {
                item {
                    AnimatedVisibility(
                        visibleState = detailVisible,
                        enter = fadeIn(animationSpec = tween(180)) +
                            expandVertically(
                                animationSpec = tween(280),
                                expandFrom = Alignment.Top,
                            ),
                        exit = fadeOut(animationSpec = tween(140)) +
                            shrinkVertically(
                                animationSpec = tween(240),
                                shrinkTowards = Alignment.Top,
                            ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewRequester(detailBringIntoView),
                        ) {
                            val detailTask = selectedTask ?: detailHolder.task
                            if (detailTask != null) {
                                OrbitTaskDetail(
                                    task = detailTask,
                                    today = today,
                                    onToggle = { onToggleTask(detailTask.id) },
                                    onEdit = { onEditTask(detailTask.id) },
                                    onClose = onClearSelection,
                                    goalTitle = detailTask.goalId?.let(goalLabels::get),
                                    stageTitle = detailTask.stageId?.let(stageLabels::get),
                                )
                            }
                            // Keeps the action row clear of the snackbar host when the
                            // list scrolls this card into view.
                            Spacer(Modifier.height(80.dp))
                        }
                    }
                }
            }
            if (focusTask != null) {
                item {
                    FocusTaskCard(
                        task = focusTask,
                        goalTitle = focusTask.goalId?.let(goalLabels::get),
                        onToggle = { onToggleTask(focusTask.id) },
                        onEdit = { onEditTask(focusTask.id) },
                    )
                }
            }
            item {
                TodayMetrics(
                    summary = summary,
                    isLoading = uiState.isLoading,
                    loadFailed = uiState.loadFailed,
                )
            }
            item {
                HomeLearningPreview(
                    todayEpochDay = today,
                    dailyPoemId = dailyPoemId,
                    dailyWordId = dailyWordId,
                    onOpenLearning = onOpenLearning,
                )
            }
            if (
                listedTasks.isNotEmpty() ||
                todayTasks.isEmpty() ||
                uiState.isLoading ||
                uiState.loadFailed
            ) {
                item {
                    TodayTasksSection(
                        tasks = listedTasks,
                        allTasksEmpty = todayTasks.isEmpty(),
                        today = today,
                        isLoading = uiState.isLoading,
                        loadFailed = uiState.loadFailed,
                        onToggleTask = onToggleTask,
                        onEditTask = onEditTask,
                        onAddTask = onAddTask,
                        onViewAllTasks = onViewAllTasks,
                        onRetryLoad = onRetryLoad,
                        goalLabels = goalLabels,
                        stageLabels = stageLabels,
                    )
                }
            }
            if (habitState != null) {
                item {
                    TodayHabitsSection(
                        state = habitState,
                        today = today,
                        onToggleHabit = onToggleHabit,
                        onEditHabit = onEditHabit,
                        onAddHabit = onAddHabit,
                        onViewAllHabits = onViewAllHabits,
                    )
                }
            }
            item {
                OrbitNote(text = stringResource(R.string.local_notice_home))
            }
            },
        )

        AiAssistantFab(
            onClick = onOpenAssistant,
            dragging = fabDragging,
            idleAlpha = fabIdleAlpha,
            modifier = Modifier
                .offset {
                    IntOffset(displayFabX.roundToInt(), displayFabY.roundToInt())
                }
                .pointerInput(maxFabX, maxFabY) {
                    detectDragGestures(
                        onDragStart = {
                            fabDragging = true
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragEnd = { fabDragging = false },
                        onDragCancel = { fabDragging = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val startX = if (fabX < 0f) maxFabX else fabX
                            val startY = if (fabY < 0f) maxFabY else fabY
                            fabX = (startX + dragAmount.x).coerceIn(0f, maxFabX)
                            fabY = (startY + dragAmount.y).coerceIn(0f, maxFabY)
                        },
                    )
                },
        )
    }

}

internal const val HomeLearningPreviewTag = "home_learning_preview"
internal const val HomeLearningPreviewOpenTag = "home_learning_preview_open"

/**
 * Three single-line rows instead of the old full cards: the home page only
 * answers "what is today's material", and any long text (poem lines, examples)
 * would turn the scroll into the thing the user asked to stop doing.
 */
@Composable
private fun HomeLearningPreview(
    todayEpochDay: Long,
    dailyPoemId: String?,
    dailyWordId: String?,
    onOpenLearning: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    val poem = remember(todayEpochDay, dailyPoemId) {
        LearningBrowse.resolvePoem(todayEpochDay, dailyPoemId)
    }
    val word = remember(todayEpochDay, dailyWordId) {
        LearningBrowse.resolveWord(todayEpochDay, dailyWordId)
    }
    val topic = remember(todayEpochDay) { LearningLibrary.mathFor(todayEpochDay) }

    Column(modifier = Modifier.fillMaxWidth()) {
        OrbitSectionTitle(
            title = stringResource(R.string.home_learning_section),
            trailing = {
                OrbitTextLink(
                    text = stringResource(R.string.home_learning_preview_open),
                    onClick = onOpenLearning,
                    modifier = Modifier.testTag(HomeLearningPreviewOpenTag),
                )
            },
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = stringResource(R.string.home_learning_preview_hint),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.sp,
            ),
            color = colors.textMuted,
        )
        Spacer(Modifier.height(10.dp))
        OrbitCard(
            modifier = Modifier.testTag(HomeLearningPreviewTag),
            onClick = onOpenLearning,
            padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        ) {
            LearningPreviewRow(
                label = stringResource(R.string.home_learning_poem_label),
                title = poem.title,
                meta = "${poem.author} · ${poem.dynasty}",
                serif = true,
            )
            GlassDivider()
            LearningPreviewRow(
                label = stringResource(R.string.home_learning_english_label),
                title = word.word,
                meta = "${word.phonetic} · ${word.meaning}",
            )
            GlassDivider()
            LearningPreviewRow(
                label = stringResource(R.string.home_learning_math_label),
                title = topic.title,
                meta = "${topic.grade.label} · ${topic.formula}",
            )
        }
    }
}

@Composable
private fun LearningPreviewRow(
    label: String,
    title: String,
    meta: String,
    serif: Boolean = false,
) {
    val colors = MaterialTheme.orbitColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.5.sp,
                letterSpacing = 1.1.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = colors.accentLine,
            modifier = Modifier.width(62.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontFamily = if (serif) FontFamily.Serif else null,
                ),
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    letterSpacing = 0.sp,
                ),
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(13.dp),
        )
    }
}

@Composable
private fun HomeHeader(
    dateText: String,
    onAddTask: () -> Unit,
    cloudSignedIn: Boolean,
    displayName: String,
) {
    val colors = MaterialTheme.orbitColors
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = stringResource(
        when (hour) {
            in 5..11 -> R.string.greeting_morning
            in 12..17 -> R.string.greeting_afternoon
            in 18..22 -> R.string.greeting_evening
            else -> R.string.greeting_late
        },
        displayName,
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OrbitMark(diameter = 34.dp)
            Spacer(Modifier.width(11.dp))
            Column {
                Text(
                    text = stringResource(R.string.home_wordmark),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.4.sp,
                    ),
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.home_tagline),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        letterSpacing = 1.7.sp,
                    ),
                    color = colors.textMuted,
                )
            }
            Spacer(Modifier.weight(1f))
            OrbitBadge(
                text = stringResource(
                    if (cloudSignedIn) R.string.cloud_synced else R.string.local_data,
                ),
            )
            Spacer(Modifier.width(8.dp))
            OrbitIconButton(
                onClick = onAddTask,
                contentDescription = stringResource(R.string.content_desc_add),
                filled = true,
            )
        }
        Spacer(Modifier.height(30.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .testTag(HomeGreetingTag),
            )
            Spacer(Modifier.width(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(2.dp)
                        .background(colors.accentLine),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 0.6.sp,
                    ),
                    color = colors.textMuted,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.home_headline),
            style = MaterialTheme.typography.displayMedium,
            color = colors.textPrimary,
        )
    }
}

@Composable
private fun OrbitFootnote(
    tasks: List<Task>,
    isLoading: Boolean,
) {
    val colors = MaterialTheme.orbitColors
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendDot(filled = false)
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.status_pending),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                color = colors.textMuted,
            )
            Spacer(Modifier.width(14.dp))
            LegendDot(filled = true)
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.status_completed),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                color = colors.textMuted,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(
                if (tasks.isEmpty()) {
                    R.string.home_orbit_hint_empty
                } else {
                    R.string.home_orbit_hint
                },
            ),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.sp,
            ),
            color = colors.textMuted,
            maxLines = 2,
        )
        val hiddenCount = (tasks.size - OrbitLayout.MAX_NODES).coerceAtLeast(0)
        if (hiddenCount > 0 && !isLoading) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.home_orbit_overflow,
                    OrbitLayout.MAX_NODES,
                    hiddenCount,
                ),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    lineHeight = 14.sp,
                ),
                color = colors.warning,
            )
        }
    }
}

@Composable
private fun LegendDot(filled: Boolean) {
    val colors = MaterialTheme.orbitColors
    Canvas(modifier = Modifier.size(9.dp)) {
        if (filled) {
            drawCircle(color = colors.textPrimary, radius = size.minDimension / 2f)
        } else {
            drawCircle(
                color = colors.textMuted.copy(alpha = 0.8f),
                radius = size.minDimension / 2f - 0.7.dp.toPx(),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
    }
}

@Composable
private fun TodayMetrics(
    summary: TaskSummary,
    isLoading: Boolean,
    loadFailed: Boolean,
) {
    val colors = MaterialTheme.orbitColors
    val placeholder = isLoading || loadFailed
    val summaryText = when {
        isLoading -> stringResource(R.string.task_loading)
        loadFailed -> stringResource(R.string.task_load_failed_title)
        summary.planned == 0 -> stringResource(R.string.home_summary_empty)
        summary.pending == 0 && summary.completed == summary.planned ->
            stringResource(R.string.home_summary_all_done)
        summary.pending == 0 -> stringResource(R.string.home_summary_no_pending)
        else -> stringResource(R.string.home_summary_remaining, summary.pending)
    }

    OrbitCard(padding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.home_summary_title),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 1.7.sp,
                ),
                color = colors.textMuted,
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(colors.accentLine),
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetricCell(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.home_metric_planned),
                value = if (placeholder) "—" else summary.planned.toString(),
            )
            MetricDivider()
            MetricCell(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.home_metric_done),
                value = if (placeholder) "—" else summary.completed.toString(),
                accent = true,
            )
            MetricDivider()
            MetricCell(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.home_metric_pending),
                value = if (placeholder) "—" else summary.pending.toString(),
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .padding(top = 7.dp)
                    .width(14.dp)
                    .height(2.dp)
                    .background(if (placeholder) colors.textMuted else colors.accentLine),
            )
            Spacer(Modifier.width(9.dp))
            Column {
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp,
                    ),
                    color = colors.textSecondary,
                )
                val completedBeforeToday = summary.completedBeforeToday
                if (completedBeforeToday > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            R.string.today_completed_earlier,
                            completedBeforeToday,
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.5.sp,
                            letterSpacing = 0.sp,
                        ),
                        color = colors.textMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    val colors = MaterialTheme.orbitColors
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 1.2.sp,
            ),
            color = colors.textMuted,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 34.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFeatureSettings = "tnum",
            ),
            color = if (accent && value != "—") colors.accentLine else colors.textPrimary,
        )
    }
}

@Composable
private fun MetricDivider() {
    val colors = MaterialTheme.orbitColors
    Box(
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .width(1.dp)
            .height(38.dp)
            .background(colors.cardBorder),
    )
}

@Composable
private fun FocusTaskCard(
    task: Task,
    goalTitle: String?,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    // One inverted hero per page: ink on paper, graphite on ink. It carries the
    // day's priority, while the orbit above carries the day's progress.
    val surface = if (colors.isDark) colors.cardElevated else colors.textPrimary
    val onSurface = if (colors.isDark) colors.textPrimary else colors.backgroundTop
    val muted = onSurface.copy(alpha = 0.58f)
    val toggleDescription = stringResource(
        if (task.isCompleted) R.string.tasks_toggle_undo else R.string.tasks_toggle_done,
        task.title,
    )
    val meta = stringResource(task.category.labelRes)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.992f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "focusPress",
    )

    CompositionLocalProvider(LocalContentColor provides onSurface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .clip(RoundedCornerShape(OrbitCorners.card))
                .background(surface)
                .clickable(
                    interactionSource = interaction,
                    indication = LocalIndication.current,
                    role = Role.Button,
                    onClick = onEdit,
                )
                .padding(horizontal = 22.dp, vertical = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(colors.primary),
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = stringResource(R.string.home_focus_label),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        letterSpacing = 1.7.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = muted,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(
                        R.string.tasks_priority_label,
                        stringResource(task.priority.labelRes),
                    ),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        letterSpacing = 0.7.sp,
                    ),
                    color = if (task.priority == TaskPriority.HIGH) {
                        colors.primary
                    } else {
                        muted
                    },
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = task.title,
                style = MaterialTheme.typography.headlineMedium,
                color = onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 0.4.sp,
                ),
                color = muted,
            )
            if (goalTitle != null) {
                Spacer(Modifier.height(5.dp))
                Text(
                    text = stringResource(R.string.tasks_goal_label, goalTitle),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        letterSpacing = 0.sp,
                    ),
                    color = muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(OrbitCorners.control))
                        .background(colors.primary)
                        .clickable(role = Role.Button, onClick = onToggle)
                        .semantics { contentDescription = toggleDescription }
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = colors.onAccent,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.home_focus_done),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = colors.onAccent,
                    )
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.home_focus_edit_hint),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.5.sp,
                            letterSpacing = 0.6.sp,
                        ),
                        color = muted,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = muted,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayTasksSection(
    tasks: List<Task>,
    allTasksEmpty: Boolean,
    today: Long,
    isLoading: Boolean,
    loadFailed: Boolean,
    onToggleTask: (Long) -> Unit,
    onEditTask: (Long) -> Unit,
    onAddTask: () -> Unit,
    onViewAllTasks: () -> Unit,
    onRetryLoad: () -> Unit,
    goalLabels: Map<Long, String>,
    stageLabels: Map<Long, String>,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OrbitSectionTitle(
            title = stringResource(R.string.home_today_tasks),
            trailing = {
                OrbitTextLink(
                    text = stringResource(R.string.home_view_all),
                    onClick = onViewAllTasks,
                )
            },
        )
        Spacer(Modifier.height(6.dp))
        when {
            isLoading -> OrbitLoadingCard()
            loadFailed -> OrbitEmptyState(
                title = stringResource(R.string.task_load_failed_title),
                description = stringResource(R.string.task_load_failed_desc),
                actionLabel = stringResource(R.string.task_retry),
                onAction = onRetryLoad,
            )
            allTasksEmpty -> OrbitEmptyState(
                title = stringResource(R.string.home_empty_today_title),
                description = stringResource(R.string.home_empty_today_desc),
                actionLabel = stringResource(R.string.home_add_first),
                onAction = onAddTask,
            )
            tasks.isNotEmpty() -> OrbitCard(padding = PaddingValues(vertical = 6.dp)) {
                tasks.forEachIndexed { index, task ->
                    CompactTaskRow(
                        task = task,
                        today = today,
                        onToggle = { onToggleTask(task.id) },
                        onEdit = { onEditTask(task.id) },
                        goalTitle = task.goalId?.let(goalLabels::get),
                        stageTitle = task.stageId?.let(stageLabels::get),
                    )
                    if (index != tasks.lastIndex) {
                        GlassDivider(startPadding = 58.dp, endPadding = 10.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayHabitsSection(
    state: HabitUiState,
    today: Long,
    onToggleHabit: (Long) -> Unit,
    onEditHabit: (Long) -> Unit,
    onAddHabit: () -> Unit,
    onViewAllHabits: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    Column(modifier = Modifier.fillMaxWidth()) {
        if (!state.isLoading && !state.loadFailed) {
            val active = state.activeHabits
            val displayed = active.take(MAX_HOME_HABITS)
            OrbitSectionTitle(
                title = stringResource(R.string.home_today_habits),
                trailing = {
                    if (active.isNotEmpty()) {
                        Text(
                            text = stringResource(
                                R.string.home_habits_ratio,
                                state.checkedTodayCount,
                                active.size,
                            ),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum",
                            ),
                            color = colors.accentLine,
                            modifier = Modifier.padding(end = 2.dp),
                        )
                    }
                    OrbitTextLink(
                        text = stringResource(R.string.home_habits_manage),
                        onClick = onViewAllHabits,
                    )
                },
            )
            Spacer(Modifier.height(6.dp))
            if (active.isEmpty()) {
                OrbitCard(padding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.home_habits_empty_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = stringResource(R.string.home_habits_empty_desc),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.5.sp,
                                    lineHeight = 15.sp,
                                ),
                                color = colors.textMuted,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        OrbitTextLink(
                            text = stringResource(R.string.home_habits_add),
                            onClick = onAddHabit,
                        )
                    }
                }
            } else {
                OrbitCard(padding = PaddingValues(vertical = 6.dp)) {
                    displayed.forEachIndexed { index, habit ->
                        CompactHabitRow(
                            habit = habit,
                            summary = state.summaryFor(habit),
                            onToggle = { onToggleHabit(habit.id) },
                            onEdit = { onEditHabit(habit.id) },
                        )
                        if (index != displayed.lastIndex) {
                            GlassDivider(startPadding = 58.dp, endPadding = 10.dp)
                        }
                    }
                    val overflow = active.size - displayed.size
                    if (overflow > 0) {
                        Text(
                            text = stringResource(R.string.home_habits_overflow, overflow),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = colors.textMuted,
                            modifier = Modifier.padding(start = 58.dp, top = 4.dp, bottom = 8.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        DailyQuoteCard(today = today)
    }
}

@Composable
private fun DailyQuoteCard(today: Long) {
    val colors = MaterialTheme.orbitColors
    var quoteOffset by rememberSaveable(today) { mutableIntStateOf(0) }
    val quote = remember(today, quoteOffset) { QuoteLibrary.forDay(today + quoteOffset) }
    val refreshLabel = stringResource(R.string.home_quote_refresh)

    OrbitCard(
        padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        elevated = true,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_quote_label),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.5.sp,
                        letterSpacing = 1.4.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = colors.accentLine,
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    text = quote.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        lineHeight = 21.sp,
                    ),
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.home_quote_source, quote.source),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        letterSpacing = 0.sp,
                    ),
                    color = colors.textMuted,
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.track)
                    .clickable(role = Role.Button) {
                        quoteOffset = (quoteOffset + 1) % QuoteLibrary.quotes.size
                    }
                    .semantics { contentDescription = refreshLabel },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun CompactHabitRow(
    habit: Habit,
    summary: HabitSummary,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    val tone = accentPreviewColors(habit.tone, colors.isDark).first
    val toggleDescription = stringResource(
        if (summary.checkedToday) R.string.habits_uncheck else R.string.habits_check,
        habit.title,
    )
    val weekDescription = stringResource(
        R.string.habits_week_desc,
        habit.title,
        summary.lastSevenDays.count { it.checked },
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onEdit)
            .padding(start = 2.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HabitCheckButton(
            checked = summary.checkedToday,
            actionDescription = toggleDescription,
            onClick = onToggle,
            accent = tone,
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = habit.title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = if (summary.streak > 0) {
                    stringResource(R.string.home_habit_streak, summary.streak)
                } else {
                    stringResource(R.string.habits_pending_today)
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 0.sp,
                ),
                color = if (summary.streak > 0) tone else colors.textMuted,
            )
        }
        Spacer(Modifier.width(6.dp))
        HabitWeekDots(
            days = summary.lastSevenDays,
            accent = tone,
            description = weekDescription,
            modifier = Modifier.width(66.dp),
            dotSize = 6.dp,
            showLabels = false,
        )
    }
}

private const val MAX_HOME_HABITS = 3

@Preview(showBackground = true, backgroundColor = 0xFF101010, widthDp = 393, heightDp = 852)
@Composable
private fun OrbitHomeScreenPreview() {
    val today = TaskDate.todayEpochDay()
    OrbitTheme {
        SpaceBackdrop {
            OrbitHomeScreen(
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
                            dueDate = today,
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
                    selectedTaskId = 1L,
                    loadStatus = TaskLoadStatus.READY,
                    todayEpochDay = today,
                ),
                onSelectTask = {},
                onClearSelection = {},
                onToggleTask = {},
                onEditTask = {},
                onAddTask = {},
                onViewAllTasks = {},
                onRetryLoad = {},
            )
        }
    }
}
