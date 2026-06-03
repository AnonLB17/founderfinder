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
class WorkExperienceViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val TAG = "WorkExperienceViewModel"

    // UI State
    private val _jobTitle = MutableStateFlow("")
    val jobTitle = _jobTitle.asStateFlow()

    private val _company = MutableStateFlow("")
    val company = _company.asStateFlow()

    private val _yearsOfExperience = MutableStateFlow("")
    val yearsOfExperience = _yearsOfExperience.asStateFlow()

    private val _workExperiences = MutableStateFlow<List<String>>(emptyList())
    val workExperiences = _workExperiences.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun updateJobTitle(value: String) {
        _jobTitle.value = value
    }

    fun updateCompany(value: String) {
        _company.value = value
    }

    fun updateYearsOfExperience(value: String) {
        _yearsOfExperience.value = value
    }

    fun addWorkExperience() {
        val years = _yearsOfExperience.value.toIntOrNull() ?: 0
        if (_jobTitle.value.isNotBlank() && _company.value.isNotBlank()) {
            val entry = "${_jobTitle.value} at ${_company.value} ($years years)"
            _workExperiences.value = _workExperiences.value + entry

            // Clear fields
            _jobTitle.value = ""
            _company.value = ""
            _yearsOfExperience.value = ""
        }
    }

    fun saveWorkExperience(userId: String, onComplete: (Boolean) -> Unit) {
        if (userId.isBlank()) {
            _errorMessage.value = "User ID is missing"
            onComplete(false)
            return
        }

        if (_workExperiences.value.isEmpty()) {
            _errorMessage.value = "Please add at least one work experience"
            onComplete(false)
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val data = mapOf(
                    "workExperiences" to _workExperiences.value,
                    "updatedAt" to System.currentTimeMillis()
                )

                firestore.collection("profiles")
                    .document(userId)
                    .set(data, SetOptions.merge())
                    .await()

                Log.d(TAG, "✅ Work experience saved successfully")
                _errorMessage.value = null
                onComplete(true)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to save work experience", e)
                _errorMessage.value = "Failed to save work experience"
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