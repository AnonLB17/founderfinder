package com.phoenixcorp.founderfinder.domain.model

data class ForumReply(
    val id: String,
    val postId: String,
    val authorId: String,
    val authorName: String,
    val content: String,
    val timestamp: Long,
    val likes: Int = 0
)