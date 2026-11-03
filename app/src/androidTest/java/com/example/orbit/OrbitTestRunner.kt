package com.example.orbit

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import com.example.orbit.account.AccountUser
import com.example.orbit.account.OrbitAccountGraph
import com.example.orbit.account.StoredAccount

/**
 * Instrumentation entry point for the mandatory-login build.
 *
 * The production app cannot render any screen without a session, so every UI
 * test would otherwise stop at the login gate. This runner installs a fixed
 * debug session before `Application.onCreate` and marks sync as blocked, which
 * keeps Room flows fully local: no test touches the Spring Boot host and the
 * seeded token never reaches MySQL.
 */
class OrbitTestRunner : AndroidJUnitRunner() {

    /**
     * Runs after the application's base context is attached. Seeding in
     * [newApplication] is too early: the returned instance has no context yet.
     */
    override fun callApplicationOnCreate(app: Application) {
        super.callApplicationOnCreate(app)
        TestSession.seed(app)
    }
}

/** Shared fixture for the seeded, backend-free QA account. */
object TestSession {

    const val TOKEN = "qa-offline-token"
    const val USER_ID = 1L
    const val USERNAME = "orbit_qa"
    const val NICKNAME = "NovaPilot"

    val user = AccountUser(
        id = USER_ID,
        username = USERNAME,
        nickname = NICKNAME,
        avatarPath = null,
        createdAt = 0L,
    )

    /** Idempotent: safe both from the runner and from test setup/teardown. */
    fun seed(context: Context) {
        OrbitAccountGraph.initialize(context)
        OrbitAccountGraph.store.seedDebugSession(TOKEN, user)
    }

    fun clear(context: Context) {
        OrbitAccountGraph.initialize(context)
        OrbitAccountGraph.store.clearSession()
    }

    fun session(context: Context): StoredAccount? {
        OrbitAccountGraph.initialize(context)
        return OrbitAccountGraph.store.session.value
    }
}
