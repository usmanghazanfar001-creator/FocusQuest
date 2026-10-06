package com.example

import com.example.domain.model.GamificationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GamificationEngineTest {

    @Test
    fun testLevelFormulaExactMatch() {
        // The user's specification:
        // Level 1: 0 XP
        // Level 2: 100 XP
        // Level 3: 250 XP
        // Level 4: 450 XP
        // Level 5: 700 XP
        assertEquals(0, GamificationEngine.totalXpRequiredForLevel(1))
        assertEquals(100, GamificationEngine.totalXpRequiredForLevel(2))
        assertEquals(250, GamificationEngine.totalXpRequiredForLevel(3))
        assertEquals(450, GamificationEngine.totalXpRequiredForLevel(4))
        assertEquals(700, GamificationEngine.totalXpRequiredForLevel(5))
        assertEquals(1000, GamificationEngine.totalXpRequiredForLevel(6))
    }

    @Test
    fun testLevelCalculationFromXp() {
        val level1 = GamificationEngine.calculateLevelInfo(0)
        assertEquals(1, level1.level)
        assertEquals(0, level1.currentLevelBaseXp)
        assertEquals(100, level1.nextLevelBaseXp)
        assertEquals(0f, level1.progressFraction, 0.001f)

        val level2Partial = GamificationEngine.calculateLevelInfo(175)
        assertEquals(2, level2Partial.level)
        assertEquals(100, level2Partial.currentLevelBaseXp)
        assertEquals(250, level2Partial.nextLevelBaseXp)
        assertEquals(75, level2Partial.xpInCurrentLevel)
        assertEquals(0.5f, level2Partial.progressFraction, 0.001f)

        val level5Exact = GamificationEngine.calculateLevelInfo(700)
        assertEquals(5, level5Exact.level)
        assertEquals(0, level5Exact.xpInCurrentLevel)
    }

    @Test
    fun testStreakConsecutiveDay() {
        val today = LocalDate.of(2026, 10, 6)
        val yesterday = "2026-10-05"

        val (newStreak, longestStreak, todayStr) = GamificationEngine.updateStreak(
            currentStreak = 4,
            longestStreak = 5,
            lastActiveDateStr = yesterday,
            today = today
        )

        assertEquals(5, newStreak)
        assertEquals(5, longestStreak)
        assertEquals("2026-10-06", todayStr)
    }

    @Test
    fun testStreakSameDayNoDuplicateIncrement() {
        val today = LocalDate.of(2026, 10, 6)
        val todayStr = "2026-10-06"

        val (newStreak, longestStreak, _) = GamificationEngine.updateStreak(
            currentStreak = 3,
            longestStreak = 3,
            lastActiveDateStr = todayStr,
            today = today
        )

        assertEquals(3, newStreak)
        assertEquals(3, longestStreak)
    }

    @Test
    fun testStreakMissedDayReset() {
        val today = LocalDate.of(2026, 10, 6)
        val twoDaysAgo = "2026-10-04"

        val (newStreak, longestStreak, _) = GamificationEngine.updateStreak(
            currentStreak = 10,
            longestStreak = 10,
            lastActiveDateStr = twoDaysAgo,
            today = today
        )

        assertEquals(1, newStreak)
        assertEquals(10, longestStreak)
    }

    @Test
    fun testProductivityScore() {
        val scoreLow = GamificationEngine.calculateProductivityScore(
            todayCompletedTasks = 0,
            todayFocusMinutes = 0,
            dailyTargetMinutes = 60,
            currentStreak = 1
        )
        assertTrue(scoreLow in 0..10)

        val scoreHigh = GamificationEngine.calculateProductivityScore(
            todayCompletedTasks = 4,
            todayFocusMinutes = 60,
            dailyTargetMinutes = 60,
            currentStreak = 10
        )
        assertEquals(100, scoreHigh)
    }

    @Test
    fun testFocusSessionRewards() {
        val full25Min = GamificationEngine.calculateFocusSessionXp(25, 25)
        // 25 mins * 1 + 15 bonus = 40 XP
        assertEquals(40, full25Min)

        val coins25Min = GamificationEngine.calculateFocusSessionCoins(25, 25)
        // 25 / 5 = 5 + 5 bonus = 10 coins
        assertEquals(10, coins25Min)
    }
}
