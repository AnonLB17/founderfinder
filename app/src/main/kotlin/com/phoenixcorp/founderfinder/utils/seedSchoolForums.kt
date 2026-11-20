package com.phoenixcorp.founderfinder.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

suspend fun seedSchoolForums() {
    val firestore = FirebaseFirestore.getInstance()
    val institutions = listOf(
        "ubc", "stanford", "mit", "harvard", "ucla", "utoronto", "mcgill", "unam"
    )
    try {
        institutions.forEach { school ->
            val forumRef = firestore.collection("category")
                .document("institutions")
                .collection("forum")
                .document(school)
            val existingForum = forumRef.get().await()
            if (!existingForum.exists()) {
                val forumData = mapOf(
                    "name" to school.uppercase(),
                    "description" to "Forum for $school",
                    "imageUrl" to "https://via.placeholder.com/150",
                    "creatorId" to "system",
                    "creatorName" to "System",
                    "timestamp" to System.currentTimeMillis()
                )
                forumRef.set(forumData).await()
                Log.d("SeedForums", "Created forum for $school")
            } else {
                Log.d("SeedForums", "Forum for $school already exists")
            }
        }
    } catch (e: Exception) {
        Log.e("SeedForums", "Error seeding forums: ${e.message}", e)
    }
}