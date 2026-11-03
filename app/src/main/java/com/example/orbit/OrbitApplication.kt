package com.example.orbit

import android.app.Application
import com.example.orbit.account.OrbitAccountGraph
import com.example.orbit.learning.LearningLibrary
import kotlin.concurrent.thread

class OrbitApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        OrbitAccountGraph.initialize(this)
        // Parsing the bundled books is lazy; warm it on a background thread so
        // the first home frame never pays for 2 MB of JSON.
        thread(name = "learning-preload", isDaemon = true) {
            runCatching { LearningLibrary.warmUp() }
        }
    }
}
