package com.phoenixcorp.founderfinder.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.phoenixcorp.founderfinder.domain.model.Comment
import com.phoenixcorp.founderfinder.domain.model.Thread
import com.phoenixcorp.founderfinder.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ThreadRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ThreadRepository {

    // Legacy path only: /category/{category}/forum/{forumId}/threads/...
    private fun getThreadCollection(category: String, forumId: String) =
        firestore.collection("category")
            .document(category)
            .collection("forum")
            .document(forumId)
            .collection("threads")

    private fun getCommentCollection(category: String, forumId: String, threadId: String) =
        getThreadCollection(category, forumId)
            .document(threadId)
            .collection("comments")

    // ==================== THREADS ====================

    override suspend fun createThread(thread: Thread): Result<String> {
        return try {
            val threadRef = getThreadCollection(thread.category, thread.forumId)
                .document(thread.id)

            threadRef.set(thread).await()
            Log.d("ThreadRepository", "Thread created in legacy path: ${thread.category}/${thread.forumId}")
            Result.success(thread.id)
        } catch (e: Exception) {
            Log.e("ThreadRepository", "Failed to create thread", e)
            Result.failure(e)
        }
    }

    override suspend fun getThreadsByForum(category: String, forumId: String): List<Thread> {
        return try {
            val threads = getThreadCollection(category, forumId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(Thread::class.java)

            Log.d("ThreadRepository", "✅ Loaded ${threads.size} threads from legacy path")
            threads
        } catch (e: Exception) {
            Log.e("ThreadRepository", "Failed to load threads", e)
            emptyList()
        }
    }

    override suspend fun getThreadById(
        category: String,
        forumId: String,
        threadId: String
    ): Thread? {
        return try {
            getThreadCollection(category, forumId)
                .document(threadId)
                .get()
                .await()
                .toObject(Thread::class.java)
        } catch (e: Exception) {
            Log.e("ThreadRepository", "Failed to load thread", e)
            null
        }
    }

    override fun getThreadsFlow(category: String, forumId: String): Flow<List<Thread>> = flow {
        try {
            getThreadCollection(category, forumId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("ThreadRepository", "Snapshot error", error)
                        return@addSnapshotListener
                    }
                    val threads = snapshot?.toObjects(Thread::class.java) ?: emptyList()
                    // Note: Real emission should use callbackFlow in production
                }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    // ==================== LIKE / FAVORITE ====================

    override suspend fun toggleLike(
        category: String,
        forumId: String,
        threadId: String,
        userId: String
    ): Result<Unit> {
        return try {
            val threadRef = getThreadCollection(category, forumId).document(threadId)
            val snapshot = threadRef.get().await()
            val likedBy = snapshot.get("likedBy") as? List<String> ?: emptyList()

            if (likedBy.contains(userId)) {
                threadRef.update(
                    mapOf(
                        "likes" to com.google.firebase.firestore.FieldValue.increment(-1),
                        "likedBy" to com.google.firebase.firestore.FieldValue.arrayRemove(userId)
                    )
                ).await()
            } else {
                threadRef.update(
                    mapOf(
                        "likes" to com.google.firebase.firestore.FieldValue.increment(1),
                        "likedBy" to com.google.firebase.firestore.FieldValue.arrayUnion(userId)
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleFavorite(
        category: String,
        forumId: String,
        threadId: String,
        userId: String,
        isFavorited: Boolean
    ): Result<Unit> {
        return try {
            getThreadCollection(category, forumId).document(threadId)
                .update("isFavorited", isFavorited).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== COMMENTS ====================

    override suspend fun createComment(comment: Comment): Result<String> {
        return try {
            val commentRef = getCommentCollection(comment.category, comment.forumId, comment.threadId)
                .document(comment.id)

            commentRef.set(comment).await()
            Result.success(comment.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCommentsForThread(
        category: String,
        forumId: String,
        threadId: String
    ): List<Comment> {
        return try {
            getCommentCollection(category, forumId, threadId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()
                .toObjects(Comment::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun getCommentsFlowForThread(
        category: String,
        forumId: String,
        threadId: String
    ): Flow<List<Comment>> = flow {
        getCommentCollection(category, forumId, threadId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                val comments = snapshot?.toObjects(Comment::class.java) ?: emptyList()
            }
    }

    override suspend fun getCommentById(commentId: String): Comment? = null
    override fun getRepliesForComment(commentId: String): Flow<List<Comment>> = flow { emit(emptyList()) }
}