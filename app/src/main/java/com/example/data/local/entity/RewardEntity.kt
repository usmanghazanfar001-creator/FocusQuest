package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rewards")
data class RewardEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val type: String, // RewardType enum name (THEME, AVATAR_FRAME, TIMER_ACCENT)
    val costCoins: Int,
    val isPurchased: Boolean = false,
    val isEquipped: Boolean = false,
    val previewColorHex: String = "#3B82F6",
    val iconName: String = "Palette"
)
