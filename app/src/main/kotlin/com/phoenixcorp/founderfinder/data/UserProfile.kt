package com.phoenixcorp.founderfinder.data

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserProfile(
    val userId: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val bio: String? = null,
    val profilePicture: String? = null,
    val twitter: String? = null,
    val linkedin: String? = null,
    val facebook: String? = null,
    val instagram: String? = null,
    val ambitionStatement: String? = null,
    val founderStatus: Boolean? = false,
    val founderEntries: List<String>? = emptyList(),
    val educationEntries: List<String>? = emptyList(),
    val workExperiences: List<String>? = emptyList(),
    val industries: List<String>? = emptyList(),
    val organizations: List<String>? = emptyList(),
    val hasInvestorProfile: Boolean? = false,
    val investmentFirmName: String? = null,
    val firmLogo: String? = null,
    val professionalBackground: String? = null,
    val notableAchievements: String? = null,
    val preferredIndustries: List<String>? = emptyList(),
    val investmentStage: String? = null,
    val investmentRangeMin: String? = null,
    val investmentRangeMax: String? = null,
    val investmentApproach: String? = null,
    val strategicInvolvement: String? = null,
    val roiExpectations: String? = null,
    val portfolioCompanies: List<String>? = emptyList(),
    val successStories: List<String>? = emptyList(),
    val testimonials: List<String>? = emptyList(),
    val equityTerms: String? = null,
    val boardRole: String? = null,
    val returnTimeline: String? = null,
    val expertise: String? = null,
    val advisor: Boolean? = null,
    val email: String? = null,
    val experienceYears: Int? = null,
    val partner: Boolean? = null
)

// ====================== MAPPINGS ======================

/**
 * Convert UserProfile to domain.model.User
 */
fun UserProfile.toUser(): com.phoenixcorp.founderfinder.domain.model.User {
    val fullName = listOfNotNull(firstName, lastName)
        .joinToString(" ")
        .ifBlank { "Unknown User" }

    return com.phoenixcorp.founderfinder.domain.model.User(
        uid = this.userId ?: "",
        name = fullName,
        email = this.email,
        school = null,                    // Not stored in UserProfile yet
        bio = this.bio,
        profileImageUrl = this.profilePicture,
        role = com.phoenixcorp.founderfinder.domain.model.UserRole.FOUNDER, // Default - adjust as needed
        interests = this.industries ?: emptyList(),
        isVerified = false
    )
}

/**
 * Convert domain User back to UserProfile
 */
fun com.phoenixcorp.founderfinder.domain.model.User.toUserProfile(): UserProfile {
    val nameParts = this.name.split(" ")
    return UserProfile(
        userId = this.uid,
        firstName = nameParts.firstOrNull(),
        lastName = if (nameParts.size > 1) nameParts.drop(1).joinToString(" ") else null,
        email = this.email,
        bio = this.bio,
        profilePicture = this.profileImageUrl,
        industries = this.interests
    )
}