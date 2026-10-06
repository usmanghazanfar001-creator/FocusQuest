package com.example.domain.model

enum class TaskPriority(val displayName: String, val xpReward: Int, val coinReward: Int) {
    LOW("Low", 10, 5),
    MEDIUM("Medium", 15, 8),
    HIGH("High", 25, 12),
    URGENT("Urgent", 35, 18);

    companion object {
        fun fromString(value: String): TaskPriority {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
        }
    }
}

enum class TaskCategory(val displayName: String, val iconName: String) {
    STUDY("Study", "School"),
    WORK("Work", "Work"),
    CODING("Coding", "Code"),
    FITNESS("Fitness", "FitnessCenter"),
    PERSONAL("Personal", "Person"),
    READING("Reading", "MenuBook"),
    OTHER("Other", "Category");

    companion object {
        fun fromString(value: String): TaskCategory {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: OTHER
        }
    }
}

enum class SessionType(val defaultMinutes: Int) {
    FOCUS(25),
    SHORT_BREAK(5),
    LONG_BREAK(15)
}

enum class ChallengeType {
    MINUTES_FOCUSED,
    TASKS_COMPLETED,
    HIGH_PRIORITY_TASKS,
    STREAK_MAINTAINED
}

enum class RewardType {
    THEME,
    AVATAR_FRAME,
    TIMER_ACCENT
}
