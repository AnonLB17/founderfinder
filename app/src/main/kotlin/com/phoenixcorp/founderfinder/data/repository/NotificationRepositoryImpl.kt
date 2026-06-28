package com.phoenixcorp.founderfinder.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.phoenixcorp.founderfinder.domain.model.Notification
import com.phoenixcorp.founderfinder.domain.repository.NotificationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : NotificationRepository {

    private val TAG = "NotificationRepo"

    override fun getUnreadNotifications(userId: String): Flow<List<Notification>> =
        getNotificationsFlow(userId, readFilter = false)

    override fun getAllNotifications(userId: String): Flow<List<Notification>> =
        getNotificationsFlow(userId, readFilter = null)

    private fun getNotificationsFlow(
        userId: String,
        readFilter: Boolean? = null
    ): Flow<List<Notification>> = callbackFlow {
        Log.d(TAG, "Starting listener for user: $userId (readFilter=$readFilter)")

        var query = firestore.collection("notifications")
            .document(userId)
            .collection("userNotifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)

        if (readFilter != null) {
            query = query.whereEqualTo("read", readFilter)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error fetching notifications", error)
                trySend(emptyList())
                return@addSnapshotListener
            }

            val notifications = snapshot?.toObjects(Notification::class.java) ?: emptyList()
            Log.d(TAG, "Received ${notifications.size} notifications")
            trySend(notifications)
        }

        awaitClose { listener.remove() }
    }

    override suspend fun createNotification(
        userId: String,
        senderId: String,
        senderName: String,
        type: String,
        title: String,
        body: String,
        chatId: String?,
        forumId: String?,
        threadId: String?,
        commentId: String?,
        messageId: String?,
        category: String?
    ) {
        try {
            val finalSenderName = if (senderName.isNotBlank()) {
                senderName
            } else {
                getSenderName(senderId)
            }

            val notificationId = when {
                threadId != null -> "${type}_${forumId}_$threadId"
                else -> firestore.collection("notifications")
                    .document(userId)
                    .collection("userNotifications")
                    .document().id
            }

            val data = mapOf(
                "id" to notificationId,
                "userId" to userId,
                "recipientId" to userId,
                "senderId" to senderId,
                "senderName" to finalSenderName,
                "type" to type,
                "title" to title,
                "body" to body,
                "chatId" to chatId,
                "forumId" to forumId,
                "threadId" to threadId,
                "commentId" to commentId,
                "messageId" to messageId,
                "category" to category,
                "screen" to "ForumTemplate",
                "timestamp" to com.google.firebase.Timestamp.now(),
                "read" to false
            )

            firestore.collection("notifications")
                .document(userId)
                .collection("userNotifications")
                .document(notificationId)
                .set(data)
                .await()

            Log.d(TAG, "✅ Created notification '$type' for user $userId from $finalSenderName (ID: $notificationId)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create notification", e)
        }
    }

    private suspend fun getSenderName(senderId: String): String {
        return try {
            val doc = firestore.collection("users").document(senderId).get().await()
            doc.getString("displayName")
                ?: doc.getString("name")
                ?: doc.getString("username")
                ?: "Unknown User"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch sender name for $senderId", e)
            "Unknown User"
        }
    }

    override suspend fun deleteNotification(notificationId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        try {
            val cleanId = notificationId.substringAfterLast("/")
            firestore.collection("notifications")
                .document(currentUserId)
                .collection("userNotifications")
                .document(cleanId)
                .delete()
                .await()
            Log.d(TAG, "✅ Successfully deleted notification: $cleanId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete notification: $notificationId", e)
            throw e
        }
    }

    override suspend fun markAsRead(notificationId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        try {
            val cleanId = notificationId.substringAfterLast("/")
            firestore.collection("notifications")
                .document(currentUserId)
                .collection("userNotifications")
                .document(cleanId)
                .update("read", true)
                .await()
            Log.d(TAG, "✅ Marked as read: $cleanId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark as read: $notificationId", e)
        }
    }

    override suspend fun markAllAsRead(userId: String) {
        try {
            val batch = firestore.batch()
            val snapshot = firestore.collection("notifications")
                .document(userId)
                .collection("userNotifications")
                .whereEqualTo("read", false)
                .get()
                .await()

            for (doc in snapshot.documents) {
                batch.update(doc.reference, "read", true)
            }
            batch.commit().await()
            Log.d(TAG, "✅ Marked all as read for user: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark all as read", e)
        }
    }
}