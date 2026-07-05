package com.phoenixcorp.founderfinder.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoenixcorp.founderfinder.domain.model.Conversation
import com.phoenixcorp.founderfinder.domain.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val currentUserId: String? get() = auth.currentUser?.uid

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        loadConversations()
    }

    fun loadConversations() {
        val userId = currentUserId ?: return

        _isLoading.value = true

        chatRepository.getUserConversations(userId)
            .onEach { list ->
                _conversations.value = list
                _isLoading.value = false
                Log.d("ChatListViewModel", "Loaded ${list.size} conversations")
            }
            .launchIn(viewModelScope)
    }

    fun startOrOpenChat(otherUserId: String, onChatReady: (chatId: String) -> Unit) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            try {
                val chatId = chatRepository.createOrGetChat(userId, otherUserId)
                onChatReady(chatId)
            } catch (e: Exception) {
                _error.value = "Failed to open chat: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}