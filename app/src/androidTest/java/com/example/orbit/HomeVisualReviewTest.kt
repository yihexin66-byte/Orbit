package com.example.orbit

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.orbit.data.GoalRepository
import com.example.orbit.data.HabitRepository
import com.example.orbit.data.OrbitPreferencesStore
import com.example.orbit.data.local.OrbitDatabase
import com.example.orbit.data.local.TaskEntity
import com.example.orbit.model.AccentTone
import com.example.orbit.model.TaskCategory
import com.example.orbit.model.TaskDate
import com.example.orbit.model.TaskPriority
import com.example.orbit.model.ThemeMode
import com.example.orbit.ui.GoalViewModel
import com.example.orbit.ui.HabitViewModel
import com.example.orbit.ui.OrbitViewModel
import com.example.orbit.ui.TaskViewModel
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Seeds a portfolio-sized data set and captures the redesigned home in both
 * themes for visual review. Data is intentionally left in place so the build
 * can be inspected on the device afterwards; regular suites clear it first.
 */
@RunWith(AndroidJUnit4::class)
class HomeVisualReviewTest {

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

    @Before
    fun seed() {
        context.getSharedPreferences(
            OrbitPreferencesStore.PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        ).edit().clear().commit()
        rule.runOnIdle {
            preferencesViewModel.setThemeMode(ThemeMode.DEEP_SPACE)
            preferencesViewModel.setAccentTone(AccentTone.CYAN)
        }
        val database = OrbitDatabase.getInstance(rule.activity)
        database.clearAllTables()

        val today = TaskDate.todayEpochDay()
        val now = System.currentTimeMillis()
        runBlocking {
            val dao = database.taskDao()
            val rows = listOf(
                TaskEntity(
                    title = "完成作品集数据层",
                    description = "Room + Repository + 状态流",
                    priority = TaskPriority.HIGH,
                    category = TaskCategory.STUDY,
                    dueDate = today,
                    isCompleted = true,
                    createdAt = now - 86_400_000L,
                    completedAt = now - 2_400_000L,
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
                    title = "跑步 5 公里",
                    description = "晚间轻松跑",
                    priority = TaskPriority.LOW,
                    category = TaskCategory.HEALTH,
                    dueDate = today,
                    isCompleted = true,
                    createdAt = now - 10_800_000L,
                    completedAt = now - 1_800_000L,
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
                    title = "更新简历项目描述",
                    description = "突出 Compose 与 Room 实践",
                    priority = TaskPriority.HIGH,
                    category = TaskCategory.WORK,
                    dueDate = today,
                    createdAt = now - 3_600_000L,
                ),
                TaskEntity(
                    title = "给家人打电话",
                    description = "周末前确认回家时间",
                    priority = TaskPriority.LOW,
                    category = TaskCategory.LIFE,
                    dueDate = today,
                    createdAt = now - 1_800_000L,
                ),
            )
            rows.forEach { dao.insert(it) }

            val goals = GoalRepository(database)
            val portfolio = goals.createGoal(
                title = "完成个人作品集",
                description = "把三个项目整理成可投递的案例",
                tone = AccentTone.CYAN,
                targetDate = today + 40,
            )
            val caseStudy = goals.addStage(portfolio, "梳理案例", today - 10)
            val visual = goals.addStage(portfolio, "完成视觉稿", today + 12)
            goals.addStage(portfolio, "上线部署", today + 38)
            goals.setStageCompleted(caseStudy, true)

            val fitness = goals.createGoal(
                title = "保持每周运动",
                description = "每周至少三次，优先夜跑",
                tone = AccentTone.GOLD,
                targetDate = today + 90,
            )
            val training = goals.addStage(fitness, "八月训练周", today + 7)

            val reading = goals.createGoal(
                title = "读完十二本书",
                description = "技术书与小说交替阅读",
                tone = AccentTone.VIOLET,
                targetDate = today + 120,
            )
            goals.addStage(reading, "第三季度书单", today + 60)

            val stored = dao.observeAll().first().associateBy { it.title }
            stored["更新简历项目描述"]?.let {
                dao.update(it.copy(goalId = portfolio, stageId = visual))
            }
            stored["整理投递记录"]?.let {
                dao.update(it.copy(goalId = portfolio, stageId = visual))
            }
            stored["跑步 5 公里"]?.let {
                dao.update(it.copy(goalId = fitness, stageId = training))
            }

            val habits = HabitRepository(database)
            val readingHabit = habits.createHabit(
                title = "阅读 20 分钟",
                note = "睡前放下手机",
                tone = AccentTone.VIOLET,
            )
            val stretchHabit = habits.createHabit(
                title = "晨间拉伸",
                note = "起床后的第一件事",
                tone = AccentTone.CYAN,
            )
            val journalHabit = habits.createHabit(
                title = "记录三件小事",
                note = "睡前写下今天值得记住的瞬间",
                tone = AccentTone.GOLD,
            )
            habits.setChecked(readingHabit, today, true)
            habits.setChecked(stretchHabit, today, true)
            habits.setChecked(journalHabit, today - 1, true)
        }
        rule.waitUntil(TIMEOUT_MS) { taskViewModel.uiState.value.tasks.size == 6 }
        rule.waitUntil(TIMEOUT_MS) { goalViewModel.uiState.value.goals.size == 3 }
        rule.waitUntil(TIMEOUT_MS) { habitViewModel.uiState.value.habits.size == 3 }
        rule.waitForIdle()
    }

    @Test
    fun captureHome() {
        waitForContentDescription("更新简历项目描述，高优先级，待完成")
        capture("v2-home-top-dark.png")
        scrollTo("今日焦点")
        capture("v2-home-focus-dark.png")
        scrollTo("学习习惯")
        capture("v2-home-habits-dark.png")
        rule.onAllNodes(hasScrollToNodeAction()).onFirst()
            .performScrollToNode(hasText("今日学习任务"))
        rule.waitForIdle()
        capture("v2-home-tasks-dark.png")
        rule.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText("全部任务"))
        rule.waitForIdle()
        capture("v2-home-list-dark.png")

        rule.runOnIdle { preferencesViewModel.setThemeMode(ThemeMode.DAWN) }
        rule.waitForIdle()
        rule.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToIndex(0)
        rule.waitForIdle()
        capture("v2-home-top-light.png")
        scrollTo("今日焦点")
        capture("v2-home-focus-light.png")
    }

    private fun capture(fileName: String) {
        rule.waitForIdle()
        val directory = File(context.getExternalFilesDir(null), "screenshots_v2").apply { mkdirs() }
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        FileOutputStream(File(directory, fileName)).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                "Could not write $fileName"
            }
        }
    }

    private fun scrollTo(text: String) {
        rule.onAllNodes(hasScrollToNodeAction()).onFirst()
            .performScrollToNode(hasText(text))
        rule.waitForIdle()
    }

    private fun waitForContentDescription(description: String) {
        rule.waitUntil(TIMEOUT_MS) {
            rule.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 30_000L
    }
}
