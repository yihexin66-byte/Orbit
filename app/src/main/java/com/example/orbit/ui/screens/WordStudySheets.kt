package com.example.orbit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbit.R
import com.example.orbit.learning.EnglishWord
import com.example.orbit.learning.LearningBrowse
import com.example.orbit.learning.LearningLibrary
import com.example.orbit.learning.WordStudyPlanner
import com.example.orbit.ui.WordStudyActions
import com.example.orbit.ui.WordStudyState
import com.example.orbit.ui.components.GlassDivider
import com.example.orbit.ui.components.OrbitIconButton
import com.example.orbit.ui.components.OrbitPrimaryButton
import com.example.orbit.ui.components.OrbitProgressBar
import com.example.orbit.ui.components.OrbitButtonStyle
import com.example.orbit.ui.theme.OrbitCorners
import com.example.orbit.ui.theme.orbitColors

const val WordStudySessionTag = "word_study_session"
const val WordStudyCloseTag = "word_study_close"
const val WordStudyRevealTag = "word_study_reveal"
const val WordStudyKnownTag = "word_study_known"
const val WordStudyUnknownTag = "word_study_unknown"
const val WordStudySummaryTag = "word_study_summary"
const val WordStudyRetryWrongTag = "word_study_retry_wrong"
const val WordStudyNextRoundTag = "word_study_next_round"
const val WordStudyFinishTag = "word_study_finish"
const val WordPickerTag = "word_picker"
const val WordPickerSearchTag = "word_picker_search"
const val WordPickerClearSearchTag = "word_picker_search_clear"
const val WordPickerStartTag = "word_picker_start"
const val WordPickerCloseTag = "word_picker_close"
const val WordPickerRowTagPrefix = "word_picker_row_"

internal fun wordPickerRowTag(wordId: String): String = WordPickerRowTagPrefix + wordId

/**
 * Full-height flip-card session: recall first, reveal, then self-assess. The
 * summary can replay only the wrong words, which is the retry loop the user
 * asked for without importing a full SRS scheduler.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordStudySheet(
    state: WordStudyState,
    today: Long,
    actions: WordStudyActions,
    onDismiss: () -> Unit,
) {
    val session = state.session ?: return
    val colors = MaterialTheme.orbitColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = false,
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
                .fillMaxHeight(0.92f)
                .testTag(WordStudySessionTag)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.word_study_eyebrow),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.5.sp,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = colors.accentLine,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.word_study_section),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                }
                Text(
                    text = stringResource(
                        R.string.word_study_today_progress,
                        state.progress.studiedOn(today),
                        state.progress.dailyTarget,
                    ),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        letterSpacing = 0.sp,
                    ),
                    color = colors.textMuted,
                )
                Spacer(Modifier.width(8.dp))
                OrbitIconButton(
                    onClick = onDismiss,
                    contentDescription = stringResource(R.string.word_study_close),
                    icon = Icons.Filled.Close,
                    size = 38.dp,
                    modifier = Modifier.testTag(WordStudyCloseTag),
                )
            }
            Spacer(Modifier.height(16.dp))
            if (session.isComplete) {
                StudySummary(
                    state = state,
                    today = today,
                    actions = actions,
                    onDismiss = onDismiss,
                )
            } else {
                StudyCard(session = session, actions = actions)
            }
        }
    }
}

@Composable
private fun ColumnScope.StudyCard(
    session: com.example.orbit.learning.WordStudySession,
    actions: WordStudyActions,
) {
    val colors = MaterialTheme.orbitColors
    val word = session.current ?: return
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(
                R.string.word_study_progress,
                session.index + 1,
                session.words.size,
            ),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.sp,
                fontFeatureSettings = "tnum",
            ),
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(
                R.string.word_study_remaining,
                session.words.size - session.answeredCount,
            ),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 0.sp,
            ),
            color = colors.textMuted,
        )
    }
    Spacer(Modifier.height(8.dp))
    OrbitProgressBar(
        progress = if (session.words.isEmpty()) 0f
        else session.answeredCount.toFloat() / session.words.size,
    )
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(26.dp))
        Text(
            text = word.word,
            style = MaterialTheme.typography.displayMedium.copy(
                fontSize = 42.sp,
                lineHeight = 50.sp,
            ),
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = word.phonetic,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                lineHeight = 19.sp,
            ),
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(26.dp))
        if (!session.revealed) {
            Text(
                text = stringResource(R.string.word_study_card_hint),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    lineHeight = 18.sp,
                ),
                color = colors.textMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(26.dp))
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.12f))
                    .border(1.dp, colors.primary.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "?",
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 38.sp),
                    fontWeight = FontWeight.Bold,
                    color = colors.accentLine,
                )
            }
        } else {
            Text(
                text = word.meaning,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 19.sp,
                    lineHeight = 28.sp,
                ),
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            if (word.example.isNotBlank()) {
                Spacer(Modifier.height(20.dp))
                GlassDivider()
                Spacer(Modifier.height(16.dp))
                Text(
                    text = word.example,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        fontStyle = FontStyle.Italic,
                    ),
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                if (word.exampleZh.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = word.exampleZh,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            lineHeight = 18.sp,
                        ),
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            if (word.tip.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = word.tip,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        lineHeight = 16.sp,
                        letterSpacing = 0.sp,
                    ),
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(28.dp))
    }
    if (!session.revealed) {
        OrbitPrimaryButton(
            text = stringResource(R.string.word_study_reveal),
            onClick = actions.onReveal,
            modifier = Modifier.testTag(WordStudyRevealTag),
        )
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            UnknownButton(
                modifier = Modifier
                    .weight(1f)
                    .testTag(WordStudyUnknownTag),
                onClick = { actions.onAnswer(false) },
            )
            OrbitPrimaryButton(
                text = stringResource(R.string.word_study_known),
                onClick = { actions.onAnswer(true) },
                modifier = Modifier
                    .weight(1f)
                    .testTag(WordStudyKnownTag),
            )
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun UnknownButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    val shape = RoundedCornerShape(OrbitCorners.inner)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(colors.warning.copy(alpha = 0.12f))
            .border(1.dp, colors.warning.copy(alpha = 0.45f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.word_study_unknown),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
            fontWeight = FontWeight.Bold,
            color = if (colors.isDark) colors.warning else colors.textPrimary,
        )
    }
}

@Composable
private fun ColumnScope.StudySummary(
    state: WordStudyState,
    today: Long,
    actions: WordStudyActions,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    val session = state.session ?: return
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .testTag(WordStudySummaryTag),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.word_study_summary_title),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = colors.accentLine,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "${session.accuracyPercent}%",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 66.sp,
                    lineHeight = 72.sp,
                    fontFeatureSettings = "tnum",
                ),
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.word_study_summary_accuracy),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    lineHeight = 17.sp,
                ),
                color = colors.textMuted,
            )
            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryStat(
                    value = session.knownCount.toString(),
                    label = stringResource(R.string.word_study_summary_known),
                    modifier = Modifier.weight(1f),
                )
                SummaryStat(
                    value = session.unknownCount.toString(),
                    label = stringResource(R.string.word_study_summary_unknown),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(18.dp))
            GlassDivider()
            Spacer(Modifier.height(14.dp))
            Text(
                text = if (session.unknownCount == 0) {
                    stringResource(R.string.word_study_summary_perfect)
                } else {
                    stringResource(R.string.word_study_summary_retry_hint)
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    lineHeight = 18.sp,
                ),
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(
                    R.string.word_study_today_progress,
                    state.progress.studiedOn(today),
                    state.progress.dailyTarget,
                ),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    letterSpacing = 0.sp,
                ),
                color = colors.textMuted,
            )
            Spacer(Modifier.height(24.dp))
        }
        if (session.unknownCount > 0) {
            OrbitPrimaryButton(
                text = stringResource(
                    R.string.word_study_retry_wrong,
                    session.unknownCount,
                ),
                onClick = actions.onRetryWrong,
                modifier = Modifier.testTag(WordStudyRetryWrongTag),
            )
            Spacer(Modifier.height(10.dp))
        }
        OrbitPrimaryButton(
            text = stringResource(R.string.word_study_next_round),
            onClick = actions.onNextRound,
            modifier = Modifier.testTag(WordStudyNextRoundTag),
            style = OrbitButtonStyle.QUIET,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(OrbitCorners.pill))
                .clickable(role = Role.Button) {
                    actions.onFinish()
                    onDismiss()
                }
                .testTag(WordStudyFinishTag),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.word_study_finish),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.5.sp),
                color = colors.textMuted,
            )
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun SummaryStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 24.sp,
                lineHeight = 29.sp,
                fontFeatureSettings = "tnum",
            ),
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                letterSpacing = 0.sp,
            ),
            color = colors.textMuted,
        )
    }
}

/**
 * Manual word selection: search the full book and pick up to 50. Selection is
 * kept in book order by the planner, so the round still reads front-to-back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordPickerSheet(
    onDismiss: () -> Unit,
    onStart: (Set<String>) -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var limitHit by remember { mutableStateOf(false) }
    val filtered = remember(query) {
        LearningBrowse.searchWords(LearningLibrary.english, query)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = false,
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
                .fillMaxHeight(0.94f)
                .testTag(WordPickerTag)
                .navigationBarsPadding()
                .padding(horizontal = 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.word_picker_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = stringResource(R.string.word_picker_subtitle),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.5.sp,
                            letterSpacing = 0.sp,
                        ),
                        color = colors.textMuted,
                    )
                }
                OrbitIconButton(
                    onClick = onDismiss,
                    contentDescription = stringResource(R.string.word_picker_close),
                    icon = Icons.Filled.Close,
                    size = 38.dp,
                    modifier = Modifier.testTag(WordPickerCloseTag),
                )
            }
            Spacer(Modifier.height(12.dp))
            WordSearchField(
                query = query,
                onQueryChange = { query = it },
                onClear = { query = "" },
            )
            Spacer(Modifier.height(9.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.word_picker_selected, selected.size),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.sp,
                        fontFeatureSettings = "tnum",
                    ),
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.weight(1f))
                if (limitHit) {
                    Text(
                        text = stringResource(R.string.word_picker_limit),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            letterSpacing = 0.sp,
                        ),
                        color = colors.warning,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            if (filtered.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.word_picker_empty),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.word_picker_empty_hint),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = colors.textMuted,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(filtered, key = { it.id }) { word ->
                        WordPickRow(
                            word = word,
                            selected = word.id in selected,
                            onClick = {
                                limitHit = false
                                selected = if (word.id in selected) {
                                    selected - word.id
                                } else if (selected.size >= WordStudyPlanner.MAX_TARGET) {
                                    limitHit = true
                                    selected
                                } else {
                                    selected + word.id
                                }
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            GlassDivider()
            Spacer(Modifier.height(10.dp))
            OrbitPrimaryButton(
                text = stringResource(R.string.word_picker_start, selected.size),
                onClick = { onStart(selected) },
                enabled = selected.isNotEmpty(),
                modifier = Modifier.testTag(WordPickerStartTag),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun WordPickRow(
    word: EnglishWord,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    val shape = RoundedCornerShape(OrbitCorners.control)
    val stateText = stringResource(
        if (selected) R.string.word_picker_selected_state
        else R.string.word_picker_unselected_state,
    )
    val description = stringResource(
        R.string.word_picker_row_meta,
        word.word,
        word.meaning,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) colors.primary.copy(alpha = 0.10f)
                else colors.cardElevated.copy(alpha = 0.55f),
            )
            .selectable(
                selected = selected,
                role = Role.Checkbox,
                onClick = onClick,
            )
            .semantics {
                contentDescription = description
                stateDescription = stateText
            }
            .testTag(wordPickerRowTag(word.id))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = word.word,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                    ),
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = word.phonetic,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        letterSpacing = 0.sp,
                    ),
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = word.meaning,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                ),
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) colors.primary else colors.track)
                .border(
                    width = 1.dp,
                    color = if (selected) colors.primary else colors.cardBorder,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = colors.onAccent,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

@Composable
private fun WordSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(OrbitCorners.pill)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            color = colors.textPrimary,
        ),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(WordPickerSearchTag)
            .onFocusChanged { focused = it.isFocused },
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(colors.cardElevated)
                    .border(1.dp, if (focused) colors.primary else colors.cardBorder, shape)
                    .heightIn(min = 46.dp)
                    .padding(start = 13.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isBlank()) {
                        Text(
                            text = stringResource(R.string.word_picker_search_hint),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                            ),
                            color = colors.textMuted,
                        )
                    }
                    innerTextField()
                }
                if (query.isNotBlank()) {
                    val clearLabel = stringResource(R.string.word_picker_clear_search)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(OrbitCorners.pill))
                            .clickable(role = Role.Button, onClick = onClear)
                            .semantics { contentDescription = clearLabel }
                            .testTag(WordPickerClearSearchTag),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
        },
    )
}
