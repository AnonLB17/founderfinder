package com.phoenixcorp.founderfinder.domain.usecase

import android.util.Log
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

        val title = if (isReplyToComment) {
            "${commenterName} Replied to your Comment"
        } else {
            "New Comment on Thread"
        }

        Log.d("CreateCommentNotificationUseCase",
            "=== CREATING NOTIFICATION ===\n" +
                    "Type: $type\n" +
                    "Title: $title\n" +
                    "To User: $threadOwnerId\n" +
                    "Category: $category\n" +
                    "ThreadId: $threadId\n" +
                    "CommentId: $commentId")

        notificationRepository.createNotification(
            userId = threadOwnerId,
            senderId = commenterId,
            senderName = commenterName,
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