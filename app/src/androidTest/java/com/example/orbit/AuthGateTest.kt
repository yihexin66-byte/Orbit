package com.example.orbit

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.orbit.account.AccountStore
import com.example.orbit.account.OrbitAccountGraph
import com.example.orbit.ui.components.OrbitTestTag
import com.example.orbit.ui.screens.ACCOUNT_LOGOUT_BUTTON_TAG
import com.example.orbit.ui.screens.ACCOUNT_LOGOUT_TAG
import com.example.orbit.ui.screens.AUTH_CONFIRM_TAG
import com.example.orbit.ui.screens.AUTH_CONFIRM_VISIBILITY_TAG
import com.example.orbit.ui.screens.AUTH_PASSWORD_TAG
import com.example.orbit.ui.screens.AUTH_PASSWORD_VISIBILITY_TAG
import com.example.orbit.ui.screens.AUTH_REGISTER_TAB_TAG
import com.example.orbit.ui.screens.AUTH_SCREEN_TAG
import com.example.orbit.ui.screens.AUTH_SUBMIT_TAG
import com.example.orbit.ui.screens.AUTH_USERNAME_TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the mandatory account gate on a real activity: without a session
 * only the auth screen composes, and the seeded session greets the user by the
 * nickname stored with the account.
 */
@RunWith(AndroidJUnit4::class)
class AuthGateTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context = ApplicationProvider.getApplicationContext<OrbitApplication>()
    private var scenario: ActivityScenario<MainActivity>? = null

    @After
    fun restoreSeededSession() {
        scenario?.close()
        TestSession.seed(context)
    }

    @Test
    fun signedOutLaunchShowsOnlyTheAuthGate() {
        TestSession.clear(context)
        scenario = ActivityScenario.launch(MainActivity::class.java)

        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithTag(AUTH_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(AUTH_USERNAME_TAG).assertExists()
        composeRule.onNodeWithTag(AUTH_PASSWORD_TAG).assertExists()
        composeRule.onNodeWithTag(AUTH_SUBMIT_TAG).assertExists()
        composeRule.onNodeWithTag(AUTH_CONFIRM_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
        // The orbit and its navigation do not exist until a session is saved.
        composeRule.onAllNodesWithTag(OrbitTestTag).assertCountEquals(0)
        composeRule.onAllNodes(hasText("ORBIT / TASKS")).assertCountEquals(0)

        // Register mode adds the confirmation field on the same standalone page.
        composeRule.onNodeWithTag(AUTH_REGISTER_TAB_TAG).performClick()
        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithTag(AUTH_CONFIRM_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(AUTH_CONFIRM_TAG).assertExists()
    }

    @Test
    fun passwordVisibilityCanBeToggledForBothFields() {
        TestSession.clear(context)
        scenario = ActivityScenario.launch(MainActivity::class.java)

        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithTag(AUTH_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(AUTH_PASSWORD_TAG).performTextInput(QA_TYPED_PASSWORD)
        // The layout text is the transformed text: bullets while hidden.
        assertNotEquals(QA_TYPED_PASSWORD, visibleInputText(AUTH_PASSWORD_TAG))
        composeRule.onNodeWithTag(AUTH_PASSWORD_VISIBILITY_TAG, useUnmergedTree = true)
            .assertContentDescriptionEquals("显示密码")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag(AUTH_PASSWORD_VISIBILITY_TAG, useUnmergedTree = true)
            .assertContentDescriptionEquals("隐藏密码")
        assertEquals(QA_TYPED_PASSWORD, visibleInputText(AUTH_PASSWORD_TAG))

        // The confirmation field carries its own independent toggle.
        composeRule.onNodeWithTag(AUTH_REGISTER_TAB_TAG).performClick()
        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithTag(AUTH_CONFIRM_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(AUTH_CONFIRM_TAG).performTextInput(QA_TYPED_PASSWORD)
        assertNotEquals(QA_TYPED_PASSWORD, visibleInputText(AUTH_CONFIRM_TAG))
        composeRule.onNodeWithTag(AUTH_CONFIRM_VISIBILITY_TAG, useUnmergedTree = true)
            .assertContentDescriptionEquals("显示密码")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag(AUTH_CONFIRM_VISIBILITY_TAG, useUnmergedTree = true)
            .assertContentDescriptionEquals("隐藏密码")
        assertEquals(QA_TYPED_PASSWORD, visibleInputText(AUTH_CONFIRM_TAG))
    }

    /** Reads the text layout, which reflects the current visual transformation. */
    private fun visibleInputText(tag: String): String {
        val results = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithTag(tag).performSemanticsAction(
            SemanticsActions.GetTextLayoutResult,
        ) { action -> action(results) }
        return results.single().layoutInput.text.text
    }

    @Test
    fun seededNicknameGreetsTheUserOnHome() {
        TestSession.seed(context)
        scenario = ActivityScenario.launch(MainActivity::class.java)

        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithText(TestSession.NICKNAME, substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onAllNodesWithText(TestSession.NICKNAME, substring = true)
            .onFirst()
            .assertExists()
        composeRule.onAllNodesWithTag(AUTH_SCREEN_TAG).assertCountEquals(0)
    }

    @Test
    fun loggingOutFromTheAccountCardReturnsToTheGateForAnotherAccount() {
        TestSession.seed(context)
        scenario = ActivityScenario.launch(MainActivity::class.java)

        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithTag(OrbitTestTag).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.navigateToBottomTab("我的")
        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithTag(ACCOUNT_LOGOUT_BUTTON_TAG)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag(ACCOUNT_LOGOUT_BUTTON_TAG).performClick()
        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithTag(ACCOUNT_LOGOUT_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(ACCOUNT_LOGOUT_TAG).performClick()

        // The gate returns with blank fields, ready for a different account.
        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithTag(AUTH_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(AUTH_USERNAME_TAG).assertExists()
        composeRule.onNodeWithTag(AUTH_PASSWORD_TAG).assertExists()
        composeRule.onNodeWithTag(AUTH_SUBMIT_TAG).assertExists()
        composeRule.onAllNodesWithTag(OrbitTestTag).assertCountEquals(0)
    }

    @Test
    fun cacheResetIsSafeFromTheMainThread() {
        // Login calls prepareForAccount() from the main dispatcher. Room's
        // clearAllTables() is blocking and must be moved to an IO thread,
        // otherwise the login fails with a main-thread database error.
        runBlocking {
            withContext(Dispatchers.Main) {
                OrbitAccountGraph.syncManager.prepareForAccount(424242L)
            }
        }
        assertEquals(AccountStore.NO_OWNER, OrbitAccountGraph.store.localOwnerUserId)
    }

    private companion object {
        const val TIMEOUT_MS = 30_000L

        /** Typed into the field and read back; never sent anywhere, so it is not a real secret. */
        const val QA_TYPED_PASSWORD = "qa-visible-check"
    }
}
