package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.entity.TaskEntity
import com.example.domain.model.SessionType
import com.example.ui.common.CelebrationDialog
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.leaderboard.LeaderboardScreen
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.pro.MonetizationScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.rewards.RewardsScreen
import com.example.ui.screens.stats.StatsScreen
import com.example.ui.screens.tasks.TaskDialog
import com.example.ui.screens.tasks.TasksScreen
import com.example.ui.screens.timer.FocusTimerScreen
import com.example.ui.viewmodel.MainViewModel

@Composable
fun FocusQuestNavGraph(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val user by viewModel.user.collectAsStateWithLifecycle()
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()
    val tasks by viewModel.filteredTasks.collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.taskFilterCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.taskSearchQuery.collectAsStateWithLifecycle()
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val totalFocusMinutes by viewModel.totalFocusMinutes.collectAsStateWithLifecycle()
    val achievements by viewModel.allAchievements.collectAsStateWithLifecycle()
    val dailyChallenges by viewModel.dailyChallenges.collectAsStateWithLifecycle()
    val rewards by viewModel.allRewards.collectAsStateWithLifecycle()
    val celebration by viewModel.celebration.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog State
    var showTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissSnackbar()
        }
    }

    val isBottomBarVisible = currentRoute in Screen.bottomNavItems.map { it.route }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("bottom_nav_bar")
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = (if (isSelected) screen.filledIcon else screen.outlinedIcon)
                                        ?: screen.filledIcon!!,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("bottom_nav_${screen.route}")
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isOnboardingCompleted) Screen.Home.route else Screen.Onboarding.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onComplete = { name, goal, dailyMins ->
                        viewModel.completeOnboarding(name, goal, dailyMins)
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    },
                    onSkip = {
                        viewModel.skipOnboarding()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                DashboardScreen(
                    user = user,
                    tasks = allTasks,
                    dailyChallenges = dailyChallenges,
                    todayFocusMinutes = totalFocusMinutes ?: 0,
                    onStartFocusClick = {
                        navController.navigate(Screen.Focus.route)
                    },
                    onAddTaskClick = {
                        taskToEdit = null
                        showTaskDialog = true
                    },
                    onViewStatsClick = {
                        navController.navigate(Screen.Stats.route)
                    },
                    onCoinsClick = {
                        navController.navigate(Screen.Rewards.route)
                    },
                    onToggleTask = { task ->
                        viewModel.toggleTask(task)
                    },
                    onClaimChallenge = { challenge ->
                        viewModel.claimChallenge(challenge)
                    }
                )
            }

            composable(Screen.Tasks.route) {
                TasksScreen(
                    tasks = tasks,
                    selectedCategory = selectedCategory,
                    searchQuery = searchQuery,
                    onCategoryFilterChange = { viewModel.setTaskCategoryFilter(it) },
                    onSearchQueryChange = { viewModel.setTaskSearchQuery(it) },
                    onToggleTask = { viewModel.toggleTask(it) },
                    onDeleteTask = { viewModel.deleteTask(it) },
                    onEditTask = {
                        taskToEdit = it
                        showTaskDialog = true
                    },
                    onStartFocusWithTask = { task ->
                        viewModel.linkTaskToTimer(task)
                        navController.navigate(Screen.Focus.route)
                    },
                    onAddNewTaskClick = {
                        taskToEdit = null
                        showTaskDialog = true
                    }
                )
            }

            composable(Screen.Focus.route) {
                FocusTimerScreen(
                    timerState = timerState,
                    onStartClick = { viewModel.startTimer() },
                    onPauseClick = { viewModel.pauseTimer() },
                    onResumeClick = { viewModel.resumeTimer() },
                    onResetClick = { viewModel.resetTimer() },
                    onFinishEarlyClick = { viewModel.finishSessionEarly() },
                    onSelectSessionType = { type, customMins ->
                        viewModel.setTimerSessionType(type, customMins)
                    },
                    onUnlinkTask = { viewModel.linkTaskToTimer(null) }
                )
            }

            composable(Screen.Stats.route) {
                StatsScreen(
                    user = user,
                    tasks = allTasks,
                    sessions = allSessions,
                    totalFocusMinutes = totalFocusMinutes ?: 0,
                    onNavigateToTimer = {
                        navController.navigate(Screen.Focus.route)
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    user = user,
                    achievements = achievements,
                    themeMode = themeMode,
                    onToggleThemeMode = { viewModel.setThemeMode(it) },
                    onToggleNotifications = { viewModel.toggleNotifications(it) },
                    onToggleSound = { viewModel.toggleSound(it) },
                    onToggleHaptic = { viewModel.toggleHaptic(it) },
                    onUpdateDailyGoal = { viewModel.updateDailyGoal(it) },
                    onNavigateToPro = { navController.navigate(Screen.ProUpgrade.route) },
                    onNavigateToRewards = { navController.navigate(Screen.Rewards.route) }
                )
            }

            composable(Screen.Rewards.route) {
                RewardsScreen(
                    user = user,
                    rewards = rewards,
                    onBuyReward = { viewModel.buyReward(it) },
                    onEquipReward = { viewModel.equipReward(it) }
                )
            }

            composable(Screen.Leaderboard.route) {
                LeaderboardScreen(
                    user = user,
                    leaderboardEntries = viewModel.getLeaderboard(user)
                )
            }

            composable(Screen.ProUpgrade.route) {
                MonetizationScreen(
                    isPro = user?.isProUser ?: false,
                    onUpgradeClick = { viewModel.upgradeToPro() },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }

    // Celebration Reward Overlay Dialog
    celebration?.let { cel ->
        CelebrationDialog(
            title = cel.title,
            message = cel.message,
            xpEarned = cel.xpEarned,
            coinsEarned = cel.coinsEarned,
            onDismiss = { viewModel.dismissCelebration() }
        )
    }

    // Task Creation/Editing Dialog
    if (showTaskDialog) {
        TaskDialog(
            taskToEdit = taskToEdit,
            onDismiss = {
                showTaskDialog = false
                taskToEdit = null
            },
            onSave = { title, desc, priority, category, estMins ->
                val current = taskToEdit
                if (current == null) {
                    viewModel.createTask(
                        title = title,
                        description = desc,
                        priority = priority,
                        category = category,
                        estimatedMinutes = estMins,
                        dueDateMillis = null
                    )
                } else {
                    viewModel.updateTask(
                        current.copy(
                            title = title,
                            description = desc,
                            priority = priority.name,
                            category = category.name,
                            estimatedMinutes = estMins
                        )
                    )
                }
                showTaskDialog = false
                taskToEdit = null
            }
        )
    }
}
