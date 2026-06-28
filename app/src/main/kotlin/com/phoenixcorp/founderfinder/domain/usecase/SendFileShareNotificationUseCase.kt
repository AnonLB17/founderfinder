package com.phoenixcorp.founderfinder.domain.usecase

import com.phoenixcorp.founderfinder.domain.repository.NotificationRepository
import javax.inject.Inject

class SendFileShareNotificationUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(
        recipientId: String,
        senderId: String,
        senderName: String,
        fileName: String,
        chatId: String? = null
    ) {
        notificationRepository.createNotification(
            userId = recipientId,
            senderId = senderId,
            senderName = senderName,
            type = "file_share",
            title = "$senderName shared a file",
            body = fileName,
            chatId = chatId,
            forumId = null,
            threadId = null,
            commentId = null,
            messageId = null,
            category = null
        )
    }
}