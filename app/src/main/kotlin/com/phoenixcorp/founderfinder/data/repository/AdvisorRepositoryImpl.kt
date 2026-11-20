package com.phoenixcorp.founderfinder.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.domain.model.Advisor
import com.phoenixcorp.founderfinder.domain.model.User
import com.phoenixcorp.founderfinder.domain.repository.AdvisorRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AdvisorRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : AdvisorRepository {

    private val profilesCollection = firestore.collection("profiles")

    override suspend fun searchAdvisors(
        query: String,
        school: String?,
        expertise: List<String>?
    ): List<Advisor> {
        return try {
            val snapshot = profilesCollection.get().await()
            val advisors = mutableListOf<Advisor>()

            for (doc in snapshot.documents) {
                val advisorDataDoc = profilesCollection
                    .document(doc.id)
                    .collection("advisor")
                    .document("data")
                    .get()
                    .await()

                if (advisorDataDoc.exists()) {
                    val profileData = doc.data ?: continue

                    // Build full name
                    val firstName = profileData["firstName"] as? String ?: ""
                    val lastName = profileData["lastName"] as? String ?: ""
                    val fullName = listOfNotNull(firstName.ifBlank { null }, lastName.ifBlank { null })
                        .joinToString(" ")
                        .ifBlank { (profileData["name"] as? String) ?: "Advisor" }

                    // CRITICAL: Pull profile picture from main profile
                    val profilePicture = profileData["profilePicture"] as? String

                    val user = User(
                        uid = doc.id,
                        name = fullName,
                        email = profileData["email"] as? String,
                        profileImageUrl = profilePicture,   // ← This was missing
                        bio = profileData["ambitionStatement"] as? String
                    )

                    // Handle expertise (String or List)
                    val expertiseList = when (val exp = advisorDataDoc.get("expertise")) {
                        is List<*> -> exp.filterIsInstance<String>()
                        is String -> exp.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        else -> emptyList()
                    }

                    val advisor = Advisor(
                        user = user,
                        expertise = expertiseList,
                        experienceYears = (advisorDataDoc.getLong("experienceYears") ?: 0L).toInt(),
                        rating = 4.5f,
                        reviewCount = 0,
                        availability = true,
                        hourlyRate = null,
                        linkedinUrl = null,
                        company = null,
                        title = null
                    )
                    advisors.add(advisor)
                }
            }

            // Client-side filtering
            val q = query.lowercase().trim()
            if (q.isBlank()) advisors else advisors.filter { advisor ->
                advisor.user.name.lowercase().contains(q) ||
                        advisor.expertise.any { it.lowercase().contains(q) }
            }

        } catch (e: Exception) {
            Log.e("AdvisorRepository", "Search error", e)
            emptyList()
        }
    }

    // ... (getAdvisorById and other methods can stay the same or be updated similarly)
    override suspend fun getAdvisorById(uid: String): Advisor? {
        return try {
            val userDoc = profilesCollection.document(uid).get().await()
            val profileData = userDoc.data ?: return null

            val firstName = profileData["firstName"] as? String ?: ""
            val lastName = profileData["lastName"] as? String ?: ""
            val fullName = listOfNotNull(firstName.ifBlank { null }, lastName.ifBlank { null })
                .joinToString(" ")
                .ifBlank { (profileData["name"] as? String) ?: "Advisor" }

            val profilePicture = profileData["profilePicture"] as? String

            val user = User(
                uid = uid,
                name = fullName,
                email = profileData["email"] as? String,
                profileImageUrl = profilePicture,
                bio = profileData["ambitionStatement"] as? String
            )

            val advisorData = profilesCollection.document(uid)
                .collection("advisor")
                .document("data")
                .get()
                .await()

            val expertiseList = when (val exp = advisorData.get("expertise")) {
                is List<*> -> exp.filterIsInstance<String>()
                is String -> exp.split(",").map { it.trim() }.filter { it.isNotBlank() }
                else -> emptyList()
            }

            Advisor(
                user = user,
                expertise = expertiseList,
                experienceYears = (advisorData.getLong("experienceYears") ?: 0L).toInt(),
                rating = 4.5f,
                reviewCount = 0,
                availability = true,
                hourlyRate = null,
                linkedinUrl = null,
                company = null,
                title = null
            )
        } catch (e: Exception) {
            Log.e("AdvisorRepository", "Get by ID error", e)
            null
        }
    }

    // ... keep the rest of your methods unchanged
    override suspend fun getFeaturedAdvisors(limit: Int): List<Advisor> {
        return searchAdvisors(query = "", school = null, expertise = null)
            .take(limit)
    }

    override suspend fun updateAdvisorProfile(advisor: Advisor): Result<Unit> {
        return try {
            profilesCollection.document(advisor.user.uid)
                .set(advisor.user, com.google.firebase.firestore.SetOptions.merge())
                .await()

            profilesCollection.document(advisor.user.uid)
                .collection("advisor")
                .document("data")
                .set(
                    mapOf(
                        "expertise" to advisor.expertise,
                        "experienceYears" to advisor.experienceYears
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AdvisorRepository", "Update error", e)
            Result.failure(e)
        }
    }
}