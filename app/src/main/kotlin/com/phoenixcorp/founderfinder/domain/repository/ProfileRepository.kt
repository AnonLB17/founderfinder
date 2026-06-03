package com.phoenixcorp.founderfinder.domain.repository

import com.phoenixcorp.founderfinder.domain.model.UserProfile

interface ProfileRepository {

    /**
     * Save or update the full user profile
     */
    suspend fun saveProfile(profile: UserProfile): Boolean

    /**
     * Get full user profile by userId
     */
    suspend fun getProfile(userId: String): UserProfile?

    /**
     * Update a specific field (useful for partial updates)
     */
    suspend fun updateProfileField(userId: String, field: String, value: Any): Boolean
}