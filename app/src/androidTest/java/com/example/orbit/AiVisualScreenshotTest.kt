package com.example.orbit

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.orbit.ai.AiLimits
import com.example.orbit.ai.AiPlan
import com.example.orbit.ai.AiPlanStage
import com.example.orbit.ai.AiPlanTask
import com.example.orbit.ai.AiReview
import com.example.orbit.ai.AiReviewAction
import com.example.orbit.ai.AiStatus
import com.example.orbit.ai.AiUsage
import com.example.orbit.ai.CachedAiReview
import com.example.orbit.model.GrowthEvent
import com.example.orbit.model.GrowthEventType
import com.example.orbit.model.Task
import com.example.orbit.model.TaskCategory
import com.example.orbit.model.TaskDate
import com.example.orbit.model.TaskPriority
import com.example.orbit.ui.AiPlanFormState
import com.example.orbit.ui.AiPlanPhase
import com.example.orbit.ui.AiPlanUiState
import com.example.orbit.ui.AiReviewUiState
import com.example.orbit.ui.GoalLoadStatus
import com.example.orbit.ui.GoalUiState
import com.example.orbit.ui.GrowthLoadStatus
import com.example.orbit.ui.GrowthUiState
import com.example.orbit.ui.HabitLoadStatus
import com.example.orbit.ui.HabitUiState
import com.example.orbit.ui.TaskLoadStatus
import com.example.orbit.ui.TaskUiState
import com.example.orbit.ui.components.SpaceBackdrop
import com.example.orbit.ui.screens.AiPlanScreen
import com.example.orbit.ui.screens.GrowthScreen
import com.example.orbit.ui.theme.OrbitTheme
import java.io.File
import java.io.FileOutputStream
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Renders the AI plan/review surfaces with deterministic state and writes
 * screenshots for the portfolio. It never contacts the backend and is separate
 * from the assertion suite; run it explicitly and pull
 * `/sdcard/Android/data/com.example.orbit/files/screenshots`.
 */
@RunWith(AndroidJUnit4::class)
class AiVisualScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val today: Long = TaskDate.todayEpochDay()

    @Test
    fun capturePlanForm() {
        rule.setContent {
            OrbitTheme {
                SpaceBackdrop {
                    AiPlanScreen(
                        uiState = AiPlanUiState(
                            phase = AiPlanPhase.FORM,
                            status = configuredStatus(planUsed = 1),
                            form = AiPlanFormState(
                                goal = "三个月内掌握 Android 开发，能独立完成并上架一个应用",
                                targetDate = today + 56,
                                dailyMinutes = 60,
                                currentLevel = "会 Kotlin 基础语法，做过课程小练习",
                                weakPoints = "状态管理、接口联调、英文文档阅读",
                            ),
                        ),
                        onBack = {},
                        onGoalChange = {},
                        onTargetDateChange = {},
                        onDailyMinutesChange = {},
                        onCurrentLevelChange = {},
                        onWeakPointsChange = {},
                        onGenerate = {},
                        onToggleStage = {},
                        onToggleTask = { _, _ -> },
                        onImport = {},
                        onDiscard = {},
                        onRetryStatus = {},
                    )
                }
            }
        }
        rule.waitUntil(TIMEOUT_MS) {
            rule.onAllNodesWithText("计划参数").fetchSemanticsNodes().isNotEmpty()
        }
        capture("ai-01-plan-form.png")
    }

    @Test
    fun capturePlanPreview() {
        val plan = samplePlan()
        rule.setContent {
            OrbitTheme {
                SpaceBackdrop {
                    AiPlanScreen(
                        uiState = AiPlanUiState(
                            phase = AiPlanPhase.PREVIEW,
                            status = configuredStatus(planUsed = 2),
                            plan = plan,
                        ),
                        onBack = {},
                        onGoalChange = {},
                        onTargetDateChange = {},
                        onDailyMinutesChange = {},
                        onCurrentLevelChange = {},
                        onWeakPointsChange = {},
                        onGenerate = {},
                        onToggleStage = {},
                        onToggleTask = { _, _ -> },
                        onImport = {},
                        onDiscard = {},
                        onRetryStatus = {},
                    )
                }
            }
        }
        capture("ai-02-plan-preview-top.png")

        scrollTo("收尾与复盘")
        capture("ai-03-plan-preview-bottom.png")
    }

    @Test
    fun captureWeeklyReview() {
        rule.setContent {
            OrbitTheme {
                SpaceBackdrop {
                    GrowthScreen(
                        taskState = reviewTaskState(),
                        habitState = HabitUiState(
                            loadStatus = HabitLoadStatus.READY,
                            todayEpochDay = today,
                        ),
                        goalState = GoalUiState(loadStatus = GoalLoadStatus.READY),
                        growthState = reviewGrowthState(),
                        onRetry = {},
                        onAddTask = {},
                        cloudSignedIn = true,
                        reviewState = AiReviewUiState(
                            hasData = true,
                            weekStartEpochDay = today - 2,
                            cached = CachedAiReview(
                                requestId = "screenshot",
                                generatedAt = System.currentTimeMillis(),
                                promptVersion = "review-v1",
                                review = AiReview(
                                    headline = "完成率 60%，节奏比上周更稳",
                                    summary = "本周完成了一半以上的计划，学习类任务是主要投入。习惯打卡集中在工作日，周末容易断档。",
                                    wins = listOf(
                                        "学习类任务完成 2 项，目标推进稳定",
                                        "连续 3 天保持习惯打卡",
                                    ),
                                    risks = listOf("周末任务容易堆积，注意提前拆分"),
                                    nextWeek = listOf(
                                        AiReviewAction(
                                            title = "把周末任务提前到周五",
                                            reason = "避免两天堆积造成拖延",
                                        ),
                                        AiReviewAction(
                                            title = "每天固定 30 分钟复习",
                                            reason = "巩固本周学的接口设计",
                                        ),
                                    ),
                                ),
                            ),
                            configured = true,
                            configuredKnown = true,
                            reviewRemaining = 3,
                        ),
                        onGenerateReview = {},
                        onRetryReviewStatus = {},
                    )
                }
            }
        }

        scrollTo("AI REVIEW")
        capture("ai-04-review-card.png")
    }

    @Test
    fun captureReviewPrompt() {
        rule.setContent {
            OrbitTheme {
                SpaceBackdrop {
                    GrowthScreen(
                        taskState = reviewTaskState(),
                        habitState = HabitUiState(
                            loadStatus = HabitLoadStatus.READY,
                            todayEpochDay = today,
                        ),
                        goalState = GoalUiState(loadStatus = GoalLoadStatus.READY),
                        growthState = reviewGrowthState(),
                        onRetry = {},
                        onAddTask = {},
                        cloudSignedIn = true,
                        reviewState = AiReviewUiState(
                            hasData = true,
                            weekStartEpochDay = today - 2,
                            configured = true,
                            configuredKnown = true,
                            reviewRemaining = 4,
                        ),
                        onGenerateReview = {},
                        onRetryReviewStatus = {},
                    )
                }
            }
        }

        scrollTo("AI REVIEW")
        capture("ai-05-review-prompt.png")
    }

    private fun configuredStatus(planUsed: Int): AiStatus = AiStatus(
        configured = true,
        planPromptVersion = "plan-v1",
        reviewPromptVersion = "review-v1",
        timeoutSeconds = 25,
        limits = AiLimits(plan = 6, review = 4),
        usage = AiUsage(planUsed = planUsed, reviewUsed = 1),
        dayKey = "2026-10-28",
    )

    private fun samplePlan(): AiPlan = AiPlan(
        goalTitle = "8 周复习 Java 后端，做出一个可演示的 CRUD 项目",
        goalDescription = "按基础、项目、收尾三段推进，每天 60 分钟。",
        targetDateEpochDay = today + 56,
        stages = listOf(
            AiPlanStage(
                title = "基础巩固",
                targetDateEpochDay = today + 14,
                tasks = listOf(
                    AiPlanTask(
                        title = "复习 Java 语法与集合",
                        dueDateEpochDay = today + 2,
                        priority = "HIGH",
                        description = "每天 30 分钟，重点补泛型与 Stream",
                    ),
                    AiPlanTask(
                        title = "手写 20 道 SQL 练习题",
                        dueDateEpochDay = today + 7,
                        priority = "MEDIUM",
                        description = "重点 JOIN、索引与分页",
                    ),
                ),
            ),
            AiPlanStage(
                title = "项目实战",
                targetDateEpochDay = today + 42,
                tasks = listOf(
                    AiPlanTask(
                        title = "搭好 Spring Boot 项目骨架",
                        dueDateEpochDay = today + 16,
                        priority = "HIGH",
                        description = "分层、统一错误体、配置环境变量",
                    ),
                    AiPlanTask(
                        title = "完成用户与任务两组接口",
                        dueDateEpochDay = today + 28,
                        priority = "HIGH",
                        description = "包含参数校验与 401/409 处理",
                    ),
                    AiPlanTask(
                        title = "补一组接口测试",
                        dueDateEpochDay = today + 35,
                        priority = "MEDIUM",
                        description = "覆盖成功、校验失败和冲突",
                    ),
                ),
            ),
            AiPlanStage(
                title = "收尾与复盘",
                targetDateEpochDay = today + 56,
                tasks = listOf(
                    AiPlanTask(
                        title = "写一份接口文档与部署说明",
                        dueDateEpochDay = today + 48,
                        priority = "MEDIUM",
                        description = "让面试官能直接跑起来",
                    ),
                    AiPlanTask(
                        title = "录一段 3 分钟演示",
                        dueDateEpochDay = today + 54,
                        priority = "LOW",
                        description = "覆盖登录、同步与 AI 规划",
                    ),
                ),
            ),
        ),
    )

    private fun reviewTaskState(): TaskUiState = TaskUiState(
        tasks = listOf(
            Task(
                id = 1,
                title = "完成作品集任务页数据层",
                priority = TaskPriority.HIGH,
                category = TaskCategory.STUDY,
                dueDate = today,
                createdAt = 1L,
            ),
            Task(
                id = 2,
                title = "整理本周投递记录",
                priority = TaskPriority.MEDIUM,
                category = TaskCategory.WORK,
                dueDate = today,
                isCompleted = true,
                createdAt = 2L,
                completedAt = System.currentTimeMillis(),
            ),
            Task(
                id = 3,
                title = "复习接口设计",
                priority = TaskPriority.HIGH,
                category = TaskCategory.STUDY,
                dueDate = today - 1,
                isCompleted = true,
                createdAt = 3L,
                completedAt = System.currentTimeMillis(),
            ),
        ),
        loadStatus = TaskLoadStatus.READY,
        todayEpochDay = today,
    )

    private fun reviewGrowthState(): GrowthUiState = GrowthUiState(
        events = listOf(
            GrowthEvent(
                type = GrowthEventType.TASK_COMPLETED,
                sourceKey = "task:2",
                xp = 10,
                occurredAt = System.currentTimeMillis(),
                epochDay = today,
            ),
            GrowthEvent(
                type = GrowthEventType.TASK_COMPLETED,
                sourceKey = "task:3",
                xp = 15,
                occurredAt = System.currentTimeMillis(),
                epochDay = today - 1,
            ),
        ),
        loadStatus = GrowthLoadStatus.READY,
        todayEpochDay = today,
    )

    private fun scrollTo(text: String) {
        rule.waitForIdle()
        rule.onAllNodes(hasScrollToNodeAction()).onFirst()
            .performScrollToNode(hasText(text))
        rule.waitForIdle()
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

    private companion object {
        const val TIMEOUT_MS = 30_000L
    }
}
