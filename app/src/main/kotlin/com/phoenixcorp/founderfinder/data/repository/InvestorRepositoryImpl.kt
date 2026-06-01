package com.phoenixcorp.founderfinder.data.repository

import com.google.firebase.firestore.FirebaseFirestore
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
            investorsCollection.get().await().toObjects(Investor::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getInvestorById(uid: String): Investor? {
        return investorsCollection.document(uid).get().await().toObject(Investor::class.java)
    }

    override suspend fun getInvestorMatchesForFounder(
        founderId: String,
        limit: Int
    ): List<Investor> {
        // You can enhance this with matching logic later
        return investorsCollection.limit(limit.toLong()).get().await().toObjects(Investor::class.java)
    }
}