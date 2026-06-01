package com.phoenixcorp.founderfinder.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.domain.model.ChatMessage
import com.phoenixcorp.founderfinder.domain.repository.ChatRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ChatRepository {

    private val chatsCollection = firestore.collection("chats")

    override suspend fun sendMessage(message: ChatMessage): Result<Unit> {
        return try {
            val chatDoc = chatsCollection.document(message.chatId)
            chatDoc.collection("messages").document(message.id).set(message).await()
            Result.success(Unit)
        } catch (e: Exception) {
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
            // Log error if needed
        }
    }

    override suspend fun createOrGetChat(userId1: String, userId2: String): String {
        val chatId = if (userId1 < userId2) "${userId1}_$userId2" else "${userId2}_$userId1"

        // Create chat document if it doesn't exist
        val chatRef = chatsCollection.document(chatId)
        val exists = chatRef.get().await().exists()
        if (!exists) {
            chatRef.set(mapOf("participants" to listOf(userId1, userId2), "createdAt" to System.currentTimeMillis()))
        }
        return chatId
    }
}