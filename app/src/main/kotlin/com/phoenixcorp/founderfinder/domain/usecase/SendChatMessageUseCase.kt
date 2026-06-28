package com.phoenixcorp.founderfinder.domain.usecase

import android.util.Log
import com.phoenixcorp.founderfinder.domain.model.ChatMessage
import com.phoenixcorp.founderfinder.domain.repository.ChatRepository
import javax.inject.Inject

class SendChatMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val sendPrivateChatNotificationUseCase: SendPrivateChatNotificationUseCase
) {

    suspend operator fun invoke(message: ChatMessage): Result<Unit> {
        Log.d("SendChatMessageUseCase", "=== START: Sending message to chatId=${message.chatId} ===")

        val sendResult = chatRepository.sendMessage(message)

        if (sendResult.isSuccess) {
            Log.d("SendChatMessageUseCase", "Message sent successfully. Triggering notification to recipient...")
            try {
                sendPrivateChatNotificationUseCase(
                    recipientId = message.recipientId ?: "",
                    message = message,
                    senderName = message.senderName ?: "Unknown"
                )
                Log.d("SendChatMessageUseCase", "✅ Notification triggered successfully")
            } catch (e: Exception) {
                Log.e("SendChatMessageUseCase", "Failed to send notification", e)
            }
        } else {
            Log.e("SendChatMessageUseCase", "Message send failed: ${sendResult.exceptionOrNull()?.message}")
        }

        return sendResult
    }
}