package com.example.orbit.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbit.R
import com.example.orbit.ai.AiPlan
import com.example.orbit.ai.AiPlanStage
import com.example.orbit.ai.AiPlanTask
import com.example.orbit.ai.AiPlanSelection
import com.example.orbit.model.TaskDate
import com.example.orbit.ui.AiPlanFormState
import com.example.orbit.ui.AiPlanPhase
import com.example.orbit.ui.AiPlanUiState
import com.example.orbit.ui.components.OrbitCard
import com.example.orbit.ui.components.OrbitEmptyState
import com.example.orbit.ui.components.OrbitIconButton
import com.example.orbit.ui.components.OrbitLazyScreen
import com.example.orbit.ui.components.OrbitLoadingCard
import com.example.orbit.ui.components.OrbitNote
import com.example.orbit.ui.components.OrbitPrimaryButton
import com.example.orbit.ui.components.OrbitSectionTitle
import com.example.orbit.ui.components.OrbitScreenHeader
import com.example.orbit.ui.components.TagPill
import com.example.orbit.ui.components.priorityColor
import com.example.orbit.ui.theme.OrbitCorners
import com.example.orbit.ui.theme.OrbitTheme
import com.example.orbit.ui.theme.orbitColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiPlanScreen(
    uiState: AiPlanUiState,
    onBack: () -> Unit,
    onGoalChange: (String) -> Unit,
    onTargetDateChange: (Long) -> Unit,
    onDailyMinutesChange: (Int) -> Unit,
    onCurrentLevelChange: (String) -> Unit,
    onWeakPointsChange: (String) -> Unit,
    onGenerate: () -> Unit,
    onToggleStage: (Int) -> Unit,
    onToggleTask: (Int, Int) -> Unit,
    onImport: () -> Unit,
    onDiscard: () -> Unit,
    onRetryStatus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    if (showDatePicker) {
        AiPlanDatePicker(
            initialEpochDay = uiState.form.targetDate,
            onDismiss = { showDatePicker = false },
            onPick = { epochDay ->
                onTargetDateChange(epochDay)
                showDatePicker = false
            },
        )
    }

    OrbitLazyScreen(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = {
            item(key = "nav") {
                AiPlanNavRow(
                    onBack = onBack,
                    remaining = uiState.status?.planRemaining,
                    configured = uiState.configured,
                )
            }

            item(key = "header") {
                OrbitScreenHeader(
                    eyebrow = stringResource(R.string.ai_plan_eyebrow),
                    title = stringResource(R.string.ai_plan_title),
                    subtitle = stringResource(R.string.ai_plan_subtitle),
                )
            }

            when {
                uiState.qaBlocked -> {
                    item(key = "offline") {
                        OrbitEmptyState(
                            title = stringResource(R.string.ai_plan_offline_title),
                            description = stringResource(R.string.ai_plan_offline_desc),
                        )
                    }
                }

                uiState.statusLoading && uiState.status == null -> {
                    item(key = "status-loading") {
                        OrbitLoadingCard()
                    }
                }

                uiState.statusError != null && uiState.status == null -> {
                    item(key = "status-error") {
                        OrbitEmptyState(
                            title = stringResource(R.string.ai_plan_status_failed_title),
                            description = aiFailureText(uiState.statusError),
                            actionLabel = stringResource(R.string.ai_plan_retry),
                            onAction = onRetryStatus,
                        )
                    }
                }

                uiState.status != null && !uiState.configured -> {
                    item(key = "unconfigured") {
                        OrbitEmptyState(
                            title = stringResource(R.string.ai_plan_not_configured_title),
                            description = stringResource(R.string.ai_plan_not_configured_desc),
                            actionLabel = stringResource(R.string.ai_plan_retry),
                            onAction = onRetryStatus,
                        )
                    }
                }

                uiState.phase == AiPlanPhase.GENERATING -> {
                    item(key = "generating") {
                        AiPlanGeneratingCard()
                    }
                }

                uiState.phase == AiPlanPhase.PREVIEW ||
                    uiState.phase == AiPlanPhase.IMPORTING ||
                    uiState.phase == AiPlanPhase.IMPORTED -> {
                    val plan = uiState.plan
                    if (plan == null) {
                        item(key = "missing-plan") {
                            OrbitEmptyState(
                                title = stringResource(R.string.ai_plan_status_failed_title),
                                description = stringResource(R.string.ai_plan_error_invalid_output),
                                actionLabel = stringResource(R.string.ai_plan_retry),
                                onAction = onGenerate,
                            )
                        }
                    } else {
                        item(key = "summary") {
                            AiPlanSummaryCard(
                                plan = plan,
                                selected = uiState.selectedTaskCount,
                            )
                        }
                        plan.stages.forEachIndexed { stageIndex, stage ->
                            item(key = "stage-$stageIndex") {
                                AiPlanStageCard(
                                    index = stageIndex,
                                    stage = stage,
                                    excluded = uiState.excluded,
                                    enabled = !uiState.isBusy,
                                    onToggleStage = { onToggleStage(stageIndex) },
                                    onToggleTask = { taskIndex ->
                                        onToggleTask(stageIndex, taskIndex)
                                    },
                                )
                            }
                        }
                        uiState.failure?.let { failure ->
                            item(key = "failure") {
                                AiFailureCard(message = aiFailureText(failure))
                            }
                        }
                        item(key = "import-footer") {
                            AiPlanImportFooter(
                                selected = uiState.selectedTaskCount,
                                total = uiState.planTaskCount,
                                importing = uiState.phase == AiPlanPhase.IMPORTING,
                                emptySelection = uiState.emptySelection,
                                importError = uiState.importError,
                                onImport = onImport,
                                onDiscard = onDiscard,
                            )
                        }
                    }
                }

                else -> {
                    item(key = "goal-label") {
                        OrbitSectionTitle(
                            title = stringResource(R.string.ai_plan_form_title),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    item(key = "form") {
                        AiPlanFormCard(
                            form = uiState.form,
                            onGoalChange = onGoalChange,
                            onPickDate = { showDatePicker = true },
                            onDailyMinutesChange = onDailyMinutesChange,
                            onCurrentLevelChange = onCurrentLevelChange,
                            onWeakPointsChange = onWeakPointsChange,
                        )
                    }
                    uiState.failure?.let { failure ->
                        item(key = "failure") {
                            AiFailureCard(message = aiFailureText(failure))
                        }
                    }
                    item(key = "generate") {
                        OrbitPrimaryButton(
                            text = stringResource(R.string.ai_plan_generate),
                            onClick = onGenerate,
                            enabled = uiState.canGenerate,
                        )
                    }
                    item(key = "quota") {
                        AiPlanQuotaLine(
                            remaining = uiState.planRemaining,
                            configured = uiState.configured,
                        )
                    }
                    item(key = "note") {
                        OrbitNote(text = stringResource(R.string.ai_plan_note))
                    }
                }
            }
        },
    )
}

@Composable
private fun AiPlanNavRow(
    onBack: () -> Unit,
    remaining: Int?,
    configured: Boolean,
) {
    val colors = MaterialTheme.orbitColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OrbitIconButton(
            onClick = onBack,
            contentDescription = stringResource(R.string.ai_plan_back),
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
            text = stringResource(R.string.ai_plan_eyebrow),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.8.sp),
            color = colors.textSecondary,
        )
        Spacer(Modifier.weight(1f))
        if (configured && remaining != null) {
            Text(
                text = if (remaining > 0) {
                    stringResource(R.string.ai_plan_quota_remaining, remaining)
                } else {
                    stringResource(R.string.ai_plan_quota_exhausted)
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    fontFeatureSettings = "tnum",
                ),
                color = if (remaining > 0) colors.textSecondary else colors.warning,
            )
        }
    }
}

@Composable
private fun AiPlanFormCard(
    form: AiPlanFormState,
    onGoalChange: (String) -> Unit,
    onPickDate: () -> Unit,
    onDailyMinutesChange: (Int) -> Unit,
    onCurrentLevelChange: (String) -> Unit,
    onWeakPointsChange: (String) -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    OrbitCard(
        padding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
    ) {
        SheetSectionLabel(stringResource(R.string.ai_plan_form_goal_label))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = form.goal,
            onValueChange = onGoalChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(stringResource(R.string.ai_plan_form_goal_placeholder))
            },
            minLines = 2,
            maxLines = 3,
            isError = form.goalError,
            supportingText = {
                Text(
                    text = stringResource(
                        R.string.ai_plan_form_goal_counter,
                        form.goal.length,
                        AiPlanFormState.MAX_GOAL_LENGTH,
                    ),
                    color = if (form.goalError) colors.danger else colors.textMuted,
                )
            },
            colors = taskFieldColors(),
        )

        Spacer(Modifier.height(14.dp))
        SheetSectionLabel(stringResource(R.string.ai_plan_form_target_label))
        Spacer(Modifier.height(8.dp))
        DateRow(date = form.targetDate, onClick = onPickDate)

        Spacer(Modifier.height(16.dp))
        SheetSectionLabel(stringResource(R.string.ai_plan_form_duration_label))
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AiPlanFormState.DAILY_MINUTE_OPTIONS.forEach { minutes ->
                ChoicePill(
                    label = stringResource(R.string.ai_plan_minutes, minutes),
                    selected = form.dailyMinutes == minutes,
                    accent = colors.primary,
                    enabled = true,
                    onClick = { onDailyMinutesChange(minutes) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SheetSectionLabel(stringResource(R.string.ai_plan_form_level_label))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = form.currentLevel,
            onValueChange = onCurrentLevelChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(stringResource(R.string.ai_plan_form_level_placeholder))
            },
            singleLine = true,
            colors = taskFieldColors(),
        )

        Spacer(Modifier.height(14.dp))
        SheetSectionLabel(stringResource(R.string.ai_plan_form_weak_label))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = form.weakPoints,
            onValueChange = onWeakPointsChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(stringResource(R.string.ai_plan_form_weak_placeholder))
            },
            minLines = 2,
            maxLines = 3,
            colors = taskFieldColors(),
        )
    }
}

@Composable
private fun DateRow(date: Long, onClick: () -> Unit) {
    val colors = MaterialTheme.orbitColors
    val shape = RoundedCornerShape(OrbitCorners.inner)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, colors.cardBorder, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = TaskDate.monthDayLabel(date),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum",
            ),
            color = colors.textPrimary,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.ai_plan_date_hint),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = colors.textMuted,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.ai_plan_date_change),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = colors.accentLine,
        )
    }
}

@Composable
private fun AiPlanQuotaLine(remaining: Int, configured: Boolean) {
    val colors = MaterialTheme.orbitColors
    if (!configured) return
    Text(
        text = if (remaining > 0) {
            stringResource(R.string.ai_plan_quota_remaining, remaining)
        } else {
            stringResource(R.string.ai_plan_quota_exhausted)
        },
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
        color = if (remaining > 0) colors.textMuted else colors.warning,
    )
}

@Composable
private fun AiPlanGeneratingCard() {
    val colors = MaterialTheme.orbitColors
    OrbitCard(padding = PaddingValues(horizontal = 20.dp, vertical = 26.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = colors.accentLine,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = stringResource(R.string.ai_plan_generating),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.ai_plan_generating_desc),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = colors.textMuted,
                )
            }
        }
    }
}

@Composable
private fun AiPlanSummaryCard(plan: AiPlan, selected: Int) {
    val colors = MaterialTheme.orbitColors
    OrbitCard(
        accent = colors.primary,
        padding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(colors.primary),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.ai_plan_preview_eyebrow),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 1.6.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = colors.textMuted,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(
                    R.string.ai_plan_preview_meta,
                    plan.stages.size,
                    plan.taskCount,
                ),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontFeatureSettings = "tnum",
                ),
                color = colors.textSecondary,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = plan.goalTitle,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = colors.textPrimary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (plan.goalDescription.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = plan.goalDescription,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                ),
                color = colors.textMuted,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.ai_plan_selected_count, selected, plan.taskCount),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    fontFeatureSettings = "tnum",
                ),
                color = colors.accentLine,
            )
            Spacer(Modifier.weight(1f))
            if (plan.targetDateEpochDay > 0) {
                Text(
                    text = stringResource(
                        R.string.goals_target_date,
                        TaskDate.monthDayLabel(plan.targetDateEpochDay),
                    ),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = colors.textMuted,
                )
            }
        }
    }
}

@Composable
private fun AiPlanStageCard(
    index: Int,
    stage: AiPlanStage,
    excluded: Set<String>,
    enabled: Boolean,
    onToggleStage: () -> Unit,
    onToggleTask: (Int) -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    val keys = stage.tasks.indices.map { AiPlanSelection.key(index, it) }
    val allSelected = keys.isNotEmpty() && keys.all { it !in excluded }
    val empty = keys.isEmpty() || keys.all { it in excluded }

    OrbitCard(padding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = (index + 1).toString().padStart(2, '0'),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFeatureSettings = "tnum",
                ),
                color = if (empty) colors.textMuted else colors.accentLine,
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stage.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = if (empty) colors.textMuted else colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (stage.targetDateEpochDay > 0) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(
                            R.string.ai_plan_stage_target,
                            TaskDate.monthDayLabel(stage.targetDateEpochDay),
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = colors.textMuted,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            SelectionCheckbox(
                checked = allSelected,
                enabled = enabled,
                label = stringResource(R.string.ai_plan_select_stage, stage.title),
                onClick = onToggleStage,
            )
        }

        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.cardBorder),
        )
        Spacer(Modifier.height(4.dp))

        stage.tasks.forEachIndexed { taskIndex, task ->
            val checked = AiPlanSelection.key(index, taskIndex) !in excluded
            AiPlanTaskRow(
                task = task,
                checked = checked,
                enabled = enabled,
                label = stringResource(R.string.ai_plan_select_task, task.title),
                onClick = { onToggleTask(taskIndex) },
            )
        }
    }
}

@Composable
private fun AiPlanTaskRow(
    task: AiPlanTask,
    checked: Boolean,
    enabled: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    val selectedState = stringResource(R.string.ai_plan_select_state_selected)
    val unselectedState = stringResource(R.string.ai_plan_select_state_unselected)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                enabled = enabled,
                role = Role.Checkbox,
                onClick = onClick,
            )
            .semantics {
                contentDescription = label
                role = Role.Checkbox
                stateDescription = if (checked) selectedState else unselectedState
            }
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Spacer(Modifier.height(1.dp))
        SelectionCheckbox(
            checked = checked,
            enabled = enabled,
            label = null,
            onClick = onClick,
        )
        Spacer(Modifier.width(10.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .alpha(if (checked) 1f else 0.45f),
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = colors.textPrimary,
            )
            if (task.description.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                    ),
                    color = colors.textMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (task.dueDateEpochDay > 0) {
                    Text(
                        text = stringResource(
                            R.string.ai_plan_task_due,
                            TaskDate.monthDayLabel(task.dueDateEpochDay),
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontFeatureSettings = "tnum",
                        ),
                        color = colors.textMuted,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                TagPill(
                    text = stringResource(task.priorityValue().labelRes),
                    color = priorityColor(task.priorityValue()),
                )
            }
        }
    }
}

@Composable
private fun SelectionCheckbox(
    checked: Boolean,
    enabled: Boolean,
    label: String?,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    val selectedState = stringResource(R.string.ai_plan_select_state_selected)
    val unselectedState = stringResource(R.string.ai_plan_select_state_unselected)
    val shape = RoundedCornerShape(7.dp)
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(shape)
            .background(if (checked) colors.primary else Color.Transparent)
            .border(
                width = 1.5.dp,
                color = if (checked) colors.primary else colors.textMuted.copy(alpha = 0.55f),
                shape = shape,
            )
            .clickable(
                enabled = enabled,
                role = Role.Checkbox,
                onClick = onClick,
            )
            .semantics {
                if (label != null) contentDescription = label
                role = Role.Checkbox
                stateDescription = if (checked) selectedState else unselectedState
            },
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = colors.onAccent,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun AiPlanImportFooter(
    selected: Int,
    total: Int,
    importing: Boolean,
    emptySelection: Boolean,
    importError: Boolean,
    onImport: () -> Unit,
    onDiscard: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    Column {
        if (emptySelection) {
            Text(
                text = stringResource(R.string.ai_plan_empty_selection),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = colors.warning,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        if (importError) {
            Text(
                text = stringResource(R.string.ai_plan_import_failed),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = colors.danger,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        OrbitPrimaryButton(
            text = if (importing) {
                stringResource(R.string.ai_plan_importing)
            } else {
                stringResource(R.string.ai_plan_import, selected)
            },
            onClick = onImport,
            enabled = !importing && selected > 0,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.ai_plan_discard),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = colors.textSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        enabled = !importing,
                        role = Role.Button,
                        onClick = onDiscard,
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
        Text(
            text = stringResource(R.string.ai_plan_selected_count, selected, total),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontFeatureSettings = "tnum",
            ),
            color = colors.textMuted,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 2.dp),
        )
    }
}

@Composable
private fun AiFailureCard(message: String) {
    val colors = MaterialTheme.orbitColors
    OrbitCard(padding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(colors.danger),
            )
            Spacer(Modifier.width(9.dp))
            Column {
                Text(
                    text = stringResource(R.string.ai_plan_error_title),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = colors.danger,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                    ),
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiPlanDatePicker(
    initialEpochDay: Long,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = TaskDate.datePickerMillis(initialEpochDay),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                colors = ButtonDefaults.textButtonColors(contentColor = colors.accentLine),
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onPick(TaskDate.epochDayFromDatePickerMillis(millis))
                    } ?: onDismiss()
                },
            ) {
                Text(stringResource(R.string.date_picker_confirm))
            }
        },
        dismissButton = {
            TextButton(
                colors = ButtonDefaults.textButtonColors(contentColor = colors.textSecondary),
                onClick = onDismiss,
            ) {
                Text(stringResource(R.string.date_picker_cancel))
            }
        },
        colors = DatePickerDefaults.colors(
            containerColor = colors.card,
            titleContentColor = colors.textPrimary,
            headlineContentColor = colors.textPrimary,
            weekdayContentColor = colors.textSecondary,
            subheadContentColor = colors.textSecondary,
            navigationContentColor = colors.textSecondary,
            yearContentColor = colors.textPrimary,
            currentYearContentColor = colors.accentLine,
            selectedYearContentColor = colors.onAccent,
            selectedYearContainerColor = colors.primary,
            dayContentColor = colors.textPrimary,
            selectedDayContentColor = colors.onAccent,
            selectedDayContainerColor = colors.primary,
            todayDateBorderColor = colors.accentLine,
            todayContentColor = colors.accentLine,
            dividerColor = colors.cardBorder,
        ),
    ) {
        DatePicker(
            state = datePickerState,
            title = {
                Text(
                    text = stringResource(R.string.ai_plan_form_target_label),
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                )
            },
            showModeToggle = false,
        )
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF101010, widthDp = 393, heightDp = 852)
@Composable
private fun AiPlanScreenPreview() {
    val today = TaskDate.todayEpochDay()
    val plan = AiPlan(
        goalTitle = "三个月掌握 Android 开发",
        goalDescription = "从语法基础走到独立完成一个可上架的应用。",
        targetDateEpochDay = today + 84,
        stages = listOf(
            AiPlanStage(
                title = "Kotlin 与 Compose 基础",
                targetDateEpochDay = today + 28,
                tasks = listOf(
                    AiPlanTask(
                        title = "完成 Kotlin 语法练习",
                        dueDateEpochDay = today + 5,
                        priority = "HIGH",
                        description = "每天 45 分钟，重点掌握集合与空安全。",
                    ),
                    AiPlanTask(
                        title = "复刻一个 Compose 列表页",
                        dueDateEpochDay = today + 10,
                        priority = "MEDIUM",
                    ),
                ),
            ),
            AiPlanStage(
                title = "接口联调与本地存储",
                targetDateEpochDay = today + 56,
                tasks = listOf(
                    AiPlanTask(
                        title = "用 Retrofit 接入公开 API",
                        dueDateEpochDay = today + 35,
                        priority = "MEDIUM",
                    ),
                ),
            ),
        ),
    )
    OrbitTheme {
        AiPlanScreen(
            uiState = AiPlanUiState(
                phase = AiPlanPhase.PREVIEW,
                plan = plan,
                status = com.example.orbit.ai.AiStatus(configured = true),
            ),
            onBack = {},
            onGoalChange = {},
            onTargetDateChange = {},
            onDailyMinutesChange = {},
            onCurrentLevelChange = {},
            onWeakPointsChange = {},
            onGenerate = {},
            onToggleStage = {},
            onToggleTask = { _, _ -> },
            onImport = {},
            onDiscard = {},
            onRetryStatus = {},
        )
    }
}
