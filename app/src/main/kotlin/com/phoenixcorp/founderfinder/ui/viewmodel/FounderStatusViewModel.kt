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
class FounderStatusViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val TAG = "FounderStatusViewModel"

    // UI State
    private val _isFounder = MutableStateFlow(false)
    val isFounder = _isFounder.asStateFlow()

    private val _startupName = MutableStateFlow("")
    val startupName = _startupName.asStateFlow()

    private val _startupStage = MutableStateFlow("")
    val startupStage = _startupStage.asStateFlow()

    private val _founderEntries = MutableStateFlow<List<String>>(emptyList())
    val founderEntries = _founderEntries.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun setFounderStatus(isFounder: Boolean) {
        _isFounder.value = isFounder
    }

    fun updateStartupName(name: String) {
        _startupName.value = name
    }

    fun updateStartupStage(stage: String) {
        _startupStage.value = stage
    }

    fun addFounderEntry() {
        if (_startupName.value.isNotBlank() && _startupStage.value.isNotBlank()) {
            val entry = "${_startupName.value} - ${_startupStage.value}"
            _founderEntries.value = _founderEntries.value + entry

            // Clear fields
            _startupName.value = ""
            _startupStage.value = ""
        }
    }

    fun saveFounderStatus(userId: String, onComplete: (Boolean) -> Unit) {
        if (userId.isBlank()) {
            _errorMessage.value = "User ID is missing"
            onComplete(false)
            return
        }

        if (_isFounder.value && _founderEntries.value.isEmpty()) {
            _errorMessage.value = "Please add at least one founder entry"
            onComplete(false)
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val data = mapOf(
                    "isFounder" to _isFounder.value,
                    "founderEntries" to _founderEntries.value,
                    "updatedAt" to System.currentTimeMillis()
                )

                firestore.collection("profiles")
                    .document(userId)
                    .set(data, SetOptions.merge())
                    .await()

                Log.d(TAG, "✅ Founder status saved successfully")
                _errorMessage.value = null
                onComplete(true)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to save founder status", e)
                _errorMessage.value = "Failed to save founder information"
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