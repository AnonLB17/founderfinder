package com.phoenixcorp.founderfinder.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.phoenixcorp.founderfinder.domain.model.ChatMessage
import com.phoenixcorp.founderfinder.domain.model.Conversation
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

            chatDoc.set(
                mapOf(
                    "lastMessage" to (message.text ?: message.imageUrl?.let { "📷 Image" } ?: ""),
                    "lastMessageAt" to System.currentTimeMillis(),
                    "lastMessageSenderId" to message.senderId
                ),
                SetOptions.merge()
            ).await()

            Log.d(TAG, "✅ Message sent to ${message.chatId}")

            sendChatNotification(message.chatId, message)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message", e)
            Result.failure(e)
        }
    }

    override suspend fun sendChatNotification(chatId: String, message: ChatMessage): Result<Unit> {
        return try {
            val chatDoc = chatsCollection.document(chatId).get().await()
            val participants = chatDoc.get("participants") as? List<String>
                ?: return Result.failure(Exception("No participants found"))

            val recipientId = participants.firstOrNull { it != message.senderId }
                ?: return Result.failure(Exception("No recipient found"))

            val senderName = getSenderName(message.senderId)

            notificationRepository.createNotification(
                userId = recipientId,
                senderId = message.senderId,
                senderName = senderName,
                type = "new_message",
                title = "New Message from $senderName",
                body = (message.text ?: "").take(120),
                chatId = chatId,
                messageId = message.id,
                screen = "PrivateChat"
            )

            sendFcmPushNotification(recipientId, senderName, message.text ?: "")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification", e)
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
        Log.w(TAG, "FCM push called - implement Cloud Function for reliable delivery")
    }

    override fun getChatMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = chatsCollection.document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)   // ASCENDING is correct
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.toObjects(ChatMessage::class.java) ?: emptyList()
                trySend(messages)   // No need to reverse here
            }
        awaitClose { listener.remove() }
    }

    override fun getUserConversations(userId: String): Flow<List<Conversation>> = callbackFlow {
        val listener = chatsCollection
            .whereArrayContains("participantIds", userId)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error loading conversations", error)
                    close(error)
                    return@addSnapshotListener
                }

                val conversations = mutableListOf<Conversation>()

                snapshot?.documents?.forEach { doc ->
                    val data = doc.data ?: return@forEach
                    val participantIds = data["participantIds"] as? List<String> ?: emptyList()
                    val otherUserId = participantIds.firstOrNull { it != userId } ?: return@forEach

                    // Fetch profile for other user
                    firestore.collection("profiles")
                        .document(otherUserId)
                        .get()
                        .addOnSuccessListener { profileDoc ->
                            val profile = profileDoc.toObject(UserProfile::class.java)

                            val conversation = Conversation(
                                conversationId = doc.id,
                                participants = participantIds,
                                otherUserId = otherUserId,
                                otherUserName = "${profile?.firstName ?: ""} ${profile?.lastName ?: ""}".trim().ifBlank { "Unknown User" },
                                otherUserProfilePicture = profile?.profilePicture,
                                lastMessage = data["lastMessage"] as? String,
                                lastMessageAt = data["lastMessageAt"] as? Long,
                                lastMessageSenderId = data["lastMessageSenderId"] as? String,
                                createdAt = (data["createdAt"] as? Long) ?: 0L
                            )

                            conversations.add(conversation)
                            trySend(conversations.toList())
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to load profile for $otherUserId", e)
                        }
                }
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
                    "participantIds" to listOf(userId1, userId2),   // Added for consistency
                    "createdAt" to System.currentTimeMillis()
                )
            ).await()
        }
        return chatId
    }

    override suspend fun deleteConversationForUser(chatId: String) {
        // TODO: Implement soft delete if needed
    }
}