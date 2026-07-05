package com.phoenixcorp.founderfinder.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.domain.model.Comment
import com.phoenixcorp.founderfinder.domain.model.Thread
import com.phoenixcorp.founderfinder.domain.usecase.CreateCommentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ThreadViewModel @Inject constructor(
    private val createCommentUseCase: CreateCommentUseCase   // ← Add this line
) : ViewModel() {

    // ... rest of your code stays the same

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _thread = MutableStateFlow<Thread?>(null)
    val thread: StateFlow<Thread?> = _thread.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Persistent favorite cache (survives navigation)
    private val favoritedCache = mutableSetOf<String>()

    private fun loadUserFavorites() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val userDoc = firestore.collection("profiles").document(userId).get().await()
                val favorites = userDoc.get("favoritedComments") as? List<String> ?: emptyList()
                favoritedCache.clear()
                favoritedCache.addAll(favorites)
                Log.d("ThreadViewModel", "Loaded ${favorites.size} user favorites from profile")
            } catch (e: Exception) {
                Log.e("ThreadViewModel", "Failed to load user favorites", e)
            }
        }
    }

    fun loadThread(category: String, forumId: String, threadId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d("ThreadViewModel", "Trying to load thread: category=$category, forum=$forumId, thread=$threadId")

                // Load user favorites once
                if (favoritedCache.isEmpty()) loadUserFavorites()

                var threadDoc = firestore.collection("forums")
                    .document(category)
                    .collection("forums")
                    .document(forumId)
                    .collection("threads")
                    .document(threadId)
                    .get()
                    .await()

                if (!threadDoc.exists()) {
                    Log.w("ThreadViewModel", "Not found in /forums/... Trying /category/... path")
                    threadDoc = firestore.collection("category")
                        .document(category)
                        .collection("forum")
                        .document(forumId)
                        .collection("threads")
                        .document(threadId)
                        .get()
                        .await()
                }

                if (threadDoc.exists()) {
                    _thread.value = threadDoc.toObject(Thread::class.java)
                    Log.d("ThreadViewModel", "✅ Thread loaded successfully!")
                } else {
                    Log.e("ThreadViewModel", "Thread still not found!")
                }

                val baseRef = if (threadDoc.exists() &&
                    firestore.collection("category").document(category).collection("forum").document(forumId).collection("threads").document(threadId).get().await().exists()) {
                    firestore.collection("category")
                        .document(category)
                        .collection("forum")
                        .document(forumId)
                        .collection("threads")
                        .document(threadId)
                } else {
                    firestore.collection("forums")
                        .document(category)
                        .collection("forums")
                        .document(forumId)
                        .collection("threads")
                        .document(threadId)
                }

                val commentsSnapshot = baseRef
                    .collection("comments")
                    .orderBy("timestamp")
                    .get()
                    .await()

                _comments.value = commentsSnapshot.toObjects(Comment::class.java).map { raw ->
                    val isFav = favoritedCache.contains(raw.id) || (raw.isFavorited == true)
                    raw.copy(isFavorited = isFav)
                }

                Log.d("ThreadViewModel", "✅ Loaded ${commentsSnapshot.size()} comments from comments subcollection")

            } catch (e: Exception) {
                Log.e("ThreadViewModel", "Error loading thread or comments", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createComment(comment: Comment) {
        viewModelScope.launch {
            try {
                val category = comment.category.ifBlank { "requestedsolutions" }

                // Use the UseCase (this triggers notification)
                val result = createCommentUseCase(
                    comment = comment,
                    threadOwnerId = _thread.value?.creatorId ?: "",
                    threadTitle = _thread.value?.message?.take(50) ?: "Thread",
                    isReplyToComment = false
                )

                if (result.isSuccess) {
                    loadThread(category, comment.forumId, comment.threadId)
                    Log.d("ThreadViewModel", "✅ Comment created successfully")
                } else {
                    Log.e("ThreadViewModel", "Failed to create comment", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e("ThreadViewModel", "Failed to create comment", e)
            }
        }
    }

    fun likeComment(commentId: String) {
        viewModelScope.launch {
            val currentUserId = auth.currentUser?.uid ?: return@launch
            val thread = _thread.value ?: return@launch
            val category = thread.category.ifBlank { "requestedsolutions" }

            try {
                val commentRef = firestore.collection("category")
                    .document(category)
                    .collection("forum")
                    .document(thread.forumId)
                    .collection("threads")
                    .document(thread.id)
                    .collection("comments")
                    .document(commentId)

                val commentSnap = commentRef.get().await()
                val likedBy = commentSnap.get("likedBy") as? List<String> ?: emptyList()

                val isCurrentlyLiked = likedBy.contains(currentUserId)

                if (isCurrentlyLiked) {
                    commentRef.update(
                        mapOf(
                            "likes" to FieldValue.increment(-1),
                            "likedBy" to FieldValue.arrayRemove(currentUserId)
                        )
                    ).await()
                    Log.d("ThreadViewModel", "Unliked comment: $commentId")
                } else {
                    commentRef.update(
                        mapOf(
                            "likes" to FieldValue.increment(1),
                            "likedBy" to FieldValue.arrayUnion(currentUserId)
                        )
                    ).await()
                    Log.d("ThreadViewModel", "Liked comment: $commentId")
                }

                loadThread(category, thread.forumId, thread.id)
            } catch (e: Exception) {
                Log.e("ThreadViewModel", "Failed to toggle like", e)
            }
        }
    }

    fun favoriteComment(commentId: String, isFavoriting: Boolean) {
        viewModelScope.launch {
            val thread = _thread.value ?: return@launch
            val category = thread.category.ifBlank { "requestedsolutions" }
            val userId = auth.currentUser?.uid ?: return@launch

            try {
                // Update local cache
                if (isFavoriting) {
                    favoritedCache.add(commentId)
                } else {
                    favoritedCache.remove(commentId)
                }

                // Optimistic UI update
                _comments.value = _comments.value.map { comment ->
                    if (comment.id == commentId) comment.copy(isFavorited = isFavoriting) else comment
                }

                // Update comment document
                val commentRef = firestore.collection("category")
                    .document(category)
                    .collection("forum")
                    .document(thread.forumId)
                    .collection("threads")
                    .document(thread.id)
                    .collection("comments")
                    .document(commentId)

                commentRef.update("isFavorited", isFavoriting).await()

                // Persist to user profile for permanence
                val userRef = firestore.collection("profiles").document(userId)
                if (isFavoriting) {
                    userRef.update("favoritedComments", FieldValue.arrayUnion(commentId)).await()
                } else {
                    userRef.update("favoritedComments", FieldValue.arrayRemove(commentId)).await()
                }

                Log.d("ThreadViewModel", "✅ Toggled favorite: $commentId -> $isFavoriting (persisted permanently)")
            } catch (e: Exception) {
                Log.e("ThreadViewModel", "Failed to favorite comment", e)
            }
        }
    }
}