package com.phoenixcorp.founderfinder.domain.model

data class ForumPost(
    val id: String,
    val authorId: String,
    val authorName: String,
    val school: String?,
    val category: String,
    val title: String,
    val content: String,
    val timestamp: Long,
    val likes: Int = 0,
    val commentCount: Int = 0,
    val tags: List<String> = emptyList()
)