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
class IndustriesOfInterestViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val TAG = "IndustriesOfInterestViewModel"

    // UI State
    private val _keyword = MutableStateFlow("")
    val keyword = _keyword.asStateFlow()

    private val _industries = MutableStateFlow<List<String>>(emptyList())
    val industries = _industries.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun updateKeyword(value: String) {
        _keyword.value = value
    }

    fun addIndustry() {
        val trimmed = _keyword.value.trim()
        if (trimmed.isNotBlank() && !_industries.value.contains(trimmed)) {
            _industries.value = _industries.value + trimmed
            _keyword.value = ""  // Clear input after adding
        }
    }

    fun removeIndustry(industry: String) {
        _industries.value = _industries.value.filter { it != industry }
    }

    fun saveIndustries(userId: String, onComplete: (Boolean) -> Unit) {
        if (userId.isBlank()) {
            _errorMessage.value = "User ID is missing"
            onComplete(false)
            return
        }

        if (_industries.value.isEmpty()) {
            _errorMessage.value = "Please add at least one industry"
            onComplete(false)
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val data = mapOf(
                    "industriesOfInterest" to _industries.value,
                    "updatedAt" to System.currentTimeMillis()
                )

                firestore.collection("profiles")
                    .document(userId)
                    .set(data, SetOptions.merge())
                    .await()

                Log.d(TAG, "✅ Industries of interest saved successfully")
                _errorMessage.value = null
                onComplete(true)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to save industries", e)
                _errorMessage.value = "Failed to save industries"
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