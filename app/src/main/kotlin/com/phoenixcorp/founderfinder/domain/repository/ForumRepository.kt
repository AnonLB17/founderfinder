package com.phoenixcorp.founderfinder.domain.repository

import com.phoenixcorp.founderfinder.domain.model.Comment
import com.phoenixcorp.founderfinder.domain.model.ForumPost
import com.phoenixcorp.founderfinder.domain.model.ForumReply
import com.phoenixcorp.founderfinder.domain.model.Thread
import kotlinx.coroutines.flow.Flow

interface ForumRepository : ThreadRepository {

    // Posts
    suspend fun createPost(post: ForumPost): Result<String>
    suspend fun getPostsByCategory(category: String): List<ForumPost>
    suspend fun getPostsBySchool(school: String): List<ForumPost>
    suspend fun likePost(postId: String)

    // Comments / Replies (override from ThreadRepository)
    override suspend fun createComment(comment: Comment): Result<String>
    fun getPostReplies(postId: String): Flow<List<ForumReply>>

    // You can add more forum-specific methods here
}