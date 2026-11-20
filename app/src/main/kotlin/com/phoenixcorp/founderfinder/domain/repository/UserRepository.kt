package com.phoenixcorp.founderfinder.domain.repository

import com.phoenixcorp.founderfinder.domain.model.User
import com.phoenixcorp.founderfinder.domain.model.UserRole

interface UserRepository {
    suspend fun getCurrentUser(): User?
    suspend fun getUserById(uid: String): User?

    suspend fun updateUser(user: User): Result<Unit>

    /** Main method used after onboarding completion */
    suspend fun saveOnboardingData(user: User): Result<Unit>

    /** Flexible partial update (good for multi-step onboarding) */
    suspend fun updateUserProfile(uid: String, updates: Map<String, Any>): Result<Unit>

    suspend fun searchUsers(query: String, role: UserRole? = null, school: String? = null): List<User>
    suspend fun getUsersBySchool(school: String): List<User>
}