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
class EducationViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val TAG = "EducationViewModel"

    // UI State
    private val _highestEducation = MutableStateFlow("")
    val highestEducation = _highestEducation.asStateFlow()

    private val _areaOfStudy = MutableStateFlow("")
    val areaOfStudy = _areaOfStudy.asStateFlow()

    private val _institution = MutableStateFlow("")
    val institution = _institution.asStateFlow()

    private val _educationEntries = MutableStateFlow<List<String>>(emptyList())
    val educationEntries = _educationEntries.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun updateHighestEducation(value: String) {
        _highestEducation.value = value
    }

    fun updateAreaOfStudy(value: String) {
        _areaOfStudy.value = value
    }

    fun updateInstitution(value: String) {
        _institution.value = value
    }

    fun addEducationEntry() {
        val entry = "${_highestEducation.value} in ${_areaOfStudy.value} from ${_institution.value}"
        if (entry.length > 20) {  // basic validation
            _educationEntries.value = _educationEntries.value + entry
            // Clear fields after adding
            _highestEducation.value = ""
            _areaOfStudy.value = ""
            _institution.value = ""
        }
    }

    fun saveEducation(userId: String, onComplete: (Boolean) -> Unit) {
        if (userId.isBlank()) {
            _errorMessage.value = "User ID is missing"
            onComplete(false)
            return
        }

        if (_educationEntries.value.isEmpty()) {
            _errorMessage.value = "Please add at least one education entry"
            onComplete(false)
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val data = mapOf(
                    "educationEntries" to _educationEntries.value,
                    "updatedAt" to System.currentTimeMillis()
                )

                firestore.collection("profiles")
                    .document(userId)
                    .set(data, SetOptions.merge())
                    .await()

                Log.d(TAG, "✅ Education data saved successfully")
                _errorMessage.value = null
                onComplete(true)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to save education data", e)
                _errorMessage.value = "Failed to save education information"
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