package com.example.orbit.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskTitleRulesTest {

    @Test
    fun blankTitlesAreRejected() {
        assertFalse(TaskTitleRules.isValid(""))
        assertFalse(TaskTitleRules.isValid("   "))
        assertFalse(TaskTitleRules.isValid("\n\t"))
    }

    @Test
    fun titlesWithContentAreAccepted() {
        assertTrue(TaskTitleRules.isValid("完成作品集"))
        assertTrue(TaskTitleRules.isValid("  完成作品集  "))
    }

    @Test
    fun normalizeTrimsOuterWhitespaceOnly() {
        assertEquals("整理投递记录", TaskTitleRules.normalize("  整理投递记录  "))
        assertEquals("a b", TaskTitleRules.normalize(" a b "))
    }
}
