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
class AmbitionStatementViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val TAG = "AmbitionStatementViewModel"

    // UI State
    private val _ambitionStatement = MutableStateFlow("")
    val ambitionStatement = _ambitionStatement.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun updateAmbitionStatement(text: String) {
        _ambitionStatement.value = text
    }

    fun saveAmbitionStatement(userId: String, onComplete: (Boolean) -> Unit) {
        if (userId.isBlank()) {
            _errorMessage.value = "User ID is missing"
            onComplete(false)
            return
        }

        if (_ambitionStatement.value.isBlank()) {
            _errorMessage.value = "Please write your ambition statement"
            onComplete(false)
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val data = mapOf(
                    "ambitionStatement" to _ambitionStatement.value,
                    "updatedAt" to System.currentTimeMillis()
                )

                firestore.collection("profiles")
                    .document(userId)
                    .set(data, SetOptions.merge())
                    .await()

                Log.d(TAG, "✅ Ambition statement saved successfully")
                _errorMessage.value = null
                onComplete(true)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to save ambition statement", e)
                _errorMessage.value = "Failed to save ambition statement"
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