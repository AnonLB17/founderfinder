package com.phoenixcorp.founderfinder.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoenixcorp.founderfinder.domain.model.UserProfile
import com.phoenixcorp.founderfinder.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val TAG = "ProfileViewModel"

    private val _userData = MutableStateFlow<UserProfile?>(null)
    val userData = _userData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    /**
     * Load full user profile from Firestore
     */
    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val profile = profileRepository.getProfile(userId)
                _userData.value = profile ?: UserProfile(userId = userId)

                Log.d(TAG, "✅ Profile loaded successfully for user: $userId")
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load profile"
                Log.e(TAG, "❌ Error loading profile for user: $userId", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update entire profile (used for editing)
     */
    fun updateProfile(updatedProfile: UserProfile) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val success = profileRepository.saveProfile(updatedProfile)
                if (success) {
                    _userData.value = updatedProfile
                    Log.d(TAG, "✅ Profile updated successfully")
                } else {
                    _errorMessage.value = "Failed to update profile"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update profile"
                Log.e(TAG, "❌ Update error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}