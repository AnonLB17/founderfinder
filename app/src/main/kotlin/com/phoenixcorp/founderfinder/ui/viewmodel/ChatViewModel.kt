package com.phoenixcorp.founderfinder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoenixcorp.founderfinder.domain.model.ChatMessage
import com.phoenixcorp.founderfinder.domain.usecase.GetChatMessagesUseCase
import com.phoenixcorp.founderfinder.domain.usecase.SendChatMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendChatMessageUseCase: SendChatMessageUseCase,
    private val getChatMessagesUseCase: GetChatMessagesUseCase
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending = _isSending.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun loadMessages(chatId: String) {
        getChatMessagesUseCase(chatId)
            .onEach { messagesList ->
                _messages.value = messagesList
            }
            .launchIn(viewModelScope)
    }

    fun sendMessage(message: ChatMessage) {
        viewModelScope.launch {
            _isSending.value = true
            _error.value = null

            val result = sendChatMessageUseCase(message)

            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to send message"
            }

            _isSending.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}