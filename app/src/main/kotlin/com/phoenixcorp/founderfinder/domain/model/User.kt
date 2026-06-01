package com.phoenixcorp.founderfinder.domain.model

data class User(
    val uid: String,
    val name: String,
    val email: String?,
    val school: String?,
    val bio: String? = null,
    val profileImageUrl: String? = null,
    val role: UserRole,
    val interests: List<String> = emptyList(),
    val isVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis()
)

enum class UserRole {
    FOUNDER,
    INVESTOR,
    ADVISOR,
    PARTNER
}