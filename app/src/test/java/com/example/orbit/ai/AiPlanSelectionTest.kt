package com.example.orbit.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPlanSelectionTest {

    private val plan = AiPlan(
        goalTitle = "目标",
        stages = listOf(
            AiPlanStage(
                title = "阶段一",
                tasks = listOf(AiPlanTask(title = "A"), AiPlanTask(title = "B")),
            ),
            AiPlanStage(
                title = "阶段二",
                tasks = listOf(AiPlanTask(title = "C")),
            ),
        ),
    )

    @Test
    fun defaultsToEverythingSelected() {
        assertEquals(3, AiPlanSelection.selectedTaskCount(plan, emptySet()))
        assertTrue(AiPlanSelection.isStageFullySelected(plan, emptySet(), 0))
        assertTrue(AiPlanSelection.isStageFullySelected(plan, emptySet(), 1))
        assertFalse(AiPlanSelection.isStageEmpty(plan, emptySet(), 0))
    }

    @Test
    fun togglingOneTaskOnlyAffectsThatKey() {
        val excluded = AiPlanSelection.toggleTask(emptySet(), 0, 1)

        assertEquals(setOf("0:1"), excluded)
        assertEquals(2, AiPlanSelection.selectedTaskCount(plan, excluded))
        assertFalse(AiPlanSelection.isStageFullySelected(plan, excluded, 0))
        assertFalse(AiPlanSelection.isStageEmpty(plan, excluded, 0))
        assertTrue(AiPlanSelection.key(1, 0) !in excluded)
    }

    @Test
    fun togglingAnEmptyStageSelectsEverything() {
        val excluded = setOf("0:0", "0:1")
        assertTrue(AiPlanSelection.isStageEmpty(plan, excluded, 0))

        val restored = AiPlanSelection.toggleStage(plan, excluded, 0)
        assertTrue(AiPlanSelection.isStageFullySelected(plan, restored, 0))
        assertTrue(AiPlanSelection.isStageFullySelected(plan, restored, 1))
        assertEquals(3, AiPlanSelection.selectedTaskCount(plan, restored))
    }

    @Test
    fun togglingAFullStageClearsIt() {
        val excluded = AiPlanSelection.toggleStage(plan, emptySet(), 1)

        assertEquals(setOf("1:0"), excluded)
        assertTrue(AiPlanSelection.isStageEmpty(plan, excluded, 1))
        assertEquals(2, AiPlanSelection.selectedTaskCount(plan, excluded))
    }

    @Test
    fun selectedPlanDropsDeselectedTasksAndEmptyStages() {
        val excluded = setOf("0:1", "0:0")
        val selected = AiPlanSelection.selectedPlan(plan, excluded)!!

        assertEquals(1, selected.stages.size)
        assertEquals(listOf("C"), selected.stages.single().tasks.map { it.title })
        assertEquals(1, selected.taskCount)
    }

    @Test
    fun selectedPlanIsNullWhenNothingIsSelected() {
        val everything = setOf("0:0", "0:1", "1:0")
        assertNull(AiPlanSelection.selectedPlan(plan, everything))
    }

    @Test
    fun missingStageIndexesAreSafe() {
        assertEquals(0, AiPlanSelection.selectedTaskCount(AiPlan(), emptySet()))
        assertFalse(AiPlanSelection.isStageFullySelected(plan, emptySet(), 9))
        assertTrue(AiPlanSelection.isStageEmpty(plan, emptySet(), 9))
        assertEquals(emptySet<String>(), AiPlanSelection.toggleStage(plan, emptySet(), 9))
    }
}
