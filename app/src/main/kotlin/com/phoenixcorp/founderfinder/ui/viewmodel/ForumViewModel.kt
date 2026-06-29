package com.phoenixcorp.founderfinder.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.phoenixcorp.founderfinder.domain.model.Comment
import com.phoenixcorp.founderfinder.domain.model.Thread
import com.phoenixcorp.founderfinder.domain.repository.ForumRepository
import com.phoenixcorp.founderfinder.domain.usecase.CreateThreadNotificationUseCase
import com.phoenixcorp.founderfinder.domain.usecase.CreateThreadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ForumViewModel @Inject constructor(
    private val forumRepository: ForumRepository,
    private val createThreadUseCase: CreateThreadUseCase,
    private val createThreadNotificationUseCase: CreateThreadNotificationUseCase
) : ViewModel() {

    private val _threads = MutableStateFlow<List<Thread>>(emptyList())
    val threads: StateFlow<List<Thread>> = _threads.asStateFlow()

    private val _selectedThread = MutableStateFlow<Thread?>(null)
    val selectedThread: StateFlow<Thread?> = _selectedThread.asStateFlow()

    private val _commentsForThread = MutableStateFlow<List<Comment>>(emptyList())
    val commentsForThread: StateFlow<List<Comment>> = _commentsForThread.asStateFlow()

    private val _selectedComment = MutableStateFlow<Comment?>(null)
    val selectedComment: StateFlow<Comment?> = _selectedComment.asStateFlow()

    private val _repliesForComment = MutableStateFlow<List<Comment>>(emptyList())
    val repliesForComment: StateFlow<List<Comment>> = _repliesForComment.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val currentUser = FirebaseAuth.getInstance().currentUser

    fun loadThreads(category: String, forumId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val loadedThreads = forumRepository.getThreadsByForum(category, forumId)
                _threads.value = loadedThreads.sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load threads"
                _threads.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadThreadDetails(category: String, forumId: String, threadId: String) {
        viewModelScope.launch {
            try {
                if (forumId.isBlank()) {
                    Log.e("ForumViewModel", "forumId is empty")
                    return@launch
                }

                val loadedThread = forumRepository.getThreadById(category, forumId, threadId)
                _selectedThread.value = loadedThread

                // TODO: Load comments
                _commentsForThread.value = emptyList()
            } catch (e: Exception) {
                Log.e("ForumViewModel", "Failed to load thread details", e)
                _error.value = e.message
            }
        }
    }

    fun loadCommentDetails(commentId: String) {
        viewModelScope.launch {
            try {
                // TODO: Implement getCommentById in repository
                _selectedComment.value = null
                _repliesForComment.value = emptyList()
            } catch (e: Exception) {
                Log.e("ForumViewModel", "Failed to load comment details", e)
                _error.value = e.message
            }
        }
    }

    fun createThread(
        message: String,
        forumId: String,
        category: String,
        forumOwnerId: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val thread = Thread(
                id = UUID.randomUUID().toString(),
                message = message,
                forumId = forumId,
                creatorId = currentUser?.uid ?: "",
                creatorName = currentUser?.displayName ?: "Anonymous",
                timestamp = System.currentTimeMillis(),
                category = category
            )

            val result = createThreadUseCase(
                thread = thread,
                forumOwnerId = forumOwnerId,
                category = category,
                forumId = forumId
            )

            if (result.isSuccess) {
                _error.value = null
                loadThreads(category, forumId) // Refresh list

                Log.d("ForumViewModel", "✅ Thread created successfully - NO notification sent")
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to create thread"
            }

            _isLoading.value = false
        }
    }
}