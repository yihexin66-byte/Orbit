package com.example.orbit.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiAssistantFabVisibilityTest {

    @Test
    fun homeCornerStaysFullyOpaque() {
        assertEquals(
            AiAssistantFabVisibility.FULL_ALPHA,
            AiAssistantFabVisibility.restingAlpha(moved = false, nearEdge = false),
            0f,
        )
        assertEquals(
            AiAssistantFabVisibility.FULL_ALPHA,
            AiAssistantFabVisibility.restingAlpha(moved = false, nearEdge = true),
            0f,
        )
    }

    @Test
    fun movingAwayDimsTheDisc() {
        val moved = AiAssistantFabVisibility.restingAlpha(moved = true, nearEdge = false)
        assertEquals(AiAssistantFabVisibility.MOVED_ALPHA, moved, 0f)
        assertTrue(moved < AiAssistantFabVisibility.FULL_ALPHA)
    }

    @Test
    fun parkingAtAnEdgeDimsItFurther() {
        val edge = AiAssistantFabVisibility.restingAlpha(moved = true, nearEdge = true)
        assertEquals(AiAssistantFabVisibility.EDGE_ALPHA, edge, 0f)
        assertTrue(edge < AiAssistantFabVisibility.MOVED_ALPHA)
    }

    @Test
    fun onlyTheMovedStatesReportAsDimmed() {
        assertFalse(AiAssistantFabVisibility.isDimmed(AiAssistantFabVisibility.FULL_ALPHA))
        assertTrue(AiAssistantFabVisibility.isDimmed(AiAssistantFabVisibility.MOVED_ALPHA))
        assertTrue(AiAssistantFabVisibility.isDimmed(AiAssistantFabVisibility.EDGE_ALPHA))
    }
}
