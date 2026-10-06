package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val priority: String = "MEDIUM", // TaskPriority enum name
    val category: String = "WORK",   // TaskCategory enum name
    val estimatedMinutes: Int = 25,
    val dueDateMillis: Long? = null,
    val isCompleted: Boolean = false,
    val completedAtMillis: Long? = null,
    val xpAwarded: Boolean = false,
    val coinsAwarded: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)
