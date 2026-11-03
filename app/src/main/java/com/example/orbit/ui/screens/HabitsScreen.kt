package com.example.orbit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbit.R
import com.example.orbit.model.AccentTone
import com.example.orbit.model.Habit
import com.example.orbit.model.HabitRecord
import com.example.orbit.model.HabitSummary
import com.example.orbit.model.TaskDate
import com.example.orbit.ui.HabitLoadStatus
import com.example.orbit.ui.HabitUiState
import com.example.orbit.ui.PlannerSegment
import com.example.orbit.ui.components.HabitCheckButton
import com.example.orbit.ui.components.HabitHeatmap
import com.example.orbit.ui.components.HabitWeekDots
import com.example.orbit.ui.components.OrbitAddTaskCard
import com.example.orbit.ui.components.OrbitCard
import com.example.orbit.ui.components.OrbitEmptyState
import com.example.orbit.ui.components.OrbitLazyScreen
import com.example.orbit.ui.components.OrbitLoadingCard
import com.example.orbit.ui.components.OrbitNote
import com.example.orbit.ui.components.OrbitProgressBar
import com.example.orbit.ui.components.OrbitSectionTitle
import com.example.orbit.ui.components.OrbitTextLink
import com.example.orbit.ui.components.PlannerHeader
import com.example.orbit.ui.components.SpaceBackdrop
import com.example.orbit.ui.theme.OrbitCorners
import com.example.orbit.ui.theme.OrbitTheme
import com.example.orbit.ui.theme.accentPreviewColors
import com.example.orbit.ui.theme.orbitColors
import kotlin.math.roundToInt

/**
 * Habit half of the planner. The day's progress is promoted into one inverted
 * editorial block; every habit then keeps its own history card so checking in
 * and reading the streak happen on the same surface.
 */
@Composable
fun HabitsScreen(
    uiState: HabitUiState,
    segment: PlannerSegment,
    onSegmentChange: (PlannerSegment) -> Unit,
    onAddHabit: () -> Unit,
    onEditHabit: (Long) -> Unit,
    onToggleHabit: (Long) -> Unit,
    onRestoreHabit: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    goalCount: Int = 0,
    taskCount: Int = 0,
    cloudSignedIn: Boolean = false,
) {
    val activeHabits = uiState.activeHabits
    val archivedHabits = uiState.archivedHabits

    OrbitLazyScreen(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = {
            item(key = "header") {
                PlannerHeader(
                    segment = segment,
                    subtitle = if (uiState.habits.isEmpty()) {
                        stringResource(R.string.planner_habits_subtitle_empty)
                    } else {
                        stringResource(
                            R.string.planner_habits_subtitle,
                            uiState.habits.size,
                            uiState.checkedTodayCount,
                        )
                    },
                    goalCount = goalCount,
                    taskCount = taskCount,
                    habitCount = activeHabits.size,
                    onSegmentChange = onSegmentChange,
                    onAdd = onAddHabit,
                    addContentDescription = stringResource(R.string.habits_add),
                    cloudSignedIn = cloudSignedIn,
                )
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
                            title = stringResource(R.string.habit_load_failed_title),
                            description = stringResource(R.string.habit_load_failed_desc),
                            actionLabel = stringResource(R.string.task_retry),
                            onAction = onRetry,
                        )
                    }
                }

                uiState.habits.isEmpty() -> {
                    item(key = "empty") {
                        OrbitEmptyState(
                            modifier = Modifier.padding(top = 6.dp),
                            title = stringResource(R.string.habits_empty_title),
                            description = stringResource(R.string.habits_empty_desc),
                            actionLabel = stringResource(R.string.habits_add_card_title),
                            onAction = onAddHabit,
                        )
                    }
                }

                else -> {
                    if (activeHabits.isNotEmpty()) {
                        item(key = "summary") {
                            HabitTodaySummary(
                                uiState = uiState,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }

                    item(key = "today-header") {
                        HabitSectionHeader(
                            title = stringResource(R.string.habits_section_today),
                            trailing = if (activeHabits.isEmpty()) {
                                null
                            } else {
                                stringResource(
                                    R.string.habits_today_ratio,
                                    uiState.checkedTodayCount,
                                    activeHabits.size,
                                )
                            },
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    if (activeHabits.isEmpty()) {
                        item(key = "no-active") {
                            OrbitNote(text = stringResource(R.string.habits_no_active))
                        }
                    } else {
                        items(
                            count = activeHabits.size,
                            key = { index -> "habit-${activeHabits[index].id}" },
                        ) { index ->
                            val habit = activeHabits[index]
                            HabitCard(
                                summary = uiState.summaryFor(habit),
                                onToggle = { onToggleHabit(habit.id) },
                                onEdit = { onEditHabit(habit.id) },
                            )
                        }
                    }

                    if (archivedHabits.isNotEmpty()) {
                        item(key = "archived-header") {
                            HabitSectionHeader(
                                title = stringResource(R.string.habits_section_archived),
                                trailing = archivedHabits.size.toString().padStart(2, '0'),
                                modifier = Modifier.padding(top = 10.dp),
                            )
                        }
                        items(
                            count = archivedHabits.size,
                            key = { index -> "archived-${archivedHabits[index].id}" },
                        ) { index ->
                            val habit = archivedHabits[index]
                            ArchivedHabitRow(
                                habit = habit,
                                checkIns = uiState.summaryFor(habit).checkIns,
                                onEdit = { onEditHabit(habit.id) },
                                onRestore = { onRestoreHabit(habit.id) },
                            )
                        }
                    }

                    item(key = "add-card") {
                        OrbitAddTaskCard(
                            title = stringResource(R.string.habits_add_card_title),
                            description = stringResource(R.string.habits_add_card_desc),
                            onClick = onAddHabit,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }

            item(key = "note") {
                OrbitNote(
                    text = stringResource(R.string.habits_note),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        },
    )
}

/**
 * Inverted progress block. It stays dark in both themes so the yellow bar and
 * the oversized ratio read as the page's key metric rather than one more card.
 */
@Composable
private fun HabitTodaySummary(
    uiState: HabitUiState,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    val surface = if (colors.isDark) colors.cardElevated else colors.textPrimary
    val onSurface = if (colors.isDark) colors.textPrimary else colors.backgroundTop
    val muted = onSurface.copy(alpha = 0.60f)
    val percent = (uiState.todayProgress * 100).roundToInt()
    val caption = when {
        uiState.todayPendingCount == 0 -> stringResource(R.string.habits_summary_done)
        else -> stringResource(R.string.habits_summary_remaining, uiState.todayPendingCount)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OrbitCorners.card))
            .background(surface)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(colors.primary),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.habits_summary_title),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            letterSpacing = 1.6.sp,
                        ),
                        color = muted,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp,
                    ),
                    color = onSurface.copy(alpha = 0.82f),
                    maxLines = 2,
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = uiState.checkedTodayCount.toString(),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 44.sp,
                            lineHeight = 46.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFeatureSettings = "tnum",
                        ),
                        color = onSurface,
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "/ ${uiState.activeCount}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.sp,
                            fontFeatureSettings = "tnum",
                        ),
                        color = muted,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.habits_summary_percent, percent),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        letterSpacing = 0.6.sp,
                        fontFeatureSettings = "tnum",
                    ),
                    color = colors.primary,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        OrbitProgressBar(
            progress = uiState.todayProgress,
            height = 4.dp,
            color = colors.primary,
            trackColor = onSurface.copy(alpha = 0.18f),
        )
    }
}

@Composable
private fun HabitCard(
    summary: HabitSummary,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    // Tone is data here: it identifies the habit and fills its history cells.
    val tone = accentPreviewColors(summary.habit.tone, colors.isDark).first
    val percent = (summary.consistency * 100).roundToInt()
    val checkedDays = summary.lastSevenDays.count { it.checked }
    val weekDescription = stringResource(
        R.string.habits_week_desc,
        summary.habit.title,
        checkedDays,
    )
    val heatmapDescription = stringResource(
        R.string.habits_heatmap_desc,
        summary.habit.title,
        summary.checkIns,
        percent,
    )
    val toggleDescription = stringResource(
        if (summary.checkedToday) R.string.habits_uncheck else R.string.habits_check,
        summary.habit.title,
    )
    val streakLabel = if (summary.streak == 0) {
        stringResource(R.string.habits_streak_zero)
    } else {
        stringResource(R.string.habits_streak, summary.streak)
    }

    OrbitCard(
        modifier = modifier,
        onClick = onEdit,
        padding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            HabitCheckButton(
                checked = summary.checkedToday,
                actionDescription = toggleDescription,
                onClick = onToggle,
                accent = colors.primary,
                modifier = Modifier.padding(top = 1.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.habit.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                summary.habit.note?.takeIf { it.isNotBlank() }?.let { note ->
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                        ),
                        color = colors.textMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(9.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(
                                if (summary.checkedToday) colors.primary else colors.cardBorder,
                            ),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(
                            if (summary.checkedToday) {
                                R.string.habits_checked_today
                            } else {
                                R.string.habits_pending_today
                            },
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.5.sp,
                            letterSpacing = 0.4.sp,
                        ),
                        color = if (summary.checkedToday) {
                            colors.accentLine
                        } else {
                            colors.textMuted
                        },
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = streakLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.5.sp,
                            letterSpacing = 0.2.sp,
                            fontFeatureSettings = "tnum",
                        ),
                        color = colors.textSecondary,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        HabitWeekDots(
            days = summary.lastSevenDays,
            accent = tone,
            description = weekDescription,
            modifier = Modifier.fillMaxWidth(),
            dotSize = 10.dp,
        )

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.habits_heatmap_label),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 0.8.sp,
                ),
                color = colors.textMuted,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.habits_consistency, percent),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 0.sp,
                    fontFeatureSettings = "tnum",
                ),
                color = tone,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.habits_checkins, summary.checkIns),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 0.sp,
                    fontFeatureSettings = "tnum",
                ),
                color = colors.textMuted,
            )
        }
        Spacer(Modifier.height(8.dp))
        HabitHeatmap(
            weeks = summary.heatmapWeeks,
            accent = tone,
            description = heatmapDescription,
        )
    }
}

@Composable
private fun ArchivedHabitRow(
    habit: Habit,
    checkIns: Int,
    onEdit: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    val tone = accentPreviewColors(habit.tone, colors.isDark).first

    OrbitCard(
        modifier = modifier,
        onClick = onEdit,
        padding = PaddingValues(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(tone.copy(alpha = 0.8f)),
            )
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(
                        R.string.habits_archived_meta,
                        stringResource(R.string.habit_status_archived),
                        checkIns,
                    ),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        letterSpacing = 0.3.sp,
                    ),
                    color = colors.textMuted,
                )
            }
            Spacer(Modifier.width(8.dp))
            OrbitTextLink(
                text = stringResource(R.string.habits_restore),
                onClick = onRestore,
            )
        }
    }
}

@Composable
private fun HabitSectionHeader(
    title: String,
    trailing: String?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    OrbitSectionTitle(
        title = title,
        modifier = modifier.fillMaxWidth(),
        trailing = if (trailing == null) {
            null
        } else {
            {
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp,
                        fontFeatureSettings = "tnum",
                    ),
                    color = colors.accentLine,
                )
            }
        },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF101010, widthDp = 393, heightDp = 852)
@Composable
private fun HabitsScreenPreview() {
    val today = TaskDate.todayEpochDay()
    val habits = listOf(
        Habit(
            id = 1,
            title = "阅读 20 分钟",
            note = "睡前放下手机",
            tone = AccentTone.VIOLET,
            createdAt = TaskDate.datePickerMillis(today - 30),
            sortOrder = 0,
        ),
        Habit(
            id = 2,
            title = "跑步 3 公里",
            tone = AccentTone.CYAN,
            createdAt = TaskDate.datePickerMillis(today - 20),
            sortOrder = 1,
        ),
        Habit(
            id = 3,
            title = "晨间冥想",
            tone = AccentTone.GOLD,
            createdAt = TaskDate.datePickerMillis(today - 10),
            archivedAt = TaskDate.datePickerMillis(today - 2),
            sortOrder = 2,
        ),
    )
    val records = buildList {
        listOf(0L, 1L, 2L, 3L, 5L, 8L).forEachIndexed { index, offset ->
            add(
                HabitRecord(
                    id = index + 1L,
                    habitId = 1,
                    epochDay = today - offset,
                    checkedAt = 1L,
                ),
            )
        }
        listOf(1L, 2L, 3L).forEachIndexed { index, offset ->
            add(
                HabitRecord(
                    id = 100L + index,
                    habitId = 2,
                    epochDay = today - offset,
                    checkedAt = 1L,
                ),
            )
        }
    }
    OrbitTheme {
        SpaceBackdrop {
            HabitsScreen(
                uiState = HabitUiState(
                    habits = habits,
                    records = records,
                    loadStatus = HabitLoadStatus.READY,
                    todayEpochDay = today,
                ),
                segment = PlannerSegment.HABITS,
                onSegmentChange = {},
                onAddHabit = {},
                onEditHabit = {},
                onToggleHabit = {},
                onRestoreHabit = {},
                onRetry = {},
                goalCount = 3,
                taskCount = 4,
            )
        }
    }
}
