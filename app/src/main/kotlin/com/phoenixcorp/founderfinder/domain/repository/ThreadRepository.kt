package com.phoenixcorp.founderfinder.domain.repository

import com.phoenixcorp.founderfinder.domain.model.Comment
import com.phoenixcorp.founderfinder.domain.model.Thread
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Thread and Comment operations.
 * All operations assume the following Firestore structure:
 * categories/{category}/forums/{forumId}/threads/{threadId}
 */
interface ThreadRepository {

    /**
     * Create a new thread in a forum and return its ID
     */
    suspend fun createThread(thread: Thread): Result<String>

    /**
     * Get all threads for a specific forum, sorted by newest first
     */
    suspend fun getThreadsByForum(category: String, forumId: String): List<Thread>

    /**
     * Get a single thread by ID
     */
    suspend fun getThreadById(category: String, forumId: String, threadId: String): Thread?

    /**
     * Real-time listener for threads in a forum (for live updates)
     */
    fun getThreadsFlow(category: String, forumId: String): Flow<List<Thread>>

    /**
     * Like / Unlike a thread
     */
    suspend fun toggleLike(
        category: String,
        forumId: String,
        threadId: String,
        userId: String
    ): Result<Unit>

    /**
     * Favorite / Unfavorite a thread
     */
    suspend fun toggleFavorite(
        category: String,
        forumId: String,
        threadId: String,
        userId: String,
        isFavorited: Boolean
    ): Result<Unit>

    // ==================== COMMENT METHODS ====================

    /**
     * Create a new comment on a thread (supports nested replies via parentId)
     */
    suspend fun createComment(comment: Comment): Result<String>

    /**
     * Get a single comment by ID
     */
    suspend fun getCommentById(commentId: String): Comment?

    /**
     * Real-time listener for replies to a specific comment
     */
    fun getRepliesForComment(commentId: String): Flow<List<Comment>>

    /**
     * Optional: Get comments for a specific thread (paginated or full)
     */
    suspend fun getCommentsForThread(
        category: String,
        forumId: String,
        threadId: String
    ): List<Comment>

    /**
     * Real-time flow for comments on a thread
     */
    fun getCommentsFlowForThread(
        category: String,
        forumId: String,
        threadId: String
    ): Flow<List<Comment>>
}