package com.phoenixcorp.founderfinder.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
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
            } catch (e: FirebaseAuthException) {
                Log.e(TAG, "Registration FirebaseAuthException: ${e.errorCode}", e)
                val message = when (e.errorCode) {
                    "ERROR_EMAIL_ALREADY_IN_USE" -> "EMAIL_ALREADY_IN_USE"
                    else -> e.message ?: "Registration failed"
                }
                onComplete(false, message)
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

    fun sendPasswordResetEmail(
        email: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                Log.d(TAG, "✅ Password reset email sent to $email")
                onComplete(true, null)
            } catch (e: Exception) {
                Log.e(TAG, "Password reset error", e)
                onComplete(false, e.message ?: "Failed to send reset email")
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
