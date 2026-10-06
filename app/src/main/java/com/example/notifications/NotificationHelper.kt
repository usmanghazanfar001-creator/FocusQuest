package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {

    const val CHANNEL_FOCUS_TIMER = "focus_timer_channel"
    const val CHANNEL_REMINDERS = "focus_reminders_channel"
    const val NOTIFICATION_ID_TIMER = 1001
    const val NOTIFICATION_ID_REMINDER = 1002

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val timerChannel = NotificationChannel(
                CHANNEL_FOCUS_TIMER,
                "Focus Timer Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a focus or break session finishes"
                enableVibration(true)
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Daily Quest Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily productivity reminders and streak protection alerts"
            }

            notificationManager.createNotificationChannels(listOf(timerChannel, reminderChannel))
        }
    }

    fun showTimerCompletedNotification(context: Context, sessionType: String, minutes: Int, xpEarned: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (sessionType.contains("BREAK", ignoreCase = true)) {
            "Break Complete! ⚡"
        } else {
            "Focus Session Complete! 🏆"
        }

        val message = if (sessionType.contains("BREAK", ignoreCase = true)) {
            "Ready to conquer your next quest?"
        } else {
            "Awesome job! Completed $minutes min session and earned +$xpEarned XP!"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_FOCUS_TIMER)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TIMER, notification)
        } catch (_: SecurityException) {
            // Notification permission might not be granted yet
        }
    }

    fun showStreakReminderNotification(context: Context, currentStreak: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Keep Your Flame Burning! 🔥")
            .setContentText("You are on a $currentStreak-day streak. Log a quick focus session today to protect it!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_REMINDER, notification)
        } catch (_: SecurityException) {
            // Handled safely
        }
    }
}
