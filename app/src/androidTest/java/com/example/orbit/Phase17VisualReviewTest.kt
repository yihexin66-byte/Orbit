package com.example.orbit

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.orbit.data.GoalRepository
import com.example.orbit.data.HabitRepository
import com.example.orbit.data.LearningProgressStore
import com.example.orbit.data.OrbitPreferencesStore
import com.example.orbit.data.local.OrbitDatabase
import com.example.orbit.data.local.TaskEntity
import com.example.orbit.learning.LearningLibrary
import com.example.orbit.model.AccentTone
import com.example.orbit.model.GoalStatus
import com.example.orbit.model.TaskCategory
import com.example.orbit.model.TaskDate
import com.example.orbit.model.TaskPriority
import com.example.orbit.model.ThemeMode
import com.example.orbit.ui.GoalViewModel
import com.example.orbit.ui.HabitViewModel
import com.example.orbit.ui.LearningViewModel
import com.example.orbit.ui.OrbitViewModel
import com.example.orbit.ui.TaskViewModel
import com.example.orbit.ui.WordStudyViewModel
import com.example.orbit.ui.screens.LearningScreenTag
import com.example.orbit.ui.screens.WordPickerClearSearchTag
import com.example.orbit.ui.screens.WordPickerSearchTag
import com.example.orbit.ui.screens.WordPickerStartTag
import com.example.orbit.ui.screens.WordPickerTag
import com.example.orbit.ui.screens.WordStudyCardTag
import com.example.orbit.ui.screens.WordStudyCloseTag
import com.example.orbit.ui.screens.WordStudyFinishTag
import com.example.orbit.ui.screens.WordStudyKnownTag
import com.example.orbit.ui.screens.WordStudyManualTag
import com.example.orbit.ui.screens.WordStudyRetryWrongTag
import com.example.orbit.ui.screens.WordStudyRevealTag
import com.example.orbit.ui.screens.WordStudySessionTag
import com.example.orbit.ui.screens.WordStudyStartTag
import com.example.orbit.ui.screens.WordStudySummaryTag
import com.example.orbit.ui.screens.WordStudyUnknownTag
import com.example.orbit.ui.screens.wordPickerRowTag
import com.example.orbit.ui.screens.wordStudyPresetTag
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase 17 visual review: the compact home preview, the three-segment 目标 tab,
 * the full 学习 tab and the flip-card word round. Captures go to
 * `screenshots_phase17` and are pulled after the run.
 */
@RunWith(AndroidJUnit4::class)
class Phase17VisualReviewTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val taskViewModel: TaskViewModel
        get() = ViewModelProvider(rule.activity)[TaskViewModel::class.java]
    private val preferencesViewModel: OrbitViewModel
        get() = ViewModelProvider(rule.activity)[OrbitViewModel::class.java]
    private val goalViewModel: GoalViewModel
        get() = ViewModelProvider(rule.activity)[GoalViewModel::class.java]
    private val habitViewModel: HabitViewModel
        get() = ViewModelProvider(rule.activity)[HabitViewModel::class.java]
    private val learningViewModel: LearningViewModel
        get() = ViewModelProvider(rule.activity)[LearningViewModel::class.java]
    private val wordStudyViewModel: WordStudyViewModel
        get() = ViewModelProvider(rule.activity)[WordStudyViewModel::class.java]

    @Before
    fun seed() {
        context.getSharedPreferences(
            OrbitPreferencesStore.PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        ).edit().clear().commit()
        context.getSharedPreferences(
            LearningProgressStore.PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        ).edit().clear().commit()
        rule.runOnIdle {
            preferencesViewModel.setThemeMode(ThemeMode.DEEP_SPACE)
            preferencesViewModel.setAccentTone(AccentTone.CYAN)
            learningViewModel.resetLocalProgress()
            wordStudyViewModel.reload()
        }
        val database = OrbitDatabase.getInstance(rule.activity)
        database.clearAllTables()

        val today = TaskDate.todayEpochDay()
        val now = System.currentTimeMillis()
        runBlocking {
            val dao = database.taskDao()
            listOf(
                TaskEntity(
                    title = "完成作品集数据层",
                    description = "Room + Repository + 状态流",
                    priority = TaskPriority.HIGH,
                    category = TaskCategory.STUDY,
                    dueDate = today,
                    isCompleted = true,
                    createdAt = now - 86_400_000L,
                    completedAt = now - 3_600_000L,
                ),
                TaskEntity(
                    title = "整理投递记录",
                    description = "投递 5 家公司并记录跟进",
                    priority = TaskPriority.HIGH,
                    category = TaskCategory.WORK,
                    dueDate = today,
                    createdAt = now - 7_200_000L,
                ),
                TaskEntity(
                    title = "更新简历项目描述",
                    description = "突出 Compose 与 Room 实践",
                    priority = TaskPriority.HIGH,
                    category = TaskCategory.WORK,
                    dueDate = today,
                    createdAt = now - 3_600_000L,
                ),
                TaskEntity(
                    title = "阅读《Android 编程权威指南》",
                    description = "每天 30 页",
                    priority = TaskPriority.MEDIUM,
                    category = TaskCategory.STUDY,
                    dueDate = today,
                    createdAt = now - 5_400_000L,
                ),
                TaskEntity(
                    title = "跑步 5 公里",
                    description = "晚间轻松跑",
                    priority = TaskPriority.LOW,
                    category = TaskCategory.HEALTH,
                    dueDate = today,
                    createdAt = now - 10_800_000L,
                ),
                TaskEntity(
                    title = "给家人打电话",
                    description = "周末前确认回家时间",
                    priority = TaskPriority.LOW,
                    category = TaskCategory.LIFE,
                    dueDate = today,
                    createdAt = now - 1_800_000L,
                ),
                TaskEntity(
                    title = "复盘本周面试",
                    priority = TaskPriority.MEDIUM,
                    category = TaskCategory.WORK,
                    dueDate = today + 1,
                    createdAt = now,
                ),
            ).forEach { dao.insert(it) }

            val goals = GoalRepository(database)
            val portfolio = goals.createGoal(
                title = "完成个人作品集",
                description = "把三个项目整理成可投递的案例",
                tone = AccentTone.CYAN,
                targetDate = today + 40,
            )
            val caseStudy = goals.addStage(portfolio, "梳理案例", today - 10)
            goals.addStage(portfolio, "完成视觉稿", today + 12)
            goals.addStage(portfolio, "上线部署", today + 38)
            goals.setStageCompleted(caseStudy, true)

            val fitness = goals.createGoal(
                title = "保持每周运动",
                description = "每周至少三次，优先夜跑",
                tone = AccentTone.GOLD,
                targetDate = today + 90,
            )
            goals.addStage(fitness, "八月训练周", today + 7)
            goals.setStatus(fitness, GoalStatus.PAUSED)

            val interview = goals.createGoal(
                title = "准备秋招面试",
                description = "整理项目故事与算法题",
                tone = AccentTone.ROSE,
                targetDate = today + 60,
            )
            goals.addStage(interview, "项目故事复盘", today + 14)

            val habits = HabitRepository(database)
            val reading = habits.createHabit(
                title = "阅读 20 分钟",
                note = "睡前放下手机",
                tone = AccentTone.VIOLET,
            )
            val stretch = habits.createHabit(
                title = "晨间拉伸",
                note = "起床后的第一件事",
                tone = AccentTone.CYAN,
            )
            val journal = habits.createHabit(
                title = "记录三件小事",
                note = "睡前写下今天值得记住的瞬间",
                tone = AccentTone.GOLD,
            )
            val habitDao = database.habitDao()
            habitDao.observeAll().first().forEach { entity ->
                habitDao.update(entity.copy(createdAt = now - 40 * 86_400_000L))
            }
            listOf(0L, 1L, 2L, 4L, 5L, 7L, 8L, 10L).forEach { offset ->
                habits.setChecked(reading, today - offset, true)
                habits.setChecked(stretch, today - offset, true)
                habits.setChecked(journal, today - offset, true)
            }
        }
        rule.waitUntil(TIMEOUT_MS) { taskViewModel.uiState.value.tasks.size == 7 }
        rule.waitUntil(TIMEOUT_MS) { goalViewModel.uiState.value.goals.size == 3 }
        rule.waitUntil(TIMEOUT_MS) { habitViewModel.uiState.value.habits.size == 3 }
        rule.waitForIdle()
    }

    @Test
    fun captureCompactHomeAndMergedPlanner() {
        waitForContentDescription("完成作品集数据层，高优先级，已完成")
        capture("p01-home-top-dark.png")

        scrollToText("今日学习")
        waitForText("完整内容与单词学习在「学习」页")
        capture("p02-home-learning-preview-dark.png")

        navigateTo("目标")
        waitForText("ORBIT / GOALS & PLAN")
        waitForText("完成个人作品集")
        capture("p03-goals-segment-dark.png")

        rule.onNode(hasText("任务") and isSelectable()).performClick()
        waitForText("更新简历项目描述")
        capture("p04-tasks-segment-dark.png")

        rule.onNode(hasText("习惯") and isSelectable()).performClick()
        waitForText("今日打卡")
        capture("p05-habits-segment-dark.png")

        // Tapping the current tab resets the merged page to its first segment.
        // The segment pill also reads 目标 and is selectable, so match the
        // bottom-bar tab by its selected state.
        rule.onNode(hasText("目标") and isSelectable() and isSelected()).performClick()
        waitForText("完成个人作品集")
        rule.onAllNodesWithText("今日打卡").fetchSemanticsNodes().let { nodes ->
            assertEquals(0, nodes.size)
        }
    }

    @Test
    fun captureLearningTodayAndWordRound() {
        openLearningTab()
        capture("p06-learning-top-dark.png")

        // Items: 0 header, 1 today title, 2 poem, 3 word, 4 math, 5 study card.
        scrollToLearningIndex(4)
        waitForText("今日数学")
        capture("p07-learning-today-math-dark.png")

        scrollToLearningIndex(5)
        waitForText("每轮词数")
        capture("p08-word-study-card-dark.png")

        rule.onNodeWithTag(wordStudyPresetTag(5)).performScrollTo().performClick()
        rule.onNodeWithTag(WordStudyStartTag).performScrollTo().performClick()
        waitForTag(WordStudySessionTag)
        captureNode(WordStudySessionTag, "p09-word-round-hidden-dark.png")

        waitForTag(WordStudyRevealTag)
        rule.onNodeWithTag(WordStudyRevealTag).performClick()
        waitForTag(WordStudyKnownTag)
        captureNode(WordStudySessionTag, "p10-word-round-revealed-dark.png")

        repeat(3) {
            rule.onNodeWithTag(WordStudyKnownTag).performClick()
            waitForTag(WordStudyRevealTag)
            rule.onNodeWithTag(WordStudyRevealTag).performClick()
            waitForTag(WordStudyKnownTag)
        }
        rule.onNodeWithTag(WordStudyKnownTag).performClick()
        waitForTag(WordStudyRevealTag)
        rule.onNodeWithTag(WordStudyRevealTag).performClick()
        waitForTag(WordStudyUnknownTag)
        rule.onNodeWithTag(WordStudyUnknownTag).performClick()

        waitForTag(WordStudySummaryTag)
        waitForText("80%")
        captureNode(WordStudySessionTag, "p11-word-round-summary-dark.png")

        rule.onNodeWithTag(WordStudyRetryWrongTag).performClick()
        waitForTag(WordStudyRevealTag)
        rule.onNodeWithTag(WordStudyRevealTag).performClick()
        waitForTag(WordStudyKnownTag)
        rule.onNodeWithTag(WordStudyKnownTag).performClick()
        waitForTag(WordStudySummaryTag)
        rule.onNodeWithTag(WordStudyFinishTag).performClick()
        waitForTagGone(WordStudySessionTag)

        scrollToLearningIndex(5)
        waitForText("5 / 5")
        rule.runOnIdle {
            val today = TaskDate.todayEpochDay()
            assertEquals(5, wordStudyViewModel.state.value.progress.studiedOn(today))
        }
        capture("p12-word-study-progress-dark.png")
    }

    @Test
    fun captureManualPickerAndRichCard() {
        openLearningTab()
        scrollToWordStudyCard()
        rule.onNodeWithTag(WordStudyManualTag).performScrollTo().performClick()
        waitForTag(WordPickerTag)

        val confident = LearningLibrary.english.first { it.word == "confident" }
        val focus = LearningLibrary.english.first { it.word == "focus" }
        rule.onNodeWithTag(WordPickerSearchTag).performTextInput("confident")
        rule.onNodeWithTag(wordPickerRowTag(confident.id), useUnmergedTree = true)
            .performClick()
        rule.onNodeWithTag(WordPickerClearSearchTag).performClick()
        rule.waitForIdle()
        rule.onNodeWithTag(WordPickerSearchTag).performTextInput("focus")
        rule.onNodeWithTag(wordPickerRowTag(focus.id), useUnmergedTree = true)
            .performClick()
        rule.onNodeWithTag(WordPickerSearchTag).performImeAction()
        waitForText("已选 2 / 50")
        captureNode(WordPickerTag, "p13-word-picker-dark.png")

        rule.onNodeWithTag(WordPickerStartTag).performClick()
        waitForTag(WordStudySessionTag)
        waitForTag(WordStudyRevealTag)
        rule.onNodeWithTag(WordStudyRevealTag).performClick()
        waitForTag(WordStudyKnownTag)
        captureNode(WordStudySessionTag, "p14-word-round-example-dark.png")

        rule.onNodeWithTag(WordStudyCloseTag).performClick()
        waitForTagGone(WordStudySessionTag)
    }

    private fun openLearningTab() {
        rule.navigateToBottomTab("学习")
        waitForTag(LearningScreenTag)
    }

    private fun scrollToWordStudyCard() {
        rule.onAllNodes(hasScrollToNodeAction()).onFirst()
            .performScrollToNode(hasTestTag(WordStudyCardTag))
        rule.waitForIdle()
    }

    private fun scrollToLearningIndex(index: Int) {
        rule.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToIndex(index)
        rule.waitForIdle()
    }

    private fun navigateTo(label: String) {
        rule.navigateToBottomTab(label)
    }

    private fun scrollToText(text: String) {
        rule.onAllNodes(hasScrollToNodeAction()).onFirst()
            .performScrollToNode(hasText(text))
        rule.waitForIdle()
    }

    private fun waitForTag(tag: String) {
        rule.waitUntil(TIMEOUT_MS) {
            rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForTagGone(tag: String) {
        rule.waitUntil(TIMEOUT_MS) {
            rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()
        }
    }

    private fun waitForText(text: String) {
        rule.waitUntil(TIMEOUT_MS) {
            rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForContentDescription(description: String) {
        rule.waitUntil(TIMEOUT_MS) {
            rule.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun capture(fileName: String) {
        rule.waitForIdle()
        writeBitmap(rule.onRoot().captureToImage().asAndroidBitmap(), fileName)
    }

    private fun captureNode(tag: String, fileName: String) {
        rule.waitForIdle()
        val bitmap = rule.onAllNodesWithTag(tag).onFirst()
            .captureToImage()
            .asAndroidBitmap()
        writeBitmap(bitmap, fileName)
    }

    private fun writeBitmap(bitmap: Bitmap, fileName: String) {
        val directory = File(context.getExternalFilesDir(null), "screenshots_phase17")
            .apply { mkdirs() }
        FileOutputStream(File(directory, fileName)).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                "Could not write $fileName"
            }
        }
    }

    private companion object {
        const val TIMEOUT_MS = 30_000L
    }
}
