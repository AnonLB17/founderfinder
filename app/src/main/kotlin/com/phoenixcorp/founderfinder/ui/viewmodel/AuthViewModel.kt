package com.phoenixcorp.founderfinder.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState = _authState.asStateFlow()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun registerUser(
        email: String,
        password: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                if (result.user != null) {
                    firestore.collection("profiles")
                        .document(result.user!!.uid)
                        .set(mapOf("userId" to result.user!!.uid, "email" to email))
                        .await()

                    onComplete(true, null)
                } else {
                    onComplete(false, "Registration failed")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false, e.message ?: "Registration failed")
            }
        }
    }

    fun getCurrentUser() = auth.currentUser

    /**
     * Saves basic user information (First Name, Last Name, Birth Date)
     */
    fun saveUserInfo(
        userId: String,
        firstName: String,
        lastName: String,
        birthDate: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val userData = mapOf(
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "birthDate" to birthDate,
                    "lastUpdated" to System.currentTimeMillis()
                )

                firestore.collection("profiles")
                    .document(userId)
                    .set(userData, com.google.firebase.firestore.SetOptions.merge())
                    .await()

                firestore.collection("users")
                    .document(userId)
                    .set(userData, com.google.firebase.firestore.SetOptions.merge())
                    .await()

                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("AuthViewModel", "Error saving user info: ${e.message}")
                onComplete(false)
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
                    onComplete(true, null)
                } else {
                    onComplete(false, "Sign in failed")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false, e.message ?: "Authentication failed")
            }
        }
    }

    /**
     * Saves user's education information
     */
    fun saveEducation(
        userId: String,
        educationEntries: List<String>,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val educationData = hashMapOf(
                    "education" to educationEntries,
                    "lastUpdated" to System.currentTimeMillis()
                )

                firestore.collection("profiles")
                    .document(userId)
                    .set(educationData, com.google.firebase.firestore.SetOptions.merge())
                    .await()

                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    /**
     * Saves user's work experience
     */
    fun saveWorkExperience(
        userId: String,
        workExperiences: List<String>,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val data = mapOf(
                    "workExperiences" to workExperiences,
                    "lastUpdated" to System.currentTimeMillis()
                )

                firestore.collection("profiles")
                    .document(userId)
                    .set(data, com.google.firebase.firestore.SetOptions.merge())
                    .await()

                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    /**
     * Saves user's founder status
     */
    fun saveFounderStatus(
        userId: String,
        isFounder: Boolean,
        founderEntries: List<String>,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val data = mapOf(
                    "isFounder" to isFounder,
                    "founderEntries" to founderEntries,
                    "lastUpdated" to System.currentTimeMillis()
                )

                firestore.collection("users")
                    .document(userId)
                    .set(data, com.google.firebase.firestore.SetOptions.merge())
                    .await()

                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    /**
     * Saves user's Industries of Interest
     */
    fun saveIndustriesOfInterest(
        userId: String,
        industries: List<String>,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val data = mapOf(
                    "industriesOfInterest" to industries,
                    "lastUpdated" to System.currentTimeMillis()
                )

                firestore.collection("profiles")   // or "users" depending on your structure
                    .document(userId)
                    .set(data, com.google.firebase.firestore.SetOptions.merge())
                    .await()

                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    /**
     * Saves user's Industries of Interest
     */
    fun saveOrganizationsOfInterest(
        userId: String,
        organizations: List<String>,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val data = mapOf(
                    "organizationsOfInterest" to organizations,
                    "lastUpdated" to System.currentTimeMillis()
                )

                firestore.collection("profiles")
                    .document(userId)
                    .set(data, com.google.firebase.firestore.SetOptions.merge())
                    .await()

                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    /**
     * Saves user's social links
     */
    fun saveSocials(
        userId: String,
        linkedin: String,
        twitter: String,
        facebook: String,
        instagram: String,
        website: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val socialData = hashMapOf(
                    "linkedinUrl" to linkedin,
                    "twitterUrl" to twitter,
                    "facebookUrl" to facebook,
                    "instagramUrl" to instagram,
                    "websiteUrl" to website,
                    "lastUpdated" to System.currentTimeMillis()
                )

                firestore.collection("profiles")
                    .document(userId)
                    .set(socialData, com.google.firebase.firestore.SetOptions.merge())
                    .await()

                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    /**
     * Saves or updates the user's ambition statement
     */
    fun saveAmbitionStatement(
        userId: String,
        ambitionStatement: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val userData = hashMapOf(
                    "ambitionStatement" to ambitionStatement,
                    "lastUpdated" to System.currentTimeMillis()
                )

                firestore.collection("profiles")
                    .document(userId)
                    .set(userData, com.google.firebase.firestore.SetOptions.merge())
                    .await()

                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }
}

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}