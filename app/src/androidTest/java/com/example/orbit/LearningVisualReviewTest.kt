package com.example.orbit

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.orbit.data.LearningProgressStore
import com.example.orbit.data.OrbitPreferencesStore
import com.example.orbit.data.local.OrbitDatabase
import com.example.orbit.data.local.TaskEntity
import com.example.orbit.learning.LearningBrowse
import com.example.orbit.learning.LearningLibrary
import com.example.orbit.learning.MathGrade
import com.example.orbit.learning.QuoteLibrary
import com.example.orbit.model.AccentTone
import com.example.orbit.model.TaskCategory
import com.example.orbit.model.TaskDate
import com.example.orbit.model.TaskPriority
import com.example.orbit.model.ThemeMode
import com.example.orbit.ui.LearningViewModel
import com.example.orbit.ui.OrbitViewModel
import com.example.orbit.ui.TaskViewModel
import com.example.orbit.ui.components.AiAssistantFabTag
import com.example.orbit.ui.components.OrbitTestTag
import com.example.orbit.ui.orbit.OrbitLayout
import com.example.orbit.ui.screens.LearningBookListTag
import com.example.orbit.ui.screens.LearningBookTag
import com.example.orbit.ui.screens.LearningPoemSearchTag
import com.example.orbit.ui.screens.LearningScreenTag
import com.example.orbit.ui.screens.LearningShelfEnglishTag
import com.example.orbit.ui.screens.LearningShelfPoemTag
import com.example.orbit.ui.screens.LearningSetTodayTag
import com.example.orbit.ui.screens.LearningShelfTag
import com.example.orbit.ui.screens.LearningWordFilterBookmarkedTag
import com.example.orbit.ui.screens.wordBookmarkTag
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.hypot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device review for the book-shaped learning library, the daily quote, the
 * draggable assistant disc and cross-ring node drops. Screenshots are written to
 * the app's external files dir so they can be pulled after the run.
 */
@RunWith(AndroidJUnit4::class)
class LearningVisualReviewTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val taskViewModel: TaskViewModel
        get() = ViewModelProvider(rule.activity)[TaskViewModel::class.java]
    private val preferencesViewModel: OrbitViewModel
        get() = ViewModelProvider(rule.activity)[OrbitViewModel::class.java]
    private val learningViewModel: LearningViewModel
        get() = ViewModelProvider(rule.activity)[LearningViewModel::class.java]

    @Before
    fun seed() {
        context.getSharedPreferences(
            OrbitPreferencesStore.PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        ).edit().clear().commit()
        // Daily picks and bookmarks are per-account local state; each test
        // starts from an empty learning layer.
        context.getSharedPreferences(
            LearningProgressStore.PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        ).edit().clear().commit()
        rule.runOnIdle {
            preferencesViewModel.setThemeMode(ThemeMode.DEEP_SPACE)
            preferencesViewModel.setAccentTone(AccentTone.CYAN)
            learningViewModel.resetLocalProgress()
        }
        val database = OrbitDatabase.getInstance(rule.activity)
        database.clearAllTables()

        val today = TaskDate.todayEpochDay()
        val now = System.currentTimeMillis()
        runBlocking {
            val dao = database.taskDao()
            listOf(
                "整理解题错因",
                "背十个单词",
                "阅读物理笔记",
                "写一段读书摘要",
                "复习数学公式",
                "整理明日计划",
            ).forEachIndexed { index, title ->
                dao.insert(
                    TaskEntity(
                        title = title,
                        description = null,
                        priority = TaskPriority.MEDIUM,
                        category = TaskCategory.STUDY,
                        dueDate = today,
                        createdAt = now + index,
                    ),
                )
            }
        }
        rule.waitUntil(TIMEOUT_MS) { taskViewModel.uiState.value.tasks.size == 6 }
        rule.waitForIdle()
    }

    @Test
    fun captureLearningLibraryAndDailyQuote() {
        waitForScrollable()
        scrollToText("今日学习")
        waitForText("完整内容与单词学习在「学习」页")
        capture("l1-home-learning-dark.png")

        scrollToText("每日一句")
        val today = TaskDate.todayEpochDay()
        val first = QuoteLibrary.forDay(today)
        waitForText(first.text)
        waitForText("学习习惯")
        capture("l2-home-quote-dark.png")

        rule.onNodeWithContentDescription("换一句").performClick()
        val next = QuoteLibrary.next(first)
        waitForText(next.text)
        rule.waitForIdle()
        capture("l3-home-quote-refreshed-dark.png")

        openLearningTab()
        scrollToText("学习书架")
        waitForText("学习书架")
        capture("l4-learning-shelf-dark.png")

        rule.onNodeWithTag(LearningShelfPoemTag).performClick()
        waitForTag(LearningBookTag)
        waitForText("OPEN BOOK")
        captureNode(LearningBookTag, "l5-poem-book-dark.png")

        // Scrolling to the last poem used to fight the sheet drag handler and
        // jitter. The frame must be identical one animation second later.
        rule.onNodeWithTag(LearningBookListTag)
            .performScrollToIndex(LearningLibrary.poems.lastIndex)
        rule.waitForIdle()
        val bottom = captureBitmap(LearningBookTag)
        rule.mainClock.advanceTimeBy(1_200)
        rule.waitForIdle()
        val bottomAgain = captureBitmap(LearningBookTag)
        assertTrue("book bottom should settle without jitter", bottom.sameAs(bottomAgain))
        captureNode(LearningBookTag, "l6-poem-book-bottom-dark.png")

        rule.onNodeWithContentDescription("返回书架").performClick()
        waitForTag(LearningShelfTag)
        rule.onNodeWithContentDescription(
            "打开高中 3500 词，共 ${LearningLibrary.english.size} 词",
        ).performClick()
        waitForTag(LearningBookTag)
        rule.onNodeWithText("a").performClick()
        waitForText("暂无例句与记忆提示，点下方按钮让 AI 生成一个高考语境例句。")
        captureNode(LearningBookTag, "l6c-english-expanded-dark.png")
        rule.onNodeWithTag(LearningBookListTag)
            .performScrollToIndex(LearningLibrary.english.lastIndex)
        rule.waitForIdle()
        val wordBottom = captureBitmap(LearningBookTag)
        rule.mainClock.advanceTimeBy(1_200)
        rule.waitForIdle()
        assertTrue(
            "word list should settle without jitter",
            wordBottom.sameAs(captureBitmap(LearningBookTag)),
        )
        captureNode(LearningBookTag, "l6b-english-book-bottom-dark.png")

        rule.onNodeWithContentDescription("返回书架").performClick()
        waitForTag(LearningShelfTag)
        rule.onNodeWithContentDescription(
            "打开数学讲义，共 ${LearningLibrary.math.size} 个",
        ).performClick()
        waitForTag(LearningBookTag)
        waitForText("高一")
        captureNode(LearningBookTag, "l7-math-book-dark.png")

        rule.onNodeWithText("高一").performClick()
        val gradeTen = LearningLibrary.mathForGrade(MathGrade.GRADE_10)
        assertEquals(3, gradeTen.size)
        waitForText(gradeTen.first().title)
        captureNode(LearningBookTag, "l8-math-grade10-dark.png")

        rule.onNodeWithText(gradeTen.first().title).performClick()
        waitForText("例题")
        waitForText("易错提醒")
        captureNode(LearningBookTag, "l9-math-expanded-dark.png")
    }

    @Test
    fun captureDraggableAssistantAndCrossRingDrop() {
        waitForScrollable()
        val fab = rule.onNodeWithTag(AiAssistantFabTag)
        val startFab = fab.fetchSemanticsNode().boundsInRoot
        assertTrue(startFab.width > 0f)
        assertEquals(
            "常亮",
            fab.fetchSemanticsNode().config[SemanticsProperties.StateDescription],
        )

        fab.performTouchInput {
            swipe(
                start = center,
                end = center + Offset(-260f, -720f),
                durationMillis = 420,
            )
        }
        rule.waitUntil(TIMEOUT_MS) {
            val bounds = runCatching {
                rule.onNodeWithTag(AiAssistantFabTag).fetchSemanticsNode().boundsInRoot
            }.getOrNull() ?: return@waitUntil false
            bounds.center.x < startFab.center.x - 120f &&
                bounds.center.y < startFab.center.y - 300f
        }
        val movedFabNode = rule.onNodeWithTag(AiAssistantFabTag).fetchSemanticsNode()
        val movedFab = movedFabNode.boundsInRoot
        assertEquals(
            "半透明",
            movedFabNode.config[SemanticsProperties.StateDescription],
        )
        capture("l10-home-fab-moved-dark.png")

        // The offset is saveable state, so scrolling must not snap it back.
        scrollToText("今日学习")
        val afterScroll = rule.onNodeWithTag(AiAssistantFabTag)
            .fetchSemanticsNode().boundsInRoot
        assertEquals(movedFab.center.x, afterScroll.center.x, 1f)
        assertEquals(movedFab.center.y, afterScroll.center.y, 1f)

        rule.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToIndex(0)
        rule.waitForIdle()

        val description = "整理解题错因，中优先级，待完成"
        waitForContentDescription(description)
        val orbitBounds = rule.onNodeWithTag(OrbitTestTag)
            .fetchSemanticsNode().boundsInRoot
        val nodeBounds = rule.onNodeWithContentDescription(description)
            .fetchSemanticsNode().boundsInRoot
        val centerInRoot = Offset(
            orbitBounds.width / 2f,
            orbitBounds.height / 2f,
        ) + orbitBounds.topLeft
        val startLocal = nodeBounds.center - orbitBounds.topLeft
        val endRoot = centerInRoot + (nodeBounds.center - centerInRoot) * 1.55f

        rule.onNodeWithTag(OrbitTestTag).performTouchInput {
            swipe(
                start = startLocal,
                end = endRoot - orbitBounds.topLeft,
                durationMillis = 520,
            )
        }

        val outerRadius = orbitBounds.width *
            OrbitLayout.RING_RADIUS_FRACTIONS[OrbitLayout.OUTER_RING]
        rule.waitUntil(TIMEOUT_MS) {
            val latest = runCatching {
                rule.onNodeWithContentDescription(description)
                    .fetchSemanticsNode().boundsInRoot.center
            }.getOrNull() ?: return@waitUntil false
            abs(hypot(latest.x - centerInRoot.x, latest.y - centerInRoot.y) - outerRadius) < 6f
        }
        rule.runOnIdle {
            val task = taskViewModel.uiState.value.tasks.first {
                it.title == "整理解题错因"
            }
            assertFalse(task.isCompleted)
        }
        capture("l11-home-cross-ring-dark.png")
    }

    @Test
    fun capturePoemSearchDailyPickAndWordBookmarks() {
        val today = TaskDate.todayEpochDay()
        val poem = LearningLibrary.poems.first { it.title == "静夜思" }
        val word = LearningLibrary.english.first { it.word == "a" }
        val todayWord = LearningLibrary.englishFor(today)
        val todayTopic = LearningLibrary.mathFor(today)

        openLearningTab()
        scrollToText("学习书架")
        rule.onNodeWithTag(LearningShelfPoemTag).performClick()
        waitForTag(LearningBookTag)

        // Search by author, then narrow down to a single title.
        val poetHits = LearningBrowse.filterPoems(LearningLibrary.poems, "李白").size
        rule.onNodeWithTag(LearningPoemSearchTag).performClick()
        rule.onNodeWithTag(LearningPoemSearchTag).performTextInput("李白")
        waitForText("找到 $poetHits 首")
        captureNode(LearningBookTag, "l12-poem-search-dark.png")

        rule.onNodeWithContentDescription("清空搜索").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag(LearningPoemSearchTag).performClick()
        rule.onNodeWithTag(LearningPoemSearchTag).performTextInput("静夜思")
        waitForText("找到 1 首")
        rule.onNode(hasText(poem.title) and !hasSetTextAction()).performClick()
        waitForText("设为今日")
        // The expanded card is taller than the sheet, so bring the action row
        // into view before tapping it.
        rule.onNodeWithTag(LearningBookListTag)
            .performScrollToNode(hasText("设为今日"))
        rule.waitForIdle()
        captureNode(LearningBookTag, "l13-poem-search-detail-dark.png")

        rule.onNodeWithTag(LearningSetTodayTag, useUnmergedTree = true).performClick()
        rule.waitUntil(TIMEOUT_MS) {
            rule.onAllNodesWithText("设为今日").fetchSemanticsNodes().isEmpty()
        }
        assertEquals(poem.id, learningViewModel.progress.value.poemIdFor(today))
        captureNode(LearningBookTag, "l14-poem-today-dark.png")

        rule.onNodeWithContentDescription("返回书架").performClick()
        waitForTag(LearningShelfTag)
        waitForText("古诗《${poem.title}》 · 单词 ${todayWord.word} · 数学「${todayTopic.title}」")
        captureNode(LearningShelfTag, "l15-shelf-poem-today-dark.png")

        rule.onNodeWithContentDescription("关闭").performClick()
        rule.waitForIdle()
        openHomeTab()
        scrollToText("今日学习")
        waitForText(poem.title)
        capture("l16-home-poem-today-dark.png")

        // Words: bookmark one entry, filter the shelf down to bookmarks, then
        // pin it as today's word.
        openLearningTab()
        scrollToText("学习书架")
        rule.onNodeWithTag(LearningShelfEnglishTag).performClick()
        waitForTag(LearningBookTag)

        rule.onNodeWithTag(wordBookmarkTag(word.word), useUnmergedTree = true)
            .performClick()
        waitForText("收藏 · 1")
        assertEquals(
            setOf(word.id),
            learningViewModel.progress.value.bookmarkedWordIds,
        )
        captureNode(LearningBookTag, "l17-word-bookmarked-dark.png")

        rule.onNodeWithTag(LearningWordFilterBookmarkedTag).performClick()
        rule.waitForIdle()
        rule.onNodeWithText(word.word).performClick()
        waitForText("设为今日")
        captureNode(LearningBookTag, "l18-word-bookmark-filter-dark.png")

        rule.onNodeWithTag(LearningSetTodayTag, useUnmergedTree = true).performClick()
        rule.waitUntil(TIMEOUT_MS) {
            rule.onAllNodesWithText("设为今日").fetchSemanticsNodes().isEmpty()
        }
        assertEquals(word.id, learningViewModel.progress.value.wordIdFor(today))
        captureNode(LearningBookTag, "l19-word-today-dark.png")

        rule.onNodeWithContentDescription("返回书架").performClick()
        waitForTag(LearningShelfTag)
        waitForText("古诗《${poem.title}》 · 单词 ${word.word} · 数学「${todayTopic.title}」")
        rule.onNodeWithContentDescription("关闭").performClick()
        rule.waitForIdle()

        // Home reads the same picks, and the activity re-reads them from disk.
        openHomeTab()
        scrollToText("今日学习")
        waitForText(poem.title)
        waitForText(word.word)
        capture("l20-home-custom-picks-dark.png")

        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
        waitForScrollable()
        scrollToText("今日学习")
        waitForText(poem.title)
        waitForText(word.word)

        // Reopen the shelf after recreation: the TODAY line is rebuilt from
        // the same on-disk picks rather than from in-memory state.
        openLearningTab()
        scrollToText("学习书架")
        rule.onNodeWithTag(LearningShelfPoemTag).performClick()
        waitForTag(LearningBookTag)
        rule.onNodeWithContentDescription("返回书架").performClick()
        waitForTag(LearningShelfTag)
        waitForText("古诗《${poem.title}》 · 单词 ${word.word} · 数学「${todayTopic.title}」")
        captureNode(LearningShelfTag, "l21-shelf-picks-persisted-dark.png")
    }

    private fun openLearningTab() {
        rule.navigateToBottomTab("学习")
        waitForTag(LearningScreenTag)
    }

    private fun openHomeTab() {
        rule.navigateToBottomTab("星轨")
        waitForScrollable()
    }

    private fun capture(fileName: String) {
        rule.waitForIdle()
        writeBitmap(rule.onRoot().captureToImage().asAndroidBitmap(), fileName)
    }

    /**
     * The bottom sheet is its own window, so the activity root capture matches
     * two roots. Capturing the sheet tag keeps the review frame unambiguous.
     */
    private fun captureNode(tag: String, fileName: String) {
        rule.waitForIdle()
        val bitmap = rule.onAllNodesWithTag(tag).onFirst()
            .captureToImage()
            .asAndroidBitmap()
        writeBitmap(bitmap, fileName)
    }

    private fun writeBitmap(bitmap: Bitmap, fileName: String) {
        val directory = File(context.getExternalFilesDir(null), "screenshots_learning")
            .apply { mkdirs() }
        FileOutputStream(File(directory, fileName)).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                "Could not write $fileName"
            }
        }
    }

    private fun captureBitmap(tag: String): Bitmap = rule.onNodeWithTag(tag)
        .captureToImage()
        .asAndroidBitmap()

    private fun waitForScrollable() {
        rule.waitUntil(TIMEOUT_MS) {
            rule.onAllNodes(hasScrollToNodeAction()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun scrollToText(text: String) {
        rule.onAllNodes(hasScrollToNodeAction()).onFirst()
            .performScrollToNode(hasText(text))
        rule.waitForIdle()
    }

    private fun waitForText(text: String) {
        rule.waitUntil(TIMEOUT_MS) {
            rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForContentDescription(description: String) {
        rule.waitUntil(TIMEOUT_MS) {
            rule.onAllNodesWithContentDescription(description)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun waitForTag(tag: String) {
        rule.waitUntil(TIMEOUT_MS) {
            rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 30_000L
    }
}
