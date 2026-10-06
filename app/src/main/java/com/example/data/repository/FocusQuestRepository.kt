package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AchievementEntity
import com.example.data.local.entity.DailyChallengeEntity
import com.example.data.local.entity.FocusSessionEntity
import com.example.data.local.entity.RewardEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.UserEntity
import com.example.domain.model.ChallengeType
import com.example.domain.model.GamificationEngine
import com.example.domain.model.SessionType
import com.example.domain.model.TaskPriority
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate

class FocusQuestRepository(
    private val database: AppDatabase
) {
    private val userDao = database.userDao()
    private val taskDao = database.taskDao()
    private val sessionDao = database.focusSessionDao()
    private val achievementDao = database.achievementDao()
    private val challengeDao = database.dailyChallengeDao()
    private val rewardDao = database.rewardDao()

    // ------------------- USER -------------------
    val userFlow: Flow<UserEntity?> = userDao.getUserFlow()

    suspend fun getUser(): UserEntity {
        return userDao.getUser() ?: run {
            AppDatabase.seedDatabase(database)
            userDao.getUser() ?: UserEntity()
        }
    }

    suspend fun updateUser(user: UserEntity) {
        userDao.insertOrUpdate(user)
    }

    suspend fun updateOnboardingProfile(name: String, mainGoal: String, dailyGoalMinutes: Int) {
        val current = getUser()
        val updated = current.copy(
            name = name.ifBlank { "Focus Hero" },
            mainGoal = mainGoal.ifBlank { "Master Productivity" },
            dailyFocusGoalMinutes = dailyGoalMinutes.coerceAtLeast(15)
        )
        userDao.insertOrUpdate(updated)
    }

    // ------------------- TASKS -------------------
    val allTasksFlow: Flow<List<TaskEntity>> = taskDao.getAllTasksFlow()
    val totalCompletedTasksCount: Flow<Int> = taskDao.getTotalCompletedTasksCount()

    fun getTodayCompletedTasks(startOfDayMillis: Long): Flow<List<TaskEntity>> {
        return taskDao.getTodayCompletedTasksFlow(startOfDayMillis)
    }

    suspend fun insertTask(task: TaskEntity): Long {
        return taskDao.insertTask(task)
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
    }

    suspend fun deleteTaskById(id: Long) {
        taskDao.deleteTaskById(id)
    }

    /**
     * Toggles task completion safely with duplicate reward protection.
     * XP and coins are only awarded ONCE per task lifetime.
     */
    suspend fun toggleTaskCompletion(task: TaskEntity): Pair<Int, Int> {
        val now = System.currentTimeMillis()
        val willBeCompleted = !task.isCompleted

        var xpGained = 0
        var coinsGained = 0

        if (willBeCompleted) {
            val priority = TaskPriority.fromString(task.priority)
            val shouldAward = !task.xpAwarded

            if (shouldAward) {
                xpGained = priority.xpReward
                coinsGained = priority.coinReward
            }

            val updatedTask = task.copy(
                isCompleted = true,
                completedAtMillis = now,
                xpAwarded = true,
                coinsAwarded = true
            )
            taskDao.updateTask(updatedTask)

            if (xpGained > 0 || coinsGained > 0) {
                awardXpAndCoins(xpGained, coinsGained)
            }

            // Update streak on task completion if needed
            touchDailyActivity()
            checkTaskAchievements()
            updateDailyChallengesProgress(
                tasksCompletedDelta = 1,
                highPriorityDelta = if (priority == TaskPriority.HIGH || priority == TaskPriority.URGENT) 1 else 0
            )
        } else {
            // Uncheck task: do not retract already earned XP to maintain positive gamification feeling,
            // but keep xpAwarded = true so user cannot toggle to farm XP!
            val updatedTask = task.copy(
                isCompleted = false,
                completedAtMillis = null
            )
            taskDao.updateTask(updatedTask)
        }

        return Pair(xpGained, coinsGained)
    }

    // ------------------- FOCUS SESSIONS -------------------
    val allSessionsFlow: Flow<List<FocusSessionEntity>> = sessionDao.getAllSessionsFlow()
    val totalFocusMinutesFlow: Flow<Int?> = sessionDao.getTotalFocusMinutesFlow()
    val totalFocusSessionsCountFlow: Flow<Int> = sessionDao.getTotalFocusSessionsCountFlow()

    fun getTodayFocusMinutesFlow(startOfDayMillis: Long): Flow<Int?> {
        return sessionDao.getTodayFocusMinutesFlow(startOfDayMillis)
    }

    suspend fun recordFocusSession(
        durationMinutes: Int,
        targetMinutes: Int,
        sessionType: SessionType,
        linkedTaskId: Long? = null,
        linkedTaskTitle: String? = null
    ): Pair<Int, Int> {
        val safeDuration = durationMinutes.coerceAtLeast(1)
        val isFocus = sessionType == SessionType.FOCUS

        val xpEarned = if (isFocus) {
            GamificationEngine.calculateFocusSessionXp(safeDuration, targetMinutes)
        } else 0

        val coinsEarned = if (isFocus) {
            GamificationEngine.calculateFocusSessionCoins(safeDuration, targetMinutes)
        } else 0

        val session = FocusSessionEntity(
            durationMinutes = safeDuration,
            targetMinutes = targetMinutes,
            sessionType = sessionType.name,
            completedAtMillis = System.currentTimeMillis(),
            xpEarned = xpEarned,
            coinsEarned = coinsEarned,
            linkedTaskId = linkedTaskId,
            linkedTaskTitle = linkedTaskTitle
        )
        sessionDao.insertSession(session)

        if (isFocus) {
            awardXpAndCoins(xpEarned, coinsEarned)
            touchDailyActivity()
            checkFocusAchievements(safeDuration)
            updateDailyChallengesProgress(focusMinutesDelta = safeDuration)
        }

        return Pair(xpEarned, coinsEarned)
    }

    // ------------------- GAMIFICATION & PROGRESS -------------------
    private suspend fun awardXpAndCoins(xp: Int, coins: Int) {
        val user = getUser()
        val newTotalXp = user.totalXp + xp
        val newCoins = user.coins + coins
        val newLevelInfo = GamificationEngine.calculateLevelInfo(newTotalXp)

        val updatedUser = user.copy(
            totalXp = newTotalXp,
            level = newLevelInfo.level,
            coins = newCoins
        )
        userDao.insertOrUpdate(updatedUser)
    }

    suspend fun touchDailyActivity() {
        val user = getUser()
        val today = LocalDate.now()
        val (newStreak, newLongest, todayStr) = GamificationEngine.updateStreak(
            currentStreak = user.currentStreak,
            longestStreak = user.longestStreak,
            lastActiveDateStr = user.lastActiveDate,
            today = today
        )
        val updated = user.copy(
            currentStreak = newStreak,
            longestStreak = newLongest,
            lastActiveDate = todayStr
        )
        userDao.insertOrUpdate(updated)

        checkStreakAchievements(newStreak)
        updateDailyChallengesProgress(streakMaintained = true)
    }

    // ------------------- ACHIEVEMENTS -------------------
    val allAchievementsFlow: Flow<List<AchievementEntity>> = achievementDao.getAllAchievementsFlow()
    val unlockedAchievementsCount: Flow<Int> = achievementDao.getUnlockedCountFlow()

    private suspend fun checkFocusAchievements(lastSessionMinutes: Int) {
        val totalSessions = sessionDao.getTotalFocusSessionsCountFlow().firstOrNull() ?: 1
        val totalMinutes = sessionDao.getTotalFocusMinutesFlow().firstOrNull() ?: lastSessionMinutes

        // Check first focus
        checkAndUnlock("first_focus", totalSessions)
        checkAndUnlock("focus_master_10", totalSessions)
        checkAndUnlock("focus_master_50", totalSessions)
        checkAndUnlock("deep_hours_5", totalMinutes)

        // Early Bird: before 9 AM
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        if (currentHour < 9) {
            checkAndUnlock("early_bird", 1)
        }
    }

    private suspend fun checkTaskAchievements() {
        val completedCount = taskDao.getTotalCompletedTasksCount().firstOrNull() ?: 1
        checkAndUnlock("task_slayer_10", completedCount)
        checkAndUnlock("productivity_beast_100", completedCount)
    }

    private suspend fun checkStreakAchievements(streak: Int) {
        checkAndUnlock("streak_3", streak)
        checkAndUnlock("streak_7", streak)
    }

    private suspend fun checkAndUnlock(achievementId: String, currentVal: Int) {
        val achievement = achievementDao.getAchievementById(achievementId) ?: return
        if (achievement.isUnlocked) return

        val updatedProgress = maxOf(achievement.currentProgress, currentVal)
        if (updatedProgress >= achievement.targetValue) {
            val unlocked = achievement.copy(
                currentProgress = achievement.targetValue,
                isUnlocked = true,
                unlockedAtMillis = System.currentTimeMillis()
            )
            achievementDao.update(unlocked)
            awardXpAndCoins(unlocked.xpReward, unlocked.coinReward)
        } else if (updatedProgress > achievement.currentProgress) {
            achievementDao.update(achievement.copy(currentProgress = updatedProgress))
        }
    }

    // ------------------- DAILY CHALLENGES -------------------
    fun getTodayChallengesFlow(): Flow<List<DailyChallengeEntity>> {
        val todayStr = LocalDate.now().toString()
        return challengeDao.getChallengesForDateFlow(todayStr)
    }

    suspend fun ensureTodayChallengesExist() {
        val todayStr = LocalDate.now().toString()
        val existing = challengeDao.getChallengesForDate(todayStr)
        if (existing.isEmpty()) {
            val generated = listOf(
                DailyChallengeEntity(
                    id = "${todayStr}_FOCUS_45",
                    date = todayStr,
                    title = "Deep Focus Sprint",
                    description = "Focus for at least 45 minutes today.",
                    challengeType = ChallengeType.MINUTES_FOCUSED.name,
                    targetValue = 45,
                    currentProgress = 0,
                    xpReward = 60,
                    coinReward = 25
                ),
                DailyChallengeEntity(
                    id = "${todayStr}_TASKS_3",
                    date = todayStr,
                    title = "Task Sprinter",
                    description = "Complete 3 tasks today.",
                    challengeType = ChallengeType.TASKS_COMPLETED.name,
                    targetValue = 3,
                    currentProgress = 0,
                    xpReward = 50,
                    coinReward = 20
                ),
                DailyChallengeEntity(
                    id = "${todayStr}_HIGH_PRIORITY",
                    date = todayStr,
                    title = "High Impact Champion",
                    description = "Complete 1 High or Urgent priority task.",
                    challengeType = ChallengeType.HIGH_PRIORITY_TASKS.name,
                    targetValue = 1,
                    currentProgress = 0,
                    xpReward = 40,
                    coinReward = 15
                ),
                DailyChallengeEntity(
                    id = "${todayStr}_STREAK_ACTIVE",
                    date = todayStr,
                    title = "Streak Guardian",
                    description = "Complete any task or session to protect your streak.",
                    challengeType = ChallengeType.STREAK_MAINTAINED.name,
                    targetValue = 1,
                    currentProgress = 1,
                    isCompleted = true,
                    xpReward = 30,
                    coinReward = 10
                )
            )
            challengeDao.insertAll(generated)
        }
    }

    private suspend fun updateDailyChallengesProgress(
        focusMinutesDelta: Int = 0,
        tasksCompletedDelta: Int = 0,
        highPriorityDelta: Int = 0,
        streakMaintained: Boolean = false
    ) {
        val todayStr = LocalDate.now().toString()
        val challenges = challengeDao.getChallengesForDate(todayStr)

        for (challenge in challenges) {
            if (challenge.isCompleted) continue

            var newProgress = challenge.currentProgress
            when (challenge.challengeType) {
                ChallengeType.MINUTES_FOCUSED.name -> {
                    newProgress += focusMinutesDelta
                }
                ChallengeType.TASKS_COMPLETED.name -> {
                    newProgress += tasksCompletedDelta
                }
                ChallengeType.HIGH_PRIORITY_TASKS.name -> {
                    newProgress += highPriorityDelta
                }
                ChallengeType.STREAK_MAINTAINED.name -> {
                    if (streakMaintained) newProgress = 1
                }
            }

            val isNowCompleted = newProgress >= challenge.targetValue
            if (newProgress != challenge.currentProgress || isNowCompleted) {
                val updated = challenge.copy(
                    currentProgress = newProgress.coerceAtMost(challenge.targetValue),
                    isCompleted = isNowCompleted
                )
                challengeDao.update(updated)
            }
        }
    }

    suspend fun claimChallengeReward(challengeId: String): Boolean {
        val challenge = challengeDao.getChallengeById(challengeId) ?: return false
        if (!challenge.isCompleted || challenge.isClaimed) return false

        val updated = challenge.copy(isClaimed = true)
        challengeDao.update(updated)
        awardXpAndCoins(challenge.xpReward, challenge.coinReward)
        return true
    }

    // ------------------- REWARDS & COSMETICS -------------------
    val allRewardsFlow: Flow<List<RewardEntity>> = rewardDao.getAllRewardsFlow()

    suspend fun purchaseReward(rewardId: String): Boolean {
        val user = getUser()
        val reward = rewardDao.getRewardById(rewardId) ?: return false
        if (reward.isPurchased) return true
        if (user.coins < reward.costCoins) return false

        val newCoins = user.coins - reward.costCoins
        userDao.insertOrUpdate(user.copy(coins = newCoins))
        rewardDao.update(reward.copy(isPurchased = true))
        return true
    }

    suspend fun equipReward(rewardId: String): Boolean {
        val reward = rewardDao.getRewardById(rewardId) ?: return false
        if (!reward.isPurchased) return false

        rewardDao.unequipAllOfType(reward.type)
        rewardDao.equipReward(reward.id)

        val user = getUser()
        if (reward.type == "THEME") {
            userDao.insertOrUpdate(user.copy(equippedTheme = reward.id))
        } else if (reward.type == "AVATAR_FRAME") {
            userDao.insertOrUpdate(user.copy(equippedAvatarFrame = reward.id))
        }
        return true
    }
}
