package com.example.orbit

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
import com.example.orbit.model.GoalStatus
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
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Seeds a deterministic portfolio data set and writes screenshots into the
 * app's external files directory. It is not part of the normal assertion
 * suite: run it explicitly, then pull
 * `/sdcard/Android/data/com.example.orbit/files/screenshots` before the test
 * APK is uninstalled.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotGalleryTest {

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
    fun seedPortfolioData() {
        context.getSharedPreferences(
            OrbitPreferencesStore.PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        ).edit().clear().commit()
        rule.runOnIdle {
            preferencesViewModel.setThemeMode(ThemeMode.DEEP_SPACE)
            preferencesViewModel.setAccentTone(AccentTone.CYAN)
        }
        OrbitDatabase.getInstance(rule.activity).clearAllTables()

        val today = TaskDate.todayEpochDay()
        val now = System.currentTimeMillis()
        runBlocking {
            val dao = OrbitDatabase.getInstance(rule.activity).taskDao()
            val rows = listOf(
                TaskEntity(
                    title = "完成作品集数据层",
                    description = "Room + Repository + 状态流",
                    priority = TaskPriority.HIGH,
                    category = TaskCategory.STUDY,
                    dueDate = today,
                    isCompleted = true,
                    createdAt = now - 86_400_000L,
                    completedAt = millisOn(today, 0, 40),
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
                    completedAt = millisOn(today, 1, 20),
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
                TaskEntity(
                    title = "复盘本周面试",
                    priority = TaskPriority.MEDIUM,
                    category = TaskCategory.WORK,
                    dueDate = today + 1,
                    createdAt = now,
                ),
                TaskEntity(
                    title = "预约体检",
                    priority = TaskPriority.MEDIUM,
                    category = TaskCategory.HEALTH,
                    dueDate = today + 1,
                    createdAt = now - 600_000L,
                ),
                TaskEntity(
                    title = "归档旧项目",
                    priority = TaskPriority.LOW,
                    category = TaskCategory.OTHER,
                    dueDate = today - 1,
                    createdAt = now - 4 * 86_400_000L,
                ),
                TaskEntity(
                    title = "整理读书笔记",
                    priority = TaskPriority.MEDIUM,
                    category = TaskCategory.STUDY,
                    dueDate = today - 3,
                    isCompleted = true,
                    createdAt = now - 6 * 86_400_000L,
                    completedAt = millisOn(today - 3, 21, 0),
                ),
                TaskEntity(
                    title = "完成一次夜跑",
                    priority = TaskPriority.LOW,
                    category = TaskCategory.HEALTH,
                    dueDate = today - 1,
                    isCompleted = true,
                    createdAt = now - 8 * 86_400_000L,
                    completedAt = millisOn(today - 1, 20, 0),
                ),
                TaskEntity(
                    title = "提交课程作业",
                    priority = TaskPriority.HIGH,
                    category = TaskCategory.STUDY,
                    dueDate = today - 5,
                    isCompleted = true,
                    createdAt = now - 9 * 86_400_000L,
                    completedAt = millisOn(today - 5, 22, 0),
                ),
            )
            rows.forEach { dao.insert(it) }

            val goals = GoalRepository(OrbitDatabase.getInstance(rule.activity))
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
            goals.setStatus(fitness, GoalStatus.PAUSED)

            val reading = goals.createGoal(
                title = "读完十二本书",
                description = "技术书与小说交替阅读",
                tone = AccentTone.VIOLET,
                targetDate = today + 120,
            )
            val readingStage = goals.addStage(reading, "第三季度书单", today + 60)

            goals.createGoal(
                title = "准备秋招面试",
                description = "整理项目故事与算法题",
                tone = AccentTone.ROSE,
                targetDate = today + 60,
            )
            val animation = goals.createGoal(
                title = "学习 Compose 动画",
                description = "完成一套可复用动效笔记",
                tone = AccentTone.CYAN,
                targetDate = today + 30,
            )
            val animationStart = goals.addStage(animation, "手势与弹簧", today - 5)
            val animationEnd = goals.addStage(animation, "页面转场", today + 20)
            goals.setStageCompleted(animationStart, true)
            goals.setStageCompleted(animationEnd, true)
            goals.createGoal(
                title = "建立个人品牌",
                description = null,
                tone = AccentTone.GOLD,
                targetDate = null,
            )

            val stored = dao.observeAll().first().associateBy { it.title }
            stored["完成作品集数据层"]?.let {
                dao.update(it.copy(goalId = portfolio, stageId = visual))
            }
            stored["整理投递记录"]?.let {
                dao.update(it.copy(goalId = portfolio, stageId = visual))
            }
            stored["跑步 5 公里"]?.let {
                dao.update(it.copy(goalId = fitness, stageId = training))
            }
            stored["完成一次夜跑"]?.let {
                dao.update(it.copy(goalId = fitness, stageId = training))
            }
            stored["阅读《Android 编程权威指南》"]?.let {
                dao.update(it.copy(goalId = reading, stageId = readingStage))
            }
            stored["整理读书笔记"]?.let {
                dao.update(it.copy(goalId = reading, stageId = readingStage))
            }

            val database = OrbitDatabase.getInstance(rule.activity)
            val habitRepository = HabitRepository(database)
            val readingHabit = habitRepository.createHabit(
                title = "阅读 20 分钟",
                note = "睡前放下手机",
                tone = AccentTone.VIOLET,
            )
            val stretchHabit = habitRepository.createHabit(
                title = "晨间拉伸",
                note = "起床后的第一件事",
                tone = AccentTone.CYAN,
            )
            val journalHabit = habitRepository.createHabit(
                title = "记录三件小事",
                note = "睡前写下今天值得记住的瞬间",
                tone = AccentTone.GOLD,
            )
            val sleepHabit = habitRepository.createHabit(
                title = "23:30 前入睡",
                note = null,
                tone = AccentTone.ROSE,
            )

            // Give every habit a believable history so the heatmap is not empty.
            val habitDao = database.habitDao()
            habitDao.observeAll().first().forEach { entity ->
                habitDao.update(entity.copy(createdAt = now - 40 * 86_400_000L))
            }

            suspend fun check(habitId: Long, offsets: List<Long>) {
                offsets.forEach { offset ->
                    habitRepository.setChecked(habitId, today - offset, true)
                }
            }

            check(
                readingHabit,
                listOf(0, 1, 2, 3, 5, 6, 7, 9, 10, 12, 14, 15, 17, 18, 20, 22, 24, 26, 28, 30),
            )
            check(stretchHabit, listOf(0, 1, 3, 4, 6, 8, 9, 11, 13, 15))
            check(journalHabit, listOf(1, 2, 4, 5, 7, 10))
            check(sleepHabit, listOf(20, 21, 22))
            habitRepository.setArchived(sleepHabit, true)
        }
        rule.waitUntil(TIMEOUT_MS) {
            taskViewModel.uiState.value.tasks.size == 12
        }
        rule.waitUntil(TIMEOUT_MS) {
            goalViewModel.uiState.value.goals.size == 6
        }
        rule.waitUntil(TIMEOUT_MS) {
            habitViewModel.uiState.value.habits.size == 4
        }
        rule.waitForIdle()
    }

    @After
    fun cleanUp() {
        OrbitDatabase.getInstance(rule.activity).clearAllTables()
        context.getSharedPreferences(
            OrbitPreferencesStore.PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        ).edit().clear().commit()
    }

    @Test
    fun capturePortfolioScreens() {
        val detailTitle = "更新简历项目描述"
        waitForContentDescription("$detailTitle，高优先级，待完成")
        capture("01-home-dark.png")

        scrollTo("学习习惯")
        scrollTo("记录三件小事")
        capture("12-home-habits-dark.png")
        rule.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToIndex(0)
        rule.waitForIdle()

        rule.onNodeWithContentDescription("$detailTitle，高优先级，待完成").performClick()
        waitForText("节点详情")
        waitUntilDisplayed("编辑任务信息")
        capture("02-node-detail-dark.png")
        rule.onNodeWithContentDescription("关闭任务详情").performClick()
        waitForTextGone("节点详情")

        navigateTo("目标")
        rule.onNode(hasText("任务") and isSelectable()).performClick()
        waitForText("ORBIT / GOALS & PLAN")
        rule.waitForIdle()
        capture("03-tasks-dark.png")

        rule.onNode(hasText("习惯") and isSelectable()).performClick()
        waitForText("今日打卡")
        rule.waitForIdle()
        capture("13-habits-planner-dark.png")
        scrollTo("已归档")
        capture("14-habits-archived-dark.png")
        rule.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToIndex(0)
        rule.waitForIdle()

        navigateTo("目标")
        waitForText("完成个人作品集")
        rule.waitForIdle()
        capture("09-goals-dark.png")
        scrollTo("准备秋招面试")
        capture("10-goals-list-dark.png")
        rule.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToIndex(0)
        rule.waitForIdle()
        rule.onAllNodesWithContentDescription("完成个人作品集，进度 50%，进行中")
            .onFirst()
            .performClick()
        waitForText("ORBIT / GOALS / DETAIL")
        rule.waitForIdle()
        capture("18-goal-detail-dark.png")
        rule.onNodeWithContentDescription("返回目标列表").performClick()
        waitForText("ORBIT / GOALS & PLAN")

        navigateTo("成长")
        waitForText("ORBIT / GROWTH")
        waitForText("成长等级")
        rule.waitForIdle()
        capture("16-growth-overview-dark.png")
        rule.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToIndex(3)
        rule.waitForIdle()
        capture("04-stats-overview-dark.png")
        rule.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToIndex(5)
        rule.waitForIdle()
        capture("05-stats-categories-dark.png")
        scrollTo("习惯一致性")
        capture("17-growth-habits-dark.png")

        navigateTo("我的")
        waitForText("ORBIT / SETTINGS")
        rule.waitForIdle()
        capture("06-settings-dark.png")
        scrollTo("数字与云端保持同步", substring = true)
        capture("19-profile-overview-dark.png")
        rule.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToIndex(0)
        rule.waitForIdle()

        scrollTo("晨光")
        rule.runOnIdle { preferencesViewModel.setThemeMode(ThemeMode.DAWN) }
        waitUntilSelected("晨光")
        capture("07-settings-dawn.png")

        navigateTo("星轨")
        waitForContentDescription("$detailTitle，高优先级，待完成")
        // Opening the detail scrolled the list to the card; return to the top
        // so the light-theme screenshot shows the header and full orbit.
        rule.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToIndex(0)
        rule.waitForIdle()
        capture("08-home-dawn.png")

        navigateTo("目标")
        waitForText("完成个人作品集")
        rule.waitForIdle()
        capture("11-goals-dawn.png")

        navigateTo("目标")
        rule.onNode(hasText("任务") and isSelectable()).performClick()
        waitForText("ORBIT / GOALS & PLAN")
        rule.onNode(hasText("习惯") and isSelectable()).performClick()
        waitForText("今日打卡")
        rule.waitForIdle()
        capture("15-habits-dawn.png")

        navigateTo("成长")
        rule.waitForIdle()
        // The previous visit left the restored list at the goal card.
        rule.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToIndex(0)
        rule.waitForIdle()
        waitForText("成长等级")
        capture("20-growth-dawn.png")
    }

    private fun capture(fileName: String) {
        rule.waitForIdle()
        val directory = File(context.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        FileOutputStream(File(directory, fileName)).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                "Could not write $fileName"
            }
        }
    }

    private fun navigateTo(label: String) {
        rule.navigateToBottomTab(label)
    }

    private fun scrollTo(text: String, substring: Boolean = false) {
        rule.onAllNodes(hasScrollToNodeAction()).onFirst()
            .performScrollToNode(hasText(text, substring = substring))
        rule.waitForIdle()
    }

    private fun waitUntilDisplayed(text: String) {
        rule.waitUntil(TIMEOUT_MS) {
            runCatching {
                rule.onNodeWithText(text).performScrollTo().assertExists()
            }.isSuccess
        }
    }

    private fun waitUntilSelected(label: String) {
        rule.waitUntil(TIMEOUT_MS) {
            rule.onAllNodes(hasText(label) and isSelectable() and isSelected())
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun waitForText(text: String) {
        rule.waitUntil(TIMEOUT_MS) {
            rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForTextGone(text: String) {
        rule.waitUntil(TIMEOUT_MS) {
            rule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()
        }
    }

    private fun waitForContentDescription(description: String) {
        rule.waitUntil(TIMEOUT_MS) {
            rule.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Local wall-clock time on [epochDay], independent of the machine time zone. */
    private fun millisOn(epochDay: Long, hour: Int, minute: Int): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            timeInMillis = epochDay * TaskDate.MILLIS_PER_DAY
        }
        return Calendar.getInstance().apply {
            clear()
            set(
                utc.get(Calendar.YEAR),
                utc.get(Calendar.MONTH),
                utc.get(Calendar.DAY_OF_MONTH),
                hour,
                minute,
            )
        }.timeInMillis
    }

    private companion object {
        const val TIMEOUT_MS = 30_000L
    }
}
