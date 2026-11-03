package com.example.orbit.ai

import com.example.orbit.model.TaskPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPlanImportTest {

    private val today = 20_000L

    private fun plan(
        title: String = "三个月学会 Compose",
        description: String = "从语法到上架",
        target: Long = today + 80,
        stages: List<AiPlanStage> = listOf(stage()),
    ) = AiPlan(
        goalTitle = title,
        goalDescription = description,
        targetDateEpochDay = target,
        stages = stages,
    )

    private fun stage(
        title: String = "基础",
        target: Long = today + 30,
        tasks: List<AiPlanTask> = listOf(task()),
    ) = AiPlanStage(title = title, targetDateEpochDay = target, tasks = tasks)

    private fun task(
        title: String = "读完官方入门文档",
        due: Long = today + 5,
        priority: String = "HIGH",
        description: String = "每天 45 分钟",
    ) = AiPlanTask(
        title = title,
        dueDateEpochDay = due,
        priority = priority,
        description = description,
    )

    @Test
    fun mapsTitlesDescriptionsAndPriorities() {
        val import = AiPlanImport.toImportPlan(plan(), fallbackTitle = "兜底", today = today)!!

        assertEquals("三个月学会 Compose", import.title)
        assertEquals("从语法到上架", import.description)
        assertEquals(today + 80, import.targetDate!!)
        assertEquals(1, import.stageCount)
        assertEquals(1, import.taskCount)

        val stage = import.stages.single()
        assertEquals("基础", stage.title)
        assertEquals(today + 30, stage.targetDate!!)
        val task = stage.tasks.single()
        assertEquals("读完官方入门文档", task.title)
        assertEquals("每天 45 分钟", task.description)
        assertEquals(TaskPriority.HIGH, task.priority)
        assertEquals(today + 5, task.dueDate!!)
    }

    @Test
    fun fallsBackToTheUserGoalWhenTheModelReturnsNoTitle() {
        val import = AiPlanImport.toImportPlan(
            plan(title = "   "),
            fallbackTitle = "  我自己写的目标  ",
            today = today,
        )!!

        assertEquals("我自己写的目标", import.title)
    }

    @Test
    fun returnsNullWhenTheFallbackIsAlsoBlank() {
        assertNull(
            AiPlanImport.toImportPlan(
                plan(title = ""),
                fallbackTitle = " ",
                today = today,
            ),
        )
    }

    @Test
    fun dropsBlankStagesAndEmptyStages() {
        val import = AiPlanImport.toImportPlan(
            plan(
                stages = listOf(
                    stage(title = "  "),
                    stage(title = "没有任务的阶段", tasks = emptyList()),
                    stage(title = "有效阶段"),
                ),
            ),
            fallbackTitle = "目标",
            today = today,
        )!!

        assertEquals(listOf("有效阶段"), import.stages.map { it.title })
    }

    @Test
    fun dropsBlankTasksAndReturnsNullWhenNothingSurvives() {
        val import = AiPlanImport.toImportPlan(
            plan(
                stages = listOf(
                    stage(
                        tasks = listOf(
                            task(title = "  "),
                            task(title = "保留这项"),
                        ),
                    ),
                ),
            ),
            fallbackTitle = "目标",
            today = today,
        )!!
        assertEquals(listOf("保留这项"), import.stages.single().tasks.map { it.title })

        assertNull(
            AiPlanImport.toImportPlan(
                plan(stages = listOf(stage(tasks = listOf(task(title = " "))))),
                fallbackTitle = "目标",
                today = today,
            ),
        )
    }

    @Test
    fun clampsDatesIntoTheSupportedHorizon() {
        val import = AiPlanImport.toImportPlan(
            plan(
                target = today - 10,
                stages = listOf(
                    stage(
                        target = today + 5_000,
                        tasks = listOf(
                            task(due = today - 3),
                            task(due = today + 5_000),
                        ),
                    ),
                ),
            ),
            fallbackTitle = "目标",
            today = today,
        )!!

        assertEquals(today, import.targetDate!!)
        assertEquals(
            today + AiPlanImport.HORIZON_DAYS,
            import.stages.single().targetDate!!,
        )
        assertEquals(today, import.stages.single().tasks[0].dueDate!!)
        assertEquals(
            today + AiPlanImport.HORIZON_DAYS,
            import.stages.single().tasks[1].dueDate!!,
        )
    }

    @Test
    fun zeroDatesBecomeNull() {
        val import = AiPlanImport.toImportPlan(
            plan(
                target = 0,
                stages = listOf(
                    stage(
                        target = 0,
                        tasks = listOf(task(due = 0)),
                    ),
                ),
            ),
            fallbackTitle = "目标",
            today = today,
        )!!

        assertNull(import.targetDate)
        assertNull(import.stages.single().targetDate)
        assertNull(import.stages.single().tasks.single().dueDate)
    }

    @Test
    fun truncatesLongText() {
        val import = AiPlanImport.toImportPlan(
            plan(
                title = "目".repeat(100),
                description = "描".repeat(400),
                stages = listOf(
                    stage(
                        title = "阶".repeat(100),
                        tasks = listOf(
                            task(
                                title = "任".repeat(120),
                                description = "说".repeat(200),
                            ),
                        ),
                    ),
                ),
            ),
            fallbackTitle = "目标",
            today = today,
        )!!

        assertEquals(60, import.title.length)
        assertEquals(AiPlanImport.MAX_GOAL_DESCRIPTION, import.description!!.length)
        assertEquals(60, import.stages.single().title.length)
        assertEquals(80, import.stages.single().tasks.single().title.length)
        assertEquals(
            AiPlanImport.MAX_TASK_DESCRIPTION,
            import.stages.single().tasks.single().description!!.length,
        )
    }

    @Test
    fun blankOptionalTextBecomesNull() {
        val import = AiPlanImport.toImportPlan(
            plan(description = "  ", stages = listOf(stage(tasks = listOf(task(description = " "))))),
            fallbackTitle = "目标",
            today = today,
        )!!

        assertNull(import.description)
        assertNull(import.stages.single().tasks.single().description)
        assertTrue(import.taskCount == 1)
    }
}
