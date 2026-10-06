package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_challenges")
data class DailyChallengeEntity(
    @PrimaryKey val id: String, // e.g. "2026-10-06_FOCUS_45"
    val date: String,          // "yyyy-MM-dd"
    val title: String,
    val description: String,
    val challengeType: String, // ChallengeType enum name
    val targetValue: Int,
    val currentProgress: Int = 0,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false,
    val xpReward: Int = 50,
    val coinReward: Int = 20
)
