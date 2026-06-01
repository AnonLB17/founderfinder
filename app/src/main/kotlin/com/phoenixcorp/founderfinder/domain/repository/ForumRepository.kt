package com.phoenixcorp.founderfinder.domain.repository

import com.phoenixcorp.founderfinder.domain.model.ForumPost
import com.phoenixcorp.founderfinder.domain.model.ForumReply
import kotlinx.coroutines.flow.Flow

interface ForumRepository {
    suspend fun createPost(post: ForumPost): Result<String>
    suspend fun getPostsByCategory(category: String): List<ForumPost>
    suspend fun getPostsBySchool(school: String): List<ForumPost>
    fun getPostReplies(postId: String): Flow<List<ForumReply>>
    suspend fun likePost(postId: String)
    suspend fun createReply(reply: ForumReply): Result<Unit>
}