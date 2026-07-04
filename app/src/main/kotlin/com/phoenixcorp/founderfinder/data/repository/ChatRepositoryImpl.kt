package com.phoenixcorp.founderfinder.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.phoenixcorp.founderfinder.domain.model.ChatMessage
import com.phoenixcorp.founderfinder.domain.model.UserProfile
import com.phoenixcorp.founderfinder.domain.repository.ChatRepository
import com.phoenixcorp.founderfinder.domain.repository.NotificationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val notificationRepository: NotificationRepository
) : ChatRepository {

    private val chatsCollection = firestore.collection("conversations")
    private val TAG = "ChatRepository"

    override suspend fun sendMessage(message: ChatMessage): Result<Unit> {
        return try {
            val chatDoc = chatsCollection.document(message.chatId)
            chatDoc.collection("messages").document(message.id).set(message).await()

            Log.d(TAG, "✅ Message saved to Firestore. Triggering notification...")

            // Trigger notification
            sendChatNotification(message.chatId, message)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message", e)
            Result.failure(e)
        }
    }

    override suspend fun sendChatNotification(
        chatId: String,
        message: ChatMessage
    ): Result<Unit> {
        return try {
            Log.d(TAG, "=== sendChatNotification STARTED for chatId=$chatId ===")

            val chatDoc = chatsCollection.document(chatId).get().await()
            val participants = chatDoc.get("participants") as? List<String>
                ?: return Result.failure(Exception("No participants found"))

            val recipientId = participants.firstOrNull { it != message.senderId }
                ?: return Result.failure(Exception("No recipient found"))

            val senderName = getSenderName(message.senderId)

            Log.d(TAG, "Sending notification from $senderName to $recipientId")

            // In-app notification
            try {
                // Inside sendChatNotification, update the createNotification call:
                notificationRepository.createNotification(
                    userId = recipientId,
                    senderId = message.senderId,
                    senderName = senderName,
                    type = "new_message",
                    title = "New Message from $senderName",
                    body = (message.text ?: "").take(120),
                    chatId = chatId,
                    messageId = message.id,           // ← Key change
                    screen = "PrivateChat"
                )
                Log.d(TAG, "✅ In-app notification created")
            } catch (e: Exception) {
                Log.e(TAG, "In-app notification failed", e)
            }

            // Push notification (FCM)
            sendFcmPushNotification(recipientId, senderName, message.text ?: "")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send chat notification", e)
            Result.failure(e)
        }
    }

    private suspend fun getSenderName(userId: String): String {
        return try {
            val doc = firestore.collection("profiles").document(userId).get().await()
            val profile = doc.toObject(UserProfile::class.java)
            val fullName = "${profile?.firstName ?: ""} ${profile?.lastName ?: ""}".trim()
            fullName.ifBlank { "Unknown User" }
        } catch (e: Exception) {
            "Unknown User"
        }
    }

    private suspend fun sendFcmPushNotification(
        recipientId: String,
        senderName: String,
        messageText: String
    ) {
        try {
            Log.d(TAG, "Attempting FCM push to recipient: $recipientId")

            val userDoc = firestore.collection("users").document(recipientId).get().await()
            val fcmToken = userDoc.getString("fcmToken")

            if (fcmToken.isNullOrBlank()) {
                Log.w(TAG, "❌ No FCM token found for recipient $recipientId")
                return
            }

            Log.d(TAG, "✅ Found FCM token for $recipientId")

            val fcmPayload = mapOf(
                "title" to "New Message from $senderName",
                "body" to messageText.take(100),
                "screen" to "PrivateChat",
                "chatId" to "",
                "senderId" to "",
                "senderName" to senderName,
                "type" to "chat_message"
            )

            val remoteMessage = RemoteMessage.Builder(fcmToken)
                .setData(fcmPayload)
                .build()

            FirebaseMessaging.getInstance().send(remoteMessage)
            Log.d(TAG, "✅ FCM Push successfully sent to token for recipient $recipientId")

        } catch (e: Exception) {
            Log.e(TAG, "❌ FCM push failed for recipient $recipientId", e)
        }
    }

    override fun getChatMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = chatsCollection.document(chatId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.toObjects(ChatMessage::class.java) ?: emptyList()
                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun markMessageAsRead(messageId: String, chatId: String) {
        try {
            chatsCollection.document(chatId)
                .collection("messages")
                .document(messageId)
                .update("isRead", true)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark message as read", e)
        }
    }

    override suspend fun createOrGetChat(userId1: String, userId2: String): String {
        val chatId = if (userId1 < userId2) "${userId1}_$userId2" else "${userId2}_$userId1"
        val chatRef = chatsCollection.document(chatId)

        if (!chatRef.get().await().exists()) {
            chatRef.set(
                mapOf(
                    "participants" to listOf(userId1, userId2),
                    "createdAt" to System.currentTimeMillis()
                )
            ).await()
        }
        return chatId
    }
}