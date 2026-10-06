package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AchievementDao
import com.example.data.local.dao.DailyChallengeDao
import com.example.data.local.dao.FocusSessionDao
import com.example.data.local.dao.RewardDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.AchievementEntity
import com.example.data.local.entity.DailyChallengeEntity
import com.example.data.local.entity.FocusSessionEntity
import com.example.data.local.entity.RewardEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

@Database(
    entities = [
        UserEntity::class,
        TaskEntity::class,
        FocusSessionEntity::class,
        AchievementEntity::class,
        DailyChallengeEntity::class,
        RewardEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun taskDao(): TaskDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun achievementDao(): AchievementDao
    abstract fun dailyChallengeDao(): DailyChallengeDao
    abstract fun rewardDao(): RewardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "focusquest_database.db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedDatabase(database)
                    }
                }
            }
        }

        suspend fun seedDatabase(database: AppDatabase) {
            // Seed Default User if not present
            val existingUser = database.userDao().getUser()
            if (existingUser == null) {
                database.userDao().insertOrUpdate(
                    UserEntity(
                        id = 1,
                        name = "Focus Hero",
                        mainGoal = "Master Productivity & Focus",
                        dailyFocusGoalMinutes = 60,
                        totalXp = 0,
                        level = 1,
                        coins = 50,
                        currentStreak = 1,
                        longestStreak = 1,
                        lastActiveDate = LocalDate.now().toString()
                    )
                )
            }

            // Seed Initial Achievements
            val defaultAchievements = listOf(
                AchievementEntity(
                    id = "first_focus",
                    title = "First Focus",
                    description = "Complete your first focus session.",
                    category = "FOCUS",
                    targetValue = 1,
                    xpReward = 50,
                    coinReward = 20,
                    iconName = "Timer"
                ),
                AchievementEntity(
                    id = "early_bird",
                    title = "Early Bird",
                    description = "Complete a focus session before 9:00 AM.",
                    category = "SPECIAL",
                    targetValue = 1,
                    xpReward = 75,
                    coinReward = 30,
                    iconName = "WbSunny"
                ),
                AchievementEntity(
                    id = "streak_3",
                    title = "Focus Spark",
                    description = "Maintain a 3-day focus streak.",
                    category = "STREAK",
                    targetValue = 3,
                    xpReward = 100,
                    coinReward = 40,
                    iconName = "LocalFireDepartment"
                ),
                AchievementEntity(
                    id = "streak_7",
                    title = "7-Day Streak",
                    description = "Maintain a 7-day focus streak.",
                    category = "STREAK",
                    targetValue = 7,
                    xpReward = 250,
                    coinReward = 80,
                    iconName = "LocalFireDepartment"
                ),
                AchievementEntity(
                    id = "focus_master_10",
                    title = "Flow Seeker",
                    description = "Complete 10 focus sessions.",
                    category = "FOCUS",
                    targetValue = 10,
                    xpReward = 200,
                    coinReward = 60,
                    iconName = "HourglassFull"
                ),
                AchievementEntity(
                    id = "focus_master_50",
                    title = "Focus Master",
                    description = "Complete 50 focus sessions.",
                    category = "FOCUS",
                    targetValue = 50,
                    xpReward = 500,
                    coinReward = 200,
                    iconName = "MilitaryTech"
                ),
                AchievementEntity(
                    id = "task_slayer_10",
                    title = "Task Slayer",
                    description = "Complete 10 tasks.",
                    category = "TASK",
                    targetValue = 10,
                    xpReward = 150,
                    coinReward = 50,
                    iconName = "CheckCircle"
                ),
                AchievementEntity(
                    id = "productivity_beast_100",
                    title = "Productivity Beast",
                    description = "Complete 100 tasks.",
                    category = "TASK",
                    targetValue = 100,
                    xpReward = 600,
                    coinReward = 250,
                    iconName = "WorkspacePremium"
                ),
                AchievementEntity(
                    id = "deep_hours_5",
                    title = "Deep Hours",
                    description = "Accumulate 300 minutes (5 hours) of focused work.",
                    category = "FOCUS",
                    targetValue = 300,
                    xpReward = 300,
                    coinReward = 100,
                    iconName = "Psychology"
                ),
                AchievementEntity(
                    id = "coin_collector",
                    title = "Treasure Hunter",
                    description = "Earn 300 coins in total.",
                    category = "SPECIAL",
                    targetValue = 300,
                    xpReward = 200,
                    coinReward = 50,
                    iconName = "MonetizationOn"
                )
            )
            database.achievementDao().insertAll(defaultAchievements)

            // Seed Initial Cosmetic Rewards
            val defaultRewards = listOf(
                RewardEntity(
                    id = "theme_deep_slate",
                    title = "Deep Slate",
                    description = "Default modern dark aesthetic with neon cyan highlights.",
                    type = "THEME",
                    costCoins = 0,
                    isPurchased = true,
                    isEquipped = true,
                    previewColorHex = "#06B6D4",
                    iconName = "Palette"
                ),
                RewardEntity(
                    id = "theme_cyber_neon",
                    title = "Cyber Neon",
                    description = "High-energy cyberpunk violet and electric magenta palette.",
                    type = "THEME",
                    costCoins = 100,
                    isPurchased = false,
                    isEquipped = false,
                    previewColorHex = "#A855F7",
                    iconName = "FlashOn"
                ),
                RewardEntity(
                    id = "theme_emerald_flow",
                    title = "Emerald Flow",
                    description = "Calming forest green and jade tones for deep clarity.",
                    type = "THEME",
                    costCoins = 150,
                    isPurchased = false,
                    isEquipped = false,
                    previewColorHex = "#10B981",
                    iconName = "Spa"
                ),
                RewardEntity(
                    id = "theme_solar_gold",
                    title = "Solar Gold",
                    description = "Warm energetic amber and golden radiance for champions.",
                    type = "THEME",
                    costCoins = 200,
                    isPurchased = false,
                    isEquipped = false,
                    previewColorHex = "#F59E0B",
                    iconName = "Brightness5"
                ),
                RewardEntity(
                    id = "frame_default",
                    title = "Standard Crest",
                    description = "Clean minimalist avatar border.",
                    type = "AVATAR_FRAME",
                    costCoins = 0,
                    isPurchased = true,
                    isEquipped = true,
                    previewColorHex = "#94A3B8",
                    iconName = "AccountCircle"
                ),
                RewardEntity(
                    id = "frame_bronze_ring",
                    title = "Bronze Ring",
                    description = "Polished bronze frame signifying perseverance.",
                    type = "AVATAR_FRAME",
                    costCoins = 60,
                    isPurchased = false,
                    isEquipped = false,
                    previewColorHex = "#CD7F32",
                    iconName = "Shield"
                ),
                RewardEntity(
                    id = "frame_golden_laurels",
                    title = "Golden Laurels",
                    description = "Woven golden laurels for seasoned focus champions.",
                    type = "AVATAR_FRAME",
                    costCoins = 140,
                    isPurchased = false,
                    isEquipped = false,
                    previewColorHex = "#FBBF24",
                    iconName = "MilitaryTech"
                ),
                RewardEntity(
                    id = "frame_flame_aura",
                    title = "Flame Aura",
                    description = "Blazing flame aura showcasing unbreakable streaks.",
                    type = "AVATAR_FRAME",
                    costCoins = 220,
                    isPurchased = false,
                    isEquipped = false,
                    previewColorHex = "#EF4444",
                    iconName = "Whatshot"
                ),
                RewardEntity(
                    id = "timer_chime_bell",
                    title = "Zen Bell Accent",
                    description = "Peaceful Tibetan singing bowl tone at end of session.",
                    type = "TIMER_ACCENT",
                    costCoins = 80,
                    isPurchased = false,
                    isEquipped = false,
                    previewColorHex = "#38BDF8",
                    iconName = "NotificationsActive"
                )
            )
            database.rewardDao().insertAll(defaultRewards)

            // Seed Initial Tasks if empty
            val taskCount = database.taskDao().getTaskById(1)
            if (taskCount == null) {
                database.taskDao().insertTask(
                    TaskEntity(
                        title = "Complete your first 25-minute Focus Session",
                        description = "Tap 'Start Focus Session' from dashboard to enter flow state",
                        priority = "HIGH",
                        category = "STUDY",
                        estimatedMinutes = 25
                    )
                )
                database.taskDao().insertTask(
                    TaskEntity(
                        title = "Review your daily goals and priorities",
                        description = "Organize tasks for today to maximize productivity XP",
                        priority = "MEDIUM",
                        category = "WORK",
                        estimatedMinutes = 15
                    )
                )
                database.taskDao().insertTask(
                    TaskEntity(
                        title = "Explore the Quest Rewards & unlock a theme",
                        description = "Earn coins by completing sessions to customize your app",
                        priority = "LOW",
                        category = "PERSONAL",
                        estimatedMinutes = 5
                    )
                )
            }
        }
    }
}
