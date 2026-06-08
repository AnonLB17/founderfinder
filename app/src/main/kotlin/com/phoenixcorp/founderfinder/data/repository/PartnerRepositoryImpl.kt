package com.phoenixcorp.founderfinder.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.domain.model.Partner
import com.phoenixcorp.founderfinder.domain.model.PartnershipType
import com.phoenixcorp.founderfinder.domain.model.User
import com.phoenixcorp.founderfinder.domain.repository.PartnerRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PartnerRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : PartnerRepository {

    private val profilesCollection = firestore.collection("profiles")

    override suspend fun searchPartners(
        query: String,
        school: String?,
        partnershipTypes: List<PartnershipType>?
    ): List<Partner> {
        return try {
            val snapshot = profilesCollection.get().await()
            val partners = mutableListOf<Partner>()

            for (doc in snapshot.documents) {
                val partnerDataDoc = profilesCollection
                    .document(doc.id)
                    .collection("partner")
                    .document("data")
                    .get()
                    .await()

                if (partnerDataDoc.exists()) {
                    val data = doc.data ?: continue

                    // Build full name
                    val firstName = data["firstName"] as? String ?: ""
                    val lastName = data["lastName"] as? String ?: ""
                    val fullName = listOfNotNull(firstName.ifBlank { null }, lastName.ifBlank { null })
                        .joinToString(" ")
                        .ifBlank { (data["name"] as? String) ?: "Partner" }

                    val user = doc.toObject(User::class.java)?.copy(
                        uid = doc.id,
                        name = fullName
                    ) ?: User(uid = doc.id, name = fullName)

                    // Map expertise → skills
                    val skillsList = when (val exp = partnerDataDoc.get("expertise")) {
                        is List<*> -> exp.filterIsInstance<String>()
                        is String -> exp.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        else -> emptyList()
                    }

                    val partner = Partner(
                        user = user,
                        partnershipType = emptyList(),
                        skills = skillsList,
                        lookingFor = partnerDataDoc.getString("lookingFor"),
                        availability = partnerDataDoc.getBoolean("availability") ?: true,
                        experienceYears = (partnerDataDoc.getLong("experienceYears") ?: 0L).toInt()
                    )
                    partners.add(partner)
                }
            }

            // Client-side filtering
            val q = query.lowercase().trim()
            if (q.isBlank()) partners else partners.filter { partner ->
                partner.user.name.lowercase().contains(q) ||
                        partner.skills.any { it.lowercase().contains(q) } ||
                        (partner.lookingFor?.lowercase()?.contains(q) == true)
            }

        } catch (e: Exception) {
            Log.e("PartnerRepository", "Search error", e)
            emptyList()
        }
    }

    override suspend fun getPartnerById(uid: String): Partner? {
        return try {
            val userDoc = profilesCollection.document(uid).get().await()
            val data = userDoc.data ?: return null

            val firstName = data["firstName"] as? String ?: ""
            val lastName = data["lastName"] as? String ?: ""
            val fullName = listOfNotNull(firstName.ifBlank { null }, lastName.ifBlank { null })
                .joinToString(" ")
                .ifBlank { (data["name"] as? String) ?: "Partner" }

            val user = userDoc.toObject(User::class.java)?.copy(
                uid = uid,
                name = fullName
            ) ?: User(uid = uid, name = fullName)

            val partnerData = profilesCollection.document(uid)
                .collection("partner")
                .document("data")
                .get()
                .await()

            val skillsList = when (val exp = partnerData.get("expertise")) {
                is List<*> -> exp.filterIsInstance<String>()
                is String -> exp.split(",").map { it.trim() }.filter { it.isNotBlank() }
                else -> emptyList()
            }

            Partner(
                user = user,
                partnershipType = emptyList(),
                skills = skillsList,
                lookingFor = partnerData.getString("lookingFor"),
                availability = partnerData.getBoolean("availability") ?: true,
                experienceYears = (partnerData.getLong("experienceYears") ?: 0L).toInt()
            )
        } catch (e: Exception) {
            Log.e("PartnerRepository", "Get by ID error", e)
            null
        }
    }
}