package com.phoenixcorp.founderfinder.domain.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String? = null,
    val school: String? = null,
    val bio: String? = null,
    val profileImageUrl: String? = null,

    val role: UserRole = UserRole.FOUNDER,

    /** NEW: Added for onboarding */
    val roleProfile: RoleProfile? = null,        // expertise + experience years

    val interests: List<String> = emptyList(),

    /** NEW: Helpful flags */
    val isOnboarded: Boolean = false,            // True after completing onboarding
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