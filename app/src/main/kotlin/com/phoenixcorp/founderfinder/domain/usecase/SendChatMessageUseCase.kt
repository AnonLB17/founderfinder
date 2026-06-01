package com.phoenixcorp.founderfinder.domain.usecase

import com.phoenixcorp.founderfinder.domain.model.ChatMessage
import com.phoenixcorp.founderfinder.domain.repository.ChatRepository
import javax.inject.Inject

class SendChatMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(message: ChatMessage): Result<Unit> {
        return chatRepository.sendMessage(message)
    }
}