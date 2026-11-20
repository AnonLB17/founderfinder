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
class OrganizationsOfInterestViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val TAG = "OrganizationsOfInterestViewModel"

    // UI State
    private val _keyword = MutableStateFlow("")
    val keyword = _keyword.asStateFlow()

    private val _organizations = MutableStateFlow<List<String>>(emptyList())
    val organizations = _organizations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun updateKeyword(value: String) {
        _keyword.value = value
    }

    fun addOrganization() {
        val trimmed = _keyword.value.trim()
        if (trimmed.isNotBlank() && !_organizations.value.contains(trimmed)) {
            _organizations.value = _organizations.value + trimmed
            _keyword.value = ""  // Clear input after adding
        }
    }

    fun removeOrganization(org: String) {
        _organizations.value = _organizations.value.filter { it != org }
    }

    fun saveOrganizations(userId: String, onComplete: (Boolean) -> Unit) {
        if (userId.isBlank()) {
            _errorMessage.value = "User ID is missing"
            onComplete(false)
            return
        }

        if (_organizations.value.isEmpty()) {
            _errorMessage.value = "Please add at least one organization"
            onComplete(false)
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val data = mapOf(
                    "organizationsOfInterest" to _organizations.value,
                    "updatedAt" to System.currentTimeMillis()
                )

                firestore.collection("profiles")
                    .document(userId)
                    .set(data, SetOptions.merge())
                    .await()

                Log.d(TAG, "✅ Organizations of interest saved successfully")
                _errorMessage.value = null
                onComplete(true)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to save organizations", e)
                _errorMessage.value = "Failed to save organizations"
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