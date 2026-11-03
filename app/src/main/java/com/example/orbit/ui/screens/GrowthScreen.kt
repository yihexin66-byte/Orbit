package com.example.orbit.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbit.R
import com.example.orbit.ai.AiReviewAction
import com.example.orbit.model.CategoryBreakdown
import com.example.orbit.model.DailyCompletion
import com.example.orbit.model.GoalOverview
import com.example.orbit.model.GrowthEvent
import com.example.orbit.model.GrowthEventType
import com.example.orbit.model.GrowthInsights
import com.example.orbit.model.GrowthRules
import com.example.orbit.model.GrowthSummary
import com.example.orbit.model.HabitConsistency
import com.example.orbit.model.Task
import com.example.orbit.model.TaskCategory
import com.example.orbit.model.TaskDate
import com.example.orbit.model.TaskPriority
import com.example.orbit.model.TaskStatistics
import com.example.orbit.ui.GoalLoadStatus
import com.example.orbit.ui.GoalUiState
import com.example.orbit.ui.GrowthLoadStatus
import com.example.orbit.ui.GrowthUiState
import com.example.orbit.ui.AiReviewUiState
import com.example.orbit.ui.HabitLoadStatus
import com.example.orbit.ui.HabitUiState
import com.example.orbit.ui.TaskLoadStatus
import com.example.orbit.ui.TaskUiState
import com.example.orbit.ui.components.OrbitBadge
import com.example.orbit.ui.components.OrbitCard
import com.example.orbit.ui.components.OrbitEmptyState
import com.example.orbit.ui.components.OrbitLazyScreen
import com.example.orbit.ui.components.OrbitLoadingCard
import com.example.orbit.ui.components.OrbitNote
import com.example.orbit.ui.components.OrbitProgressBar
import com.example.orbit.ui.components.OrbitPrimaryButton
import com.example.orbit.ui.components.OrbitProgressRing
import com.example.orbit.ui.components.OrbitScreenHeader
import com.example.orbit.ui.components.SpaceBackdrop
import com.example.orbit.ui.components.categoryColor
import com.example.orbit.ui.theme.OrbitCorners
import com.example.orbit.ui.theme.OrbitTheme
import com.example.orbit.ui.theme.orbitColors
import kotlin.math.roundToInt

@Composable
fun GrowthScreen(
    taskState: TaskUiState,
    habitState: HabitUiState,
    goalState: GoalUiState,
    growthState: GrowthUiState,
    onRetry: () -> Unit,
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier,
    cloudSignedIn: Boolean = false,
    reviewState: AiReviewUiState = AiReviewUiState(),
    onGenerateReview: () -> Unit = {},
    onRetryReviewStatus: () -> Unit = {},
) {
    val stats = taskState.statistics
    val hasAnyData = taskState.tasks.isNotEmpty() ||
        goalState.goals.isNotEmpty() ||
        habitState.habits.isNotEmpty() ||
        growthState.events.isNotEmpty()
    val isLoading = taskState.isLoading || goalState.isLoading ||
        habitState.isLoading || growthState.isLoading
    val loadFailed = taskState.loadFailed || goalState.loadFailed ||
        habitState.loadFailed || growthState.loadFailed
    val habitConsistency = remember(
        habitState.habits,
        habitState.records,
        habitState.todayEpochDay,
    ) {
        GrowthInsights.habitConsistency(
            habits = habitState.habits,
            records = habitState.records,
            today = habitState.todayEpochDay,
        )
    }
    val goalOverview = remember(goalState.goals, goalState.stages, goalState.tasks) {
        GrowthInsights.goalOverview(
            goals = goalState.goals,
            stages = goalState.stages,
            tasks = goalState.tasks,
        )
    }

    OrbitLazyScreen(
        modifier = modifier,
        content = {
            item(key = "header") {
                OrbitScreenHeader(
                    eyebrow = stringResource(R.string.growth_eyebrow),
                    title = stringResource(R.string.growth_title),
                    subtitle = stringResource(R.string.growth_subtitle),
                    trailing = {
                        OrbitBadge(
                            text = stringResource(
                                if (cloudSignedIn) R.string.cloud_synced else R.string.local_data,
                            ),
                        )
                    },
                )
            }

            when {
                isLoading -> {
                    item(key = "loading") {
                        OrbitLoadingCard(modifier = Modifier.padding(top = 6.dp))
                    }
                }

                loadFailed -> {
                    item(key = "load-error") {
                        OrbitEmptyState(
                            modifier = Modifier.padding(top = 6.dp),
                            title = stringResource(R.string.growth_load_failed_title),
                            description = stringResource(R.string.growth_load_failed_desc),
                            actionLabel = stringResource(R.string.task_retry),
                            onAction = onRetry,
                        )
                    }
                }

                !hasAnyData -> {
                    item(key = "empty") {
                        OrbitEmptyState(
                            modifier = Modifier.padding(top = 6.dp),
                            title = stringResource(R.string.stats_empty_title),
                            description = stringResource(R.string.stats_empty_desc),
                            actionLabel = stringResource(R.string.stats_empty_action),
                            onAction = onAddTask,
                        )
                    }
                }

                else -> {
                    item(key = "growth-overview") {
                        GrowthOverviewCard(summary = growthState.summary)
                    }
                    item(key = "today") {
                        TodayStatsCard(uiState = taskState)
                    }
                    item(key = "overview") {
                        OverviewCard(stats = stats)
                    }
                    item(key = "week") {
                        WeekChartCard(stats = stats)
                    }
                    item(key = "categories") {
                        CategoryCard(stats = stats)
                    }
                    item(key = "streak") {
                        StreakCard(stats = stats)
                    }
                    item(key = "habit-consistency") {
                        HabitConsistencyCard(consistency = habitConsistency)
                    }
                    item(key = "goal-overview") {
                        GoalOverviewCard(overview = goalOverview)
                    }
                    item(key = "ai-review") {
                        AiReviewCard(
                            state = reviewState,
                            onGenerate = onGenerateReview,
                            onRetry = onRetryReviewStatus,
                        )
                    }
                }
            }
        },
    )
}

/**
 * The one inverted module on the growth page. Level is the largest number in
 * the product; XP, progress and the next threshold sit on the same block so the
 * hierarchy is readable at a glance.
 */
@Composable
private fun GrowthOverviewCard(summary: GrowthSummary) {
    val colors = MaterialTheme.orbitColors
    val atMaxLevel = summary.level >= GrowthRules.MAX_LEVEL
    val progress = if (atMaxLevel) 1f else summary.progressToNextLevel
    val percent = (progress * 100).roundToInt()
    val remaining = if (atMaxLevel) {
        0
    } else {
        (summary.xpForNextLevel - summary.xpIntoLevel).coerceAtLeast(0)
    }
    val description = stringResource(
        R.string.growth_xp_desc,
        summary.level,
        summary.totalXp,
        percent,
        summary.todayXp,
        summary.thisWeekXp,
    )
    val surface = if (colors.isDark) colors.cardElevated else colors.textPrimary
    val onSurface = if (colors.isDark) colors.textPrimary else colors.backgroundTop
    val muted = onSurface.copy(alpha = 0.56f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OrbitCorners.card))
            .background(surface)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(horizontal = 22.dp, vertical = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(colors.primary),
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = stringResource(R.string.growth_level_title),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    letterSpacing = 1.7.sp,
                ),
                color = muted,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.growth_total_xp, summary.totalXp),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 0.4.sp,
                ),
                color = muted,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = stringResource(R.string.growth_level_short),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                ),
                color = muted,
                modifier = Modifier.padding(bottom = 9.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = summary.level.toString(),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 56.sp,
                    lineHeight = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFeatureSettings = "tnum",
                ),
                color = onSurface,
            )
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                XpCell(
                    label = stringResource(R.string.growth_today_label),
                    value = stringResource(R.string.growth_xp_value, summary.todayXp),
                    valueColor = colors.primary,
                    labelColor = muted,
                )
                XpCell(
                    label = stringResource(R.string.growth_week_label),
                    value = stringResource(R.string.growth_xp_value, summary.thisWeekXp),
                    valueColor = onSurface,
                    labelColor = muted,
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (atMaxLevel) {
                    stringResource(R.string.growth_max_level)
                } else {
                    stringResource(
                        R.string.growth_to_next_level,
                        summary.level + 1,
                        remaining,
                    )
                },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = muted,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.stats_percent, percent),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                ),
                color = colors.primary,
            )
        }
        Spacer(Modifier.height(8.dp))
        OrbitProgressBar(
            progress = progress,
            height = 6.dp,
            color = colors.primary,
            trackColor = onSurface.copy(alpha = 0.18f),
        )
    }
}

@Composable
private fun XpCell(
    label: String,
    value: String,
    valueColor: Color,
    labelColor: Color,
) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                letterSpacing = 0.7.sp,
            ),
            color = labelColor,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum",
            ),
            color = valueColor,
        )
    }
}

@Composable
private fun TodayStatsCard(uiState: TaskUiState) {
    val colors = MaterialTheme.orbitColors
    val summary = uiState.todaySummary
    val percent = (summary.progress * 100).roundToInt()
    val ringDescription = stringResource(
        R.string.home_progress_desc,
        percent,
        summary.completed,
        summary.planned,
    )
    val detail = when {
        summary.planned == 0 -> stringResource(R.string.stats_today_empty)
        else -> stringResource(
            R.string.stats_today_counts,
            summary.completed,
            summary.planned,
        )
    }
    val statusText = when {
        summary.planned == 0 -> stringResource(R.string.home_summary_empty)
        summary.pending == 0 && summary.completed == summary.planned ->
            stringResource(R.string.stats_today_all_done)
        summary.pending == 0 -> stringResource(R.string.home_summary_no_pending)
        else -> stringResource(R.string.stats_today_remaining, summary.pending)
    }

    OrbitCard(padding = androidx.compose.foundation.layout.PaddingValues(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clearAndSetSemantics { contentDescription = ringDescription },
                contentAlignment = Alignment.Center,
            ) {
                OrbitProgressRing(
                    progress = summary.progress,
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 9.dp,
                    color = colors.primary,
                    trackColor = colors.track,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.stats_today_tag),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = colors.textMuted,
                            letterSpacing = 1.4.sp,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.stats_percent, percent),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 28.sp,
                                lineHeight = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFeatureSettings = "tnum",
                            ),
                            color = colors.textPrimary,
                        )
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text = stringResource(
                                R.string.home_center_ratio,
                                summary.completed,
                                summary.planned,
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                letterSpacing = 0.sp,
                            ),
                            color = colors.textSecondary,
                        )
                    }
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.stats_today_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                if (summary.pending == 0) colors.success else colors.primary,
                            ),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = if (summary.pending == 0) colors.success else colors.textPrimary,
                    )
                }
                if (summary.completedBeforeToday > 0) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = stringResource(
                            R.string.today_completed_earlier,
                            summary.completedBeforeToday,
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            lineHeight = 15.sp,
                        ),
                        color = colors.textMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(stats: TaskStatistics) {
    val colors = MaterialTheme.orbitColors
    val percent = (stats.completionRate * 100).roundToInt()
    val description = stringResource(
        R.string.stats_overview_desc,
        stats.pending,
        stats.completed,
        stats.total,
        percent,
    )
    OrbitCard(
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = description
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.stats_overview_title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.stats_overview_total, stats.total),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp,
                ),
                color = colors.textMuted,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.stats_overview_note),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = colors.textMuted,
        )
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stats.pending.toString(),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 40.sp,
                        lineHeight = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFeatureSettings = "tnum",
                    ),
                    color = colors.primary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.stats_overview_pending),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = colors.textMuted,
                )
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(52.dp)
                    .background(colors.cardBorder),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 20.dp),
            ) {
                Text(
                    text = stats.completed.toString(),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 40.sp,
                        lineHeight = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFeatureSettings = "tnum",
                    ),
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.stats_overview_completed),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = colors.textMuted,
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.stats_overview_rate),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = colors.textSecondary,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.stats_overview_rate_value, percent),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                ),
                color = colors.primary,
            )
        }
        Spacer(Modifier.height(8.dp))
        OrbitProgressBar(
            progress = stats.completionRate,
            height = 5.dp,
            color = colors.primary,
        )
    }
}

@Composable
private fun WeekChartCard(stats: TaskStatistics) {
    val colors = MaterialTheme.orbitColors
    OrbitCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.stats_week_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.stats_week_legend),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = colors.textMuted,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.stats_week_total_label),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.5.sp,
                        letterSpacing = 0.7.sp,
                    ),
                    color = colors.textMuted,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stats.weekCompleted.toString(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 28.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFeatureSettings = "tnum",
                    ),
                    color = colors.primary,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        if (stats.hasCompletionHistory) {
            WeekBarChart(days = stats.days)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TrendText(delta = stats.trendDelta)
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.stats_week_summary, stats.weekCompleted),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = colors.textMuted,
                )
            }
        } else {
            WeekEmptyBlock()
        }
    }
}

@Composable
private fun TrendText(delta: Int) {
    val colors = MaterialTheme.orbitColors
    val text = when {
        delta > 0 -> stringResource(R.string.stats_trend_up, delta)
        delta < 0 -> stringResource(R.string.stats_trend_down, -delta)
        else -> stringResource(R.string.stats_trend_flat)
    }
    val color = when {
        delta > 0 -> colors.success
        delta < 0 -> colors.warning
        else -> colors.textSecondary
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFeatureSettings = "tnum",
        ),
        color = color,
    )
}

@Composable
private fun WeekEmptyBlock() {
    val colors = MaterialTheme.orbitColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OrbitCorners.inner))
            .background(colors.track.copy(alpha = 0.55f))
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.stats_week_empty_title),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.stats_week_empty_desc),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 15.sp),
            color = colors.textMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WeekBarChart(
    days: List<DailyCompletion>,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    val maxCount = remember(days) { days.maxOfOrNull { it.completed }?.coerceAtLeast(1) ?: 1 }
    val labels = remember(days) { days.map { TaskDate.weekdayLabel(it.epochDay) } }
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEachIndexed { index, day ->
            val fraction = if (revealed && day.completed > 0) {
                (day.completed.toFloat() / maxCount).coerceIn(0.08f, 1f)
            } else {
                0f
            }
            val animatedFraction by animateFloatAsState(
                targetValue = fraction,
                animationSpec = tween(
                    durationMillis = 560,
                    delayMillis = index * 40,
                    easing = FastOutSlowInEasing,
                ),
                label = "weekBar$index",
            )
            val label = if (day.isToday) {
                stringResource(R.string.stats_today_tag)
            } else {
                labels[index]
            }
            val barDescription = stringResource(
                R.string.stats_bar_desc,
                label,
                day.completed,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics { contentDescription = barDescription },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = day.completed.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 0.sp,
                        fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                        fontFeatureSettings = "tnum",
                    ),
                    color = when {
                        day.isToday -> colors.primary
                        day.completed > 0 -> colors.textSecondary
                        else -> colors.textMuted
                    },
                    maxLines = 1,
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(96.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.track),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (day.completed > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(animatedFraction)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (day.isToday) colors.primary else colors.textPrimary,
                                ),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        letterSpacing = 0.sp,
                    ),
                    color = if (day.isToday) colors.primary else colors.textMuted,
                    fontWeight = if (day.isToday) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(stats: TaskStatistics) {
    val colors = MaterialTheme.orbitColors
    val visibleCategories = stats.categories.filter { it.total > 0 }

    OrbitCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.stats_category_title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.stats_overview_total, stats.total),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp,
                ),
                color = colors.textMuted,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.stats_category_legend),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 10.sp,
                lineHeight = 15.sp,
            ),
            color = colors.textMuted,
        )
        Spacer(Modifier.height(18.dp))
        if (visibleCategories.isEmpty()) {
            Text(
                text = stringResource(R.string.stats_category_empty),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        } else {
            visibleCategories.forEachIndexed { index, category ->
                CategoryRow(category = category, totalTasks = stats.total)
                if (index != visibleCategories.lastIndex) {
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: CategoryBreakdown,
    totalTasks: Int,
) {
    val colors = MaterialTheme.orbitColors
    val categoryLabel = stringResource(category.category.labelRes)
    val tint = categoryColor(category.category)
    val share = category.shareOf(totalTasks)
    val sharePercent = (share * 100).roundToInt()
    val description = stringResource(
        R.string.stats_category_bar_desc,
        categoryLabel,
        category.total,
        category.completed,
        category.pending,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(tint),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = categoryLabel,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = colors.textPrimary,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(
                    R.string.stats_category_share,
                    category.total,
                    sharePercent,
                ),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 0.sp,
                    fontFeatureSettings = "tnum",
                ),
                color = colors.textSecondary,
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(OrbitCorners.pill))
                .background(colors.track),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(share.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(OrbitCorners.pill))
                    .background(tint.copy(alpha = 0.22f)),
            ) {
                if (category.completed > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(category.completionRate)
                            .fillMaxHeight()
                            .background(tint),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(
                R.string.stats_category_meta,
                category.completed,
                category.pending,
            ),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = colors.textMuted,
        )
    }
}

@Composable
private fun StreakCard(stats: TaskStatistics) {
    val colors = MaterialTheme.orbitColors
    val activeDays = stats.days.count { it.completed > 0 }
    val windowDescription = stringResource(R.string.stats_streak_window, activeDays)

    OrbitCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.stats_streak_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.stats_streak_desc),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        lineHeight = 15.sp,
                    ),
                    color = colors.textMuted,
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = stringResource(R.string.stats_streak_value, stats.streakDays),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFeatureSettings = "tnum",
                ),
                color = colors.primary,
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics { contentDescription = windowDescription },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            stats.days.forEach { day ->
                val active = day.completed > 0
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(OrbitCorners.pill))
                        .background(
                            when {
                                !active -> colors.track
                                day.isToday -> colors.primary
                                else -> colors.textPrimary
                            },
                        ),
                )
            }
        }
        Spacer(Modifier.height(9.dp))
        Text(
            text = windowDescription,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = colors.textMuted,
        )
    }
}

@Composable
private fun HabitConsistencyCard(consistency: HabitConsistency) {
    val colors = MaterialTheme.orbitColors
    val percent = (consistency.sevenDayRate * 100).roundToInt()
    val hasActiveHabits = consistency.activeHabits > 0
    val description = stringResource(
        R.string.growth_habit_desc,
        consistency.checkedToday,
        consistency.activeHabits,
        percent,
        consistency.longestStreak,
    )

    OrbitCard(
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = description
        },
    ) {
        Text(
            text = stringResource(R.string.growth_habit_title),
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.growth_habit_legend),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 15.sp),
            color = colors.textMuted,
        )
        Spacer(Modifier.height(18.dp))
        if (!hasActiveHabits) {
            Text(
                text = stringResource(R.string.growth_habit_empty),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                ),
                color = colors.textMuted,
            )
        } else {
            Row(modifier = Modifier.fillMaxWidth()) {
                GrowthStat(
                    value = stringResource(
                        R.string.growth_habit_today_value,
                        consistency.checkedToday,
                        consistency.activeHabits,
                    ),
                    label = stringResource(R.string.growth_habit_today),
                    color = colors.primary,
                    modifier = Modifier.weight(1f),
                )
                GrowthStat(
                    value = stringResource(R.string.growth_habit_week_value, percent),
                    label = stringResource(R.string.growth_habit_week),
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                GrowthStat(
                    value = stringResource(
                        R.string.growth_habit_streak_value,
                        consistency.longestStreak,
                    ),
                    label = stringResource(R.string.growth_habit_streak),
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(16.dp))
            OrbitProgressBar(
                progress = consistency.sevenDayRate,
                height = 5.dp,
                color = colors.primary,
            )
        }
    }
}

@Composable
private fun GoalOverviewCard(overview: GoalOverview) {
    val colors = MaterialTheme.orbitColors
    val percent = (overview.averageProgress * 100).roundToInt()
    val hasActiveGoals = overview.activeGoals > 0
    val description = stringResource(
        R.string.growth_goal_desc,
        overview.activeGoals,
        overview.completedStages,
        percent,
    )

    OrbitCard(
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = description
        },
    ) {
        Text(
            text = stringResource(R.string.growth_goal_title),
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.growth_goal_legend),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 15.sp),
            color = colors.textMuted,
        )
        Spacer(Modifier.height(18.dp))
        if (!hasActiveGoals) {
            Text(
                text = stringResource(R.string.growth_goal_empty),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                ),
                color = colors.textMuted,
            )
        } else {
            Row(modifier = Modifier.fillMaxWidth()) {
                GrowthStat(
                    value = stringResource(R.string.growth_goal_value, overview.activeGoals),
                    label = stringResource(R.string.growth_goal_active),
                    color = colors.primary,
                    modifier = Modifier.weight(1f),
                )
                GrowthStat(
                    value = stringResource(
                        R.string.growth_goal_stages_value,
                        overview.completedStages,
                    ),
                    label = stringResource(R.string.growth_goal_stages),
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                GrowthStat(
                    value = stringResource(R.string.stats_percent, percent),
                    label = stringResource(R.string.growth_goal_progress),
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(16.dp))
            OrbitProgressBar(
                progress = overview.averageProgress,
                height = 5.dp,
                color = colors.primary,
            )
        }
    }
}

/**
 * Weekly reflection card. The generated text is read-only and cached; every
 * count and title shown here came from local rows before the request.
 */
@Composable
private fun AiReviewCard(
    state: AiReviewUiState,
    onGenerate: () -> Unit,
    onRetry: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    val cached = state.cached
    val review = cached?.review

    OrbitCard(
        accent = if (review != null) colors.primary else null,
        padding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(if (review != null) colors.primary else colors.textMuted),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.growth_ai_review_eyebrow),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 1.6.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = colors.textMuted,
            )
            Spacer(Modifier.weight(1f))
            if (!state.qaBlocked && state.configuredKnown && state.configured) {
                Text(
                    text = if (state.reviewRemaining > 0) {
                        stringResource(
                            R.string.growth_ai_review_quota,
                            state.reviewRemaining,
                        )
                    } else {
                        stringResource(R.string.growth_ai_review_quota_exhausted)
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontFeatureSettings = "tnum",
                    ),
                    color = if (state.reviewRemaining > 0) {
                        colors.textMuted
                    } else {
                        colors.warning
                    },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (review == null) {
            Text(
                text = stringResource(R.string.growth_ai_review_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.growth_ai_review_desc),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                ),
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(12.dp))
            OrbitNote(text = stringResource(R.string.growth_ai_review_privacy))
            Spacer(Modifier.height(14.dp))

            when {
                state.qaBlocked -> {
                    Text(
                        text = stringResource(R.string.growth_ai_review_offline),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = colors.warning,
                    )
                }

                state.configuredKnown && !state.configured -> {
                    Text(
                        text = stringResource(R.string.growth_ai_review_unavailable),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = colors.textMuted,
                    )
                    Spacer(Modifier.height(4.dp))
                    ReviewTextAction(
                        text = stringResource(R.string.growth_ai_review_check),
                        enabled = true,
                        onClick = onRetry,
                    )
                }

                state.generating -> {
                    ReviewGeneratingRow()
                }

                else -> {
                    if (!state.hasData) {
                        Text(
                            text = stringResource(R.string.growth_ai_review_empty_data),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                            ),
                            color = colors.textMuted,
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    OrbitPrimaryButton(
                        text = stringResource(R.string.growth_ai_review_generate),
                        onClick = onGenerate,
                        enabled = state.canGenerate,
                    )
                    if (state.hasData && state.configuredKnown && state.reviewRemaining <= 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.growth_ai_review_quota_exhausted),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = colors.warning,
                        )
                    }
                }
            }

            state.failure?.let { failure ->
                Spacer(Modifier.height(10.dp))
                ReviewFailure(text = aiFailureText(failure))
            }
        } else {
            Text(
                text = review.headline,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = colors.textPrimary,
            )
            if (review.summary.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = review.summary,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 19.sp,
                    ),
                    color = colors.textSecondary,
                )
            }

            ReviewBulletSection(
                title = stringResource(R.string.growth_ai_review_wins),
                items = review.wins,
                accent = colors.success,
            )
            ReviewBulletSection(
                title = stringResource(R.string.growth_ai_review_risks),
                items = review.risks,
                accent = colors.warning,
            )
            if (review.nextWeek.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                ReviewSectionLabel(text = stringResource(R.string.growth_ai_review_next))
                Spacer(Modifier.height(8.dp))
                review.nextWeek.forEachIndexed { index, action ->
                    ReviewActionRow(index = index, action = action)
                }
            }

            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.cardBorder),
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (cached != null && cached.generatedAt > 0) {
                    Text(
                        text = stringResource(
                            R.string.growth_ai_review_generated_at,
                            TaskDate.monthDayLabel(
                                TaskDate.epochDayOfMillis(cached.generatedAt),
                            ),
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = colors.textMuted,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.growth_ai_review_cached),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = colors.textMuted,
                    )
                }
                Spacer(Modifier.weight(1f))
                when {
                    state.generating -> ReviewGeneratingRow()
                    state.canGenerate -> ReviewTextAction(
                        text = stringResource(R.string.growth_ai_review_regenerate),
                        enabled = true,
                        onClick = onGenerate,
                    )
                }
            }

            state.failure?.let { failure ->
                Spacer(Modifier.height(8.dp))
                ReviewFailure(text = aiFailureText(failure))
            }
        }
    }
}

@Composable
private fun ReviewSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            letterSpacing = 1.3.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        color = MaterialTheme.orbitColors.textMuted,
    )
}

@Composable
private fun ReviewBulletSection(title: String, items: List<String>, accent: Color) {
    if (items.isEmpty()) return
    Spacer(Modifier.height(14.dp))
    ReviewSectionLabel(text = title)
    Spacer(Modifier.height(7.dp))
    items.forEach { item ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(5.dp)
                    .background(accent),
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = item,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                ),
                color = MaterialTheme.orbitColors.textSecondary,
            )
        }
    }
}

@Composable
private fun ReviewActionRow(index: Int, action: AiReviewAction) {
    val colors = MaterialTheme.orbitColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 9.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = (index + 1).toString().padStart(2, '0'),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum",
            ),
            color = colors.accentLine,
            modifier = Modifier.width(26.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = action.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = colors.textPrimary,
            )
            if (action.reason.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = action.reason,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                    ),
                    color = colors.textMuted,
                )
            }
        }
    }
}

@Composable
private fun ReviewGeneratingRow() {
    val colors = MaterialTheme.orbitColors
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            color = colors.accentLine,
            strokeWidth = 2.dp,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.growth_ai_review_generating),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = colors.textMuted,
        )
    }
}

@Composable
private fun ReviewFailure(text: String) {
    val colors = MaterialTheme.orbitColors
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(5.dp)
                .background(colors.danger),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                lineHeight = 16.sp,
            ),
            color = colors.danger,
        )
    }
}

@Composable
private fun ReviewTextAction(text: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.orbitColors
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = if (enabled) colors.accentLine else colors.textMuted,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

@Composable
private fun GrowthStat(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFeatureSettings = "tnum",
            ),
            color = color,
            maxLines = 1,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = colors.textMuted,
            maxLines = 1,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF101010, widthDp = 393, heightDp = 852)
@Composable
private fun GrowthScreenPreview() {
    val today = TaskDate.todayEpochDay()
    fun completedAt(epochDay: Long): Long =
        TaskDate.datePickerMillis(epochDay) + 12 * 60 * 60 * 1000L

    OrbitTheme {
        SpaceBackdrop {
            GrowthScreen(
                taskState = TaskUiState(
                    tasks = listOf(
                        Task(
                            id = 1,
                            title = "完成作品集任务页数据层",
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
                            isCompleted = true,
                            createdAt = 2L,
                            completedAt = completedAt(today),
                        ),
                        Task(
                            id = 3,
                            title = "跑步 5 公里",
                            priority = TaskPriority.LOW,
                            category = TaskCategory.HEALTH,
                            dueDate = today - 1,
                            isCompleted = true,
                            createdAt = 3L,
                            completedAt = completedAt(today - 1),
                        ),
                        Task(
                            id = 4,
                            title = "阅读《Android 编程权威指南》",
                            priority = TaskPriority.MEDIUM,
                            category = TaskCategory.STUDY,
                            dueDate = today + 1,
                            createdAt = 4L,
                        ),
                    ),
                    loadStatus = TaskLoadStatus.READY,
                    todayEpochDay = today,
                ),
                habitState = HabitUiState(
                    loadStatus = HabitLoadStatus.READY,
                    todayEpochDay = today,
                ),
                goalState = GoalUiState(loadStatus = GoalLoadStatus.READY),
                growthState = GrowthUiState(
                    events = listOf(
                        GrowthEvent(
                            type = GrowthEventType.TASK_COMPLETED,
                            sourceKey = "task:2",
                            xp = 10,
                            occurredAt = completedAt(today),
                            epochDay = today,
                        ),
                        GrowthEvent(
                            type = GrowthEventType.TASK_COMPLETED,
                            sourceKey = "task:3",
                            xp = 10,
                            occurredAt = completedAt(today - 1),
                            epochDay = today - 1,
                        ),
                        GrowthEvent(
                            type = GrowthEventType.STAGE_COMPLETED,
                            sourceKey = "stage:1",
                            xp = 40,
                            occurredAt = completedAt(today - 2),
                            epochDay = today - 2,
                        ),
                    ),
                    loadStatus = GrowthLoadStatus.READY,
                    todayEpochDay = today,
                ),
                onRetry = {},
                onAddTask = {},
            )
        }
    }
}
