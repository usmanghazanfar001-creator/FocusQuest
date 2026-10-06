package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val durationMinutes: Int,
    val targetMinutes: Int,
    val sessionType: String = "FOCUS", // SessionType enum name
    val completedAtMillis: Long = System.currentTimeMillis(),
    val xpEarned: Int = 0,
    val coinsEarned: Int = 0,
    val linkedTaskId: Long? = null,
    val linkedTaskTitle: String? = null
)
