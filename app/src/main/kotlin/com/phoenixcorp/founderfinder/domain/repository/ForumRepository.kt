// domain/repository/ForumRepository.kt
package com.phoenixcorp.founderfinder.domain.repository

import com.phoenixcorp.founderfinder.domain.model.Comment
import com.phoenixcorp.founderfinder.domain.model.Forum
import com.phoenixcorp.founderfinder.domain.model.ForumPost
import com.phoenixcorp.founderfinder.domain.model.ForumReply
import com.phoenixcorp.founderfinder.domain.model.Thread
import kotlinx.coroutines.flow.Flow

interface ForumRepository {

    // ==================== NEW: FORUM METADATA ====================
    suspend fun getForum(category: String, forumId: String): Forum?

    // ==================== THREAD OPERATIONS ====================
    suspend fun createThread(thread: Thread): Result<String>
    suspend fun getThreadsByForum(category: String, forumId: String): List<Thread>
    suspend fun getThreadById(category: String, forumId: String, threadId: String): Thread?
    fun getThreadsFlow(category: String, forumId: String): Flow<List<Thread>>

    suspend fun toggleLike(
        category: String,
        forumId: String,
        threadId: String,
        userId: String
    ): Result<Unit>

    suspend fun toggleFavorite(
        category: String,
        forumId: String,
        threadId: String,
        userId: String,
        isFavorited: Boolean
    ): Result<Unit>

    // ==================== COMMENT OPERATIONS ====================
    suspend fun createComment(comment: Comment): Result<String>
    suspend fun getCommentById(commentId: String): Comment?
    fun getRepliesForComment(commentId: String): Flow<List<Comment>>

    // New methods from ThreadRepository
    suspend fun getCommentsForThread(
        category: String,
        forumId: String,
        threadId: String
    ): List<Comment>

    fun getCommentsFlowForThread(
        category: String,
        forumId: String,
        threadId: String
    ): Flow<List<Comment>>

    // Legacy methods
    suspend fun createPost(post: ForumPost): Result<String>
    suspend fun getPostsByCategory(category: String): List<ForumPost>
    suspend fun getPostsBySchool(school: String): List<ForumPost>
    suspend fun likePost(postId: String)
    fun getPostReplies(postId: String): Flow<List<ForumReply>>
}