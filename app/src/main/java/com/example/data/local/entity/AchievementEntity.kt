package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String, // "FOCUS", "TASK", "STREAK", "SPECIAL"
    val targetValue: Int,
    val currentProgress: Int = 0,
    val isUnlocked: Boolean = false,
    val unlockedAtMillis: Long? = null,
    val xpReward: Int = 50,
    val coinReward: Int = 25,
    val iconName: String = "EmojiEvents"
)
