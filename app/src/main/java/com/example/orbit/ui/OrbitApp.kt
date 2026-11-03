package com.example.orbit.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.orbit.R
import com.example.orbit.ai.AiChatContext
import com.example.orbit.ai.AiChatGoal
import com.example.orbit.learning.LearningProgress
import com.example.orbit.model.AccentTone
import com.example.orbit.model.GoalStatus
import com.example.orbit.model.TaskCategory
import com.example.orbit.model.TaskFilter
import com.example.orbit.model.TaskPriority
import com.example.orbit.model.ThemeMode
import com.example.orbit.ui.components.OrbitBottomBar
import com.example.orbit.ui.components.SpaceBackdrop
import com.example.orbit.ui.navigation.OrbitDestination
import com.example.orbit.ui.navigation.TabSwipe
import com.example.orbit.ui.navigation.TabSwipeDirection
import com.example.orbit.ui.navigation.TabSwipeTransition
import com.example.orbit.ui.navigation.orbitTabSwipe
import com.example.orbit.ui.screens.AccountActions
import com.example.orbit.ui.screens.AiAssistantSheet
import com.example.orbit.ui.screens.AiPlanScreen
import com.example.orbit.ui.screens.AuthScreen
import com.example.orbit.ui.screens.GoalDetailScreen
import com.example.orbit.ui.screens.GoalEditorSheet
import com.example.orbit.ui.screens.GoalsScreen
import com.example.orbit.ui.screens.GrowthScreen
import com.example.orbit.ui.screens.HabitEditorSheet
import com.example.orbit.ui.screens.HabitsScreen
import com.example.orbit.ui.screens.LearningScreen
import com.example.orbit.ui.screens.OrbitDataOverview
import com.example.orbit.ui.screens.OrbitHomeScreen
import com.example.orbit.ui.screens.SettingsScreen
import com.example.orbit.ui.screens.StageEditorSheet
import com.example.orbit.ui.screens.SyncBootScreen
import com.example.orbit.ui.screens.TaskEditorSheet
import com.example.orbit.ui.screens.TasksScreen
import com.example.orbit.ui.theme.OrbitCorners
import com.example.orbit.ui.theme.OrbitTheme
import com.example.orbit.ui.theme.orbitColors

@Composable
fun OrbitRoot(
    taskViewModel: TaskViewModel = viewModel(),
    goalViewModel: GoalViewModel = viewModel(),
    habitViewModel: HabitViewModel = viewModel(),
    growthViewModel: GrowthViewModel = viewModel(),
    preferencesViewModel: OrbitViewModel = viewModel(),
    accountViewModel: AccountViewModel = viewModel(),
    aiChatViewModel: AiChatViewModel = viewModel(),
    learningViewModel: LearningViewModel = viewModel(),
    wordStudyViewModel: WordStudyViewModel = viewModel(),
) {
    val taskState by taskViewModel.uiState.collectAsStateWithLifecycle()
    val editorState by taskViewModel.editorState.collectAsStateWithLifecycle()
    val goalState by goalViewModel.uiState.collectAsStateWithLifecycle()
    val goalEditorState by goalViewModel.editorState.collectAsStateWithLifecycle()
    val stageEditorState by goalViewModel.stageEditorState.collectAsStateWithLifecycle()
    val goalDeletion by goalViewModel.goalDeletion.collectAsStateWithLifecycle()
    val habitState by habitViewModel.uiState.collectAsStateWithLifecycle()
    val habitEditorState by habitViewModel.editorState.collectAsStateWithLifecycle()
    val growthState by growthViewModel.uiState.collectAsStateWithLifecycle()
    val preferences by preferencesViewModel.uiState.collectAsStateWithLifecycle()
    val accountState by accountViewModel.uiState.collectAsStateWithLifecycle()
    val aiChatState by aiChatViewModel.uiState.collectAsStateWithLifecycle()
    val learningProgress by learningViewModel.progress.collectAsStateWithLifecycle()
    val learningActions = remember(learningViewModel) {
        LearningActions(
            onSetPoemToday = learningViewModel::setPoemToday,
            onSetWordToday = learningViewModel::setWordToday,
            onToggleWordBookmark = learningViewModel::toggleWordBookmark,
        )
    }
    val wordStudyState by wordStudyViewModel.state.collectAsStateWithLifecycle()
    val wordStudyActions = remember(wordStudyViewModel) {
        WordStudyActions(
            onSetTarget = wordStudyViewModel::setTarget,
            onStartAuto = wordStudyViewModel::startAuto,
            onStartManual = wordStudyViewModel::startManual,
            onReveal = wordStudyViewModel::reveal,
            onAnswer = wordStudyViewModel::answer,
            onRetryWrong = wordStudyViewModel::retryWrong,
            onNextRound = wordStudyViewModel::nextRound,
            onFinish = wordStudyViewModel::finish,
        )
    }
    // Aggregate numbers and goal titles only: the assistant never receives
    // task bodies, habit names or the nickname.
    val chatContext = AiChatContext(
        todayPlanned = taskState.todaySummary.planned,
        todayCompleted = taskState.todaySummary.completed,
        pendingCount = taskState.pendingCount,
        completedCount = taskState.completedCount,
        habitTodayChecked = habitState.checkedTodayCount,
        habitTodayTotal = habitState.activeCount,
        totalXp = growthState.summary.totalXp,
        thisWeekXp = growthState.summary.thisWeekXp,
        goals = goalState.activeGoals.take(MAX_CHAT_GOALS).map { goal ->
            AiChatGoal(
                title = goal.title,
                progress = goalState.progressFor(goal).value,
            )
        },
    )
    val accountActions = remember(accountViewModel) {
        AccountActions(
            onUsernameChange = accountViewModel::updateUsername,
            onPasswordChange = accountViewModel::updatePassword,
            onConfirmPasswordChange = accountViewModel::updateConfirmPassword,
            onModeChange = accountViewModel::setMode,
            onSubmit = accountViewModel::submit,
            onLogout = accountViewModel::logout,
            onStartNicknameEdit = accountViewModel::startNicknameEdit,
            onNicknameDraftChange = accountViewModel::updateNicknameDraft,
            onSaveNickname = accountViewModel::saveNickname,
            onCancelNicknameEdit = accountViewModel::cancelNicknameEdit,
            onGalleryPicked = accountViewModel::onGalleryPicked,
            onCreateCameraTarget = accountViewModel::createCameraTarget,
            onCameraResult = accountViewModel::onCameraResult,
            onSync = accountViewModel::requestSync,
            onConsumeMessage = accountViewModel::consumeMessage,
            onClearError = accountViewModel::clearError,
            onReportError = accountViewModel::reportError,
        )
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        taskViewModel.refreshToday()
        habitViewModel.refreshToday()
        growthViewModel.refreshToday()
    }

    OrbitTheme(
        themeMode = preferences.themeMode,
        accentTone = preferences.accentTone,
    ) {
        SystemBarAppearance()
        val user = accountState.user
        val displayName = user?.nickname?.takeIf { it.isNotBlank() }
            ?: user?.username
            ?: "宇航员"
        when {
            user == null -> SpaceBackdrop {
                AuthScreen(state = accountState, actions = accountActions)
            }

            accountState.initialSyncing -> SpaceBackdrop {
                SyncBootScreen(nickname = displayName)
            }

            else -> OrbitApp(
                displayName = displayName,
                aiChatState = aiChatState,
                learningProgress = learningProgress,
                learningActions = learningActions,
                wordStudyState = wordStudyState,
                wordStudyActions = wordStudyActions,
                onChatInputChange = aiChatViewModel::updateInput,
                onChatSend = { aiChatViewModel.send(chatContext) },
                onChatRetry = { aiChatViewModel.retry(chatContext) },
                onChatRefreshStatus = aiChatViewModel::refreshStatus,
                onChatSendPrompt = { prompt ->
                    aiChatViewModel.sendText(prompt, chatContext)
                },
                onChatClear = aiChatViewModel::clearConversation,
                onChatDismissFailure = aiChatViewModel::dismissFailure,
                taskState = taskState,
            editorState = editorState,
            goalState = goalState,
            goalEditorState = goalEditorState,
            stageEditorState = stageEditorState,
            goalDeletion = goalDeletion,
            habitState = habitState,
            habitEditorState = habitEditorState,
            growthState = growthState,
            preferences = preferences,
            accountState = accountState,
            accountActions = accountActions,
            onSelectTask = taskViewModel::selectTask,
            onClearSelection = taskViewModel::clearSelection,
            onToggleTask = taskViewModel::toggleTask,
            onFilterChange = taskViewModel::setFilter,
            onOpenCreateTask = { taskViewModel.openCreateTask() },
            onOpenCreateTaskLinked = { goalId, stageId ->
                taskViewModel.openCreateTask(goalId, stageId)
            },
            onOpenEditTask = taskViewModel::openEditTask,
            onDismissEditor = taskViewModel::dismissEditor,
            onEditorTitleChange = taskViewModel::updateEditorTitle,
            onEditorDescriptionChange = taskViewModel::updateEditorDescription,
            onEditorPriorityChange = taskViewModel::updateEditorPriority,
            onEditorCategoryChange = taskViewModel::updateEditorCategory,
            onEditorDueDateChange = taskViewModel::updateEditorDueDate,
            onEditorLinkChange = taskViewModel::updateEditorLink,
            onEditorSave = taskViewModel::saveEditor,
            onEditorDeleteRequest = taskViewModel::requestDelete,
            onEditorDeleteDismiss = taskViewModel::dismissDeleteRequest,
            onEditorDeleteConfirm = taskViewModel::confirmDelete,
            onConsumeFeedback = taskViewModel::consumeFeedback,
            onRetryLoad = taskViewModel::retryLoad,
            onOpenCreateGoal = goalViewModel::openCreateGoal,
            onOpenEditGoal = goalViewModel::openEditGoal,
            onDismissGoalEditor = goalViewModel::dismissEditor,
            onGoalTitleChange = goalViewModel::updateEditorTitle,
            onGoalDescriptionChange = goalViewModel::updateEditorDescription,
            onGoalToneChange = goalViewModel::updateEditorTone,
            onGoalStatusChange = goalViewModel::updateEditorStatus,
            onGoalTargetDateChange = goalViewModel::updateEditorTargetDate,
            onGoalSave = goalViewModel::saveEditor,
            onGoalDeleteRequest = goalViewModel::requestDelete,
            onGoalDeleteDismiss = goalViewModel::dismissDeleteRequest,
            onGoalDeleteConfirm = goalViewModel::confirmDelete,
            onConsumeGoalFeedback = goalViewModel::consumeFeedback,
            onRetryGoals = goalViewModel::retryLoad,
            onOpenCreateStage = goalViewModel::openCreateStage,
            onOpenEditStage = goalViewModel::openEditStage,
            onDismissStageEditor = goalViewModel::dismissStageEditor,
            onStageTitleChange = goalViewModel::updateStageTitle,
            onStageTargetDateChange = goalViewModel::updateStageTargetDate,
            onStageSave = goalViewModel::saveStageEditor,
            onStageDeleteRequest = goalViewModel::requestDeleteStage,
            onStageDeleteDismiss = goalViewModel::dismissDeleteStage,
            onStageDeleteConfirm = goalViewModel::confirmDeleteStage,
            onToggleStage = goalViewModel::toggleStage,
            onGoalStatusAction = goalViewModel::setGoalStatus,
            onGoalDeleteAction = goalViewModel::requestGoalDelete,
            onGoalDeleteActionDismiss = goalViewModel::dismissGoalDelete,
            onGoalDeleteActionConfirm = goalViewModel::confirmGoalDelete,
            onAddHabit = habitViewModel::openCreateHabit,
            onEditHabit = habitViewModel::openEditHabit,
            onToggleHabit = habitViewModel::toggleChecked,
            onArchiveHabitChange = habitViewModel::setArchived,
            onDismissHabitEditor = habitViewModel::dismissEditor,
            onHabitTitleChange = habitViewModel::updateEditorTitle,
            onHabitNoteChange = habitViewModel::updateEditorNote,
            onHabitToneChange = habitViewModel::updateEditorTone,
            onHabitSave = habitViewModel::saveEditor,
            onHabitDeleteRequest = habitViewModel::requestDelete,
            onHabitDeleteDismiss = habitViewModel::dismissDeleteRequest,
            onHabitDeleteConfirm = habitViewModel::confirmDelete,
            onConsumeHabitFeedback = habitViewModel::consumeFeedback,
            onRetryHabits = habitViewModel::retryLoad,
            onRetryGrowth = growthViewModel::retryLoad,
            onThemeModeChange = preferencesViewModel::setThemeMode,
            onAccentToneChange = preferencesViewModel::setAccentTone,
            )
        }
    }
}

@Composable
private fun OrbitApp(
    displayName: String,
    aiChatState: AiChatUiState,
    learningProgress: LearningProgress,
    learningActions: LearningActions,
    wordStudyState: WordStudyState,
    wordStudyActions: WordStudyActions,
    onChatInputChange: (String) -> Unit,
    onChatSend: () -> Unit,
    onChatRetry: () -> Unit,
    onChatRefreshStatus: () -> Unit,
    onChatSendPrompt: (String) -> Unit,
    onChatClear: () -> Unit,
    onChatDismissFailure: () -> Unit,
    taskState: TaskUiState,
    editorState: TaskEditorState?,
    goalState: GoalUiState,
    goalEditorState: GoalEditorState?,
    stageEditorState: StageEditorState?,
    goalDeletion: GoalDeletionState?,
    habitState: HabitUiState,
    habitEditorState: HabitEditorState?,
    growthState: GrowthUiState,
    preferences: OrbitPreferencesUiState,
    accountState: AccountUiState,
    accountActions: AccountActions,
    onSelectTask: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onToggleTask: (Long) -> Unit,
    onFilterChange: (TaskFilter) -> Unit,
    onOpenCreateTask: () -> Unit,
    onOpenCreateTaskLinked: (Long, Long?) -> Unit,
    onOpenEditTask: (Long) -> Unit,
    onDismissEditor: () -> Unit,
    onEditorTitleChange: (String) -> Unit,
    onEditorDescriptionChange: (String) -> Unit,
    onEditorPriorityChange: (TaskPriority) -> Unit,
    onEditorCategoryChange: (TaskCategory) -> Unit,
    onEditorDueDateChange: (Long?) -> Unit,
    onEditorLinkChange: (Long?, Long?) -> Unit,
    onEditorSave: () -> Unit,
    onEditorDeleteRequest: () -> Unit,
    onEditorDeleteDismiss: () -> Unit,
    onEditorDeleteConfirm: () -> Unit,
    onConsumeFeedback: (Long) -> Unit,
    onRetryLoad: () -> Unit,
    onOpenCreateGoal: () -> Unit,
    onOpenEditGoal: (Long) -> Unit,
    onDismissGoalEditor: () -> Unit,
    onGoalTitleChange: (String) -> Unit,
    onGoalDescriptionChange: (String) -> Unit,
    onGoalToneChange: (AccentTone) -> Unit,
    onGoalStatusChange: (GoalStatus) -> Unit,
    onGoalTargetDateChange: (Long?) -> Unit,
    onGoalSave: () -> Unit,
    onGoalDeleteRequest: () -> Unit,
    onGoalDeleteDismiss: () -> Unit,
    onGoalDeleteConfirm: () -> Unit,
    onConsumeGoalFeedback: (Long) -> Unit,
    onRetryGoals: () -> Unit,
    onOpenCreateStage: (Long) -> Unit,
    onOpenEditStage: (Long) -> Unit,
    onDismissStageEditor: () -> Unit,
    onStageTitleChange: (String) -> Unit,
    onStageTargetDateChange: (Long?) -> Unit,
    onStageSave: () -> Unit,
    onStageDeleteRequest: () -> Unit,
    onStageDeleteDismiss: () -> Unit,
    onStageDeleteConfirm: () -> Unit,
    onToggleStage: (Long, Boolean) -> Unit,
    onGoalStatusAction: (Long, GoalStatus) -> Unit,
    onGoalDeleteAction: (Long) -> Unit,
    onGoalDeleteActionDismiss: () -> Unit,
    onGoalDeleteActionConfirm: () -> Unit,
    onAddHabit: () -> Unit,
    onEditHabit: (Long) -> Unit,
    onToggleHabit: (Long) -> Unit,
    onArchiveHabitChange: (Long, Boolean) -> Unit,
    onDismissHabitEditor: () -> Unit,
    onHabitTitleChange: (String) -> Unit,
    onHabitNoteChange: (String) -> Unit,
    onHabitToneChange: (AccentTone) -> Unit,
    onHabitSave: () -> Unit,
    onHabitDeleteRequest: () -> Unit,
    onHabitDeleteDismiss: () -> Unit,
    onHabitDeleteConfirm: () -> Unit,
    onConsumeHabitFeedback: (Long) -> Unit,
    onRetryHabits: () -> Unit,
    onRetryGrowth: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentToneChange: (AccentTone) -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: OrbitDestination.ORBIT.route
    val isNestedGoalRoute = currentRoute.startsWith("${OrbitDestination.GOALS.route}/")
    val isGoalDetail = isNestedGoalRoute &&
        currentRoute != OrbitDestination.AI_PLAN_ROUTE
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val cloudSignedIn = accountState.user != null
    val goalLabels = goalState.goals.associate { it.id to it.title }
    val stageLabels = goalState.stages.associate { it.id to it.title }
    var plannerSegment by rememberSaveable { mutableStateOf(PlannerSegment.GOALS) }
    // The assistant is a global overlay: home owns the draggable disc, but any
    // screen can hand it a prompt and open the same conversation.
    var showAssistant by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(taskState.feedback?.id) {
        val feedback = taskState.feedback ?: return@LaunchedEffect
        val isFailure = feedback.type.isFailure
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(
            message = context.getString(feedback.type.messageRes()),
            withDismissAction = isFailure,
            duration = if (isFailure) SnackbarDuration.Long else SnackbarDuration.Short,
        )
        onConsumeFeedback(feedback.id)
    }

    LaunchedEffect(goalState.feedback?.id) {
        val feedback = goalState.feedback ?: return@LaunchedEffect
        val isFailure = feedback.type.isFailure
        if (feedback.type == GoalFeedbackType.DELETED && isGoalDetail) {
            navController.popBackStack(OrbitDestination.GOALS.route, inclusive = false)
        }
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(
            message = context.getString(feedback.type.messageRes()),
            withDismissAction = isFailure,
            duration = if (isFailure) SnackbarDuration.Long else SnackbarDuration.Short,
        )
        onConsumeGoalFeedback(feedback.id)
    }

    LaunchedEffect(habitState.feedback?.id) {
        val feedback = habitState.feedback ?: return@LaunchedEffect
        val isFailure = feedback.type.isFailure
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(
            message = context.getString(feedback.type.messageRes()),
            withDismissAction = isFailure,
            duration = if (isFailure) SnackbarDuration.Long else SnackbarDuration.Short,
        )
        onConsumeHabitFeedback(feedback.id)
    }

    // Profile and avatar changes now surface through the shared toast; the
    // settings card no longer owns a message slot.
    LaunchedEffect(accountState.message) {
        val message = accountState.message ?: return@LaunchedEffect
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(
            message = message,
            duration = SnackbarDuration.Short,
        )
        accountActions.onConsumeMessage()
    }

    val navigateTo: (OrbitDestination) -> Unit = { destination ->
        val route = navController.currentDestination?.route
        when {
            route == destination.route -> {
                // Re-tapping the 目标 tab returns to the goals third, mirroring
                // how the other tabs surface their root content.
                if (destination == OrbitDestination.GOALS) {
                    plannerSegment = PlannerSegment.GOALS
                }
            }

            route?.startsWith("${destination.route}/") == true -> {
                if (destination == OrbitDestination.GOALS) {
                    plannerSegment = PlannerSegment.GOALS
                }
                navController.popBackStack(destination.route, inclusive = false)
            }

            else -> {
                navController.navigate(destination.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    val navigateHome: () -> Unit = {
        navController.navigate(OrbitDestination.ORBIT.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Swiping the screen performs exactly what the bottom bar would: one step
    // along the five root tabs, slid rather than faded. Nested routes (goal
    // detail, AI plan) are not swipeable, so an unfinished form can never be
    // flicked away.
    val tabSwipeTransition = remember { TabSwipeTransition() }
    val swipeTab: (TabSwipeDirection) -> Unit = { direction ->
        TabSwipe.targetFor(currentRoute, direction)?.let { navigateTo(it) }
    }

    BackHandler(
        enabled = currentRoute != OrbitDestination.ORBIT.route && !isNestedGoalRoute,
        onBack = navigateHome,
    )
    // Back from an open node detail returns to the orbit before leaving the app.
    BackHandler(
        enabled = currentRoute == OrbitDestination.ORBIT.route &&
            taskState.selectedTaskId != null,
        onBack = onClearSelection,
    )

    SpaceBackdrop {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    OrbitToast(data = data)
                }
            },
            bottomBar = {
                OrbitBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = navigateTo,
                )
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = OrbitDestination.ORBIT.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .orbitTabSwipe(
                        transition = tabSwipeTransition,
                        canSwipeTo = { direction ->
                            TabSwipe.targetFor(currentRoute, direction) != null
                        },
                        onSwipeTo = swipeTab,
                    ),
                // A swipe hands one page over to the next while both are
                // off-screen; the host must not cross-fade them on top of that
                // slide. Taps on the bottom bar are not a swipe, so they keep
                // the fade they have always had.
                enterTransition = {
                    if (tabSwipeTransition.swapping) {
                        EnterTransition.None
                    } else {
                        fadeIn(animationSpec = tween(220))
                    }
                },
                exitTransition = {
                    if (tabSwipeTransition.swapping) {
                        ExitTransition.None
                    } else {
                        fadeOut(animationSpec = tween(140))
                    }
                },
                popEnterTransition = {
                    if (tabSwipeTransition.swapping) {
                        EnterTransition.None
                    } else {
                        fadeIn(animationSpec = tween(220))
                    }
                },
                popExitTransition = {
                    if (tabSwipeTransition.swapping) {
                        ExitTransition.None
                    } else {
                        fadeOut(animationSpec = tween(140))
                    }
                },
            ) {
                composable(OrbitDestination.ORBIT.route) {
                    OrbitHomeScreen(
                        uiState = taskState,
                        onSelectTask = onSelectTask,
                        onClearSelection = onClearSelection,
                        onToggleTask = onToggleTask,
                        onEditTask = onOpenEditTask,
                        onAddTask = onOpenCreateTask,
                        onViewAllTasks = {
                            plannerSegment = PlannerSegment.TASKS
                            onFilterChange(TaskFilter.ALL)
                            navigateTo(OrbitDestination.GOALS)
                        },
                        onRetryLoad = onRetryLoad,
                        goalLabels = goalLabels,
                        stageLabels = stageLabels,
                        habitState = habitState,
                        onToggleHabit = onToggleHabit,
                        onEditHabit = onEditHabit,
                        onAddHabit = onAddHabit,
                        onViewAllHabits = {
                            plannerSegment = PlannerSegment.HABITS
                            navigateTo(OrbitDestination.GOALS)
                        },
                        cloudSignedIn = cloudSignedIn,
                        displayName = displayName,
                        dailyPoemId = learningProgress.poemIdFor(taskState.todayEpochDay),
                        dailyWordId = learningProgress.wordIdFor(taskState.todayEpochDay),
                        onOpenLearning = { navigateTo(OrbitDestination.LEARNING) },
                        onOpenAssistant = { showAssistant = true },
                    )
                }
                composable(OrbitDestination.GOALS.route) {
                    when (plannerSegment) {
                        PlannerSegment.GOALS -> GoalsScreen(
                            uiState = goalState,
                            onAddGoal = onOpenCreateGoal,
                            onOpenGoal = { goalId ->
                                navController.navigate(OrbitDestination.goalDetailRoute(goalId))
                            },
                            onRetry = onRetryGoals,
                            segment = plannerSegment,
                            onSegmentChange = { plannerSegment = it },
                            taskCount = taskState.pendingCount,
                            habitCount = habitState.activeCount,
                            cloudSignedIn = cloudSignedIn,
                            onOpenAiPlan = {
                                navController.navigate(OrbitDestination.AI_PLAN_ROUTE)
                            },
                        )

                        PlannerSegment.TASKS -> TasksScreen(
                            uiState = taskState,
                            onFilterChange = onFilterChange,
                            onToggleTask = onToggleTask,
                            onEditTask = onOpenEditTask,
                            onAddTask = onOpenCreateTask,
                            onRetry = onRetryLoad,
                            goalLabels = goalLabels,
                            stageLabels = stageLabels,
                            segment = plannerSegment,
                            onSegmentChange = { plannerSegment = it },
                            goalCount = goalState.goals.size,
                            habitCount = habitState.activeCount,
                            cloudSignedIn = cloudSignedIn,
                        )

                        PlannerSegment.HABITS -> HabitsScreen(
                            uiState = habitState,
                            segment = plannerSegment,
                            onSegmentChange = { plannerSegment = it },
                            onAddHabit = onAddHabit,
                            onEditHabit = onEditHabit,
                            onToggleHabit = onToggleHabit,
                            onRestoreHabit = { habitId ->
                                onArchiveHabitChange(habitId, false)
                            },
                            onRetry = onRetryHabits,
                            goalCount = goalState.goals.size,
                            taskCount = taskState.pendingCount,
                            cloudSignedIn = cloudSignedIn,
                        )
                    }
                }
                composable(OrbitDestination.AI_PLAN_ROUTE) {
                    val aiPlanViewModel: AiPlanViewModel = viewModel()
                    val aiPlanState by aiPlanViewModel.uiState.collectAsStateWithLifecycle()
                    LaunchedEffect(aiPlanState.importedGoalId) {
                        val goalId = aiPlanState.importedGoalId ?: return@LaunchedEffect
                        aiPlanViewModel.consumeImportedGoal()
                        navController.navigate(OrbitDestination.goalDetailRoute(goalId)) {
                            popUpTo(OrbitDestination.AI_PLAN_ROUTE) { inclusive = true }
                        }
                    }
                    AiPlanScreen(
                        uiState = aiPlanState,
                        onBack = {
                            // Reached from Goals in the usual flow; from the
                            // home shortcut there is no Goals entry below, so
                            // fall back to a plain pop instead of doing nothing.
                            val popped = navController.popBackStack(
                                OrbitDestination.GOALS.route,
                                inclusive = false,
                            )
                            if (!popped) navController.popBackStack()
                        },
                        onGoalChange = aiPlanViewModel::updateGoal,
                        onTargetDateChange = aiPlanViewModel::updateTargetDate,
                        onDailyMinutesChange = aiPlanViewModel::updateDailyMinutes,
                        onCurrentLevelChange = aiPlanViewModel::updateCurrentLevel,
                        onWeakPointsChange = aiPlanViewModel::updateWeakPoints,
                        onGenerate = aiPlanViewModel::generate,
                        onToggleStage = aiPlanViewModel::toggleStage,
                        onToggleTask = aiPlanViewModel::toggleTask,
                        onImport = aiPlanViewModel::importSelected,
                        onDiscard = aiPlanViewModel::discardPlan,
                        onRetryStatus = aiPlanViewModel::refreshStatus,
                    )
                }
                composable(
                    route = OrbitDestination.GOAL_DETAIL_ROUTE,
                    arguments = listOf(
                        navArgument(OrbitDestination.GOAL_ID_ARG) {
                            type = NavType.LongType
                        },
                    ),
                ) { entry ->
                    val goalId = entry.arguments?.getLong(OrbitDestination.GOAL_ID_ARG)
                    if (goalId != null) {
                        GoalDetailScreen(
                            uiState = goalState,
                            goalId = goalId,
                            deletion = goalDeletion,
                            onBack = {
                                navController.popBackStack(
                                    OrbitDestination.GOALS.route,
                                    inclusive = false,
                                )
                            },
                            onEditGoal = onOpenEditGoal,
                            onAddStage = onOpenCreateStage,
                            onEditStage = onOpenEditStage,
                            onToggleStage = onToggleStage,
                            onAddTask = onOpenCreateTaskLinked,
                            onToggleTask = onToggleTask,
                            onEditTask = onOpenEditTask,
                            onStatusChange = onGoalStatusAction,
                            onDeleteGoal = onGoalDeleteAction,
                            onDeleteDismiss = onGoalDeleteActionDismiss,
                            onDeleteConfirm = onGoalDeleteActionConfirm,
                            onRetry = onRetryGoals,
                        )
                    }
                }
                composable(OrbitDestination.LEARNING.route) {
                    LearningScreen(
                        todayEpochDay = taskState.todayEpochDay,
                        learningProgress = learningProgress,
                        learningActions = learningActions,
                        wordStudyState = wordStudyState,
                        wordStudyActions = wordStudyActions,
                        onAskAi = { prompt ->
                            showAssistant = true
                            onChatSendPrompt(prompt)
                        },
                    )
                }
                composable(OrbitDestination.GROWTH.route) {
                    val reviewViewModel: AiReviewViewModel = viewModel()
                    val reviewState by reviewViewModel.uiState.collectAsStateWithLifecycle()
                    GrowthScreen(
                        taskState = taskState,
                        habitState = habitState,
                        goalState = goalState,
                        growthState = growthState,
                        onRetry = {
                            onRetryLoad()
                            onRetryHabits()
                            onRetryGoals()
                            onRetryGrowth()
                        },
                        onAddTask = onOpenCreateTask,
                        cloudSignedIn = cloudSignedIn,
                        reviewState = reviewState,
                        onGenerateReview = reviewViewModel::generate,
                        onRetryReviewStatus = reviewViewModel::refreshStatus,
                    )
                }
                composable(OrbitDestination.PROFILE.route) {
                    SettingsScreen(
                        uiState = preferences,
                        onThemeModeChange = onThemeModeChange,
                        onAccentToneChange = onAccentToneChange,
                        accountState = accountState,
                        accountActions = accountActions,
                        dataOverview = OrbitDataOverview(
                            tasks = taskState.tasks.size,
                            goals = goalState.goals.size,
                            habits = habitState.habits.size,
                            checkIns = habitState.records.size,
                        ),
                    )
                }
            }
        }
    }

    if (showAssistant) {
        AiAssistantSheet(
            state = aiChatState,
            onDismiss = { showAssistant = false },
            onInputChange = onChatInputChange,
            onSend = onChatSend,
            onRetry = onChatRetry,
            onRefreshStatus = onChatRefreshStatus,
            onOpenPlan = {
                showAssistant = false
                navController.navigate(OrbitDestination.AI_PLAN_ROUTE)
            },
            onOpenReview = {
                showAssistant = false
                navigateTo(OrbitDestination.GROWTH)
            },
            onOpenLearning = {
                showAssistant = false
                navigateTo(OrbitDestination.LEARNING)
            },
            onSendPrompt = onChatSendPrompt,
            onClearConversation = onChatClear,
            onDismissFailure = onChatDismissFailure,
        )
    }

    editorState?.let { editor ->
        TaskEditorSheet(
            editor = editor,
            onDismiss = onDismissEditor,
            onTitleChange = onEditorTitleChange,
            onDescriptionChange = onEditorDescriptionChange,
            onPriorityChange = onEditorPriorityChange,
            onCategoryChange = onEditorCategoryChange,
            onDueDateChange = onEditorDueDateChange,
            // Archived goals stay hidden, except when the edited task is
            // already linked to one so its selection remains visible.
            goals = goalState.orderedGoals.filter {
                it.status != GoalStatus.ARCHIVED || it.id == editor.goalId
            },
            stages = goalState.stages,
            onLinkChange = onEditorLinkChange,
            onSave = onEditorSave,
            onDeleteRequest = onEditorDeleteRequest,
            onDeleteDismiss = onEditorDeleteDismiss,
            onDeleteConfirm = onEditorDeleteConfirm,
        )
    }

    goalEditorState?.let { editor ->
        GoalEditorSheet(
            editor = editor,
            onDismiss = onDismissGoalEditor,
            onTitleChange = onGoalTitleChange,
            onDescriptionChange = onGoalDescriptionChange,
            onToneChange = onGoalToneChange,
            onStatusChange = onGoalStatusChange,
            onTargetDateChange = onGoalTargetDateChange,
            onSave = onGoalSave,
            onDeleteRequest = onGoalDeleteRequest,
            onDeleteDismiss = onGoalDeleteDismiss,
            onDeleteConfirm = onGoalDeleteConfirm,
        )
    }

    stageEditorState?.let { editor ->
        StageEditorSheet(
            editor = editor,
            onDismiss = onDismissStageEditor,
            onTitleChange = onStageTitleChange,
            onTargetDateChange = onStageTargetDateChange,
            onSave = onStageSave,
            onDeleteRequest = onStageDeleteRequest,
            onDeleteDismiss = onStageDeleteDismiss,
            onDeleteConfirm = onStageDeleteConfirm,
        )
    }

    habitEditorState?.let { editor ->
        HabitEditorSheet(
            editor = editor,
            onDismiss = onDismissHabitEditor,
            onTitleChange = onHabitTitleChange,
            onNoteChange = onHabitNoteChange,
            onToneChange = onHabitToneChange,
            onArchiveChange = { archived ->
                editor.habitId?.let { habitId ->
                    onArchiveHabitChange(habitId, archived)
                }
            },
            onSave = onHabitSave,
            onDeleteRequest = onHabitDeleteRequest,
            onDeleteDismiss = onHabitDeleteDismiss,
            onDeleteConfirm = onHabitDeleteConfirm,
        )
    }
}

/** Mirrors the gateway's cap; extra goals are dropped server-side anyway. */
private const val MAX_CHAT_GOALS = 5

private val TaskFeedbackType.isFailure: Boolean
    get() = this == TaskFeedbackType.SAVE_FAILED ||
        this == TaskFeedbackType.DELETE_FAILED ||
        this == TaskFeedbackType.TOGGLE_FAILED

@StringRes
private fun TaskFeedbackType.messageRes(): Int = when (this) {
    TaskFeedbackType.CREATED -> R.string.task_created
    TaskFeedbackType.UPDATED -> R.string.task_updated
    TaskFeedbackType.DELETED -> R.string.task_deleted
    TaskFeedbackType.COMPLETED -> R.string.task_completed
    TaskFeedbackType.REOPENED -> R.string.task_reopened
    TaskFeedbackType.SAVE_FAILED -> R.string.task_save_failed
    TaskFeedbackType.DELETE_FAILED -> R.string.task_delete_failed
    TaskFeedbackType.TOGGLE_FAILED -> R.string.task_toggle_failed
}

private val GoalFeedbackType.isFailure: Boolean
    get() = this == GoalFeedbackType.SAVE_FAILED ||
        this == GoalFeedbackType.DELETE_FAILED ||
        this == GoalFeedbackType.STAGE_SAVE_FAILED ||
        this == GoalFeedbackType.STAGE_DELETE_FAILED ||
        this == GoalFeedbackType.STATUS_FAILED

@StringRes
private fun GoalFeedbackType.messageRes(): Int = when (this) {
    GoalFeedbackType.CREATED -> R.string.goal_created
    GoalFeedbackType.UPDATED -> R.string.goal_updated
    GoalFeedbackType.DELETED -> R.string.goal_deleted
    GoalFeedbackType.STATUS_UPDATED -> R.string.goal_status_updated
    GoalFeedbackType.STAGE_CREATED -> R.string.stage_created
    GoalFeedbackType.STAGE_UPDATED -> R.string.stage_updated
    GoalFeedbackType.STAGE_DELETED -> R.string.stage_deleted
    GoalFeedbackType.STAGE_COMPLETED -> R.string.stage_completed
    GoalFeedbackType.STAGE_REOPENED -> R.string.stage_reopened
    GoalFeedbackType.SAVE_FAILED -> R.string.goal_save_failed
    GoalFeedbackType.DELETE_FAILED -> R.string.goal_delete_failed
    GoalFeedbackType.STAGE_SAVE_FAILED -> R.string.stage_save_failed
    GoalFeedbackType.STAGE_DELETE_FAILED -> R.string.stage_delete_failed
    GoalFeedbackType.STATUS_FAILED -> R.string.goal_status_failed
}

private val HabitFeedbackType.isFailure: Boolean
    get() = this == HabitFeedbackType.SAVE_FAILED ||
        this == HabitFeedbackType.DELETE_FAILED ||
        this == HabitFeedbackType.ARCHIVE_FAILED ||
        this == HabitFeedbackType.TOGGLE_FAILED

@StringRes
private fun HabitFeedbackType.messageRes(): Int = when (this) {
    HabitFeedbackType.CREATED -> R.string.habit_created
    HabitFeedbackType.UPDATED -> R.string.habit_updated
    HabitFeedbackType.ARCHIVED -> R.string.habit_archived
    HabitFeedbackType.RESTORED -> R.string.habit_restored
    HabitFeedbackType.DELETED -> R.string.habit_deleted
    HabitFeedbackType.CHECKED -> R.string.habit_checked
    HabitFeedbackType.UNCHECKED -> R.string.habit_unchecked
    HabitFeedbackType.SAVE_FAILED -> R.string.habit_save_failed
    HabitFeedbackType.DELETE_FAILED -> R.string.habit_delete_failed
    HabitFeedbackType.ARCHIVE_FAILED -> R.string.habit_archive_failed
    HabitFeedbackType.TOGGLE_FAILED -> R.string.habit_toggle_failed
}

/**
 * Floating status line. Deliberately built from plain layout nodes instead of a
 * Material Surface/Snackbar: those attach an empty pointerInput node, which
 * becomes the hit target for every tap inside their bounds and silently blocks
 * the control underneath (e.g. a task's complete button or a delete action).
 * This toast stays fully pass-through except for its optional dismiss control.
 */
@Composable
private fun OrbitToast(data: SnackbarData) {
    val colors = MaterialTheme.orbitColors
    val dismissLabel = stringResource(R.string.content_desc_close)
    val surface = if (colors.isDark) colors.cardElevated else colors.textPrimary
    val onSurface = if (colors.isDark) colors.textPrimary else colors.backgroundTop
    val shape = RoundedCornerShape(OrbitCorners.control)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .clip(shape)
                .background(surface)
                .then(
                    if (colors.isDark) {
                        Modifier.border(1.dp, colors.cardBorder, shape)
                    } else {
                        Modifier
                    },
                )
                .heightIn(min = 52.dp)
                .padding(
                    start = 18.dp,
                    end = if (data.visuals.withDismissAction) 2.dp else 18.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(colors.primary),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = data.visuals.message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (data.visuals.withDismissAction) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(OrbitCorners.pill))
                        .clickable(role = Role.Button, onClick = data::dismiss)
                        .semantics { contentDescription = dismissLabel },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemBarAppearance() {
    val darkTheme = MaterialTheme.orbitColors.isDark
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var context: Context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
