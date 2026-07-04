package com.phoenixcorp.founderfinder.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.phoenixcorp.founderfinder.domain.model.Notification
import com.phoenixcorp.founderfinder.domain.model.UserProfile
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
        screen: String?,
        forumId: String?,
        threadId: String?,
        commentId: String?,
        messageId: String?,
        category: String?,
        activityId: String?,
        eventTime: Long?,
        activityType: String?,
        organizationId: String?,
        organizationName: String?
    ) {
        try {
            Log.d(TAG, "Creating notification for senderId: $senderId | type: $type | messageId: $messageId")

            val senderProfile = getSenderProfile(senderId)
            val fullName = senderProfile.getFullName()

            val finalSenderName = when {
                senderName.isNotBlank() -> senderName
                fullName.isNotBlank() && fullName != "Unknown User" -> fullName
                else -> "Unknown User"
            }

            // GUARANTEED UNIQUE ID
            val notificationId = when {
                messageId != null -> "chat_msg_$messageId"
                threadId != null && forumId != null -> "${type}_${forumId}_$threadId"
                activityId != null -> "activity_$activityId"
                chatId != null -> "chat_${chatId}_${System.currentTimeMillis()}"
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
                "senderFirstName" to (senderProfile.firstName ?: ""),
                "senderLastName" to (senderProfile.lastName ?: ""),
                "type" to type,
                "title" to title,
                "body" to body,
                "chatId" to chatId,
                "forumId" to forumId,
                "threadId" to threadId,
                "commentId" to commentId,
                "messageId" to messageId,
                "activityId" to activityId,
                "eventTime" to eventTime,
                "activityType" to activityType,
                "organizationId" to organizationId,
                "organizationName" to organizationName,
                "screen" to (screen ?: when {
                    activityId != null -> "CalendarScreen"
                    chatId != null -> "PrivateChat"
                    threadId != null -> "ThreadScreen"
                    else -> "ForumTemplate"
                }),
                "timestamp" to com.google.firebase.Timestamp.now(),
                "read" to false
            )

            firestore.collection("notifications")
                .document(userId)
                .collection("userNotifications")
                .document(notificationId)
                .set(data, SetOptions.merge())
                .await()

            Log.d(TAG, "✅ Created $type notification for $userId with ID: $notificationId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create notification", e)
        }
    }

    // ... (keep the rest of your file unchanged - getSenderProfile, markAsRead, etc.)
    private suspend fun getSenderProfile(senderId: String): UserProfile {
        return try {
            Log.d(TAG, "Fetching profile for sender: $senderId")

            val profileDoc = firestore.collection("profiles")
                .document(senderId)
                .get()
                .await()

            if (profileDoc.exists()) {
                val profile = profileDoc.toObject(UserProfile::class.java)
                if (profile != null) {
                    Log.d(TAG, "Found in 'profiles' - First: ${profile.firstName}, Last: ${profile.lastName}")
                    return profile
                }
            }

            val userDoc = firestore.collection("users")
                .document(senderId)
                .get()
                .await()

            if (userDoc.exists()) {
                val firstName = userDoc.getString("firstName")
                    ?: userDoc.getString("displayName")?.split(" ")?.firstOrNull()
                    ?: ""

                val lastName = userDoc.getString("lastName")
                    ?: userDoc.getString("displayName")?.split(" ")?.drop(1)?.joinToString(" ")
                    ?: ""

                Log.d(TAG, "Found in 'users' - First: $firstName, Last: $lastName")

                return UserProfile(
                    userId = senderId,
                    firstName = firstName,
                    lastName = lastName
                )
            }

            Log.w(TAG, "No profile found for $senderId")
            UserProfile(userId = senderId)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching sender profile", e)
            UserProfile(userId = senderId)
        }
    }

    private fun UserProfile.getFullName(): String {
        return when {
            !firstName.isNullOrBlank() && !lastName.isNullOrBlank() -> "$firstName $lastName".trim()
            !firstName.isNullOrBlank() -> firstName!!.trim()
            else -> "Unknown User"
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
            Log.d(TAG, "✅ Deleted notification: $cleanId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete notification", e)
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
            Log.e(TAG, "Failed to mark as read", e)
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
            Log.d(TAG, "✅ Marked all notifications as read for user: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark all as read", e)
        }
    }
}