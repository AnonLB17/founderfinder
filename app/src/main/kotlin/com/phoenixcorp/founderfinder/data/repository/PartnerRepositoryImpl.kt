package com.phoenixcorp.founderfinder.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.phoenixcorp.founderfinder.domain.model.Partner
import com.phoenixcorp.founderfinder.domain.model.PartnershipType
import com.phoenixcorp.founderfinder.domain.repository.PartnerRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PartnerRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : PartnerRepository {

    private val partnersCollection = firestore.collection("partners")

    override suspend fun searchPartners(
        query: String,
        school: String?,
        partnershipTypes: List<PartnershipType>?
    ): List<Partner> {
        return try {
            var queryRef: Query = partnersCollection

            if (school != null) {
                queryRef = queryRef.whereEqualTo("user.school", school)
            }

            queryRef.get().await().toObjects(Partner::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getPartnerById(uid: String): Partner? {
        return try {
            partnersCollection.document(uid).get().await()
                .toObject(Partner::class.java)
        } catch (e: Exception) {
            null
        }
    }
}