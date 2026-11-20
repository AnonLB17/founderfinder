package com.phoenixcorp.founderfinder.domain.usecase

import android.util.Log
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
        category: String
    ) {
        if (creatorId == forumOwnerId) {
            Log.d("CreateThreadNotification", "Skipping self-notification")
            return
        }

        if (category.isBlank()) {
            Log.e("CreateThreadNotification", "CRITICAL: Category is blank/null when creating notification!")
        } else {
            Log.d("CreateThreadNotification", "Creating notification with category: $category")
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
            category = category,        // Pass exactly as received
            screen = "ForumTemplate"
        )
    }
}