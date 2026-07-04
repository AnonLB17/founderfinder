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
        chatId: String? = null,
        screen: String? = null,
        forumId: String? = null,
        threadId: String? = null,
        commentId: String? = null,
        messageId: String? = null,
        category: String? = null,
        activityId: String? = null,
        eventTime: Long? = null,
        activityType: String? = null,      // NEW
        organizationId: String? = null,    // NEW
        organizationName: String? = null   // NEW
    )

    suspend fun deleteNotification(notificationId: String)
    suspend fun markAsRead(notificationId: String)
    suspend fun markAllAsRead(userId: String)
}