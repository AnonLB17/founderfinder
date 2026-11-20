package com.phoenixcorp.founderfinder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoenixcorp.founderfinder.domain.model.Advisor
import com.phoenixcorp.founderfinder.domain.usecase.SearchAdvisorsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdvisorSearchViewModel @Inject constructor(
    private val searchAdvisorsUseCase: SearchAdvisorsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdvisorSearchUiState>(AdvisorSearchUiState.Initial)
    val uiState = _uiState.asStateFlow()

    fun searchAdvisors(query: String, school: String? = null) {
        viewModelScope.launch {
            _uiState.value = AdvisorSearchUiState.Loading
            try {
                val results = searchAdvisorsUseCase(query, school)
                _uiState.value = AdvisorSearchUiState.Success(results)
            } catch (e: Exception) {
                _uiState.value = AdvisorSearchUiState.Error(e.message ?: "Search failed")
            }
        }
    }
}

// UI State Sealed Class
sealed class AdvisorSearchUiState {
    object Initial : AdvisorSearchUiState()
    object Loading : AdvisorSearchUiState()
    data class Success(val advisors: List<Advisor>) : AdvisorSearchUiState()
    data class Error(val message: String) : AdvisorSearchUiState()
}