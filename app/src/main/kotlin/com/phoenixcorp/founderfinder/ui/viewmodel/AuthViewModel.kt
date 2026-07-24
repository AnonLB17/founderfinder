package com.phoenixcorp.founderfinder.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    private val TAG = "AuthViewModel"

    fun getCurrentUser() = auth.currentUser

    fun registerUser(
        email: String,
        password: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user

                if (user != null) {
                    Log.d(TAG, "✅ User registered successfully: ${user.uid}")
                    onComplete(true, null)
                } else {
                    onComplete(false, "Registration failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Registration error", e)
                onComplete(false, e.message ?: "Registration failed")
            }
        }
    }

    fun signInUser(
        email: String,
        password: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                if (result.user != null) {
                    Log.d(TAG, "✅ Sign in successful")
                    onComplete(true, null)
                } else {
                    onComplete(false, "Sign in failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sign in error", e)
                onComplete(false, e.message ?: "Sign in failed")
            }
        }
    }

    /**
     * Signs the user out of Firebase Auth.
     * Call this before navigating to Splash so the splash auth-check
     * sees no currentUser and shows Get Started / Sign In.
     */
    fun signOut() {
        try {
            auth.signOut()
            Log.d(TAG, "✅ User signed out")
        } catch (e: Exception) {
            Log.e(TAG, "Sign out error", e)
        }
    }
}