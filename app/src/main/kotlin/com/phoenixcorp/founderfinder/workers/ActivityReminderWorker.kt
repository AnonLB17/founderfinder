package com.phoenixcorp.founderfinder.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.MainActivity
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.utils.TimeZoneUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ActivityReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    private val firestore = FirebaseFirestore.getInstance()

    override fun doWork(): Result {
        val reminderId = inputData.getString("reminderId") ?: return Result.failure()
        val activityId = inputData.getString("activityId") ?: return Result.failure()
        val title = inputData.getString("title") ?: "Activity Reminder"
        val baseMessage = inputData.getString("message") ?: "Your activity is starting soon!"
        val eventUtcMillis = inputData.getLong("eventTime", 0L)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                firestore.collection("activityReminders")
                    .document(reminderId)
                    .update("triggered", true)
                    .await()

                val timeUntil = TimeZoneUtils.getTimeUntilEvent(eventUtcMillis)
                val fullMessage = "$baseMessage ($timeUntil)"

                // Create in-app notification
                createInAppNotification(activityId, title, fullMessage, eventUtcMillis)

                // Send push notification
                sendPushNotification(title, fullMessage, activityId, eventUtcMillis)

            } catch (e: Exception) {
                android.util.Log.e("ActivityReminderWorker", "Error processing reminder", e)
            }
        }

        return Result.success()
    }

    private suspend fun createInAppNotification(
        activityId: String,
        title: String,
        message: String,
        eventUtcMillis: Long
    ) {
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return

        val notificationData = mapOf(
            "id" to "reminder_$activityId",
            "userId" to currentUserId,
            "recipientId" to currentUserId,
            "senderId" to "system",
            "senderName" to "System",
            "type" to "activity_reminder",
            "title" to title,
            "body" to message,
            "activityId" to activityId,
            "eventTime" to eventUtcMillis,
            "screen" to "CalendarScreen",
            "timestamp" to com.google.firebase.Timestamp.now(),
            "read" to false
        )

        try {
            firestore.collection("notifications")
                .document(currentUserId)
                .collection("userNotifications")
                .document("reminder_$activityId")
                .set(notificationData)
                .await()

            android.util.Log.d("ActivityReminderWorker", "✅ In-app reminder notification created")
        } catch (e: Exception) {
            android.util.Log.e("ActivityReminderWorker", "Failed to create in-app notification", e)
        }
    }

    private fun sendPushNotification(
        title: String,
        message: String,
        activityId: String,
        eventUtcMillis: Long
    ) {
        val channelId = "activity_reminders"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Activity Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Reminders for scheduled activities"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("screen", "CalendarScreen")
            putExtra("activityId", activityId)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            activityId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val localTime = TimeZoneUtils.formatLocalTime(eventUtcMillis, "hh:mm a")

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ff_logo)
            .setContentTitle(title)
            .setContentText(message)                    // Now includes "in X minutes"
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(activityId.hashCode(), notification)

        android.util.Log.d("ActivityReminderWorker", "✅ Push notification sent: $title")
    }
}