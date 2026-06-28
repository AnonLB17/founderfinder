package com.phoenixcorp.founderfinder.data.remote.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.phoenixcorp.founderfinder.MainActivity
import com.phoenixcorp.founderfinder.R

class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val TAG = "FCM"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "📩 FCM Received. Full Data: ${remoteMessage.data}")

        val data = remoteMessage.data

        val senderName = data["senderName"] ?: remoteMessage.notification?.title ?: "Unknown"
        val title = data["title"] ?: "New Activity from $senderName"
        val body = data["body"] ?: remoteMessage.notification?.body ?: "New update"

        val screen = data["screen"] ?: data["type"] ?: ""
        val chatId = data["chatId"]
        val forumId = data["forumId"]
        val category = data["category"]
        val threadId = data["threadId"]

        Log.d(TAG, "Processing: screen=$screen, forumId=$forumId, category=$category")

        sendNotification(title, body, screen, chatId, forumId, category, threadId)
    }

    private fun sendNotification(
        title: String,
        body: String,
        screen: String,
        chatId: String?,
        forumId: String?,
        category: String?,
        threadId: String?
    ) {
        val channelId = "founderfinder_notifications"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "FounderFinder Notifications", NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("screen", screen)
            chatId?.let { putExtra("chatId", it) }
            forumId?.let { putExtra("forumId", it) }
            category?.let { putExtra("category", it) }
            threadId?.let { putExtra("threadId", it) }
        }

        val requestCode = (chatId ?: forumId ?: threadId ?: "default").hashCode()
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ff_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(requestCode, notification)
        Log.d(TAG, "🔔 Notification displayed: $title")
    }
}