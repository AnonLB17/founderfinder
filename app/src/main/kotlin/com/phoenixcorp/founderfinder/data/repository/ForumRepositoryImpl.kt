package com.phoenixcorp.founderfinder.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.domain.model.Comment
import com.phoenixcorp.founderfinder.domain.model.Forum
import com.phoenixcorp.founderfinder.domain.model.ForumPost
import com.phoenixcorp.founderfinder.domain.model.ForumReply
import com.phoenixcorp.founderfinder.domain.model.Thread
import com.phoenixcorp.founderfinder.domain.repository.ForumRepository
import com.phoenixcorp.founderfinder.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ForumRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val threadRepository: ThreadRepository
) : ForumRepository {

    // ==================== FORUM METADATA (Legacy Path Only) ====================

    override suspend fun getForum(category: String, forumId: String): Forum? {
        return try {
            val doc = firestore.collection("category")
                .document(category)
                .collection("forum")
                .document(forumId)
                .get()
                .await()

            if (doc.exists()) {
                Log.d("ForumRepository", "✅ Found forum in legacy path: category/$category/forum/$forumId")
                return doc.toObject(Forum::class.java)?.copy(
                    id = forumId,
                    category = category
                )
            } else {
                Log.w("ForumRepository", "Forum not found: category/$category/forum/$forumId")
                null
            }
        } catch (e: Exception) {
            Log.e("ForumRepository", "Error loading forum", e)
            null
        }
    }

    // ==================== THREAD OPERATIONS (Delegated) ====================

    override suspend fun createThread(thread: Thread): Result<String> =
        threadRepository.createThread(thread)

    override suspend fun getThreadsByForum(category: String, forumId: String): List<Thread> =
        threadRepository.getThreadsByForum(category, forumId)

    override suspend fun getThreadById(category: String, forumId: String, threadId: String): Thread? =
        threadRepository.getThreadById(category, forumId, threadId)

    override fun getThreadsFlow(category: String, forumId: String): Flow<List<Thread>> =
        threadRepository.getThreadsFlow(category, forumId)

    override suspend fun toggleLike(
        category: String,
        forumId: String,
        threadId: String,
        userId: String
    ): Result<Unit> = threadRepository.toggleLike(category, forumId, threadId, userId)

    override suspend fun toggleFavorite(
        category: String,
        forumId: String,
        threadId: String,
        userId: String,
        isFavorited: Boolean
    ): Result<Unit> = threadRepository.toggleFavorite(category, forumId, threadId, userId, isFavorited)

    override suspend fun createComment(comment: Comment): Result<String> =
        threadRepository.createComment(comment)

    override suspend fun getCommentById(commentId: String): Comment? =
        threadRepository.getCommentById(commentId)

    override fun getRepliesForComment(commentId: String): Flow<List<Comment>> =
        threadRepository.getRepliesForComment(commentId)

    // New methods from ThreadRepository interface
    override suspend fun getCommentsForThread(
        category: String,
        forumId: String,
        threadId: String
    ): List<Comment> = threadRepository.getCommentsForThread(category, forumId, threadId)

    override fun getCommentsFlowForThread(
        category: String,
        forumId: String,
        threadId: String
    ): Flow<List<Comment>> = threadRepository.getCommentsFlowForThread(category, forumId, threadId)

    // ==================== LEGACY FORUM POSTS ====================

    override suspend fun createPost(post: ForumPost): Result<String> {
        return Result.success("")
    }

    override suspend fun getPostsByCategory(category: String): List<ForumPost> {
        return emptyList()
    }

    override suspend fun getPostsBySchool(school: String): List<ForumPost> {
        return emptyList()
    }

    override suspend fun likePost(postId: String) {
        // TODO
    }

    override fun getPostReplies(postId: String): Flow<List<ForumReply>> {
        return kotlinx.coroutines.flow.flowOf(emptyList())
    }
}