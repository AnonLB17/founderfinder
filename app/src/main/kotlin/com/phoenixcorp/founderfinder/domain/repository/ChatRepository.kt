package com.phoenixcorp.founderfinder.domain.repository

import com.phoenixcorp.founderfinder.domain.model.ChatMessage
import com.phoenixcorp.founderfinder.domain.model.Conversation
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun sendMessage(message: ChatMessage): Result<Unit>

    suspend fun sendChatNotification(chatId: String, message: ChatMessage): Result<Unit>

    fun getChatMessages(chatId: String): Flow<List<ChatMessage>>

    fun getUserConversations(userId: String): Flow<List<Conversation>>

    suspend fun markMessageAsRead(messageId: String, chatId: String)

    suspend fun createOrGetChat(userId1: String, userId2: String): String

    // Optional: Delete conversation for current user (soft delete)
    suspend fun deleteConversationForUser(chatId: String)
}