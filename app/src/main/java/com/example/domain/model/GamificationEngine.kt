package com.example.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class UserLevelInfo(
    val level: Int,
    val title: String,
    val currentLevelBaseXp: Int,
    val nextLevelBaseXp: Int,
    val xpInCurrentLevel: Int,
    val xpNeededForNextLevel: Int,
    val progressFraction: Float
)

object GamificationEngine {

    /**
     * Calculates the cumulative total XP required to reach a specific level.
     * Level 1: 0 XP
     * Level 2: 100 XP
     * Level 3: 250 XP
     * Level 4: 450 XP
     * Level 5: 700 XP
     * Scalable formula: xpForLevel(L) = 25 * L * (L + 1) - 50 for L >= 2
     */
    fun totalXpRequiredForLevel(level: Int): Int {
        if (level <= 1) return 0
        return 25 * level * (level + 1) - 50
    }

    /**
     * Given total XP, determines current level and progress info.
     */
    fun calculateLevelInfo(totalXp: Int): UserLevelInfo {
        val safeXp = totalXp.coerceAtLeast(0)
        var level = 1
        while (totalXpRequiredForLevel(level + 1) <= safeXp) {
            level++
        }

        val currentLevelBaseXp = totalXpRequiredForLevel(level)
        val nextLevelBaseXp = totalXpRequiredForLevel(level + 1)
        val xpNeededForNext = (nextLevelBaseXp - currentLevelBaseXp).coerceAtLeast(1)
        val xpInLevel = (safeXp - currentLevelBaseXp).coerceIn(0, xpNeededForNext)
        val progress = (xpInLevel.toFloat() / xpNeededForNext.toFloat()).coerceIn(0f, 1f)

        return UserLevelInfo(
            level = level,
            title = getTitleForLevel(level),
            currentLevelBaseXp = currentLevelBaseXp,
            nextLevelBaseXp = nextLevelBaseXp,
            xpInCurrentLevel = xpInLevel,
            xpNeededForNextLevel = xpNeededForNext,
            progressFraction = progress
        )
    }

    fun getTitleForLevel(level: Int): String {
        return when {
            level <= 1 -> "Novice Seeker"
            level == 2 -> "Focused Apprentice"
            level == 3 -> "Time Guardian"
            level == 4 -> "Sprint Knight"
            level == 5 -> "Flow Wanderer"
            level in 6..7 -> "Productivity Beast"
            level in 8..9 -> "Focus Champion"
            level in 10..14 -> "Deep Work Sage"
            level in 15..19 -> "Zen Grandmaster"
            else -> "Mythic Focus Legend"
        }
    }

    /**
     * Calculates updated streak based on last active date string ("yyyy-MM-dd").
     * Returns Triple(newCurrentStreak, newLongestStreak, todayString)
     */
    fun updateStreak(
        currentStreak: Int,
        longestStreak: Int,
        lastActiveDateStr: String?,
        today: LocalDate = LocalDate.now()
    ): Triple<Int, Int, String> {
        val todayStr = today.toString()
        if (lastActiveDateStr.isNullOrBlank()) {
            return Triple(1, maxOf(longestStreak, 1), todayStr)
        }

        return try {
            val lastDate = LocalDate.parse(lastActiveDateStr)
            val daysBetween = ChronoUnit.DAYS.between(lastDate, today)

            when {
                daysBetween == 0L -> {
                    // Already active today, streak remains unchanged
                    Triple(currentStreak.coerceAtLeast(1), maxOf(longestStreak, currentStreak.coerceAtLeast(1)), todayStr)
                }
                daysBetween == 1L -> {
                    // Consecutive day! Streak increases by 1
                    val newStreak = currentStreak + 1
                    Triple(newStreak, maxOf(longestStreak, newStreak), todayStr)
                }
                else -> {
                    // Missed more than one day, reset streak to 1
                    Triple(1, longestStreak, todayStr)
                }
            }
        } catch (_: Exception) {
            Triple(1, maxOf(longestStreak, 1), todayStr)
        }
    }

    /**
     * Dynamic Productivity Score (0-100) based on today's completed tasks,
     * focused minutes, and streak.
     */
    fun calculateProductivityScore(
        todayCompletedTasks: Int,
        todayFocusMinutes: Int,
        dailyTargetMinutes: Int,
        currentStreak: Int
    ): Int {
        val target = dailyTargetMinutes.coerceAtLeast(20)
        val timeScore = (todayFocusMinutes.toFloat() / target.toFloat()).coerceIn(0f, 1f) * 60f
        val taskScore = (todayCompletedTasks.toFloat() / 4f).coerceIn(0f, 1f) * 30f
        val streakBonus = (currentStreak.coerceAtMost(10).toFloat() / 10f) * 10f

        return (timeScore + taskScore + streakBonus).toInt().coerceIn(0, 100)
    }

    /**
     * Focus Session XP calculation:
     * 1 XP per minute completed + 15 bonus XP if completed full target.
     */
    fun calculateFocusSessionXp(completedMinutes: Int, targetMinutes: Int): Int {
        val base = completedMinutes.coerceAtLeast(0) * 1
        val bonus = if (completedMinutes >= targetMinutes && targetMinutes > 0) 15 else 0
        return base + bonus
    }

    /**
     * Focus Session Coin calculation:
     * 1 coin per 5 minutes completed + 5 bonus coins for full target completion.
     */
    fun calculateFocusSessionCoins(completedMinutes: Int, targetMinutes: Int): Int {
        val base = (completedMinutes / 5).coerceAtLeast(0)
        val bonus = if (completedMinutes >= targetMinutes && targetMinutes >= 15) 5 else 0
        return (base + bonus).coerceAtLeast(1)
    }
}
