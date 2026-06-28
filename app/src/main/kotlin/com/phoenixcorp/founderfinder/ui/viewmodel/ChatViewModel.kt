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

    fun loadMessages(chatId: String) {
        getChatMessagesUseCase(chatId)
            .onEach { _messages.value = it }
            .launchIn(viewModelScope)
    }

    fun sendMessage(message: ChatMessage) {
        viewModelScope.launch {
            val result = sendChatMessageUseCase(message)
            if (result.isFailure) {
                // TODO: Show error to user (Snackbar, etc.)
                // You can expose a error state flow if needed
            }
        }
    }
}