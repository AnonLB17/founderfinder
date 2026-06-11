package com.phoenixcorp.founderfinder.domain.model

import androidx.annotation.Keep

@Keep
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String? = null,
    val school: String? = null,
    val bio: String? = null,
    val profileImageUrl: String? = null,

    val role: UserRole = UserRole.FOUNDER,

    val roleProfile: RoleProfile? = null,

    val interests: List<String> = emptyList(),

    val isOnboarded: Boolean = false,
    val isVerified: Boolean = false,

    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis()
)

@Keep
enum class UserRole {
    FOUNDER,
    INVESTOR,
    ADVISOR,
    PARTNER,
    ORGANIZATION
}