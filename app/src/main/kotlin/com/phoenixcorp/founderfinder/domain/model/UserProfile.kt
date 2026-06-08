package com.phoenixcorp.founderfinder.domain.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName

@Keep
data class UserProfile(
    val userId: String = "",

    val email: String? = null,

    // Basic Info
    val firstName: String? = null,
    val lastName: String? = null,
    val birthDate: String? = null,

    // Education
    val educationEntries: List<String> = emptyList(),

    // Work Experience
    val workExperiences: List<String> = emptyList(),

    // Founder Status
    val isFounder: Boolean = false,
    val founderEntries: List<String> = emptyList(),

    // Ambition
    val ambitionStatement: String? = null,

    // Role (Critical for Advisor/Partner search)
    val role: String = "FOUNDER",

    // Advisor / Partner specific (flattened)
    val expertise: String? = null,
    val experienceYears: Int? = null,
    val skills: List<String> = emptyList(),

    // Interests
    val industriesOfInterest: List<String> = emptyList(),
    val organizationsOfInterest: List<String> = emptyList(),

    // Socials
    val linkedinUrl: String? = null,
    val twitterUrl: String? = null,
    val facebookUrl: String? = null,
    val instagramUrl: String? = null,
    val websiteUrl: String? = null,

    // Profile
    val profilePicture: String? = null,

    // Timestamps
    @get:PropertyName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    @get:PropertyName("updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable {

    // Manual Parcelable implementation (minimal)
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: android.os.Parcel, flags: Int) {
        // Firebase + simple fields usually don't need full parceling for now
    }
}

// ====================== MAPPINGS ======================

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
        role = when (this.role.uppercase()) {
            "ADVISOR" -> UserRole.ADVISOR
            "PARTNER" -> UserRole.PARTNER
            "INVESTOR" -> UserRole.INVESTOR
            "ORGANIZATION" -> UserRole.ORGANIZATION
            else -> UserRole.FOUNDER
        },
        interests = this.industriesOfInterest,
        isVerified = false
    )
}

fun User.toUserProfile(): UserProfile {
    val nameParts = this.name.split(" ")
    return UserProfile(
        userId = this.uid,
        firstName = nameParts.firstOrNull(),
        lastName = if (nameParts.size > 1) nameParts.drop(1).joinToString(" ") else null,
        email = this.email,
        ambitionStatement = this.bio,
        profilePicture = this.profileImageUrl,
        industriesOfInterest = this.interests,
        role = when (this.role) {
            UserRole.ADVISOR -> "ADVISOR"
            UserRole.PARTNER -> "PARTNER"
            UserRole.INVESTOR -> "INVESTOR"
            UserRole.ORGANIZATION -> "ORGANIZATION"
            else -> "FOUNDER"
        }
    )
}