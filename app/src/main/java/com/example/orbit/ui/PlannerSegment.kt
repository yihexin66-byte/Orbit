package com.example.orbit.ui

import androidx.annotation.StringRes
import com.example.orbit.R

/** The three halves of the merged 目标 page: goals, dated tasks, habits. */
enum class PlannerSegment(@param:StringRes val labelRes: Int) {
    GOALS(R.string.goals_title),
    TASKS(R.string.tasks_title),
    HABITS(R.string.planner_segment_habits),
}
