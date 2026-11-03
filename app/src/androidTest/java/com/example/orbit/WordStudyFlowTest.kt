package com.example.orbit

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.orbit.data.LearningProgressStore
import com.example.orbit.learning.LearningLibrary
import com.example.orbit.model.TaskDate
import com.example.orbit.ui.LearningViewModel
import com.example.orbit.ui.WordStudyViewModel
import com.example.orbit.ui.screens.LearningScreenTag
import com.example.orbit.ui.screens.WordPickerStartTag
import com.example.orbit.ui.screens.WordPickerTag
import com.example.orbit.ui.screens.WordStudyCardTag
import com.example.orbit.ui.screens.WordStudyCloseTag
import com.example.orbit.ui.screens.WordStudyFinishTag
import com.example.orbit.ui.screens.WordStudyKnownTag
import com.example.orbit.ui.screens.WordStudyManualTag
import com.example.orbit.ui.screens.WordStudyRevealTag
import com.example.orbit.ui.screens.WordStudyRetryWrongTag
import com.example.orbit.ui.screens.WordStudySessionTag
import com.example.orbit.ui.screens.WordStudyStartTag
import com.example.orbit.ui.screens.WordStudySummaryTag
import com.example.orbit.ui.screens.WordStudyUnknownTag
import com.example.orbit.ui.screens.wordPickerRowTag
import com.example.orbit.ui.screens.wordStudyPresetTag
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device flow for the market-style word round: auto selection by target,
 * reveal-then-self-assess, accuracy summary, wrong-word retry, manual picking
 * and on-disk progress after the activity is recreated.
 */
@RunWith(AndroidJUnit4::class)
class WordStudyFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val learningViewModel: LearningViewModel
        get() = ViewModelProvider(rule.activity)[LearningViewModel::class.java]
    private val wordStudyViewModel: WordStudyViewModel
        get() = ViewModelProvider(rule.activity)[WordStudyViewModel::class.java]

    @Before
    fun resetProgress() {
        context.getSharedPreferences(
            LearningProgressStore.PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        ).edit().clear().commit()
        rule.waitForIdle()
        rule.runOnIdle {
            learningViewModel.resetLocalProgress()
            wordStudyViewModel.reload()
        }
        rule.waitForIdle()
    }

    @Test
    fun autoRoundRetryWrongAndPersistAcrossRecreate() {
        val today = TaskDate.todayEpochDay()
        openLearningTab()
        scrollToWordStudyCard()

        rule.onNodeWithTag(wordStudyPresetTag(5)).performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals(5, wordStudyViewModel.state.value.progress.dailyTarget)
        }

        rule.onNodeWithTag(WordStudyStartTag).performScrollTo().performClick()
        waitForTag(WordStudySessionTag)
        rule.runOnIdle {
            assertEquals(5, wordStudyViewModel.state.value.session?.words?.size)
        }
        // Before revealing there is nothing to self-assess.
        rule.onNodeWithTag(WordStudyRevealTag).assertExists()
        rule.onNodeWithTag(WordStudyKnownTag).assertDoesNotExist()

        repeat(5) { index ->
            waitForTag(WordStudyRevealTag)
            rule.onNodeWithTag(WordStudyRevealTag).performClick()
            waitForTag(WordStudyKnownTag)
            if (index < 4) {
                rule.onNodeWithTag(WordStudyKnownTag).performClick()
            } else {
                rule.onNodeWithTag(WordStudyUnknownTag).performClick()
            }
            rule.waitForIdle()
        }

        waitForTag(WordStudySummaryTag)
        waitForText("80%")
        rule.runOnIdle {
            val progress = wordStudyViewModel.state.value.progress
            assertEquals(5, progress.studiedOn(today))
            assertEquals(5, progress.learnedCount)
            assertEquals(1, progress.reviewCount)
        }

        // Only the wrong word comes back for the retry round.
        rule.onNodeWithTag(WordStudyRetryWrongTag).performClick()
        waitForTag(WordStudyRevealTag)
        rule.runOnIdle {
            assertEquals(1, wordStudyViewModel.state.value.session?.words?.size)
        }
        rule.onNodeWithTag(WordStudyRevealTag).performClick()
        waitForTag(WordStudyKnownTag)
        rule.onNodeWithTag(WordStudyKnownTag).performClick()
        waitForTag(WordStudySummaryTag)
        waitForText("100%")
        rule.runOnIdle {
            assertEquals(0, wordStudyViewModel.state.value.progress.reviewCount)
        }

        rule.onNodeWithTag(WordStudyFinishTag).performClick()
        waitForTagGone(WordStudySessionTag)
        rule.runOnIdle {
            assertEquals(null, wordStudyViewModel.state.value.session)
        }
        capture("word-study-finished-dark.png")

        // The round's records and target survive process-level recreation.
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
        openLearningTab()
        scrollToWordStudyCard()
        rule.runOnIdle {
            val state = wordStudyViewModel.state.value
            assertEquals(5, state.progress.dailyTarget)
            assertEquals(5, state.progress.studiedOn(today))
            assertEquals(5, state.progress.learnedCount)
        }
    }

    @Test
    fun manualPickerStartsBookOrderAndKeepsSession() {
        openLearningTab()
        scrollToWordStudyCard()

        val first = LearningLibrary.english[0]
        val second = LearningLibrary.english[1]
        rule.onNodeWithTag(WordStudyManualTag).performScrollTo().performClick()
        waitForTag(WordPickerTag)

        rule.onNodeWithTag(wordPickerRowTag(first.id), useUnmergedTree = true)
            .performClick()
        rule.onNodeWithTag(wordPickerRowTag(second.id), useUnmergedTree = true)
            .performClick()
        waitForText("已选 2 / 50")

        rule.onNodeWithTag(WordPickerStartTag).performClick()
        waitForTag(WordStudySessionTag)
        rule.runOnIdle {
            val session = wordStudyViewModel.state.value.session
            assertNotNull(session)
            assertEquals(listOf(first.id, second.id), session?.words?.map { it.id })
        }

        // Closing the sheet keeps the round for "继续学习".
        rule.onNodeWithTag(WordStudyCloseTag).performClick()
        waitForTagGone(WordStudySessionTag)
        scrollToWordStudyCard()
        waitForText("继续学习 0 / 2")

        rule.onNodeWithTag(WordStudyStartTag).performScrollTo().performClick()
        waitForTag(WordStudySessionTag)
        rule.runOnIdle {
            assertEquals(2, wordStudyViewModel.state.value.session?.words?.size)
        }
        assertTrue(
            rule.onAllNodesWithText("不认识").fetchSemanticsNodes().isEmpty(),
        )
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

    private fun capture(fileName: String) {
        rule.waitForIdle()
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val directory = File(context.getExternalFilesDir(null), "screenshots_word_study")
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
