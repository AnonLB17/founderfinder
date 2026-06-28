package com.phoenixcorp.founderfinder.domain.usecase

import com.phoenixcorp.founderfinder.domain.model.Thread
import com.phoenixcorp.founderfinder.domain.repository.ThreadRepository
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class CreateThreadUseCase @Inject constructor(
    private val threadRepository: ThreadRepository,           // Changed to ThreadRepository
    private val createThreadNotificationUseCase: CreateThreadNotificationUseCase
) {

    suspend operator fun invoke(
        thread: Thread,
        forumOwnerId: String,
        category: String,
        forumId: String
    ): Result<String> {
        return try {
            val result = threadRepository.createThread(thread)

            if (result.isSuccess) {
                val threadId = result.getOrNull() ?: thread.id

                val currentUser = FirebaseAuth.getInstance().currentUser

                if (thread.creatorId != forumOwnerId && currentUser != null) {
                    createThreadNotificationUseCase(
                        forumOwnerId = forumOwnerId,
                        creatorId = currentUser.uid,
                        creatorName = currentUser.displayName ?: "Unknown",
                        threadId = threadId,
                        forumId = forumId,
                        category = category
                    )
                }
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}