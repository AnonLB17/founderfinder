package com.phoenixcorp.founderfinder.domain.repository

import com.phoenixcorp.founderfinder.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun sendMessage(message: ChatMessage): Result<Unit>

    /** Sends push notification to the recipient */
    suspend fun sendChatNotification(chatId: String, message: ChatMessage): Result<Unit>

    fun getChatMessages(chatId: String): Flow<List<ChatMessage>>
    suspend fun markMessageAsRead(messageId: String, chatId: String)
    suspend fun createOrGetChat(userId1: String, userId2: String): String
}