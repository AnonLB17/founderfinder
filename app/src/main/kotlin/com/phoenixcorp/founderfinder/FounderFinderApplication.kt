package com.phoenixcorp.founderfinder

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FounderFinderApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)

        setupPersistentAuth()
        setupFirestore()
    }

    private fun setupPersistentAuth() {
        val auth = FirebaseAuth.getInstance()

        // This is the most reliable way to keep the user signed in
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                Log.d("Auth", "✅ User signed in persistently: ${user.uid} | Email: ${user.email}")
            } else {
                Log.w("Auth", "⚠️ No signed-in user detected")
            }
        }

        // Optional refresh - make it non-blocking and ignore network errors
        auth.currentUser?.let { user ->
            user.reload()
                .addOnSuccessListener {
                    Log.d("Auth", "✅ Auth session refreshed successfully")
                }
                .addOnFailureListener { e ->
                    Log.w("Auth", "⚠️ Could not refresh auth session (network or other issue): ${e.message}")
                    // This is normal in some cases - we still have the persisted session
                }
        }

        Log.d("Auth", "Firebase Auth persistence initialized")
    }

    private fun setupFirestore() {
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()

            FirebaseFirestore.getInstance().firestoreSettings = settings
            Log.d("Firestore", "✅ Offline persistence enabled")
        } catch (e: Exception) {
            Log.e("Firestore", "Failed to configure Firestore", e)
        }
    }
}