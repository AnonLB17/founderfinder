package com.phoenixcorp.founderfinder.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.domain.model.Comment
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

    // Delegate all thread operations to ThreadRepository
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

    // Existing Forum methods (Posts, etc.)
    override suspend fun createPost(post: ForumPost): Result<String> {
        // your existing implementation
        return Result.success("")
    }

    override suspend fun getPostsByCategory(category: String): List<ForumPost> {
        // your existing implementation
        return emptyList()
    }

    override suspend fun getPostsBySchool(school: String): List<ForumPost> {
        // your existing implementation
        return emptyList()
    }

    override suspend fun likePost(postId: String) {
        // your existing implementation
    }

    override fun getPostReplies(postId: String): Flow<List<ForumReply>> {
        // your existing implementation
        return kotlinx.coroutines.flow.flowOf(emptyList())
    }
}