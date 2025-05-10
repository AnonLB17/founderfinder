package com.phoenixcorp.founderfinder.data

import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.phoenixcorp.founderfinder.ui.screens.Organization
import kotlinx.coroutines.tasks.await
import java.io.File

interface OrganizationRepository {
    suspend fun getOrganizations(): List<Organization>
    suspend fun saveOrganization(organization: Organization)
}

class OrganizationRepositoryImpl : OrganizationRepository {
    private val db = Firebase.firestore
    private val storage = Firebase.storage
    private val TAG = "OrganizationRepository"

    override suspend fun getOrganizations(): List<Organization> {
        return try {
            val result = db.collection("organizations").get().await()
            Log.d(TAG, "Fetched ${result.size()} organizations from Firestore")
            result.documents.mapNotNull { doc ->
                Organization(
                    name = doc.getString("name") ?: "",
                    description = doc.getString("description") ?: "",
                    imageUri = doc.getString("imageUrl")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching organizations: ${e.message}")
            emptyList()
        }
    }

    override suspend fun saveOrganization(organization: Organization) {
        try {
            val storageRef = storage.reference
            val imageUrl = organization.imageUri?.let { uri ->
                val file = File(Uri.parse(uri).path ?: throw IllegalStateException("Invalid image URI"))
                val imageRef = storageRef.child("images/${organization.name}_${System.currentTimeMillis()}.jpg")
                imageRef.putFile(Uri.fromFile(file)).await()
                val url = imageRef.downloadUrl.await().toString()
                Log.d(TAG, "Image uploaded to: $url")
                url
            }

            val orgData = hashMapOf(
                "name" to organization.name,
                "description" to organization.description,
                "imageUrl" to (imageUrl ?: "")
            )
            db.collection("organizations").add(orgData).await()
            Log.d(TAG, "Organization saved: ${organization.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving organization: ${e.message}")
            throw e // Re-throw to let ViewModel handle it
        }
    }
}