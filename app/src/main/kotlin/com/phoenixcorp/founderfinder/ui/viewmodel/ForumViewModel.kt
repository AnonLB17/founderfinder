package com.phoenixcorp.founderfinder.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.domain.model.Comment
import com.phoenixcorp.founderfinder.domain.model.Forum
import com.phoenixcorp.founderfinder.domain.model.Thread
import com.phoenixcorp.founderfinder.domain.repository.ForumRepository
import com.phoenixcorp.founderfinder.domain.usecase.CreateThreadNotificationUseCase
import com.phoenixcorp.founderfinder.domain.usecase.CreateThreadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ForumViewModel @Inject constructor(
    private val forumRepository: ForumRepository,
    private val createThreadUseCase: CreateThreadUseCase,
    private val createThreadNotificationUseCase: CreateThreadNotificationUseCase
) : ViewModel() {

    private val _forum = MutableStateFlow<Forum?>(null)
    val forum: StateFlow<Forum?> = _forum.asStateFlow()

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

    fun loadForum(category: String, forumId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val forumData = forumRepository.getForum(category, forumId)
                if (forumData != null) {
                    _forum.value = forumData
                    loadThreads(category, forumId)
                } else {
                    _error.value = "Forum not found: $category/$forumId"
                    Log.w("ForumViewModel", "Forum document not found at categories/$category/forums/$forumId")
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load forum"
                Log.e("ForumViewModel", "Error loading forum", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadThreads(category: String, forumId: String) {
        viewModelScope.launch {
            try {
                val loadedThreads = forumRepository.getThreadsByForum(category, forumId)
                _threads.value = loadedThreads.sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load threads"
                Log.e("ForumViewModel", "Error loading threads", e)
            }
        }
    }

    fun loadThreadDetails(category: String, forumId: String, threadId: String) {
        viewModelScope.launch {
            try {
                if (forumId.isBlank() || threadId.isBlank()) return@launch

                val loadedThread = forumRepository.getThreadById(category, forumId, threadId)
                _selectedThread.value = loadedThread

                // TODO: Load comments for selected thread
                _commentsForThread.value = emptyList()
            } catch (e: Exception) {
                Log.e("ForumViewModel", "Failed to load thread details", e)
                _error.value = e.message
            }
        }
    }

    fun createThread(
        message: String,
        forumId: String,
        routeCategory: String,
        forumOwnerId: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val actualCategory = forum.value?.category?.takeIf { it.isNotBlank() }
                ?: routeCategory.takeIf { it.isNotBlank() }
                ?: ""

            if (actualCategory.isBlank()) {
                Log.e("ForumViewModel", "Cannot create thread - category is blank!")
                _error.value = "Category is required"
                _isLoading.value = false
                return@launch
            }

            val currentUser = FirebaseAuth.getInstance().currentUser
            val uid = currentUser?.uid ?: ""

            // Name resolution
            var creatorName = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: ""

            if (creatorName.isBlank()) {
                try {
                    val profileDoc = FirebaseFirestore.getInstance()
                        .collection("profiles")
                        .document(uid)
                        .get()
                        .await()

                    val firstName = profileDoc.getString("firstName") ?: ""
                    val lastName = profileDoc.getString("lastName") ?: ""
                    creatorName = "$firstName $lastName".trim()
                } catch (e: Exception) {
                    Log.w("ForumViewModel", "Could not fetch profile name", e)
                }
            }

            if (creatorName.isBlank()) {
                creatorName = "User"
            }

            // Profile Picture - Better resolution
            var profilePicture = currentUser?.photoUrl?.toString() ?: ""

            if (profilePicture.isBlank()) {
                try {
                    val profileDoc = FirebaseFirestore.getInstance()
                        .collection("profiles")
                        .document(uid)
                        .get()
                        .await()

                    profilePicture = profileDoc.getString("profilePicture") ?: ""
                    Log.d("ForumViewModel", "Fetched profile picture from profiles collection")
                } catch (e: Exception) {
                    Log.w("ForumViewModel", "Could not fetch profile picture", e)
                }
            }

            val thread = Thread(
                id = UUID.randomUUID().toString(),
                message = message,
                forumId = forumId,
                creatorId = uid,
                creatorName = creatorName,
                creatorProfilePicture = profilePicture,     // ← Should now have value
                timestamp = System.currentTimeMillis(),
                category = actualCategory
            )

            Log.d("ForumViewModel", "Creating thread by: $creatorName | pic=${profilePicture.isNotBlank()}")

            val result = createThreadUseCase(
                thread = thread,
                forumOwnerId = forumOwnerId,
                category = actualCategory,
                forumId = forumId
            )

            if (result.isSuccess) {
                loadThreads(actualCategory, forumId)
                Log.d("ForumViewModel", "✅ Thread created successfully")
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to create thread"
            }

            _isLoading.value = false
        }
    }

    // Keep other methods for now
    fun loadCommentDetails(commentId: String) { /* TODO */ }
}