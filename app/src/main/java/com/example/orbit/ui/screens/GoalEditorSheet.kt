package com.example.orbit.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbit.R
import com.example.orbit.model.AccentTone
import com.example.orbit.model.GoalStatus
import com.example.orbit.model.TaskDate
import com.example.orbit.ui.GoalEditorActionError
import com.example.orbit.ui.GoalEditorState
import com.example.orbit.ui.components.OrbitIconButton
import com.example.orbit.ui.components.OrbitNote
import com.example.orbit.ui.components.OrbitPrimaryButton
import com.example.orbit.ui.theme.OrbitCorners
import com.example.orbit.ui.theme.accentPreviewColors
import com.example.orbit.ui.theme.orbitColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GoalEditorSheet(
    editor: GoalEditorState,
    onDismiss: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onToneChange: (AccentTone) -> Unit,
    onStatusChange: (GoalStatus) -> Unit,
    onTargetDateChange: (Long?) -> Unit,
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
                            if (editor.isEditing) {
                                R.string.goal_editor_edit_title
                            } else {
                                R.string.goal_editor_create_title
                            },
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.textPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            if (editor.isEditing) {
                                R.string.goal_editor_edit_subtitle
                            } else {
                                R.string.goal_editor_create_subtitle
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = colors.textMuted,
                    )
                }
                Spacer(Modifier.width(12.dp))
                OrbitIconButton(
                    onClick = dismissWithAnimation,
                    contentDescription = stringResource(R.string.goal_editor_close),
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
                        text = stringResource(R.string.goal_editor_title_label),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.goal_editor_title_placeholder),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = colors.textMuted,
                    )
                },
                supportingText = if (editor.titleError) {
                    {
                        Text(
                            text = stringResource(R.string.goal_editor_title_required),
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
                        text = stringResource(R.string.goal_editor_desc_label),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.goal_editor_desc_placeholder),
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
            SheetSectionLabel(text = stringResource(R.string.goal_editor_tone_label))
            Spacer(Modifier.height(9.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AccentTone.entries.forEach { tone ->
                    ChoicePill(
                        label = stringResource(tone.labelRes),
                        selected = editor.tone == tone,
                        accent = accentPreviewColors(tone, colors.isDark).first,
                        enabled = inputsEnabled,
                        onClick = { onToneChange(tone) },
                    )
                }
            }

            if (editor.isEditing) {
                Spacer(Modifier.height(20.dp))
                SheetSectionLabel(text = stringResource(R.string.goal_editor_status_label))
                Spacer(Modifier.height(9.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GoalStatus.entries.forEach { status ->
                        ChoicePill(
                            label = stringResource(status.labelRes),
                            selected = editor.status == status,
                            accent = goalStatusColor(status),
                            enabled = inputsEnabled,
                            onClick = { onStatusChange(status) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SheetSectionLabel(text = stringResource(R.string.goal_editor_target_label))
            Spacer(Modifier.height(9.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val isCustomDate = editor.targetDate != null &&
                    editor.targetDate != today &&
                    editor.targetDate != today + 1
                val customLabel = if (isCustomDate) {
                    TaskDate.monthDayLabel(requireNotNull(editor.targetDate))
                } else {
                    stringResource(R.string.due_pick)
                }

                ChoicePill(
                    label = stringResource(R.string.due_none),
                    selected = editor.targetDate == null,
                    accent = colors.textSecondary,
                    enabled = inputsEnabled,
                    onClick = { onTargetDateChange(null) },
                )
                ChoicePill(
                    label = stringResource(R.string.due_today),
                    selected = editor.targetDate == today,
                    accent = colors.primary,
                    enabled = inputsEnabled,
                    onClick = { onTargetDateChange(today) },
                )
                ChoicePill(
                    label = stringResource(R.string.due_tomorrow),
                    selected = editor.targetDate == today + 1,
                    accent = colors.primary,
                    enabled = inputsEnabled,
                    onClick = { onTargetDateChange(today + 1) },
                )
                ChoicePill(
                    label = customLabel,
                    selected = isCustomDate,
                    accent = colors.primary,
                    enabled = inputsEnabled,
                    onClick = { showDatePicker = true },
                )
            }

            Spacer(Modifier.height(18.dp))
            editor.actionError?.let { error ->
                Text(
                    text = stringResource(
                        when (error) {
                            GoalEditorActionError.SAVE_FAILED -> R.string.goal_save_failed
                            GoalEditorActionError.DELETE_FAILED -> R.string.goal_delete_failed
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = colors.danger,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                Spacer(Modifier.height(8.dp))
            }

            OrbitNote(text = stringResource(R.string.goal_editor_note))

            Spacer(Modifier.height(18.dp))
            OrbitPrimaryButton(
                text = stringResource(
                    when {
                        editor.isSaving -> R.string.goal_editor_saving
                        editor.isEditing -> R.string.goal_editor_save
                        else -> R.string.goal_editor_create
                    },
                ),
                onClick = onSave,
                enabled = inputsEnabled,
                icon = if (editor.isEditing) Icons.Filled.Check else Icons.Filled.Add,
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
                            if (editor.isDeleting) {
                                R.string.goal_editor_deleting
                            } else {
                                R.string.goal_editor_delete
                            },
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }

    if (showDatePicker) {
        val initialSelection = TaskDate.datePickerMillis(editor.targetDate ?: today)
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
                            onTargetDateChange(
                                TaskDate.epochDayFromDatePickerMillis(millis),
                            )
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
                        text = stringResource(R.string.goal_editor_target_label),
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
                    text = stringResource(R.string.goal_editor_delete_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.goal_editor_delete_message,
                        editor.title,
                    ),
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
