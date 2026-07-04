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
import com.phoenixcorp.founderfinder.MainActivity
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.utils.TimeZoneUtils

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val title = inputData.getString("title") ?: "Activity Reminder"
        val message = inputData.getString("message") ?: "Your activity is starting soon!"
        val activityId = inputData.getString("activityId")
        val isOrg = inputData.getBoolean("isOrganizationActivity", false)
        val orgId = inputData.getString("organizationId")
        val eventUtcMillis = inputData.getLong("eventTime", 0L)

        showNotification(title, message, activityId, isOrg, orgId, eventUtcMillis)
        return Result.success()
    }

    private fun showNotification(
        title: String,
        message: String,
        activityId: String?,
        isOrg: Boolean,
        orgId: String?,
        eventUtcMillis: Long
    ) {
        val channelId = "activity_reminders"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Activity Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for scheduled activities and events"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("screen", "CalendarScreen")
            putExtra("type", "calendar")
            activityId?.let { putExtra("activityId", it) }
            putExtra("isOrganization", isOrg)
            orgId?.let { putExtra("organizationId", it) }
        }

        val requestCode = activityId?.hashCode() ?: System.currentTimeMillis().toInt()

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val localTime = TimeZoneUtils.formatLocalTime(eventUtcMillis, "hh:mm a")

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ff_logo)
            .setContentTitle(title)
            .setContentText("$message • $localTime")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(requestCode, notification)

        android.util.Log.d("ReminderWorker", "✅ Reminder sent: $title | activityId=$activityId | localTime=$localTime")
    }
}