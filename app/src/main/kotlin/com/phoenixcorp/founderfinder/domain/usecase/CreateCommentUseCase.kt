package com.phoenixcorp.founderfinder.domain.usecase

import android.util.Log
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
                val commentId = result.getOrNull() ?: comment.id

                if (comment.creatorId != threadOwnerId) {
                    Log.d("CreateCommentUseCase", "Creating notification for reply=$isReplyToComment")

                    createCommentNotificationUseCase(
                        threadOwnerId = threadOwnerId,
                        commenterId = comment.creatorId,
                        commenterName = comment.creatorName,
                        commentText = comment.message,
                        threadId = comment.threadId,
                        commentId = commentId,
                        forumId = comment.forumId,
                        category = comment.category,
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