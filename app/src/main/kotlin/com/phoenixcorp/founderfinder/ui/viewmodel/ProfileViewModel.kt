package com.phoenixcorp.founderfinder.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.phoenixcorp.founderfinder.domain.model.UserProfile
import com.phoenixcorp.founderfinder.domain.model.toUser
import com.phoenixcorp.founderfinder.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _userData = MutableStateFlow<UserProfile?>(null)
    val userData = _userData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    /**
     * Load full user profile (including all onboarding data)
     */
    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val basicUser = userRepository.getUserById(userId)
                val fullProfile = fetchFullProfile(userId)

                _userData.value = fullProfile?.copy(
                    firstName = fullProfile.firstName ?: basicUser?.name?.split(" ")?.firstOrNull(),
                    lastName = fullProfile.lastName ?: basicUser?.name?.split(" ")?.drop(1)?.joinToString(" ")
                ) ?: UserProfile(userId = userId)

            } catch (e: Exception) {
                _errorMessage.value = "Failed to load profile: ${e.message}"
                Log.e("ProfileViewModel", "Error loading profile", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchFullProfile(userId: String): UserProfile? {
        return try {
            val firestore: FirebaseFirestore = Firebase.firestore
            val document = firestore.collection("profiles")
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                document.toObject(UserProfile::class.java)?.copy(userId = userId)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Error fetching full profile", e)
            null
        }
    }

    fun updateProfile(updatedProfile: UserProfile) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userToUpdate = updatedProfile.toUser()
                userRepository.updateUser(userToUpdate)
                _userData.value = updatedProfile
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update profile: ${e.message}"
                Log.e("ProfileViewModel", "Update error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}