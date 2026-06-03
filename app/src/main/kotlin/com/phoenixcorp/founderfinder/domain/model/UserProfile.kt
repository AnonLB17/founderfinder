// domain/model/UserProfile.kt
package com.phoenixcorp.founderfinder.domain.model

data class UserProfile(
    val userId: String = "",
    val email: String? = null,

    // Basic Info
    val firstName: String? = null,
    val lastName: String? = null,
    val birthDate: String? = null,           // MM/DD/YYYY

    // Education
    val educationEntries: List<String> = emptyList(),

    // Work Experience
    val workExperiences: List<String> = emptyList(),

    // Founder Status
    val isFounder: Boolean = false,
    val founderEntries: List<String> = emptyList(),

    // Ambition
    val ambitionStatement: String? = null,

    // Socials (matching Firestore field names)
    val linkedinUrl: String? = null,
    val twitterUrl: String? = null,
    val facebookUrl: String? = null,
    val instagramUrl: String? = null,
    val websiteUrl: String? = null,

    // Interests (matching Firestore field names)
    val industriesOfInterest: List<String> = emptyList(),
    val organizationsOfInterest: List<String> = emptyList(),

    // Public Appearance
    val profilePicture: String? = null,

    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// ====================== MAPPINGS ======================

/**
 * Convert UserProfile to domain.model.User (if you have a separate User class)
 */
fun UserProfile.toUser(): User {
    val fullName = listOfNotNull(firstName, lastName)
        .joinToString(" ")
        .ifBlank { "Unknown User" }

    return User(
        uid = this.userId,
        name = fullName,
        email = this.email,
        school = null,
        bio = this.ambitionStatement,
        profileImageUrl = this.profilePicture,
        role = UserRole.FOUNDER,
        interests = this.industriesOfInterest,
        isVerified = false
    )
}

/**
 * Convert domain User back to UserProfile
 */
fun User.toUserProfile(): UserProfile {
    val nameParts = this.name.split(" ")
    return UserProfile(
        userId = this.uid,
        firstName = nameParts.firstOrNull(),
        lastName = if (nameParts.size > 1) nameParts.drop(1).joinToString(" ") else null,
        email = this.email,
        ambitionStatement = this.bio,
        profilePicture = this.profileImageUrl,
        industriesOfInterest = this.interests
        // Other fields remain default for now
    )
}