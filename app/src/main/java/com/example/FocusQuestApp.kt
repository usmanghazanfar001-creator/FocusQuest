package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.notifications.FocusReminderWorker
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FocusQuestApp : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            NotificationHelper.createNotificationChannels(this)
            FocusReminderWorker.scheduleDailyReminder(this)
        } catch (_: Throwable) {}

        // Preload/seed database in background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getInstance(this@FocusQuestApp)
                AppDatabase.seedDatabase(database)
            } catch (_: Throwable) {}
        }
    }
}
