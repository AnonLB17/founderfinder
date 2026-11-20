package com.phoenixcorp.founderfinder.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ConnectSocialsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val TAG = "ConnectSocialsViewModel"

    // UI State
    private val _linkedin = MutableStateFlow("")
    val linkedin = _linkedin.asStateFlow()

    private val _twitter = MutableStateFlow("")
    val twitter = _twitter.asStateFlow()

    private val _facebook = MutableStateFlow("")
    val facebook = _facebook.asStateFlow()

    private val _instagram = MutableStateFlow("")
    val instagram = _instagram.asStateFlow()

    private val _website = MutableStateFlow("")
    val website = _website.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun updateLinkedin(url: String) {
        _linkedin.value = url
    }

    fun updateTwitter(url: String) {
        _twitter.value = url
    }

    fun updateFacebook(url: String) {
        _facebook.value = url
    }

    fun updateInstagram(url: String) {
        _instagram.value = url
    }

    fun updateWebsite(url: String) {
        _website.value = url
    }

    fun saveSocials(userId: String, onComplete: (Boolean) -> Unit) {
        if (userId.isBlank()) {
            _errorMessage.value = "User ID is missing"
            onComplete(false)
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val data = mapOf(
                    "linkedinUrl" to _linkedin.value.trim(),
                    "twitterUrl" to _twitter.value.trim(),
                    "facebookUrl" to _facebook.value.trim(),
                    "instagramUrl" to _instagram.value.trim(),
                    "websiteUrl" to _website.value.trim(),
                    "updatedAt" to System.currentTimeMillis()
                )

                firestore.collection("profiles")
                    .document(userId)
                    .set(data, SetOptions.merge())
                    .await()

                Log.d(TAG, "✅ Social links saved successfully")
                _errorMessage.value = null
                onComplete(true)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to save social links", e)
                _errorMessage.value = "Failed to save social information"
                onComplete(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}