package com.phoenixcorp.founderfinder.data.remote.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.phoenixcorp.founderfinder.MainActivity
import com.phoenixcorp.founderfinder.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val TAG = "FCM"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New token: $token")
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Log.w(TAG, "No user logged in, cannot save token")
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .set(
                        mapOf(
                            "fcmToken" to token,
                            "updatedAt" to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    )
                    .await()
                Log.d(TAG, "Token saved for user: $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save token for user $userId: ${e.message}", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received: ${message.data}")
        Log.d(TAG, "Notification payload: ${message.notification?.toString()}")
        Log.d(TAG, "From: ${message.from}, MessageId: ${message.messageId}, Priority: ${message.priority}")

        message.notification?.let {
            Log.d(TAG, "Notification: title=${it.title}, body=${it.body}, data=${message.data}")
            sendNotification(
                it.title ?: "FounderFinder",
                it.body ?: "New event received",
                message.data["screen"],
                message.data["id"],
                message.data["senderId"],
                message.data["notificationId"],
                message.data["content"],
                message.data["recipientId"],
                message.data["type"]
            )
        } ?: run {
            Log.w(TAG, "No notification payload, processing data-only message")
            sendNotification(
                message.data["title"] ?: "FounderFinder",
                message.data["body"] ?: "New event received",
                message.data["screen"],
                message.data["id"],
                message.data["senderId"],
                message.data["notificationId"],
                message.data["content"],
                message.data["recipientId"],
                message.data["type"]
            )
        }
    }

    private fun sendNotification(
        title: String,
        body: String,
        screen: String?,
        id: String?,
        senderId: String?,
        notificationId: String?,
        content: String?,
        recipientId: String?,
        type: String?
    ) {
        val channelId = "founderfinder_notifications"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "FounderFinder Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("screen", screen)
            putExtra("id", id)
            putExtra("senderId", senderId)
            putExtra("notificationId", notificationId)
            putExtra("content", content)
            putExtra("recipientId", recipientId)
            putExtra("type", type)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(
                this,
                notificationId?.hashCode() ?: System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getActivity(
                this,
                notificationId?.hashCode() ?: System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ff_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        Log.d(TAG, "Sending notification: title=$title, body=$body, screen=$screen, id=$id, senderId=$senderId, type=$type, recipientId=$recipientId")
        try {
            notificationManager.notify(notificationId?.hashCode() ?: System.currentTimeMillis().toInt(), notification)
            Log.d(TAG, "Notification sent successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification: ${e.message}", e)
        }
    }
}