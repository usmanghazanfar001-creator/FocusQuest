package com.example.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class FocusReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getInstance(applicationContext)
            val user = database.userDao().getUser()
            if (user != null && user.notificationsEnabled) {
                val todayStr = LocalDate.now().toString()
                if (user.lastActiveDate != todayStr) {
                    NotificationHelper.showStreakReminderNotification(
                        applicationContext,
                        user.currentStreak
                    )
                }
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "focus_quest_daily_reminder"

        fun scheduleDailyReminder(context: Context) {
            try {
                val reminderRequest = PeriodicWorkRequestBuilder<FocusReminderWorker>(
                    24, TimeUnit.HOURS,
                    6, TimeUnit.HOURS // flex interval
                ).build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    reminderRequest
                )
            } catch (_: Throwable) {
                // Safely ignore in environments without WorkManager initialization (e.g. tests)
            }
        }
    }
}
