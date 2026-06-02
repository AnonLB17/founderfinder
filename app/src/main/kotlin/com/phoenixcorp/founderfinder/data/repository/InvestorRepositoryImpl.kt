package com.phoenixcorp.founderfinder.data.repository

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

    private val investorsCollection = firestore.collection("investors")

    override suspend fun searchInvestors(
        query: String,
        minInvestment: Long?,
        stages: List<StartupStage>?,
        focusAreas: List<String>?
    ): List<Investor> {
        return try {
            var queryRef: Query = investorsCollection

            // Basic text search (Firestore doesn't support full-text search natively)
            if (query.isNotBlank()) {
                queryRef = queryRef.whereEqualTo("name", query)
                // Note: For better search, consider using Algolia or Typesense later
            }

            // Filter by minimum investment range
            if (minInvestment != null) {
                queryRef = queryRef.whereGreaterThanOrEqualTo("investmentRangeMin", minInvestment.toString())
            }

            // TODO: Add more filters for stages and focusAreas when needed

            queryRef.get().await().toObjects(Investor::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getInvestorById(uid: String): Investor? {
        return try {
            investorsCollection.document(uid).get().await().toObject(Investor::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getInvestorMatchesForFounder(
        founderId: String,
        limit: Int
    ): List<Investor> {
        return try {
            investorsCollection.limit(limit.toLong()).get().await().toObjects(Investor::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}