package com.phoenixcorp.founderfinder.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.phoenixcorp.founderfinder.domain.model.Investor
import com.phoenixcorp.founderfinder.domain.model.StartupStage
import com.phoenixcorp.founderfinder.domain.repository.InvestorRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class InvestorRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : InvestorRepository {

    // Main profiles collection
    private val profilesCollection = firestore.collection("profiles")

    override suspend fun searchInvestors(
        query: String,
        minInvestment: Long?,
        stages: List<StartupStage>?,
        focusAreas: List<String>?
    ): List<Investor> {
        return try {
            Log.d("InvestorRepositoryImpl", "Searching investors from profiles/investor/data")

            val profileSnapshot = profilesCollection.get().await()

            profileSnapshot.documents.mapNotNull { profileDoc ->
                val userId = profileDoc.id

                try {
                    val investorDataDoc = profilesCollection
                        .document(userId)
                        .collection("investor")
                        .document("data")
                        .get()
                        .await()

                    if (!investorDataDoc.exists()) return@mapNotNull null

                    val data = investorDataDoc.data ?: emptyMap<String, Any>()

                    val firstName = profileDoc.getString("firstName") ?: ""
                    val lastName = profileDoc.getString("lastName") ?: ""
                    val fullName = "$firstName $lastName".trim().ifBlank {
                        profileDoc.getString("name") ?: "Investor"
                    }

                    Investor(
                        userId = userId,
                        name = fullName,
                        email = profileDoc.getString("email"),
                        profilePicture = profileDoc.getString("profilePicture"),
                        industry = data["industry"] as? String ?: "",
                        philosophy = data["philosophy"] as? String ?: "",
                        preferredIndustries = data["preferredIndustries"] as? List<String> ?: emptyList(),
                        investmentStage = data["investmentStage"] as? String ?: "",
                        investmentRangeMin = data["investmentRangeMin"] as? String ?: "",
                        investmentRangeMax = data["investmentRangeMax"] as? String ?: "",
                        approachAndInvolvement = data["approachAndInvolvement"] as? String ?: "",
                        roiExpectations = data["roiExpectations"] as? String ?: "",
                        portfolioCompanies = data["portfolioCompanies"] as? List<String> ?: emptyList(),
                        testimonials = data["testimonials"] as? List<String> ?: emptyList(),
                        equityTerms = data["equityTerms"] as? String ?: "",
                        boardRole = data["boardRole"] as? String ?: "",
                        returnTimeline = data["returnTimeline"] as? String ?: "",
                        createdAt = (data["createdAt"] as? Long) ?: 0L
                    )
                } catch (e: Exception) {
                    Log.e("InvestorRepositoryImpl", "Error parsing investor for user $userId", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("InvestorRepositoryImpl", "Error searching investors", e)
            emptyList()
        }
    }

    override suspend fun getInvestorById(uid: String): Investor? {
        return try {
            val profileDoc = profilesCollection.document(uid).get().await()
            val investorDataDoc = profilesCollection
                .document(uid)
                .collection("investor")
                .document("data")
                .get()
                .await()

            if (!investorDataDoc.exists()) return null

            val data = investorDataDoc.data ?: emptyMap()
            val firstName = profileDoc.getString("firstName") ?: ""
            val lastName = profileDoc.getString("lastName") ?: ""
            val fullName = "$firstName $lastName".trim().ifBlank { profileDoc.getString("name") ?: "Investor" }

            Investor(
                userId = uid,
                name = fullName,
                email = profileDoc.getString("email"),
                profilePicture = profileDoc.getString("profilePicture"),
                industry = data["industry"] as? String ?: "",
                philosophy = data["philosophy"] as? String ?: "",
                preferredIndustries = data["preferredIndustries"] as? List<String> ?: emptyList(),
                investmentStage = data["investmentStage"] as? String ?: "",
                investmentRangeMin = data["investmentRangeMin"] as? String ?: "",
                investmentRangeMax = data["investmentRangeMax"] as? String ?: "",
                approachAndInvolvement = data["approachAndInvolvement"] as? String ?: "",
                roiExpectations = data["roiExpectations"] as? String ?: "",
                portfolioCompanies = data["portfolioCompanies"] as? List<String> ?: emptyList(),
                testimonials = data["testimonials"] as? List<String> ?: emptyList(),
                equityTerms = data["equityTerms"] as? String ?: "",
                boardRole = data["boardRole"] as? String ?: "",
                returnTimeline = data["returnTimeline"] as? String ?: "",
                createdAt = (data["createdAt"] as? Long) ?: 0L
            )
        } catch (e: Exception) {
            Log.e("InvestorRepositoryImpl", "Error getting investor by id", e)
            null
        }
    }

    override suspend fun getInvestorMatchesForFounder(
        founderId: String,
        limit: Int
    ): List<Investor> {
        // For now, return all investors (you can enhance with matching logic later)
        return searchInvestors(query = "", minInvestment = null, stages = null, focusAreas = null)
            .take(limit)
    }

    override suspend fun createInvestorProfile(investorData: Map<String, Any>): Result<Unit> {
        return try {
            val userId = investorData["userId"] as? String
                ?: return Result.failure(IllegalArgumentException("userId is required"))

            // Update main profile role
            profilesCollection.document(userId)
                .set(mapOf("role" to "INVESTOR"), com.google.firebase.firestore.SetOptions.merge())
                .await()

            // Save investor-specific data in subcollection
            profilesCollection.document(userId)
                .collection("investor")
                .document("data")
                .set(investorData, com.google.firebase.firestore.SetOptions.merge())
                .await()

            Log.d("InvestorRepositoryImpl", "Investor profile created/updated for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("InvestorRepositoryImpl", "Error creating investor profile", e)
            Result.failure(e)
        }
    }
}