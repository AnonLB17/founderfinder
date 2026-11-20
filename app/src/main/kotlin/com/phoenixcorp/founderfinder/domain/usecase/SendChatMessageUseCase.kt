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
        Log.d("SendChatMessageUseCase", "Sending message to chatId=${message.chatId}, recipient=${message.recipientId}")

        val result = chatRepository.sendMessage(message)

        if (result.isSuccess) {
            Log.d("SendChatMessageUseCase", "Message saved successfully → triggering notification")
            try {
                sendPrivateChatNotificationUseCase(
                    senderId = message.senderId,
                    recipientId = message.recipientId ?: return result, // safety
                    chatId = message.chatId,
                    messageText = message.text
                )
            } catch (e: Exception) {
                Log.e("SendChatMessageUseCase", "Notification failed", e)
            }
        } else {
            Log.e("SendChatMessageUseCase", "Failed to send message", result.exceptionOrNull())
        }

        return result
    }
}