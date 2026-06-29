package com.phoenixcorp.founderfinder.domain.model

data class Comment(
    val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val creatorProfilePicture: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val parentId: String? = null,
    val depth: Int = 0,
    val isFavorited: Boolean? = false,
    val likes: Long? = 0,
    val likedBy: List<String> = emptyList(),

    val threadId: String = "",
    val forumId: String = "",
    val category: String = "marketpotential",

    val location: String? = null
) {
    // This helps with Firestore deserialization issues
    val isActuallyFavorited: Boolean
        get() = isFavorited == true
}