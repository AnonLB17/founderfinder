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
            Log.d("SendChatMessageUseCase", "✅ Message saved successfully. Now triggering notifications...")

            try {
                // Trigger BOTH in-app + push
                sendPrivateChatNotificationUseCase(
                    senderId = message.senderId,
                    recipientId = message.recipientId ?: "",
                    chatId = message.chatId,
                    messageText = message.text ?: ""
                )
                Log.d("SendChatMessageUseCase", "✅ Notification use case called successfully")
            } catch (e: Exception) {
                Log.e("SendChatMessageUseCase", "❌ Failed to trigger notification", e)
            }
        } else {
            Log.e("SendChatMessageUseCase", "❌ Message send failed: ${sendResult.exceptionOrNull()?.message}")
        }

        return sendResult
    }
}