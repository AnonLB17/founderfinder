package com.phoenixcorp.founderfinder.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.domain.model.Comment
import com.phoenixcorp.founderfinder.domain.model.Thread
import com.phoenixcorp.founderfinder.domain.repository.ThreadRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ThreadRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ThreadRepository {

    private val TAG = "ThreadRepository"

    override suspend fun createThread(thread: Thread): Result<String> {
        return try {
            val category = thread.category.ifBlank { "marketpotential" }
            val forumId = thread.forumId.ifBlank { throw IllegalArgumentException("forumId is required") }

            firestore.collection("category")
                .document(category)
                .collection("forum")
                .document(forumId)
                .collection("threads")
                .document(thread.id)
                .set(thread)
                .await()

            Log.d(TAG, "✅ Thread created: ${thread.id} in $category/$forumId")
            Result.success(thread.id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create thread", e)
            Result.failure(e)
        }
    }

    override suspend fun getThreadsByForum(category: String, forumId: String): List<Thread> {
        return try {
            val snapshot = firestore.collection("category")
                .document(category)
                .collection("forum")
                .document(forumId)
                .collection("threads")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Thread::class.java)?.copy(forumId = forumId, category = category)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get threads", e)
            emptyList()
        }
    }

    override suspend fun getThreadById(category: String, forumId: String, threadId: String): Thread? {
        return try {
            val doc = firestore.collection("category")
                .document(category)
                .collection("forum")
                .document(forumId)
                .collection("threads")
                .document(threadId)
                .get()
                .await()

            doc.toObject(Thread::class.java)?.copy(forumId = forumId, category = category)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get thread $threadId", e)
            null
        }
    }

    override fun getThreadsFlow(category: String, forumId: String): Flow<List<Thread>> = callbackFlow {
        val listener = firestore.collection("category")
            .document(category)
            .collection("forum")
            .document(forumId)
            .collection("threads")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Snapshot error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val threads = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Thread::class.java)?.copy(forumId = forumId, category = category)
                } ?: emptyList()
                trySend(threads)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun createComment(comment: Comment): Result<String> {
        return try {
            val category = comment.category.ifBlank { "marketpotential" }
            val forumId = comment.forumId.ifBlank { throw IllegalArgumentException("forumId is required") }
            val threadId = comment.threadId.ifBlank { throw IllegalArgumentException("threadId is required") }

            firestore.collection("category")
                .document(category)
                .collection("forum")
                .document(forumId)
                .collection("threads")
                .document(threadId)
                .collection("comments")
                .document(comment.id)
                .set(comment)
                .await()

            Log.d(TAG, "✅ Comment created: ${comment.id} on thread $threadId")
            Result.success(comment.id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create comment", e)
            Result.failure(e)
        }
    }

    override suspend fun getCommentById(commentId: String): Comment? {
        // TODO: Full implementation (needs category/forum/thread context)
        return null
    }

    override fun getRepliesForComment(commentId: String): Flow<List<Comment>> = callbackFlow {
        // TODO: Real-time replies
        trySend(emptyList())
    }

    override suspend fun toggleLike(
        category: String,
        forumId: String,
        threadId: String,
        userId: String
    ): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun toggleFavorite(
        category: String,
        forumId: String,
        threadId: String,
        userId: String,
        isFavorited: Boolean
    ): Result<Unit> {
        return try {
            firestore.collection("category")
                .document(category)
                .collection("forum")
                .document(forumId)
                .collection("threads")
                .document(threadId)
                .update("isFavorited", isFavorited)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle favorite", e)
            Result.failure(e)
        }
    }
}