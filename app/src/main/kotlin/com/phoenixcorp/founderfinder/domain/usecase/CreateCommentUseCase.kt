package com.phoenixcorp.founderfinder.domain.usecase

import com.phoenixcorp.founderfinder.domain.model.Comment
import com.phoenixcorp.founderfinder.domain.repository.ForumRepository
import com.phoenixcorp.founderfinder.domain.repository.NotificationRepository
import javax.inject.Inject

class CreateCommentUseCase @Inject constructor(
    private val forumRepository: ForumRepository,
    private val createCommentNotificationUseCase: CreateCommentNotificationUseCase
) {

    suspend operator fun invoke(
        comment: Comment,
        threadOwnerId: String,
        threadTitle: String,
        isReplyToComment: Boolean = false
    ): Result<String> {
        return try {
            val result = forumRepository.createComment(comment)

            if (result.isSuccess) {
                val commentId = result.getOrNull() ?: ""

                if (comment.creatorId != threadOwnerId) {
                    createCommentNotificationUseCase(
                        threadOwnerId = threadOwnerId,
                        commenterId = comment.creatorId,
                        commenterName = comment.creatorName,
                        commentText = comment.message,
                        threadId = comment.threadId,           // Now available
                        commentId = commentId,
                        isReplyToComment = isReplyToComment
                    )
                }
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}