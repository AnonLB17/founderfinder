package com.phoenixcorp.founderfinder.domain.usecase

import com.phoenixcorp.founderfinder.domain.repository.NotificationRepository
import javax.inject.Inject

class CreateCommentNotificationUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(
        threadOwnerId: String,
        commenterId: String,
        commenterName: String,
        commentText: String,
        threadId: String,
        commentId: String? = null,
        isReplyToComment: Boolean = false
    ) {
        val type = if (isReplyToComment) "comment_reply" else "new_comment"
        val title = if (isReplyToComment) "Reply to your comment" else "New Comment"

        notificationRepository.createNotification(
            userId = threadOwnerId,
            senderId = commenterId,
            senderName = commenterName,
            type = type,
            title = title,
            body = commentText.take(100),
            chatId = null,
            forumId = null,           // Add these
            threadId = threadId,
            commentId = commentId,
            messageId = null,
            category = null
        )
    }
}