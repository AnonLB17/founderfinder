package com.phoenixcorp.founderfinder.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoenixcorp.founderfinder.data.OrganizationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class Organization(val name: String, val description: String, val imageUri: String?)

class IdeaCreationViewModel(private val repository: OrganizationRepository) : ViewModel() {
    private val _organizations = MutableStateFlow<List<Organization>>(emptyList())
    val organizations: StateFlow<List<Organization>> = _organizations

    private val _isCreatingOrganization = MutableStateFlow(false)
    val isCreatingOrganization: StateFlow<Boolean> = _isCreatingOrganization

    private val _selectedOrganization = MutableStateFlow<Organization?>(null)
    val selectedOrganization: StateFlow<Organization?> = _selectedOrganization

    private val _businessName = MutableStateFlow("")
    val businessName: StateFlow<String> = _businessName

    private val _ideaDescription = MutableStateFlow("")
    val ideaDescription: StateFlow<String> = _ideaDescription

    private val _selectedImageUri = MutableStateFlow<String?>(null)
    val selectedImageUri: StateFlow<String?> = _selectedImageUri

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val TAG = "IdeaCreationViewModel"

    init {
        loadOrganizations()
    }

    private fun loadOrganizations() {
        viewModelScope.launch(Dispatchers.IO) {
            _organizations.value = repository.getOrganizations()
        }
    }

    fun submitOrganization() {
        val name = _businessName.value
        val description = _ideaDescription.value
        val imageUri = _selectedImageUri.value
        if (name.isNotBlank() && description.isNotBlank()) {
            val newOrg = Organization(name, description, imageUri)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    Log.d(TAG, "Submitting organization: $name")
                    repository.saveOrganization(newOrg)
                    _organizations.value = _organizations.value + newOrg
                    resetForm()
                    _errorMessage.value = null
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to submit organization: ${e.message}")
                    _errorMessage.value = "Failed to save organization: ${e.message}"
                }
            }
        } else {
            _errorMessage.value = "Please fill in both name and description"
            Log.w(TAG, "Submit failed: Name or description is blank")
        }
    }

    fun updateOrganization() {
        val selectedOrg = _selectedOrganization.value ?: return
        val name = _businessName.value
        val description = _ideaDescription.value
        val imageUri = _selectedImageUri.value ?: selectedOrg.imageUri
        if (name.isNotBlank() && description.isNotBlank()) {
            val updatedOrg = Organization(name, description, imageUri)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    Log.d(TAG, "Updating organization: $name")
                    repository.saveOrganization(updatedOrg)
                    _organizations.value = _organizations.value.map {
                        if (it == selectedOrg) updatedOrg else it
                    }
                    resetForm()
                    _errorMessage.value = null
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update organization: ${e.message}")
                    _errorMessage.value = "Failed to update organization: ${e.message}"
                }
            }
        } else {
            _errorMessage.value = "Please fill in both name and description"
            Log.w(TAG, "Update failed: Name or description is blank")
        }
    }

    fun setSelectedOrganization(org: Organization?) {
        _selectedOrganization.value = org
        if (org != null) {
            _businessName.value = org.name
            _ideaDescription.value = org.description
            _selectedImageUri.value = org.imageUri
        } else {
            resetForm()
        }
    }

    fun setCreatingOrganization(isCreating: Boolean) {
        _isCreatingOrganization.value = isCreating
        if (!isCreating) resetForm()
    }

    fun setBusinessName(value: String) {
        _businessName.value = value
    }

    fun setIdeaDescription(value: String) {
        _ideaDescription.value = value
    }

    fun setSelectedImageUri(uri: String?) {
        _selectedImageUri.value = uri
    }

    private fun resetForm() {
        _businessName.value = ""
        _ideaDescription.value = ""
        _selectedImageUri.value = null
        _isCreatingOrganization.value = false
        _selectedOrganization.value = null
    }
}