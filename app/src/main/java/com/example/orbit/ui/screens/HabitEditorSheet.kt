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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbit.R
import com.example.orbit.model.AccentTone
import com.example.orbit.ui.HabitEditorActionError
import com.example.orbit.ui.HabitEditorState
import com.example.orbit.ui.components.OrbitIconButton
import com.example.orbit.ui.components.OrbitNote
import com.example.orbit.ui.components.OrbitPrimaryButton
import com.example.orbit.ui.theme.OrbitCorners
import com.example.orbit.ui.theme.accentPreviewColors
import com.example.orbit.ui.theme.orbitColors
import kotlinx.coroutines.launch

/** Stable selector for the habit editor primary action in UI tests. */
internal const val HABIT_EDITOR_SAVE_TAG = "habit_editor_save"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitEditorSheet(
    editor: HabitEditorState,
    onDismiss: () -> Unit,
    onTitleChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onToneChange: (AccentTone) -> Unit,
    onArchiveChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDeleteRequest: () -> Unit,
    onDeleteDismiss: () -> Unit,
    onDeleteConfirm: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val inputsEnabled = !editor.isSaving && !editor.isDeleting

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
                                R.string.habit_editor_edit_title
                            } else {
                                R.string.habit_editor_create_title
                            },
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.textPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            if (editor.isEditing) {
                                R.string.habit_editor_edit_subtitle
                            } else {
                                R.string.habit_editor_create_subtitle
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = colors.textMuted,
                    )
                }
                Spacer(Modifier.width(12.dp))
                OrbitIconButton(
                    onClick = dismissWithAnimation,
                    contentDescription = stringResource(R.string.habit_editor_close),
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
                        text = stringResource(R.string.habit_editor_title_label),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.habit_editor_title_placeholder),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = colors.textMuted,
                    )
                },
                supportingText = if (editor.titleError) {
                    {
                        Text(
                            text = stringResource(R.string.habit_editor_title_required),
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
                value = editor.note,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = inputsEnabled,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = colors.textPrimary,
                ),
                label = {
                    Text(
                        text = stringResource(R.string.habit_editor_note_label),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.habit_editor_note_placeholder),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = colors.textMuted,
                    )
                },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(OrbitCorners.inner),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                colors = taskFieldColors(),
            )

            Spacer(Modifier.height(20.dp))
            SheetSectionLabel(text = stringResource(R.string.habit_editor_tone_label))
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

            Spacer(Modifier.height(18.dp))
            editor.actionError?.let { error ->
                Text(
                    text = stringResource(
                        when (error) {
                            HabitEditorActionError.SAVE_FAILED -> R.string.habit_save_failed
                            HabitEditorActionError.DELETE_FAILED -> R.string.habit_delete_failed
                            HabitEditorActionError.ARCHIVE_FAILED -> R.string.habit_archive_failed
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = colors.danger,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                Spacer(Modifier.height(8.dp))
            }

            OrbitNote(text = stringResource(R.string.habit_editor_note))

            Spacer(Modifier.height(18.dp))
            OrbitPrimaryButton(
                text = stringResource(
                    when {
                        editor.isSaving -> R.string.habit_editor_saving
                        editor.isEditing -> R.string.habit_editor_save
                        else -> R.string.habit_editor_create
                    },
                ),
                onClick = onSave,
                enabled = inputsEnabled,
                icon = if (editor.isEditing) Icons.Filled.Check else Icons.Filled.Add,
                modifier = Modifier.testTag(HABIT_EDITOR_SAVE_TAG),
            )

            if (editor.isEditing) {
                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = { onArchiveChange(!editor.isArchived) },
                    enabled = inputsEnabled,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colors.primary,
                        disabledContentColor = colors.textMuted,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(
                            if (editor.isArchived) {
                                R.string.habit_editor_restore
                            } else {
                                R.string.habit_editor_archive
                            },
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
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
                                R.string.habit_editor_deleting
                            } else {
                                R.string.habit_editor_delete
                            },
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
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
                    text = stringResource(R.string.habit_editor_delete_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.habit_editor_delete_message,
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
