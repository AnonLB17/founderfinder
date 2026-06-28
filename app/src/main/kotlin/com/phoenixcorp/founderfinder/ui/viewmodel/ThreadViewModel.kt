package com.phoenixcorp.founderfinder.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import com.phoenixcorp.founderfinder.domain.model.Comment
import com.phoenixcorp.founderfinder.domain.model.Thread
import com.phoenixcorp.founderfinder.domain.repository.ThreadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ThreadViewModel @Inject constructor(
    private val threadRepository: ThreadRepository
) : ViewModel() {

    private val _thread = MutableStateFlow<Thread?>(null)
    val thread: StateFlow<Thread?> = _thread.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    fun loadThread(category: String, forumId: String, threadId: String) {
        viewModelScope.launch {
            try {
                Log.d("ThreadViewModel", "🔍 Loading thread from: /category/$category/forum/$forumId/threads/$threadId")

                val doc = FirebaseFirestore.getInstance()
                    .collection("category")
                    .document(category)
                    .collection("forum")
                    .document(forumId)
                    .collection("threads")
                    .document(threadId)
                    .get()
                    .await()

                if (doc.exists()) {
                    val data = doc.data ?: emptyMap<String, Any>()

                    val loadedThread = Thread(
                        id = doc.id,
                        forumId = forumId,
                        category = category,
                        creatorId = data["creatorId"] as? String ?: "",
                        creatorName = data["creatorName"] as? String ?: "Anonymous",
                        creatorProfilePicture = data["creatorProfilePicture"] as? String ?: "",
                        message = data["message"] as? String ?: "[No message]",
                        timestamp = (data["timestamp"] as? Long) ?: System.currentTimeMillis(),
                        likes = (data["likes"] as? Long) ?: 0L,
                        isFavorited = (data["favorited"] as? Boolean) ?: false,   // Note: "favorited" not "isFavorited"
                        institutionName = data["institutionName"] as? String ?: forumId,
                        imageUrl = data["imageUrl"] as? String,
                        location = data["location"] as? String,
                        topicHeader = data["topicHeader"] as? String
                    )

                    _thread.value = loadedThread
                    Log.d("ThreadViewModel", "✅ Thread loaded successfully: \"${loadedThread.message}\" by ${loadedThread.creatorName}")
                } else {
                    Log.e("ThreadViewModel", "❌ Document does not exist")
                }

                loadComments(category, forumId, threadId)
            } catch (e: Exception) {
                Log.e("ThreadViewModel", "💥 Failed to load thread", e)
            }
        }
    }

    private fun loadComments(category: String, forumId: String, threadId: String) {
        viewModelScope.launch {
            try {
                Log.d("ThreadViewModel", "Loading comments for thread: $threadId")

                val snapshot = FirebaseFirestore.getInstance()
                    .collection("category")
                    .document(category)
                    .collection("forum")
                    .document(forumId)
                    .collection("threads")
                    .document(threadId)
                    .collection("comments")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                    .get()
                    .await()

                val loadedComments = snapshot.toObjects<Comment>()
                _comments.value = loadedComments

                Log.d("ThreadViewModel", "✅ Loaded ${loadedComments.size} comments successfully")
            } catch (e: Exception) {
                Log.e("ThreadViewModel", "Failed to load comments for thread $threadId", e)
                _comments.value = emptyList()
            }
        }
    }

    fun createComment(comment: Comment) {
        viewModelScope.launch {
            try {
                val result = threadRepository.createComment(comment)
                if (result.isSuccess) {
                    Log.d("ThreadViewModel", "✅ Comment created successfully: ${comment.id}")
                    // Refresh comments
                    loadComments(
                        comment.category.ifBlank { "marketpotential" },
                        comment.forumId,
                        comment.threadId
                    )
                } else {
                    Log.e("ThreadViewModel", "❌ Failed to create comment", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e("ThreadViewModel", "Error creating comment", e)
            }
        }
    }
}