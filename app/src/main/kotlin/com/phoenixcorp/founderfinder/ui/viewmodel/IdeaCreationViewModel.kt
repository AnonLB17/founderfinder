package com.phoenixcorp.founderfinder.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoenixcorp.founderfinder.domain.model.Organization
import com.phoenixcorp.founderfinder.domain.repository.OrganizationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class IdeaCreationViewModel(
    private val repository: OrganizationRepository
) : ViewModel() {

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
            try {
                _organizations.value = repository.getOrganizations()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load organizations", e)
                _errorMessage.value = "Failed to load organizations"
            }
        }
    }

    fun submitOrganization() {
        val name = _businessName.value
        val description = _ideaDescription.value
        val imageUri = _selectedImageUri.value

        if (name.isBlank() || description.isBlank()) {
            _errorMessage.value = "Please fill in both name and description"
            return
        }

        val newOrg = Organization(
            name = name,
            description = description,
            imageUri = imageUri
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Submitting new organization: $name")
                repository.saveOrganization(newOrg)

                _organizations.value = _organizations.value + newOrg
                resetForm()
                _errorMessage.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to submit organization", e)
                _errorMessage.value = "Failed to save organization: ${e.message}"
            }
        }
    }

    fun updateOrganization() {
        val selectedOrg = _selectedOrganization.value ?: return
        val name = _businessName.value
        val description = _ideaDescription.value
        val imageUri = _selectedImageUri.value ?: selectedOrg.imageUri

        if (name.isBlank() || description.isBlank()) {
            _errorMessage.value = "Please fill in both name and description"
            return
        }

        val updatedOrg = Organization(
            id = selectedOrg.id,
            name = name,
            description = description,
            imageUri = imageUri
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Updating organization: $name")
                repository.saveOrganization(updatedOrg)

                _organizations.value = _organizations.value.map {
                    if (it.id == selectedOrg.id) updatedOrg else it
                }
                resetForm()
                _errorMessage.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update organization", e)
                _errorMessage.value = "Failed to update organization: ${e.message}"
            }
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
        _selectedOrganization.value = null
        _isCreatingOrganization.value = false
    }
}