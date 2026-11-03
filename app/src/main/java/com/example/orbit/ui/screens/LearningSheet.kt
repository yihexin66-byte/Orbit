package com.example.orbit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbit.R
import com.example.orbit.learning.EnglishWord
import com.example.orbit.learning.LearningBrowse
import com.example.orbit.learning.LearningKind
import com.example.orbit.learning.LearningLibrary
import com.example.orbit.learning.MathGrade
import com.example.orbit.learning.MathTopic
import com.example.orbit.learning.Poem
import com.example.orbit.learning.WordShelfFilter
import com.example.orbit.ui.components.OrbitEmptyState
import com.example.orbit.ui.components.OrbitIconButton
import com.example.orbit.ui.theme.OrbitCorners
import com.example.orbit.ui.theme.orbitColors

const val LearningSheetTag = "learning_sheet"
const val LearningSheetAskTag = "learning_sheet_ask"
const val LearningShelfTag = "learning_shelf"
const val LearningBookTag = "learning_book"
const val LearningBookListTag = "learning_book_list"
const val LearningPoemSearchTag = "learning_poem_search"
const val LearningPoemSearchClearTag = "learning_poem_search_clear"
const val LearningSetTodayTag = "learning_set_today"
const val LearningWordFilterAllTag = "learning_word_filter_all"
const val LearningWordFilterBookmarkedTag = "learning_word_filter_bookmarked"
const val LearningWordBookmarkTagPrefix = "learning_word_bookmark_"

internal fun wordBookmarkTag(wordId: String): String =
    LearningWordBookmarkTagPrefix + wordId

/**
 * Book shelf and reading page.
 *
 * The library is split into three books so the growing collection stays
 * browsable: a shelf of covers, then one book at a time. Sheet gestures are
 * disabled because nested scrolling past the last row used to bounce against
 * the sheet's own drag handler on a real device.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningSheet(
    todayEpochDay: Long,
    initialKind: LearningKind?,
    onDismiss: () -> Unit,
    onAskAi: (String) -> Unit,
    initialMathGrade: MathGrade? = null,
    dailyPoemId: String? = null,
    dailyWordId: String? = null,
    bookmarkedWordIds: Set<String> = emptySet(),
    onSetPoemToday: (String) -> Unit = {},
    onSetWordToday: (String) -> Unit = {},
    onToggleWordBookmark: (String) -> Unit = {},
) {
    val colors = MaterialTheme.orbitColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val todayPoem = remember(todayEpochDay, dailyPoemId) {
        LearningBrowse.resolvePoem(todayEpochDay, dailyPoemId)
    }
    val todayWord = remember(todayEpochDay, dailyWordId) {
        LearningBrowse.resolveWord(todayEpochDay, dailyWordId)
    }
    val todayTopic = remember(todayEpochDay) { LearningLibrary.mathFor(todayEpochDay) }
    var kind by rememberSaveable { mutableStateOf<LearningKind?>(initialKind) }
    var mathGrade by rememberSaveable {
        mutableStateOf(initialMathGrade ?: todayTopic.grade)
    }
    val initialExpanded = remember(initialKind, initialMathGrade, todayEpochDay) {
        when (initialKind) {
            LearningKind.POEM -> todayPoem.id
            LearningKind.ENGLISH -> todayWord.id
            LearningKind.MATH -> todayTopic.id.takeIf {
                initialMathGrade == null || initialMathGrade == todayTopic.grade
            }
            null -> null
        }
    }
    var expandedId by rememberSaveable { mutableStateOf(initialExpanded) }

    LaunchedEffect(kind, mathGrade) {
        listState.scrollToItem(0)
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
                .fillMaxHeight(0.92f)
                .testTag(LearningSheetTag)
                .navigationBarsPadding()
                .padding(horizontal = 18.dp),
        ) {
            val currentKind = kind
            if (currentKind == null) {
                LibraryShelf(
                    todayPoem = todayPoem,
                    todayWord = todayWord,
                    todayTopic = todayTopic,
                    onOpenBook = { opened ->
                        kind = opened
                        expandedId = when (opened) {
                            LearningKind.POEM -> todayPoem.id
                            LearningKind.ENGLISH -> todayWord.id
                            LearningKind.MATH -> todayTopic.id.takeIf {
                                mathGrade == todayTopic.grade
                            }
                        }
                    },
                    onDismiss = onDismiss,
                )
            } else {
                BookPage(
                    kind = currentKind,
                    todayPoem = todayPoem,
                    todayWord = todayWord,
                    todayTopic = todayTopic,
                    mathGrade = mathGrade,
                    onMathGradeChange = { grade ->
                        mathGrade = grade
                        expandedId = todayTopic.id.takeIf { todayTopic.grade == grade }
                    },
                    expandedId = expandedId,
                    onToggle = { id -> expandedId = if (expandedId == id) null else id },
                    onBack = {
                        kind = null
                        expandedId = null
                    },
                    onDismiss = onDismiss,
                    onAskAi = onAskAi,
                    listState = listState,
                    bookmarkedWordIds = bookmarkedWordIds,
                    onSetPoemToday = onSetPoemToday,
                    onSetWordToday = onSetWordToday,
                    onToggleWordBookmark = onToggleWordBookmark,
                )
            }
        }
    }
}

@Composable
private fun LibraryShelf(
    todayPoem: Poem,
    todayWord: EnglishWord,
    todayTopic: MathTopic,
    onOpenBook: (LearningKind) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .testTag(LearningShelfTag),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "LEARNING LIBRARY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.5.sp,
                        letterSpacing = 2.sp,
                    ),
                    color = colors.textMuted,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.learning_shelf_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.learning_shelf_subtitle),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        letterSpacing = 0.sp,
                        lineHeight = 14.sp,
                    ),
                    color = colors.textMuted,
                )
            }
            OrbitIconButton(
                onClick = onDismiss,
                contentDescription = stringResource(R.string.learning_close),
                icon = Icons.Filled.Close,
                size = 38.dp,
            )
        }
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LearningBookCover(
                modifier = Modifier.weight(1f),
                glyph = "诗",
                title = stringResource(R.string.learning_book_poem),
                count = stringResource(
                    R.string.learning_poem_count,
                    LearningLibrary.poems.size,
                ),
                caption = stringResource(R.string.learning_book_poem_desc),
                accent = colors.star,
                onClick = { onOpenBook(LearningKind.POEM) },
            )
            LearningBookCover(
                modifier = Modifier.weight(1f),
                glyph = "A",
                title = stringResource(R.string.learning_book_english),
                count = stringResource(
                    R.string.learning_word_count,
                    LearningLibrary.english.size,
                ),
                caption = stringResource(R.string.learning_book_english_desc),
                accent = colors.secondary,
                onClick = { onOpenBook(LearningKind.ENGLISH) },
            )
            LearningBookCover(
                modifier = Modifier.weight(1f),
                glyph = "∑",
                title = stringResource(R.string.learning_book_math),
                count = stringResource(
                    R.string.learning_topic_count,
                    LearningLibrary.math.size,
                ),
                caption = stringResource(R.string.learning_book_math_desc),
                accent = colors.accentLine,
                onClick = { onOpenBook(LearningKind.MATH) },
            )
        }
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(OrbitCorners.inner))
                .background(colors.cardElevated)
                .border(1.dp, colors.cardBorder, RoundedCornerShape(OrbitCorners.inner))
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                text = stringResource(R.string.learning_shelf_today_label),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.5.sp,
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = colors.accentLine,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.learning_shelf_today,
                    todayPoem.title,
                    todayWord.word,
                    todayTopic.title,
                ),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.5.sp,
                    lineHeight = 19.sp,
                ),
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.learning_shelf_hint),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    lineHeight = 14.sp,
                    letterSpacing = 0.sp,
                ),
                color = colors.textMuted,
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.learning_shelf_footer),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.5.sp,
                letterSpacing = 0.8.sp,
            ),
            color = colors.textMuted.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun LearningBookCover(
    glyph: String,
    title: String,
    count: String,
    caption: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    val shape = RoundedCornerShape(18.dp)
    val description = stringResource(R.string.learning_book_desc, title, count)
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        accent.copy(alpha = if (colors.isDark) 0.30f else 0.22f),
                        colors.cardElevated,
                    ),
                ),
            )
            .border(1.dp, accent.copy(alpha = 0.34f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 9.dp)
                .width(2.dp)
                .fillMaxHeight(0.80f)
                .clip(RoundedCornerShape(OrbitCorners.pill))
                .background(accent.copy(alpha = 0.65f)),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 17.dp, top = 15.dp, end = 10.dp, bottom = 13.dp),
        ) {
            Text(
                text = glyph,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 26.sp,
                    lineHeight = 30.sp,
                ),
                fontWeight = FontWeight.Bold,
                color = if (colors.isDark) accent else colors.textPrimary,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                ),
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = count,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.5.sp,
                    letterSpacing = 0.sp,
                ),
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    lineHeight = 11.sp,
                    letterSpacing = 0.sp,
                ),
                color = colors.textMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.learning_book_open),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(10.dp),
                )
            }
        }
    }
}

@Composable
private fun BookPage(
    kind: LearningKind,
    todayPoem: Poem,
    todayWord: EnglishWord,
    todayTopic: MathTopic,
    mathGrade: MathGrade,
    onMathGradeChange: (MathGrade) -> Unit,
    expandedId: String?,
    onToggle: (String) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onAskAi: (String) -> Unit,
    listState: LazyListState,
    bookmarkedWordIds: Set<String>,
    onSetPoemToday: (String) -> Unit,
    onSetWordToday: (String) -> Unit,
    onToggleWordBookmark: (String) -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    val focusManager = LocalFocusManager.current
    var poemQuery by rememberSaveable(kind) { mutableStateOf("") }
    var wordFilter by rememberSaveable(kind) { mutableStateOf(WordShelfFilter.ALL) }
    val title = when (kind) {
        LearningKind.POEM -> stringResource(R.string.learning_book_poem)
        LearningKind.ENGLISH -> stringResource(R.string.learning_book_english)
        LearningKind.MATH -> stringResource(R.string.learning_book_math)
    }
    val subtitle = when (kind) {
        LearningKind.POEM -> stringResource(
            R.string.learning_poem_count,
            LearningLibrary.poems.size,
        ) + " · " + stringResource(R.string.learning_book_poem_desc)
        LearningKind.ENGLISH -> stringResource(
            R.string.learning_word_count,
            LearningLibrary.english.size,
        ) + " · " + stringResource(R.string.learning_book_english_desc)
        LearningKind.MATH -> stringResource(
            R.string.learning_topic_count,
            LearningLibrary.math.size,
        ) + " · " + stringResource(R.string.learning_book_math_desc)
    }
    val mathTopics = remember(mathGrade) { LearningLibrary.mathForGrade(mathGrade) }
    val filteredPoems = remember(poemQuery) {
        LearningBrowse.filterPoems(LearningLibrary.poems, poemQuery)
    }
    val filteredWords = remember(wordFilter, bookmarkedWordIds) {
        LearningBrowse.filterWords(
            words = LearningLibrary.english,
            filter = wordFilter,
            bookmarkedWordIds = bookmarkedWordIds,
        )
    }

    LaunchedEffect(kind, poemQuery, wordFilter) {
        listState.scrollToItem(0)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .testTag(LearningBookTag),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OrbitIconButton(
                onClick = onBack,
                contentDescription = stringResource(R.string.learning_back),
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                size = 38.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "OPEN BOOK",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        letterSpacing = 1.8.sp,
                    ),
                    color = colors.textMuted,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        letterSpacing = 0.sp,
                    ),
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OrbitIconButton(
                onClick = onDismiss,
                contentDescription = stringResource(R.string.learning_close),
                icon = Icons.Filled.Close,
                size = 38.dp,
            )
        }
        if (kind == LearningKind.MATH) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                MathGrade.entries.forEach { grade ->
                    GradeChip(
                        text = grade.label,
                        selected = grade == mathGrade,
                        onClick = { onMathGradeChange(grade) },
                    )
                }
            }
        }
        if (kind == LearningKind.POEM) {
            Spacer(Modifier.height(10.dp))
            PoemSearchField(
                query = poemQuery,
                onQueryChange = { poemQuery = it },
                onClear = {
                    poemQuery = ""
                    focusManager.clearFocus()
                },
            )
            if (poemQuery.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            R.string.learning_poem_search_count,
                            filteredPoems.size,
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
        if (kind == LearningKind.ENGLISH) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                WordFilterChip(
                    text = stringResource(R.string.learning_word_filter_all),
                    selected = wordFilter == WordShelfFilter.ALL,
                    onClick = { wordFilter = WordShelfFilter.ALL },
                    modifier = Modifier.testTag(LearningWordFilterAllTag),
                )
                WordFilterChip(
                    text = stringResource(
                        R.string.learning_word_filter_bookmarked,
                        bookmarkedWordIds.size,
                    ),
                    selected = wordFilter == WordShelfFilter.BOOKMARKED,
                    onClick = { wordFilter = WordShelfFilter.BOOKMARKED },
                    modifier = Modifier.testTag(LearningWordFilterBookmarkedTag),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag(LearningBookListTag),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            when (kind) {
                LearningKind.POEM -> {
                    if (filteredPoems.isEmpty()) {
                        item(key = "poem-search-empty") {
                            OrbitEmptyState(
                                title = stringResource(R.string.learning_poem_search_empty),
                                description = stringResource(
                                    R.string.learning_poem_search_empty_hint,
                                ),
                                actionLabel = stringResource(R.string.learning_poem_search_clear),
                                onAction = {
                                    poemQuery = ""
                                    focusManager.clearFocus()
                                },
                            )
                        }
                    } else {
                        items(
                            items = filteredPoems,
                            key = { it.id },
                        ) { poem ->
                            PoemCard(
                                poem = poem,
                                isToday = poem.id == todayPoem.id,
                                expanded = expandedId == poem.id,
                                onToggle = { onToggle(poem.id) },
                                onAskAi = { onAskAi(LearningLibrary.poemPrompt(poem)) },
                                onSetToday = { onSetPoemToday(poem.id) },
                            )
                        }
                    }
                }

                LearningKind.ENGLISH -> {
                    if (filteredWords.isEmpty()) {
                        item(key = "word-filter-empty") {
                            OrbitEmptyState(
                                title = stringResource(R.string.learning_word_bookmarks_empty),
                                description = stringResource(
                                    R.string.learning_word_bookmarks_empty_hint,
                                ),
                                actionLabel = stringResource(R.string.learning_word_filter_all),
                                onAction = { wordFilter = WordShelfFilter.ALL },
                            )
                        }
                    } else {
                        items(
                            items = filteredWords,
                            key = { it.id },
                        ) { word ->
                            EnglishCard(
                                word = word,
                                isToday = word.id == todayWord.id,
                                expanded = expandedId == word.id,
                                bookmarked = word.id in bookmarkedWordIds,
                                onToggle = { onToggle(word.id) },
                                onToggleBookmark = { onToggleWordBookmark(word.id) },
                                onAskAi = { onAskAi(LearningLibrary.englishPrompt(word)) },
                                onSetToday = { onSetWordToday(word.id) },
                            )
                        }
                    }
                }

                LearningKind.MATH -> items(
                    items = mathTopics,
                    key = { it.id },
                ) { topic ->
                    MathCard(
                        topic = topic,
                        isToday = topic.id == todayTopic.id,
                        expanded = expandedId == topic.id,
                        onToggle = { onToggle(topic.id) },
                        onAskAi = { onAskAi(LearningLibrary.mathPrompt(topic)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GradeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(OrbitCorners.pill))
            .background(if (selected) colors.primary else colors.track)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 36.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 11.sp,
                letterSpacing = 0.2.sp,
            ),
            fontWeight = FontWeight.Bold,
            color = if (selected) colors.onAccent else colors.textSecondary,
        )
    }
}

@Composable
private fun WordFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(OrbitCorners.pill))
            .background(if (selected) colors.primary else colors.track)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 40.dp)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 10.5.sp,
                letterSpacing = 0.2.sp,
            ),
            fontWeight = FontWeight.Bold,
            color = if (selected) colors.onAccent else colors.textSecondary,
        )
    }
}

@Composable
private fun PoemSearchField(
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
            .testTag(LearningPoemSearchTag)
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
                            text = stringResource(R.string.learning_poem_search_hint),
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
                    val clearLabel = stringResource(R.string.learning_poem_search_clear)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(OrbitCorners.pill))
                            .clickable(role = Role.Button, onClick = onClear)
                            .semantics { contentDescription = clearLabel }
                            .testTag(LearningPoemSearchClearTag),
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

@Composable
private fun SetTodayButton(onClick: () -> Unit) {
    val colors = MaterialTheme.orbitColors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(OrbitCorners.pill))
            .border(1.dp, colors.primary, RoundedCornerShape(OrbitCorners.pill))
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 44.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag(LearningSetTodayTag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.learning_set_today),
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 10.5.sp,
                letterSpacing = 0.3.sp,
            ),
            fontWeight = FontWeight.Bold,
            color = colors.primary,
        )
    }
}

@Composable
private fun WordBookmarkTag(
    word: String,
    bookmarked: Boolean,
    onToggle: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    val label = stringResource(
        if (bookmarked) R.string.learning_word_bookmarked else R.string.learning_word_bookmark,
    )
    val description = stringResource(
        if (bookmarked) {
            R.string.learning_word_bookmark_remove_desc
        } else {
            R.string.learning_word_bookmark_add_desc
        },
        word,
    )
    val state = stringResource(
        if (bookmarked) R.string.learning_word_bookmarked_state else
            R.string.learning_word_unbookmarked_state,
    )
    Box(
        modifier = Modifier
            .height(48.dp)
            .widthIn(min = 48.dp)
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .semantics {
                contentDescription = description
                stateDescription = state
            }
            .testTag(wordBookmarkTag(word)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(OrbitCorners.pill))
                .background(
                    if (bookmarked) colors.primary.copy(alpha = 0.16f) else colors.track,
                )
                .border(
                    1.dp,
                    if (bookmarked) colors.primary else colors.cardBorder,
                    RoundedCornerShape(OrbitCorners.pill),
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.5.sp,
                    letterSpacing = 0.sp,
                ),
                fontWeight = FontWeight.Bold,
                color = if (bookmarked) colors.primary else colors.textSecondary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PoemCard(
    poem: Poem,
    isToday: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAskAi: () -> Unit,
    onSetToday: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    LearningCardShell(
        expanded = expanded,
        onToggle = onToggle,
    ) {
        LearningCardHeader(
            label = "${poem.author} · ${poem.dynasty}",
            title = poem.title,
            isToday = isToday,
            expanded = expanded,
        )
        Spacer(Modifier.height(8.dp))
        // Some poems in the full collection run to dozens of lines, so the
        // collapsed card previews four lines and expands to the complete text.
        Text(
            text = poem.lines.joinToString("\n"),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                lineHeight = 21.sp,
            ),
            color = colors.textPrimary,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
        )
        if (expanded) {
            // The pick action sits right under the poem itself: the notes and
            // appreciation that follow can run long, and the button must stay
            // within one screen of the text it belongs to.
            if (!isToday) {
                Spacer(Modifier.height(10.dp))
                SetTodayButton(onClick = onSetToday)
            }
            if (poem.note.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                LearningDetailBlock(
                    label = stringResource(R.string.learning_poem_note_label),
                    body = poem.note,
                )
            }
            if (poem.appreciation.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                LearningDetailBlock(
                    label = stringResource(R.string.learning_poem_appreciation_label),
                    body = poem.appreciation,
                )
            }
            Spacer(Modifier.height(10.dp))
            AskAiButton(
                onClick = onAskAi,
            )
        }
    }
}

@Composable
private fun EnglishCard(
    word: EnglishWord,
    isToday: Boolean,
    expanded: Boolean,
    bookmarked: Boolean,
    onToggle: () -> Unit,
    onToggleBookmark: () -> Unit,
    onAskAi: () -> Unit,
    onSetToday: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    LearningCardShell(
        expanded = expanded,
        onToggle = onToggle,
    ) {
        LearningCardHeader(
            label = word.phonetic.ifBlank {
                stringResource(R.string.learning_english_meta_fallback)
            },
            title = word.word,
            isToday = isToday,
            expanded = expanded,
            titleMaxLines = 2,
            trailing = {
                WordBookmarkTag(
                    word = word.word,
                    bookmarked = bookmarked,
                    onToggle = onToggleBookmark,
                )
                Spacer(Modifier.width(4.dp))
            },
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = word.meaning,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
            ),
            color = colors.textSecondary,
        )
        if (word.example.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = word.example,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    lineHeight = 17.sp,
                    fontStyle = FontStyle.Italic,
                ),
                color = colors.textPrimary,
            )
        }
        if (word.exampleZh.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = word.exampleZh,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    lineHeight = 14.sp,
                    letterSpacing = 0.sp,
                ),
                color = colors.textMuted,
            )
        }
        if (expanded) {
            if (word.tip.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                LearningDetailBlock(
                    label = stringResource(R.string.learning_english_tip_label),
                    body = word.tip,
                )
            } else if (word.example.isBlank() && word.exampleZh.isBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.learning_english_empty_hint),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        lineHeight = 14.sp,
                        letterSpacing = 0.sp,
                    ),
                    color = colors.textMuted,
                )
            }
            if (!isToday) {
                Spacer(Modifier.height(10.dp))
                SetTodayButton(onClick = onSetToday)
            }
            Spacer(Modifier.height(10.dp))
            AskAiButton(onClick = onAskAi)
        }
    }
}

@Composable
private fun MathCard(
    topic: MathTopic,
    isToday: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAskAi: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    LearningCardShell(
        expanded = expanded,
        onToggle = onToggle,
    ) {
        LearningCardHeader(
            label = "${topic.grade.label} · ${topic.grade.stage}",
            title = topic.title,
            isToday = isToday,
            expanded = expanded,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = topic.formula,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.5.sp,
                lineHeight = 17.sp,
                fontFamily = FontFamily.Monospace,
            ),
            color = if (colors.isDark) colors.primary else colors.textPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = topic.summary,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                lineHeight = 17.sp,
            ),
            color = colors.textSecondary,
            maxLines = if (expanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (expanded) {
            Spacer(Modifier.height(10.dp))
            LearningDetailBlock(
                label = stringResource(R.string.learning_math_example_label),
                body = topic.example,
            )
            Spacer(Modifier.height(8.dp))
            LearningDetailBlock(
                label = stringResource(R.string.learning_math_answer_label),
                body = topic.answer,
            )
            Spacer(Modifier.height(8.dp))
            LearningDetailBlock(
                label = stringResource(R.string.learning_math_tip_label),
                body = topic.tip,
            )
            Spacer(Modifier.height(10.dp))
            AskAiButton(onClick = onAskAi)
        }
    }
}

@Composable
private fun LearningCardShell(
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    val shape = RoundedCornerShape(OrbitCorners.inner)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.cardElevated)
            .border(1.dp, if (expanded) colors.primary else colors.cardBorder, shape)
            .clickable(role = Role.Button, onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        content = content,
    )
}

@Composable
private fun LearningCardHeader(
    label: String,
    title: String,
    isToday: Boolean,
    expanded: Boolean,
    titleMaxLines: Int = 1,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.orbitColors
    val expandLabel = stringResource(R.string.learning_expand)
    val collapseLabel = stringResource(R.string.learning_collapse)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 0.8.sp,
                ),
                color = colors.textMuted,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                ),
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = titleMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) {
            trailing()
        }
        if (isToday) {
            TodayBadge()
            Spacer(Modifier.width(8.dp))
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.track)
                .semantics {
                    contentDescription = if (expanded) collapseLabel else expandLabel
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (expanded) "－" else "＋",
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun TodayBadge() {
    val colors = MaterialTheme.orbitColors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(OrbitCorners.pill))
            .background(colors.primary.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = stringResource(R.string.learning_today),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.5.sp,
                letterSpacing = 0.4.sp,
            ),
            fontWeight = FontWeight.Bold,
            color = if (colors.isDark) colors.primary else colors.textPrimary,
        )
    }
}

@Composable
private fun LearningDetailBlock(label: String, body: String) {
    val colors = MaterialTheme.orbitColors
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.5.sp,
                letterSpacing = 1.2.sp,
            ),
            color = colors.textMuted,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                lineHeight = 17.sp,
            ),
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun AskAiButton(onClick: () -> Unit) {
    val colors = MaterialTheme.orbitColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OrbitCorners.control))
            .background(colors.primary)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 44.dp)
            .testTag(LearningSheetAskTag)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.learning_ask_ai),
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = colors.onAccent,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = colors.onAccent,
            modifier = Modifier.size(16.dp),
        )
    }
}
