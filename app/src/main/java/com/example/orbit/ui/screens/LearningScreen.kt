package com.example.orbit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbit.R
import com.example.orbit.learning.EnglishWord
import com.example.orbit.learning.LearningBrowse
import com.example.orbit.learning.LearningKind
import com.example.orbit.learning.LearningLibrary
import com.example.orbit.learning.LearningProgress
import com.example.orbit.learning.MathTopic
import com.example.orbit.learning.Poem
import com.example.orbit.model.TaskDate
import com.example.orbit.ui.LearningActions
import com.example.orbit.ui.WordStudyActions
import com.example.orbit.ui.WordStudyState
import com.example.orbit.ui.components.GlassDivider
import com.example.orbit.ui.components.OrbitBadge
import com.example.orbit.ui.components.OrbitCard
import com.example.orbit.ui.components.OrbitLazyScreen
import com.example.orbit.ui.components.OrbitPrimaryButton
import com.example.orbit.ui.components.OrbitProgressBar
import com.example.orbit.ui.components.OrbitScreenHeader
import com.example.orbit.ui.components.OrbitSectionTitle
import com.example.orbit.ui.components.OrbitButtonStyle
import com.example.orbit.ui.components.TagPill
import com.example.orbit.ui.theme.OrbitCorners
import com.example.orbit.ui.theme.orbitColors

const val LearningScreenTag = "learning_screen"
const val LearningTodaySectionTag = "learning_today_section"
const val LearningShelfPoemTag = "learning_shelf_poem"
const val LearningShelfEnglishTag = "learning_shelf_english"
const val LearningShelfMathTag = "learning_shelf_math"
const val WordStudyCardTag = "word_study_card"
const val WordStudyStartTag = "word_study_start"
const val WordStudyManualTag = "word_study_manual"
const val WordStudyTargetMinusTag = "word_study_target_minus"
const val WordStudyTargetPlusTag = "word_study_target_plus"
const val WordStudyTargetValueTag = "word_study_target_value"
const val WordStudyPresetTagPrefix = "word_study_preset_"

internal fun wordStudyPresetTag(count: Int): String = WordStudyPresetTagPrefix + count

/**
 * The full learning tab. Home keeps a three-line preview; everything the
 * material actually contains — the whole poem, the word example, the math
 * derivation — lives here, together with the word-study round.
 */
@Composable
fun LearningScreen(
    todayEpochDay: Long,
    learningProgress: LearningProgress,
    learningActions: LearningActions,
    wordStudyState: WordStudyState,
    wordStudyActions: WordStudyActions,
    onAskAi: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = todayEpochDay
    val poem = remember(today, learningProgress.dailyPoemId) {
        LearningBrowse.resolvePoem(today, learningProgress.poemIdFor(today))
    }
    val word = remember(today, learningProgress.dailyWordId) {
        LearningBrowse.resolveWord(today, learningProgress.wordIdFor(today))
    }
    val topic = remember(today) { LearningLibrary.mathFor(today) }

    var bookKind by rememberSaveable { mutableStateOf<LearningKind?>(null) }
    var showStudy by rememberSaveable { mutableStateOf(false) }
    var showPicker by rememberSaveable { mutableStateOf(false) }

    OrbitLazyScreen(
        modifier = modifier.testTag(LearningScreenTag),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = {
            item(key = "header") {
                LearningHeader(today = today)
            }
            item(key = "today-title") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OrbitSectionTitle(
                        title = stringResource(R.string.home_learning_section),
                        modifier = Modifier.testTag(LearningTodaySectionTag),
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = stringResource(R.string.learning_screen_today_hint),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.5.sp,
                            lineHeight = 14.sp,
                            letterSpacing = 0.sp,
                        ),
                        color = MaterialTheme.orbitColors.textMuted,
                    )
                }
            }
            item(key = "today-poem") {
                TodayPoemCard(
                    poem = poem,
                    onOpenBook = { bookKind = LearningKind.POEM },
                    onAskAi = { onAskAi(LearningLibrary.poemPrompt(poem)) },
                )
            }
            item(key = "today-word") {
                TodayWordCard(
                    word = word,
                    onOpenBook = { bookKind = LearningKind.ENGLISH },
                    onAskAi = { onAskAi(LearningLibrary.englishPrompt(word)) },
                )
            }
            item(key = "today-math") {
                TodayMathCard(
                    topic = topic,
                    onOpenBook = { bookKind = LearningKind.MATH },
                    onAskAi = { onAskAi(LearningLibrary.mathPrompt(topic)) },
                )
            }
            item(key = "word-study") {
                WordStudyCard(
                    state = wordStudyState,
                    today = today,
                    onSetTarget = wordStudyActions.onSetTarget,
                    onPrimary = {
                        if (wordStudyState.session == null) {
                            wordStudyActions.onStartAuto()
                        }
                        if (LearningLibrary.english.isNotEmpty()) {
                            showStudy = true
                        }
                    },
                    onManual = {
                        showPicker = true
                    },
                )
            }
            item(key = "shelf") {
                LearningShelfSection(
                    onOpenBook = { kind -> bookKind = kind },
                )
            }
            item(key = "footer") {
                Text(
                    text = stringResource(R.string.learning_screen_footer),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        letterSpacing = 0.sp,
                    ),
                    color = MaterialTheme.orbitColors.textMuted,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )
            }
        },
    )

    if (showStudy) {
        WordStudySheet(
            state = wordStudyState,
            today = today,
            actions = wordStudyActions,
            onDismiss = { showStudy = false },
        )
    }

    if (showPicker) {
        WordPickerSheet(
            onDismiss = { showPicker = false },
            onStart = { ids ->
                wordStudyActions.onStartManual(ids)
                showPicker = false
                showStudy = true
            },
        )
    }

    val openKind = bookKind
    if (openKind != null) {
        LearningSheet(
            todayEpochDay = today,
            initialKind = openKind,
            dailyPoemId = learningProgress.poemIdFor(today),
            dailyWordId = learningProgress.wordIdFor(today),
            bookmarkedWordIds = learningProgress.bookmarkedWordIds,
            onSetPoemToday = { poemId ->
                learningActions.onSetPoemToday(today, poemId)
            },
            onSetWordToday = { wordId ->
                learningActions.onSetWordToday(today, wordId)
            },
            onToggleWordBookmark = learningActions.onToggleWordBookmark,
            onDismiss = { bookKind = null },
            onAskAi = { prompt ->
                bookKind = null
                onAskAi(prompt)
            },
        )
    }
}

@Composable
private fun LearningHeader(today: Long) {
    val colors = MaterialTheme.orbitColors
    OrbitScreenHeader(
        eyebrow = stringResource(R.string.learning_screen_eyebrow),
        title = stringResource(R.string.learning_screen_title),
        subtitle = stringResource(R.string.learning_screen_subtitle),
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                OrbitBadge(text = stringResource(R.string.learning_screen_badge))
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${TaskDate.monthDayLabel(today)} · ${TaskDate.weekdayLabel(today)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        letterSpacing = 0.2.sp,
                    ),
                    color = colors.textMuted,
                )
            }
        },
    )
}

@Composable
private fun TodayPoemCard(
    poem: Poem,
    onOpenBook: () -> Unit,
    onAskAi: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    OrbitCard(elevated = true, padding = PaddingValues(18.dp)) {
        DetailLabel(text = stringResource(R.string.home_learning_poem_label))
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = poem.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 21.sp,
                        lineHeight = 27.sp,
                        fontFamily = FontFamily.Serif,
                    ),
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${poem.author} · ${poem.dynasty}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 0.3.sp,
                    ),
                    color = colors.textSecondary,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                LearningActionLink(
                    text = stringResource(R.string.learning_screen_open_book),
                    onClick = onOpenBook,
                )
                Spacer(Modifier.width(2.dp))
                LearningActionLink(
                    text = stringResource(R.string.home_learning_ai_action),
                    onClick = onAskAi,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        GlassDivider()
        Spacer(Modifier.height(14.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            poem.lines.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        lineHeight = 27.sp,
                        fontFamily = FontFamily.Serif,
                    ),
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.learning_screen_poem_note),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 0.sp,
            ),
            color = colors.textMuted,
        )
    }
}

@Composable
private fun TodayWordCard(
    word: EnglishWord,
    onOpenBook: () -> Unit,
    onAskAi: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    OrbitCard(elevated = true, padding = PaddingValues(18.dp)) {
        DetailLabel(text = stringResource(R.string.home_learning_english_label))
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = word.word,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 30.sp,
                            lineHeight = 34.sp,
                        ),
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = word.phonetic,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                        ),
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    text = word.meaning,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        lineHeight = 23.sp,
                    ),
                    color = colors.textPrimary,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                LearningActionLink(
                    text = stringResource(R.string.learning_screen_open_book),
                    onClick = onOpenBook,
                )
                Spacer(Modifier.width(2.dp))
                LearningActionLink(
                    text = stringResource(R.string.home_learning_ai_action),
                    onClick = onAskAi,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        GlassDivider()
        Spacer(Modifier.height(12.dp))
        if (word.example.isBlank()) {
            Text(
                text = stringResource(R.string.word_study_empty_example),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    lineHeight = 18.sp,
                ),
                color = colors.textMuted,
            )
        } else {
            DetailLabel(text = stringResource(R.string.learning_english_example_label))
            Spacer(Modifier.height(5.dp))
            Text(
                text = word.example,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    fontStyle = FontStyle.Italic,
                ),
                color = colors.textPrimary,
            )
            if (word.exampleZh.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = word.exampleZh,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        lineHeight = 18.sp,
                    ),
                    color = colors.textSecondary,
                )
            }
        }
        if (word.tip.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            DetailLabel(text = stringResource(R.string.learning_english_tip_label))
            Spacer(Modifier.height(5.dp))
            Text(
                text = word.tip,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    lineHeight = 18.sp,
                ),
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun TodayMathCard(
    topic: MathTopic,
    onOpenBook: () -> Unit,
    onAskAi: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    OrbitCard(elevated = true, padding = PaddingValues(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DetailLabel(text = stringResource(R.string.home_learning_math_label))
            Spacer(Modifier.width(8.dp))
            TagPill(text = topic.grade.label, color = colors.accentLine)
            Spacer(Modifier.weight(1f))
            LearningActionLink(
                text = stringResource(R.string.learning_screen_open_book),
                onClick = onOpenBook,
            )
            Spacer(Modifier.width(2.dp))
            LearningActionLink(
                text = stringResource(R.string.home_learning_ai_action),
                onClick = onAskAi,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = topic.title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 21.sp,
                lineHeight = 27.sp,
            ),
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(7.dp))
        Text(
            text = topic.summary,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
            ),
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(11.dp))
        Text(
            text = topic.formula,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                lineHeight = 19.sp,
                fontFamily = FontFamily.Monospace,
            ),
            color = colors.textPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(OrbitCorners.control))
                .background(colors.track)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
        Spacer(Modifier.height(13.dp))
        DetailLabel(text = stringResource(R.string.learning_math_example_label))
        Spacer(Modifier.height(5.dp))
        Text(
            text = topic.example,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                lineHeight = 19.sp,
            ),
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(9.dp))
        DetailLabel(text = stringResource(R.string.learning_math_answer_label))
        Spacer(Modifier.height(5.dp))
        Text(
            text = topic.answer,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                lineHeight = 19.sp,
            ),
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(11.dp))
        Text(
            text = topic.tip,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                lineHeight = 17.sp,
            ),
            color = colors.textMuted,
        )
    }
}

@Composable
private fun DetailLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 8.5.sp,
            letterSpacing = 1.3.sp,
            fontWeight = FontWeight.Bold,
        ),
        color = MaterialTheme.orbitColors.accentLine,
    )
}

@Composable
private fun LearningActionLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    Row(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(OrbitCorners.pill))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 0.1.sp,
            ),
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            maxLines = 1,
        )
        Spacer(Modifier.width(3.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = colors.accentLine,
            modifier = Modifier.size(11.dp),
        )
    }
}

@Composable
private fun WordStudyCard(
    state: WordStudyState,
    today: Long,
    onSetTarget: (Int) -> Unit,
    onPrimary: () -> Unit,
    onManual: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    val progress = state.progress
    val session = state.session
    val target = progress.dailyTarget
    val studied = progress.studiedOn(today)
    val hasWords = LearningLibrary.english.isNotEmpty()
    val primaryLabel = when {
        !hasWords -> stringResource(R.string.word_study_none)
        session == null -> stringResource(R.string.word_study_start)
        session.isComplete -> stringResource(R.string.word_study_result)
        else -> stringResource(
            R.string.word_study_continue,
            session.answeredCount,
            session.words.size,
        )
    }

    OrbitCard(
        modifier = Modifier.testTag(WordStudyCardTag),
        elevated = true,
        padding = PaddingValues(20.dp),
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
                        letterSpacing = 1.9.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = colors.accentLine,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.word_study_section),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
            }
            OrbitBadge(text = stringResource(R.string.word_study_auto_badge))
        }
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            StudyStat(
                value = "$studied / $target",
                label = stringResource(R.string.word_study_today_label),
                modifier = Modifier.weight(1.2f),
            )
            StudyStat(
                value = "${progress.learnedCount}",
                label = stringResource(R.string.word_study_learned),
                modifier = Modifier.weight(1f),
            )
            StudyStat(
                value = "${progress.reviewCount}",
                label = stringResource(R.string.word_study_review),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        OrbitProgressBar(
            progress = if (target > 0) studied.toFloat() / target else 0f,
            height = 7.dp,
        )
        if (studied >= target) {
            Spacer(Modifier.height(7.dp))
            Text(
                text = stringResource(R.string.word_study_target_reached),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 0.sp,
                ),
                color = colors.accentLine,
            )
        }
        Spacer(Modifier.height(16.dp))
        GlassDivider()
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.word_study_round_size),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(
                        R.string.word_study_library_meta,
                        LearningLibrary.english.size,
                    ),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        letterSpacing = 0.sp,
                    ),
                    color = colors.textMuted,
                )
            }
            TargetStepper(target = target, onSetTarget = onSetTarget)
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            com.example.orbit.learning.WordStudyPlanner.PRESETS.forEach { preset ->
                val selected = target == preset
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(OrbitCorners.pill))
                        .background(if (selected) colors.primary else colors.track)
                        .clickable(role = Role.Button) { onSetTarget(preset) }
                        .testTag(wordStudyPresetTag(preset)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = preset.toString(),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) colors.onAccent else colors.textSecondary,
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OrbitPrimaryButton(
                text = primaryLabel,
                onClick = onPrimary,
                modifier = Modifier
                    .weight(1.35f)
                    .testTag(WordStudyStartTag),
                enabled = hasWords,
            )
            OrbitPrimaryButton(
                text = stringResource(R.string.word_study_manual),
                onClick = onManual,
                modifier = Modifier
                    .weight(1f)
                    .testTag(WordStudyManualTag),
                enabled = hasWords,
                style = OrbitButtonStyle.QUIET,
            )
        }
        Spacer(Modifier.height(9.dp))
        Text(
            text = stringResource(R.string.word_study_rule_hint),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.sp,
            ),
            color = colors.textMuted,
        )
    }
}

@Composable
private fun StudyStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontFeatureSettings = "tnum",
            ),
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.5.sp,
                lineHeight = 12.sp,
                letterSpacing = 0.sp,
            ),
            color = colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TargetStepper(
    target: Int,
    onSetTarget: (Int) -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepperButton(
            glyph = "−",
            contentDescription = stringResource(R.string.word_study_target_minus),
            enabled = target > com.example.orbit.learning.WordStudyPlanner.MIN_TARGET,
            tag = WordStudyTargetMinusTag,
            onClick = {
                onSetTarget(
                    com.example.orbit.learning.WordStudyPlanner.clampTarget(target - 1),
                )
            },
        )
        Text(
            text = stringResource(R.string.word_study_target_value, target),
            modifier = Modifier
                .widthIn(min = 62.dp)
                .testTag(WordStudyTargetValueTag),
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 14.sp,
                fontFeatureSettings = "tnum",
            ),
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        StepperButton(
            glyph = "+",
            contentDescription = stringResource(R.string.word_study_target_plus),
            enabled = target < com.example.orbit.learning.WordStudyPlanner.MAX_TARGET,
            tag = WordStudyTargetPlusTag,
            onClick = {
                onSetTarget(
                    com.example.orbit.learning.WordStudyPlanner.clampTarget(target + 1),
                )
            },
        )
    }
}

@Composable
private fun StepperButton(
    glyph: String,
    contentDescription: String,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(colors.track)
            .border(1.dp, colors.cardBorder, CircleShape)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                this.contentDescription = contentDescription
            }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                lineHeight = 20.sp,
            ),
            fontWeight = FontWeight.Bold,
            color = if (enabled) colors.textPrimary else colors.textMuted.copy(alpha = 0.4f),
        )
    }
}

@Composable
private fun LearningShelfSection(
    onOpenBook: (LearningKind) -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    Column(modifier = Modifier.fillMaxWidth()) {
        OrbitSectionTitle(title = stringResource(R.string.learning_screen_shelf_title))
        Spacer(Modifier.height(5.dp))
        Text(
            text = stringResource(R.string.learning_screen_shelf_hint),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.sp,
            ),
            color = colors.textMuted,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ShelfTile(
                modifier = Modifier
                    .weight(1f)
                    .testTag(LearningShelfPoemTag),
                glyph = "诗",
                title = stringResource(R.string.learning_book_poem),
                count = stringResource(
                    R.string.learning_poem_count,
                    LearningLibrary.poems.size,
                ),
                accent = colors.star,
                onClick = { onOpenBook(LearningKind.POEM) },
            )
            ShelfTile(
                modifier = Modifier
                    .weight(1f)
                    .testTag(LearningShelfEnglishTag),
                glyph = "A",
                title = stringResource(R.string.learning_book_english),
                count = stringResource(
                    R.string.learning_word_count,
                    LearningLibrary.english.size,
                ),
                accent = colors.secondary,
                onClick = { onOpenBook(LearningKind.ENGLISH) },
            )
            ShelfTile(
                modifier = Modifier
                    .weight(1f)
                    .testTag(LearningShelfMathTag),
                glyph = "∑",
                title = stringResource(R.string.learning_book_math),
                count = stringResource(
                    R.string.learning_topic_count,
                    LearningLibrary.math.size,
                ),
                accent = colors.accentLine,
                onClick = { onOpenBook(LearningKind.MATH) },
            )
        }
    }
}

@Composable
private fun ShelfTile(
    glyph: String,
    title: String,
    count: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    val shape = RoundedCornerShape(OrbitCorners.inner)
    Column(
        modifier = modifier
            .height(118.dp)
            .clip(shape)
            .background(colors.cardElevated)
            .border(1.dp, accent.copy(alpha = 0.30f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = glyph,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                ),
                fontWeight = FontWeight.Bold,
                color = if (colors.isDark) accent else colors.textPrimary,
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(12.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.sp,
                lineHeight = 15.sp,
            ),
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = count,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.5.sp,
                letterSpacing = 0.sp,
            ),
            color = colors.textMuted,
            maxLines = 1,
        )
    }
}
