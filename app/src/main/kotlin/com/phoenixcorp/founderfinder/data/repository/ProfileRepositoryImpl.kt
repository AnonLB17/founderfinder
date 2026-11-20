package com.phoenixcorp.founderfinder.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.phoenixcorp.founderfinder.domain.model.UserProfile
import com.phoenixcorp.founderfinder.domain.repository.ProfileRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ProfileRepository {

    private val TAG = "ProfileRepositoryImpl"

    override suspend fun saveProfile(profile: UserProfile): Boolean {
        val userId = profile.userId.ifBlank { return false }

        return try {
            val data = mapOf(
                "userId" to userId,
                "email" to (profile.email ?: ""),
                "firstName" to (profile.firstName ?: ""),
                "lastName" to (profile.lastName ?: ""),
                "birthDate" to (profile.birthDate ?: ""),

                "educationEntries" to profile.educationEntries,
                "workExperiences" to profile.workExperiences,

                "isFounder" to profile.isFounder,
                "founderEntries" to profile.founderEntries,

                "ambitionStatement" to (profile.ambitionStatement ?: ""),

                // Socials
                "linkedinUrl" to (profile.linkedinUrl ?: ""),
                "twitterUrl" to (profile.twitterUrl ?: ""),
                "facebookUrl" to (profile.facebookUrl ?: ""),
                "instagramUrl" to (profile.instagramUrl ?: ""),
                "websiteUrl" to (profile.websiteUrl ?: ""),

                // Interests
                "industriesOfInterest" to profile.industriesOfInterest,
                "organizationsOfInterest" to profile.organizationsOfInterest,

                // Public Appearance
                "profilePicture" to (profile.profilePicture ?: ""),

                "updatedAt" to System.currentTimeMillis()
            )

            firestore.collection("profiles")
                .document(userId)
                .set(data, SetOptions.merge())
                .await()

            Log.d(TAG, "✅ Profile saved successfully for user: $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save profile for user $userId", e)
            false
        }
    }

    override suspend fun getProfile(userId: String): UserProfile? {
        return try {
            val document = firestore.collection("profiles")
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                document.toObject(UserProfile::class.java)?.copy(userId = userId)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get profile for user $userId", e)
            null
        }
    }

    override suspend fun updateProfileField(userId: String, field: String, value: Any): Boolean {
        return try {
            firestore.collection("profiles")
                .document(userId)
                .set(
                    mapOf(field to value, "updatedAt" to System.currentTimeMillis()),
                    SetOptions.merge()
                )
                .await()

            Log.d(TAG, "✅ Updated field '$field' for user: $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update field '$field'", e)
            false
        }
    }
}