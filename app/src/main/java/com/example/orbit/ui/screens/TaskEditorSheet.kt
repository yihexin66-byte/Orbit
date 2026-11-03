package com.example.orbit.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbit.R
import com.example.orbit.model.Goal
import com.example.orbit.model.GoalStage
import com.example.orbit.model.TaskCategory
import com.example.orbit.model.TaskDate
import com.example.orbit.model.TaskPriority
import com.example.orbit.ui.TaskEditorActionError
import com.example.orbit.ui.TaskEditorState
import com.example.orbit.ui.components.OrbitIconButton
import com.example.orbit.ui.components.OrbitNote
import com.example.orbit.ui.components.OrbitPrimaryButton
import com.example.orbit.ui.components.categoryColor
import com.example.orbit.ui.components.priorityColor
import com.example.orbit.ui.theme.OrbitCorners
import com.example.orbit.ui.theme.accentPreviewColors
import com.example.orbit.ui.theme.orbitColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorSheet(
    editor: TaskEditorState,
    onDismiss: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit,
    onCategoryChange: (TaskCategory) -> Unit,
    onDueDateChange: (Long?) -> Unit,
    goals: List<Goal> = emptyList(),
    stages: List<GoalStage> = emptyList(),
    onLinkChange: (Long?, Long?) -> Unit = { _, _ -> },
    onSave: () -> Unit,
    onDeleteRequest: () -> Unit,
    onDeleteDismiss: () -> Unit,
    onDeleteConfirm: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val today = remember { TaskDate.todayEpochDay() }
    val inputsEnabled = !editor.isSaving && !editor.isDeleting
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val dismissWithAnimation: () -> Unit = {
        scope.launch {
            sheetState.hide()
            onDismiss()
        }
        Unit
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.card,
        scrimColor = colors.scrim.copy(alpha = 0.78f),
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 10.dp)
                    .width(36.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(OrbitCorners.pill))
                    .background(colors.textMuted.copy(alpha = 0.42f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            if (editor.isEditing) R.string.add_edit_title else R.string.add_title,
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.textPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            if (editor.isEditing) R.string.add_edit_subtitle else R.string.add_subtitle,
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = colors.textMuted,
                    )
                }
                Spacer(Modifier.width(12.dp))
                OrbitIconButton(
                    onClick = dismissWithAnimation,
                    contentDescription = stringResource(R.string.content_desc_close),
                    icon = Icons.Filled.Close,
                )
            }

            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = editor.title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = inputsEnabled,
                isError = editor.titleError,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = colors.textPrimary,
                ),
                label = {
                    Text(
                        text = stringResource(R.string.add_field_label),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.add_field_placeholder),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = colors.textMuted,
                    )
                },
                supportingText = if (editor.titleError) {
                    {
                        Text(
                            text = stringResource(R.string.task_title_required),
                            color = colors.danger,
                        )
                    }
                } else {
                    null
                },
                singleLine = true,
                shape = RoundedCornerShape(OrbitCorners.inner),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                colors = taskFieldColors(),
            )

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = editor.description,
                onValueChange = onDescriptionChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = inputsEnabled,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = colors.textPrimary,
                ),
                label = {
                    Text(
                        text = stringResource(R.string.add_description_label),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.add_description_placeholder),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = colors.textMuted,
                    )
                },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(OrbitCorners.inner),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                colors = taskFieldColors(),
            )

            Spacer(Modifier.height(20.dp))
            SheetSectionLabel(text = stringResource(R.string.add_priority_label))
            Spacer(Modifier.height(9.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TaskPriority.entries.forEach { option ->
                    ChoicePill(
                        label = stringResource(option.labelRes),
                        selected = editor.priority == option,
                        accent = priorityColor(option),
                        enabled = inputsEnabled,
                        onClick = { onPriorityChange(option) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            SheetSectionLabel(text = stringResource(R.string.add_category_label))
            Spacer(Modifier.height(9.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TaskCategory.entries.forEach { option ->
                    ChoicePill(
                        label = stringResource(option.labelRes),
                        selected = editor.category == option,
                        accent = categoryColor(option),
                        enabled = inputsEnabled,
                        onClick = { onCategoryChange(option) },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            SheetSectionLabel(text = stringResource(R.string.add_schedule_label))
            Spacer(Modifier.height(9.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val isCustomDate = editor.dueDate != null &&
                    editor.dueDate != today &&
                    editor.dueDate != today + 1
                val customLabel = if (isCustomDate) {
                    TaskDate.monthDayLabel(requireNotNull(editor.dueDate))
                } else {
                    stringResource(R.string.due_pick)
                }

                ChoicePill(
                    label = stringResource(R.string.due_none),
                    selected = editor.dueDate == null,
                    accent = colors.textSecondary,
                    enabled = inputsEnabled,
                    onClick = { onDueDateChange(null) },
                )
                ChoicePill(
                    label = stringResource(R.string.due_today),
                    selected = editor.dueDate == today,
                    accent = colors.primary,
                    enabled = inputsEnabled,
                    onClick = { onDueDateChange(today) },
                )
                ChoicePill(
                    label = stringResource(R.string.due_tomorrow),
                    selected = editor.dueDate == today + 1,
                    accent = colors.primary,
                    enabled = inputsEnabled,
                    onClick = { onDueDateChange(today + 1) },
                )
                ChoicePill(
                    label = customLabel,
                    selected = isCustomDate,
                    accent = colors.primary,
                    enabled = inputsEnabled,
                    onClick = { showDatePicker = true },
                )
            }

            if (goals.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                SheetSectionLabel(text = stringResource(R.string.task_editor_link_label))
                Spacer(Modifier.height(9.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ChoicePill(
                        label = stringResource(R.string.task_editor_link_none),
                        selected = editor.goalId == null,
                        accent = colors.textSecondary,
                        enabled = inputsEnabled,
                        onClick = { onLinkChange(null, null) },
                    )
                    goals.forEach { goal ->
                        ChoicePill(
                            label = goal.title,
                            selected = editor.goalId == goal.id,
                            accent = accentPreviewColors(goal.tone, colors.isDark).first,
                            enabled = inputsEnabled,
                            onClick = { onLinkChange(goal.id, null) },
                        )
                    }
                }

                val selectedGoalId = editor.goalId
                if (selectedGoalId != null) {
                    val goalStages = stages
                        .filter { it.goalId == selectedGoalId }
                        .sortedBy { it.position }
                    Spacer(Modifier.height(18.dp))
                    SheetSectionLabel(text = stringResource(R.string.task_editor_stage_label))
                    Spacer(Modifier.height(9.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ChoicePill(
                            label = stringResource(R.string.task_editor_stage_none),
                            selected = editor.stageId == null,
                            accent = colors.textSecondary,
                            enabled = inputsEnabled,
                            onClick = { onLinkChange(selectedGoalId, null) },
                        )
                        goalStages.forEach { stage ->
                            ChoicePill(
                                label = stage.title,
                                selected = editor.stageId == stage.id,
                                accent = colors.secondary,
                                enabled = inputsEnabled,
                                onClick = { onLinkChange(selectedGoalId, stage.id) },
                            )
                        }
                    }
                    if (goalStages.isEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.task_editor_stage_empty),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = colors.textMuted,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                OrbitNote(text = stringResource(R.string.task_editor_link_note))
            }

            Spacer(Modifier.height(18.dp))
            editor.actionError?.let { error ->
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(
                        when (error) {
                            TaskEditorActionError.SAVE_FAILED -> R.string.task_save_failed
                            TaskEditorActionError.DELETE_FAILED -> R.string.task_delete_failed
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = colors.danger,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                Spacer(Modifier.height(8.dp))
            }

            OrbitNote(text = stringResource(R.string.add_note))

            Spacer(Modifier.height(18.dp))
            OrbitPrimaryButton(
                text = stringResource(
                    when {
                        editor.isSaving -> R.string.add_saving
                        editor.isEditing -> R.string.add_edit_confirm
                        else -> R.string.add_confirm
                    },
                ),
                onClick = onSave,
                enabled = inputsEnabled,
            )

            if (editor.isEditing) {
                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = onDeleteRequest,
                    enabled = inputsEnabled,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colors.danger,
                        disabledContentColor = colors.textMuted,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(
                            if (editor.isDeleting) R.string.editor_deleting else R.string.editor_delete,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }

    if (showDatePicker) {
        val initialSelection = TaskDate.datePickerMillis(editor.dueDate ?: today)
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialSelection,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colors.accentLine,
                    ),
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDueDateChange(TaskDate.epochDayFromDatePickerMillis(millis))
                        }
                        showDatePicker = false
                    },
                ) {
                    Text(stringResource(R.string.date_picker_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colors.textSecondary,
                    ),
                    onClick = { showDatePicker = false },
                ) {
                    Text(stringResource(R.string.date_picker_cancel))
                }
            },
            colors = androidx.compose.material3.DatePickerDefaults.colors(
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
                        text = stringResource(R.string.due_pick),
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                    )
                },
                showModeToggle = false,
            )
        }
    }

    if (editor.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = onDeleteDismiss,
            containerColor = colors.cardElevated,
            shape = RoundedCornerShape(26.dp),
            tonalElevation = 0.dp,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textSecondary,
            title = {
                Text(
                    text = stringResource(R.string.delete_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_message, editor.title),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onDeleteConfirm,
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.danger),
                ) {
                    Text(stringResource(R.string.delete_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDeleteDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.textSecondary),
                ) {
                    Text(stringResource(R.string.delete_cancel))
                }
            },
        )
    }
}

@Composable
internal fun taskFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.orbitColors.accentLine,
    unfocusedBorderColor = MaterialTheme.orbitColors.cardBorder,
    focusedLabelColor = MaterialTheme.orbitColors.accentLine,
    unfocusedLabelColor = MaterialTheme.orbitColors.textMuted,
    cursorColor = MaterialTheme.orbitColors.accentLine,
    focusedContainerColor = MaterialTheme.orbitColors.cardElevated,
    unfocusedContainerColor = MaterialTheme.orbitColors.cardElevated,
    disabledContainerColor = MaterialTheme.orbitColors.cardElevated,
    errorBorderColor = MaterialTheme.orbitColors.danger,
    errorLabelColor = MaterialTheme.orbitColors.danger,
    errorCursorColor = MaterialTheme.orbitColors.danger,
    focusedTextColor = MaterialTheme.orbitColors.textPrimary,
    unfocusedTextColor = MaterialTheme.orbitColors.textPrimary,
    disabledTextColor = MaterialTheme.orbitColors.textMuted,
    focusedPlaceholderColor = MaterialTheme.orbitColors.textMuted,
    unfocusedPlaceholderColor = MaterialTheme.orbitColors.textMuted,
)

@Composable
internal fun SheetSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            letterSpacing = 1.4.sp,
            fontWeight = FontWeight.Medium,
        ),
        color = MaterialTheme.orbitColors.textMuted,
    )
}

@Composable
internal fun ChoicePill(
    label: String,
    selected: Boolean,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    val shape = RoundedCornerShape(12.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "choicePillPress",
    )
    val fill = when {
        selected -> colors.primary
        pressed && enabled -> accent.copy(alpha = 0.10f)
        else -> Color.Transparent
    }
    Box(
        modifier = modifier
            .height(48.dp)
            .alpha(if (enabled) 1f else 0.55f)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(shape)
            .background(fill)
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier.border(1.dp, colors.cardBorder, shape)
                },
            )
            .selectable(
                selected = selected,
                enabled = enabled,
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
            color = if (selected) colors.onAccent else colors.textSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}
