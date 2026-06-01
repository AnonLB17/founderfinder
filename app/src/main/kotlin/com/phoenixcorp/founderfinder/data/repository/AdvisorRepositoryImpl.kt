package com.phoenixcorp.founderfinder.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.phoenixcorp.founderfinder.domain.model.Advisor
import com.phoenixcorp.founderfinder.domain.repository.AdvisorRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AdvisorRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : AdvisorRepository {

    private val advisorsCollection = firestore.collection("advisors")

    override suspend fun searchAdvisors(
        query: String,
        school: String?,
        expertise: List<String>?
    ): List<Advisor> {
        return try {
            var queryRef: Query = advisorsCollection

            // Filter by school if provided
            if (school != null) {
                queryRef = queryRef.whereEqualTo("user.school", school)
            }

            // You can add more filters here later
            // Example:
            // if (!expertise.isNullOrEmpty()) {
            //     queryRef = queryRef.whereArrayContainsAny("expertise", expertise)
            // }

            queryRef.get().await().toObjects(Advisor::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getAdvisorById(uid: String): Advisor? {
        return try {
            advisorsCollection.document(uid).get().await()
                .toObject(Advisor::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getFeaturedAdvisors(limit: Int): List<Advisor> {
        return try {
            advisorsCollection
                .whereEqualTo("availability", true)
                .limit(limit.toLong())
                .get().await()
                .toObjects(Advisor::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun updateAdvisorProfile(advisor: Advisor): Result<Unit> {
        return try {
            advisorsCollection.document(advisor.user.uid).set(advisor).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}