package com.phoenixcorp.founderfinder.domain.usecase

import com.phoenixcorp.founderfinder.domain.model.Thread
import com.phoenixcorp.founderfinder.domain.repository.ThreadRepository
import javax.inject.Inject

class CreateThreadUseCase @Inject constructor(
    private val threadRepository: ThreadRepository,
    private val createThreadNotificationUseCase: CreateThreadNotificationUseCase
) {

    suspend operator fun invoke(
        thread: Thread,
        forumOwnerId: String,
        category: String,      // This must be passed correctly
        forumId: String
    ): Result<String> {
        return try {
            val result = threadRepository.createThread(thread)

            if (result.isSuccess) {
                val threadId = result.getOrNull() ?: thread.id

                if (thread.creatorId != forumOwnerId) {
                    createThreadNotificationUseCase(
                        forumOwnerId = forumOwnerId,
                        creatorId = thread.creatorId,
                        creatorName = thread.creatorName,
                        threadId = threadId,
                        forumId = forumId,
                        category = category   // Pass exactly what was given
                    )
                }
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}