package com.phoenixcorp.founderfinder.domain.usecase

import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.domain.model.UserProfile
import com.phoenixcorp.founderfinder.domain.repository.NotificationRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SendPrivateChatNotificationUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {

    suspend operator fun invoke(
        senderId: String,
        recipientId: String,
        chatId: String,
        messageText: String
    ) {
        val senderName = getSenderName(senderId)

        notificationRepository.createNotification(
            userId = recipientId,
            senderId = senderId,
            senderName = senderName,
            type = "new_message",
            title = "Private Message from $senderName",
            body = messageText.take(100),
            chatId = chatId,
            messageId = null,           // optional
            screen = "PrivateChat"
        )
    }

    private suspend fun getSenderName(userId: String): String {
        return try {
            val doc = FirebaseFirestore.getInstance()
                .collection("profiles")
                .document(userId)
                .get()
                .await()

            val profile = doc.toObject(UserProfile::class.java)
            val fullName = "${profile?.firstName ?: ""} ${profile?.lastName ?: ""}".trim()
            fullName.ifBlank { "Unknown User" }
        } catch (e: Exception) {
            "Unknown User"
        }
    }
}