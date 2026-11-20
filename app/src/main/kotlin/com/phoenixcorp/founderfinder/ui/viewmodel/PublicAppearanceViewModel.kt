package com.phoenixcorp.founderfinder.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class PublicAppearanceViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : ViewModel() {

    private val TAG = "PublicAppearanceViewModel"

    private val _profileImageUri = MutableStateFlow<Uri?>(null)
    val profileImageUri = _profileImageUri.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun setProfileImageUri(uri: Uri?) {
        _profileImageUri.value = uri
    }

    fun uploadProfilePictureAndComplete(
        userId: String,
        onComplete: (Boolean) -> Unit
    ) {
        val uri = _profileImageUri.value
        if (userId.isBlank()) {
            _errorMessage.value = "User ID is missing"
            onComplete(false)
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val downloadUrl = if (uri != null) {
                    // Upload image to Firebase Storage
                    val storageRef = storage.reference
                        .child("profilePictures/$userId/profile.jpg")

                    storageRef.putFile(uri).await()
                    val url = storageRef.downloadUrl.await().toString()
                    Log.d(TAG, "✅ Profile picture uploaded: $url")
                    url
                } else {
                    ""
                }

                // Final save to Firestore
                val finalData = mapOf(
                    "profilePicture" to downloadUrl,
                    "updatedAt" to System.currentTimeMillis()
                )

                firestore.collection("profiles")
                    .document(userId)
                    .set(finalData, SetOptions.merge())
                    .await()

                Log.d(TAG, "✅ Public appearance completed successfully")
                _errorMessage.value = null
                onComplete(true)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to complete public appearance", e)
                _errorMessage.value = "Failed to upload profile picture"
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