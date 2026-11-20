package com.phoenixcorp.founderfinder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoenixcorp.founderfinder.domain.model.Partner
import com.phoenixcorp.founderfinder.domain.usecase.SearchPartnersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PartnerSearchViewModel @Inject constructor(
    private val searchPartnersUseCase: SearchPartnersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<PartnerSearchUiState>(PartnerSearchUiState.Initial)
    val uiState = _uiState.asStateFlow()

    fun searchPartners(query: String, school: String? = null) {
        viewModelScope.launch {
            _uiState.value = PartnerSearchUiState.Loading
            try {
                val results = searchPartnersUseCase(query, school)
                _uiState.value = PartnerSearchUiState.Success(results)
            } catch (e: Exception) {
                _uiState.value = PartnerSearchUiState.Error(e.message ?: "Search failed")
            }
        }
    }
}

sealed class PartnerSearchUiState {
    object Initial : PartnerSearchUiState()
    object Loading : PartnerSearchUiState()
    data class Success(val partners: List<Partner>) : PartnerSearchUiState()
    data class Error(val message: String) : PartnerSearchUiState()
}