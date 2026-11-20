package com.phoenixcorp.founderfinder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoenixcorp.founderfinder.domain.model.Investor
import com.phoenixcorp.founderfinder.domain.usecase.CreateInvestorProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InvestorViewModel @Inject constructor(
    private val createInvestorProfileUseCase: CreateInvestorProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<InvestorUiState>(InvestorUiState.Initial)
    val uiState = _uiState.asStateFlow()

    fun createInvestorProfile(investorData: Map<String, Any>) {
        viewModelScope.launch {
            _uiState.value = InvestorUiState.Loading

            try {
                val result = createInvestorProfileUseCase(investorData)
                if (result.isSuccess) {
                    _uiState.value = InvestorUiState.Success
                } else {
                    _uiState.value = InvestorUiState.Error(result.exceptionOrNull()?.message ?: "Failed to create profile")
                }
            } catch (e: Exception) {
                _uiState.value = InvestorUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

// UI State
sealed class InvestorUiState {
    object Initial : InvestorUiState()
    object Loading : InvestorUiState()
    object Success : InvestorUiState()
    data class Error(val message: String) : InvestorUiState()
}