package com.example.orbit.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.orbit.data.local.OrbitDatabase
import com.example.orbit.model.AccentTone
import com.example.orbit.model.GoalImportPlan
import com.example.orbit.model.GoalImportStage
import com.example.orbit.model.GoalImportTask
import com.example.orbit.model.GoalStatus
import com.example.orbit.model.TaskCategory
import com.example.orbit.model.TaskDate
import com.example.orbit.model.TaskPriority
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrbitRepositoriesTest {

    private lateinit var database: OrbitDatabase
    private lateinit var tasks: TaskRepository
    private lateinit var goals: GoalRepository
    private lateinit var habits: HabitRepository
    private lateinit var growth: GrowthRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, OrbitDatabase::class.java).build()
        tasks = TaskRepository(database)
        goals = GoalRepository(database)
        habits = HabitRepository(database)
        growth = GrowthRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun taskCompletionAwardsXpOnlyOnceEvenAfterReopen() = runBlocking {
        val id = createTask(priority = TaskPriority.HIGH)

        tasks.setCompleted(id, true)
        tasks.setCompleted(id, false)
        tasks.setCompleted(id, true)

        val events = growth.observeEvents().first().filter { it.sourceKey == "task:$id" }
        assertEquals(1, events.size)
        assertEquals(15, events.single().xp)
        assertTrue(tasks.getTask(id)!!.isCompleted)
    }

    @Test
    fun stageMustBelongToTheSelectedGoal() = runBlocking {
        val goalA = createGoal("目标 A")
        val goalB = createGoal("目标 B")
        val stageB = goals.addStage(goalB, "B 的阶段")

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                tasks.createTask(
                    title = "跨目标任务",
                    description = null,
                    priority = TaskPriority.MEDIUM,
                    category = TaskCategory.WORK,
                    dueDate = null,
                    goalId = goalA,
                    stageId = stageB,
                )
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                tasks.createTask(
                    title = "无目标阶段",
                    description = null,
                    priority = TaskPriority.MEDIUM,
                    category = TaskCategory.WORK,
                    dueDate = null,
                    goalId = null,
                    stageId = stageB,
                )
            }
        }
        assertEquals(0, tasks.observeTasks().first().size)
    }

    @Test
    fun deletingGoalCascadesStagesAndDetachesTasks() = runBlocking {
        val goalId = createGoal("准备求职")
        val stageId = goals.addStage(goalId, "更新简历")
        val taskId = tasks.createTask(
            title = "改写项目描述",
            description = null,
            priority = TaskPriority.HIGH,
            category = TaskCategory.WORK,
            dueDate = null,
            goalId = goalId,
            stageId = stageId,
        )

        val info = goals.deletionInfo(goalId)!!
        assertEquals(1, info.stageCount)
        assertEquals(1, info.taskCount)

        goals.deleteGoal(goalId)

        assertNull(goals.getGoal(goalId))
        assertEquals(0, goals.observeStages().first().size)
        val detached = tasks.getTask(taskId)!!
        assertNull(detached.goalId)
        assertNull(detached.stageId)
        assertEquals(1, tasks.observeTasks().first().size)
    }

    @Test
    fun deletingStageKeepsTheGoalLinkOnTasks() = runBlocking {
        val goalId = createGoal("准备求职")
        val stageId = goals.addStage(goalId, "更新简历")
        val taskId = tasks.createTask(
            title = "改写项目描述",
            description = null,
            priority = TaskPriority.MEDIUM,
            category = TaskCategory.WORK,
            dueDate = null,
            goalId = goalId,
            stageId = stageId,
        )

        goals.deleteStage(stageId)

        val detached = tasks.getTask(taskId)!!
        assertEquals(goalId, detached.goalId)
        assertNull(detached.stageId)
    }

    @Test
    fun stageAndGoalCompletionEachAwardOnce() = runBlocking {
        val goalId = createGoal("完成作品集")
        val stageId = goals.addStage(goalId, "数据层")

        goals.setStageCompleted(stageId, true)
        goals.setStageCompleted(stageId, false)
        goals.setStageCompleted(stageId, true)
        goals.setStatus(goalId, GoalStatus.COMPLETED)
        goals.setStatus(goalId, GoalStatus.ACTIVE)
        goals.setStatus(goalId, GoalStatus.COMPLETED)

        val events = growth.observeEvents().first()
        assertEquals(1, events.count { it.sourceKey == "stage:$stageId" })
        assertEquals(1, events.count { it.sourceKey == "goal:$goalId" })
        assertEquals(40 + 120, events.sumOf { it.xp })
    }

    @Test
    fun habitCheckInIsUniqueAndEarnedXpSurvivesUncheck() = runBlocking {
        val habitId = habits.createHabit("晨间阅读", null, AccentTone.VIOLET)
        val today = TaskDate.todayEpochDay()

        habits.setChecked(habitId, today, true)
        habits.setChecked(habitId, today, true)
        assertEquals(1, database.habitRecordDao().count())
        assertEquals(8, database.growthDao().totalXp())

        habits.setChecked(habitId, today, false)
        assertEquals(0, database.habitRecordDao().count())
        assertEquals(8, database.growthDao().totalXp())

        habits.setChecked(habitId, today, true)
        assertEquals(1, database.habitRecordDao().count())
        assertEquals(8, database.growthDao().totalXp())
    }

    @Test
    fun futureCheckInIsRejectedAndDeleteCascadesRecords() = runBlocking {
        val habitId = habits.createHabit("夜跑", "轻松跑", AccentTone.CYAN)
        val today = TaskDate.todayEpochDay()

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { habits.setChecked(habitId, today + 1, true) }
        }
        habits.setChecked(habitId, today, true)
        habits.setChecked(habitId, today - 1, true)
        assertEquals(2, database.habitRecordDao().count())

        habits.deleteHabit(habitId)
        assertEquals(0, database.habitRecordDao().count())
        assertTrue(habits.observeHabits().first().isEmpty())
    }

    @Test
    fun importPlanWritesGoalStagesAndStudyTasks() = runBlocking {
        val today = TaskDate.todayEpochDay()
        val plan = GoalImportPlan(
            title = "  8 周 Java 后端  ",
            description = "AI 生成的计划",
            tone = AccentTone.CYAN,
            targetDate = today + 56,
            stages = listOf(
                GoalImportStage(
                    title = "基础",
                    targetDate = today + 21,
                    tasks = listOf(
                        GoalImportTask(
                            title = "Java 语法复习",
                            description = "每天 30 分钟",
                            priority = TaskPriority.HIGH,
                            dueDate = today + 3,
                        ),
                        GoalImportTask(
                            title = "SQL 练习",
                            priority = TaskPriority.MEDIUM,
                            dueDate = today + 7,
                        ),
                    ),
                ),
                GoalImportStage(
                    title = "项目",
                    targetDate = today + 56,
                    tasks = listOf(
                        GoalImportTask(
                            title = "完成 CRUD 接口",
                            priority = TaskPriority.MEDIUM,
                            dueDate = today + 35,
                        ),
                    ),
                ),
            ),
        )

        val goalId = goals.importPlan(plan)

        val goal = goals.getGoal(goalId)!!
        assertEquals("8 周 Java 后端", goal.title)
        assertEquals("AI 生成的计划", goal.description)
        assertEquals(today + 56, goal.targetDate!!)
        assertEquals(2, goals.observeStages().first().count { it.goalId == goalId })

        val imported = tasks.observeTasks().first().filter { it.goalId == goalId }
        assertEquals(3, imported.size)
        assertTrue(imported.all { it.category == TaskCategory.STUDY })
        val first = imported.first { it.title == "Java 语法复习" }
        assertEquals(TaskPriority.HIGH, first.priority)
        assertEquals(today + 3, first.dueDate!!)
        assertEquals("每天 30 分钟", first.description)
        assertTrue(imported.all { it.stageId != null })
        assertTrue(growth.observeEvents().first().isEmpty())
    }

    @Test
    fun importPlanRejectsAnEmptyPlan() = runBlocking {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                goals.importPlan(
                    GoalImportPlan(
                        title = "空计划",
                        stages = emptyList(),
                    ),
                )
            }
        }
        assertEquals(0, goals.observeGoals().first().size)
    }

    private suspend fun createGoal(title: String): Long = goals.createGoal(
        title = title,
        description = null,
        tone = AccentTone.CYAN,
        targetDate = null,
    )

    private suspend fun createTask(priority: TaskPriority): Long = tasks.createTask(
        title = "完成作品集",
        description = null,
        priority = priority,
        category = TaskCategory.STUDY,
        dueDate = null,
    )
}
