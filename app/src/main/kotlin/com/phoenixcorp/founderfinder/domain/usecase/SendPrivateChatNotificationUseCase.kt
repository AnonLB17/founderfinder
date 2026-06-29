package com.phoenixcorp.founderfinder.domain.usecase

import com.phoenixcorp.founderfinder.domain.model.ChatMessage
import com.phoenixcorp.founderfinder.domain.repository.NotificationRepository
import javax.inject.Inject

class SendPrivateChatNotificationUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(
        recipientId: String,
        message: ChatMessage,
        senderName: String
    ) {
        notificationRepository.createNotification(
            userId = recipientId,
            senderId = message.senderId,
            senderName = senderName,
            type = "new_message",
            title = "New Message",
            body = message.text.take(120),
            chatId = message.chatId,
            forumId = null,
            threadId = null,
            commentId = null,
            messageId = message.id,
            category = null
        )
    }
}