package com.example.ui.viewmodel

import android.app.Application
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AchievementEntity
import com.example.data.local.entity.DailyChallengeEntity
import com.example.data.local.entity.FocusSessionEntity
import com.example.data.local.entity.RewardEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.preferences.UserPreferencesDataStore
import com.example.data.repository.FocusQuestRepository
import com.example.domain.model.GamificationEngine
import com.example.domain.model.SessionType
import com.example.domain.model.TaskCategory
import com.example.domain.model.TaskPriority
import com.example.domain.model.UserLevelInfo
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CelebrationState(
    val title: String,
    val message: String,
    val xpEarned: Int,
    val coinsEarned: Int
)

data class TimerState(
    val sessionType: SessionType = SessionType.FOCUS,
    val totalSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val selectedTask: TaskEntity? = null
)

data class LeaderboardEntry(
    val rank: Int,
    val name: String,
    val level: Int,
    val xp: Int,
    val streak: Int,
    val isCurrentUser: Boolean = false,
    val badge: String = "🛡️"
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val repository = FocusQuestRepository(database)
    val preferences = UserPreferencesDataStore(application)

    // User & Preferences
    val user: StateFlow<UserEntity?> = repository.userFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val isOnboardingCompleted: StateFlow<Boolean> = preferences.isOnboardingCompleted.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val activeThemeId: StateFlow<String> = preferences.activeThemeId.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "theme_deep_slate"
    )

    val themeMode: StateFlow<String> = preferences.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "DARK"
    )

    // Tasks
    val allTasks: StateFlow<List<TaskEntity>> = repository.allTasksFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val taskFilterCategory = MutableStateFlow<TaskCategory?>(null)
    val taskSearchQuery = MutableStateFlow("")

    val filteredTasks: StateFlow<List<TaskEntity>> = combine(
        allTasks,
        taskFilterCategory,
        taskSearchQuery
    ) { tasks, catFilter, query ->
        tasks.filter { task ->
            val matchesCategory = (catFilter == null) || (task.category == catFilter.name)
            val matchesQuery = query.isBlank() || task.title.contains(query, ignoreCase = true) ||
                    task.description.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sessions & Stats
    val allSessions: StateFlow<List<FocusSessionEntity>> = repository.allSessionsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalFocusMinutes: StateFlow<Int?> = repository.totalFocusMinutesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    // Achievements & Challenges
    val allAchievements: StateFlow<List<AchievementEntity>> = repository.allAchievementsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val dailyChallenges: StateFlow<List<DailyChallengeEntity>> = repository.getTodayChallengesFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allRewards: StateFlow<List<RewardEntity>> = repository.allRewardsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Timer State
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null
    private var targetEndTimestamp: Long = 0L

    // Celebration / Reward Popups
    private val _celebration = MutableStateFlow<CelebrationState?>(null)
    val celebration: StateFlow<CelebrationState?> = _celebration.asStateFlow()

    // Snackbar / Feedback message
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureTodayChallengesExist()
        }
    }

    fun dismissSnackbar() {
        _snackbarMessage.value = null
    }

    fun dismissCelebration() {
        _celebration.value = null
    }

    // ------------------- ONBOARDING -------------------
    fun completeOnboarding(name: String, mainGoal: String, dailyGoalMinutes: Int) {
        viewModelScope.launch {
            repository.updateOnboardingProfile(name, mainGoal, dailyGoalMinutes)
            preferences.setOnboardingCompleted(true)
        }
    }

    fun skipOnboarding() {
        viewModelScope.launch {
            preferences.setOnboardingCompleted(true)
        }
    }

    // ------------------- TASKS -------------------
    fun setTaskCategoryFilter(category: TaskCategory?) {
        taskFilterCategory.value = category
    }

    fun setTaskSearchQuery(query: String) {
        taskSearchQuery.value = query
    }

    fun createTask(
        title: String,
        description: String,
        priority: TaskPriority,
        category: TaskCategory,
        estimatedMinutes: Int,
        dueDateMillis: Long?
    ) {
        viewModelScope.launch {
            if (title.isBlank()) return@launch
            val task = TaskEntity(
                title = title.trim(),
                description = description.trim(),
                priority = priority.name,
                category = category.name,
                estimatedMinutes = estimatedMinutes,
                dueDateMillis = dueDateMillis
            )
            repository.insertTask(task)
            _snackbarMessage.value = "Task created! Complete it to earn +${priority.xpReward} XP"
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
            _snackbarMessage.value = "Task deleted"
        }
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            val (xpGained, coinsGained) = repository.toggleTaskCompletion(task)
            if (xpGained > 0 || coinsGained > 0) {
                triggerHapticFeedback()
                _celebration.value = CelebrationState(
                    title = "Quest Completed! ⚔️",
                    message = "\"${task.title}\"",
                    xpEarned = xpGained,
                    coinsEarned = coinsGained
                )
            }
        }
    }

    // ------------------- TIMER -------------------
    fun setTimerSessionType(sessionType: SessionType, customMinutes: Int? = null) {
        pauseTimer()
        val minutes = customMinutes ?: sessionType.defaultMinutes
        val seconds = minutes * 60
        _timerState.update {
            it.copy(
                sessionType = sessionType,
                totalSeconds = seconds,
                remainingSeconds = seconds,
                isRunning = false,
                isPaused = false
            )
        }
    }

    fun setCustomTimerMinutes(minutes: Int) {
        pauseTimer()
        val safeMinutes = minutes.coerceIn(1, 180)
        val seconds = safeMinutes * 60
        _timerState.update {
            it.copy(
                totalSeconds = seconds,
                remainingSeconds = seconds,
                isRunning = false,
                isPaused = false
            )
        }
    }

    fun linkTaskToTimer(task: TaskEntity?) {
        _timerState.update { it.copy(selectedTask = task) }
    }

    fun startTimer() {
        val current = _timerState.value
        if (current.isRunning) return

        targetEndTimestamp = System.currentTimeMillis() + (current.remainingSeconds * 1000L)
        _timerState.update { it.copy(isRunning = true, isPaused = false) }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timerState.value.remainingSeconds > 0) {
                delay(1000)
                val remaining = ((targetEndTimestamp - System.currentTimeMillis()) / 1000L).toInt()
                if (remaining <= 0) {
                    _timerState.update { it.copy(remainingSeconds = 0, isRunning = false) }
                    onTimerFinished()
                    break
                } else {
                    _timerState.update { it.copy(remainingSeconds = remaining) }
                }
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
        _timerState.update { it.copy(isRunning = false, isPaused = true) }
    }

    fun resumeTimer() {
        startTimer()
    }

    fun resetTimer() {
        pauseTimer()
        val current = _timerState.value
        _timerState.update {
            it.copy(
                remainingSeconds = it.totalSeconds,
                isRunning = false,
                isPaused = false
            )
        }
    }

    fun finishSessionEarly() {
        val current = _timerState.value
        val elapsedSeconds = current.totalSeconds - current.remainingSeconds
        val elapsedMinutes = (elapsedSeconds / 60).coerceAtLeast(1)

        pauseTimer()
        completeSession(elapsedMinutes, current.totalSeconds / 60)
        resetTimer()
    }

    private fun onTimerFinished() {
        val current = _timerState.value
        val fullMinutes = current.totalSeconds / 60
        completeSession(fullMinutes, fullMinutes)
        resetTimer()
    }

    private fun completeSession(completedMinutes: Int, targetMinutes: Int) {
        viewModelScope.launch {
            val current = _timerState.value
            val isFocus = current.sessionType == SessionType.FOCUS
            val linkedTask = current.selectedTask

            val (xpEarned, coinsEarned) = repository.recordFocusSession(
                durationMinutes = completedMinutes,
                targetMinutes = targetMinutes,
                sessionType = current.sessionType,
                linkedTaskId = linkedTask?.id,
                linkedTaskTitle = linkedTask?.title
            )

            // If a task was linked, auto-complete it if appropriate!
            if (linkedTask != null && !linkedTask.isCompleted) {
                repository.toggleTaskCompletion(linkedTask)
            }

            triggerHapticFeedback()

            NotificationHelper.showTimerCompletedNotification(
                getApplication(),
                current.sessionType.name,
                completedMinutes,
                xpEarned
            )

            if (isFocus) {
                _celebration.value = CelebrationState(
                    title = "Focus Session Victorious! 🎯",
                    message = "You mastered $completedMinutes minutes of deep focus!",
                    xpEarned = xpEarned,
                    coinsEarned = coinsEarned
                )
            }
        }
    }

    private fun triggerHapticFeedback() {
        try {
            val vibrator = getApplication<Application>().getSystemService(Vibrator::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120)
            }
        } catch (_: Exception) {}
    }

    // ------------------- CHALLENGES -------------------
    fun claimChallenge(challenge: DailyChallengeEntity) {
        viewModelScope.launch {
            val success = repository.claimChallengeReward(challenge.id)
            if (success) {
                triggerHapticFeedback()
                _celebration.value = CelebrationState(
                    title = "Challenge Conquered! ⚡",
                    message = challenge.title,
                    xpEarned = challenge.xpReward,
                    coinsEarned = challenge.coinReward
                )
            }
        }
    }

    // ------------------- REWARDS SHOP -------------------
    fun buyReward(reward: RewardEntity) {
        viewModelScope.launch {
            val success = repository.purchaseReward(reward.id)
            if (success) {
                _snackbarMessage.value = "Purchased ${reward.title}!"
                equipReward(reward)
            } else {
                _snackbarMessage.value = "Not enough coins! Complete more quests."
            }
        }
    }

    fun equipReward(reward: RewardEntity) {
        viewModelScope.launch {
            val success = repository.equipReward(reward.id)
            if (success) {
                if (reward.type == "THEME") {
                    preferences.setActiveTheme(reward.id)
                }
                _snackbarMessage.value = "Equipped ${reward.title}"
            }
        }
    }

    // ------------------- SETTINGS -------------------
    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferences.setThemeMode(mode)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            val u = repository.getUser()
            repository.updateUser(u.copy(notificationsEnabled = enabled))
        }
    }

    fun toggleSound(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setSoundEnabled(enabled)
            val u = repository.getUser()
            repository.updateUser(u.copy(soundEnabled = enabled))
        }
    }

    fun toggleHaptic(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setHapticEnabled(enabled)
            val u = repository.getUser()
            repository.updateUser(u.copy(hapticEnabled = enabled))
        }
    }

    fun updateDailyGoal(minutes: Int) {
        viewModelScope.launch {
            val u = repository.getUser()
            repository.updateUser(u.copy(dailyFocusGoalMinutes = minutes))
            _snackbarMessage.value = "Daily target updated to $minutes minutes"
        }
    }

    fun upgradeToPro() {
        viewModelScope.launch {
            val u = repository.getUser()
            repository.updateUser(u.copy(isProUser = true))
            _snackbarMessage.value = "Welcome to FocusQuest Pro! All premium features unlocked."
        }
    }

    // ------------------- LEADERBOARD (Dynamic cohort) -------------------
    fun getLeaderboard(currentUser: UserEntity?): List<LeaderboardEntry> {
        val userXp = currentUser?.totalXp ?: 150
        val userLevel = currentUser?.level ?: 2
        val userStreak = currentUser?.currentStreak ?: 3
        val userName = currentUser?.name ?: "You"

        val cohort = listOf(
            LeaderboardEntry(1, "Aria 'Cyber' V.", 12, maxOf(userXp + 620, 2400), 14, false, "👑"),
            LeaderboardEntry(2, "Kenji Sato", 9, maxOf(userXp + 340, 1850), 11, false, "💎"),
            LeaderboardEntry(3, "Elena Rostova", 8, maxOf(userXp + 120, 1420), 8, false, "🥇"),
            LeaderboardEntry(4, "$userName (You)", userLevel, userXp, userStreak, true, "⚡"),
            LeaderboardEntry(5, "Marcus Vance", 6, (userXp - 80).coerceAtLeast(400), 5, false, "🥈"),
            LeaderboardEntry(6, "Chloe Bennett", 5, (userXp - 190).coerceAtLeast(320), 4, false, "🥉"),
            LeaderboardEntry(7, "Devon Cross", 4, (userXp - 280).coerceAtLeast(210), 3, false, "🛡️"),
            LeaderboardEntry(8, "Sophia Lin", 3, (userXp - 350).coerceAtLeast(150), 2, false, "🛡️")
        )
        return cohort.sortedByDescending { it.xp }.mapIndexed { index, entry ->
            entry.copy(rank = index + 1)
        }
    }
}
