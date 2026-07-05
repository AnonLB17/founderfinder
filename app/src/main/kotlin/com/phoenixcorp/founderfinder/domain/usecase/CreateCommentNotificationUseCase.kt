package com.phoenixcorp.founderfinder.domain.usecase

import com.phoenixcorp.founderfinder.domain.model.UserProfile
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
        forumId: String? = null,
        category: String? = null,
        isReplyToComment: Boolean = false
    ) {
        val type = if (isReplyToComment) "comment_reply" else "new_comment"
        val title = if (isReplyToComment) "Reply to your comment" else "New Comment on Thread"

        notificationRepository.createNotification(
            userId = threadOwnerId,
            senderId = commenterId,
            senderName = commenterName,           // Pass the real name
            type = type,
            title = title,
            body = commentText.take(100),
            forumId = forumId,
            threadId = threadId,
            commentId = commentId,
            category = category,
            screen = "ThreadScreen"
        )
    }
}