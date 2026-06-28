package com.phoenixcorp.founderfinder.domain.usecase

import com.phoenixcorp.founderfinder.domain.model.Notification
import com.phoenixcorp.founderfinder.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllNotificationsUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    operator fun invoke(userId: String): Flow<List<Notification>> {
        return notificationRepository.getAllNotifications(userId)
    }
}