package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "Quest Hunter",
    val mainGoal: String = "Improve Focus & Productivity",
    val dailyFocusGoalMinutes: Int = 60,
    val totalXp: Int = 0,
    val level: Int = 1,
    val coins: Int = 50,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveDate: String = "",
    val equippedTheme: String = "DEEP_SLATE",
    val equippedAvatarFrame: String = "DEFAULT",
    val notificationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val defaultFocusDuration: Int = 25,
    val defaultBreakDuration: Int = 5,
    val isProUser: Boolean = false
)
