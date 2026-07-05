package com.phoenixcorp.founderfinder.domain.usecase

import android.util.Log
import com.phoenixcorp.founderfinder.domain.model.UserProfile
import com.phoenixcorp.founderfinder.domain.repository.NotificationRepository
import javax.inject.Inject

class CreateThreadNotificationUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {

    suspend operator fun invoke(
        forumOwnerId: String,
        creatorId: String,
        creatorName: String,
        threadId: String,
        forumId: String,
        category: String   // This should be "requestedsolutions"
    ) {
        if (creatorId == forumOwnerId) {
            Log.d("CreateThreadNotification", "Skipping self-notification")
            return
        }

        notificationRepository.createNotification(
            userId = forumOwnerId,
            senderId = creatorId,
            senderName = creatorName,
            type = "new_thread",
            title = "New Thread in Your Forum",
            body = "A new thread was created in your forum",
            forumId = forumId,
            threadId = threadId,
            category = category,   // Make sure this is "requestedsolutions"
            screen = "ForumTemplate"
        )
    }
}