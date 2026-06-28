package com.phoenixcorp.founderfinder.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.phoenixcorp.founderfinder.domain.model.ChatMessage
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

            val senderDoc = firestore.collection("users").document(message.senderId).get().await()
            val senderName = senderDoc.getString("displayName")
                ?: senderDoc.getString("name")
                ?: senderDoc.getString("username")
                ?: "Unknown User"   // ← Better fallback

            Log.d(TAG, "Sending notification from $senderName to $recipientId for chat $chatId")

            // Inside sendChatNotification function, replace the createNotification call:
            notificationRepository.createNotification(
                userId = recipientId,
                senderId = message.senderId,
                senderName = senderName,
                type = "new_message",
                title = "New Message from $senderName",
                body = message.text.take(120),
                chatId = chatId,
                forumId = null,
                threadId = null,
                commentId = null,
                messageId = message.id,
                category = null
            )

            Log.d(TAG, "✅ Firestore notification created with chatId=$chatId")

            // FCM - Topic-based (most reliable from client)
            val userDoc = firestore.collection("users").document(recipientId).get().await()
            val fcmToken = userDoc.getString("fcmToken")

            if (!fcmToken.isNullOrBlank()) {
                val fcmPayload = mapOf(
                    "title" to "New Message from $senderName",
                    "body" to message.text.take(100),
                    "screen" to "PrivateChat",
                    "chatId" to chatId,
                    "senderId" to message.senderId,
                    "senderName" to senderName,
                    "type" to "chat_message"
                )

                try {
                    val topic = "user_$recipientId"
                    FirebaseMessaging.getInstance().subscribeToTopic(topic).await()

                    val remoteMessage = RemoteMessage.Builder("/topics/$topic")
                        .setData(fcmPayload)
                        .build()

                    FirebaseMessaging.getInstance().send(remoteMessage)
                    Log.d(TAG, "✅ FCM sent via topic to recipient $recipientId")
                } catch (e: Exception) {
                    Log.e(TAG, "FCM topic send failed", e)
                    // Fallback direct token
                    try {
                        val directMessage = RemoteMessage.Builder(fcmToken)
                            .setData(fcmPayload)
                            .build()
                        FirebaseMessaging.getInstance().send(directMessage)
                        Log.d(TAG, "✅ FCM sent via direct token (fallback)")
                    } catch (fallbackEx: Exception) {
                        Log.e(TAG, "Both FCM methods failed", fallbackEx)
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send chat notification", e)
            Result.failure(e)
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