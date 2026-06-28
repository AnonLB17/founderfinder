package com.phoenixcorp.founderfinder.domain.repository

import com.phoenixcorp.founderfinder.domain.model.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getUnreadNotifications(userId: String): Flow<List<Notification>>
    fun getAllNotifications(userId: String): Flow<List<Notification>>

    suspend fun createNotification(
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
    )

    suspend fun markAsRead(notificationId: String)
    suspend fun markAllAsRead(userId: String)
    suspend fun deleteNotification(notificationId: String)   // ← Add this line
}