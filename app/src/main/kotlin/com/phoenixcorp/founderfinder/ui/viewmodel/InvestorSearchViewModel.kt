package com.phoenixcorp.founderfinder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoenixcorp.founderfinder.domain.model.Investor
import com.phoenixcorp.founderfinder.domain.usecase.SearchInvestorsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InvestorSearchViewModel @Inject constructor(
    private val searchInvestorsUseCase: SearchInvestorsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<InvestorSearchUiState>(InvestorSearchUiState.Initial)
    val uiState = _uiState.asStateFlow()

    fun searchInvestors(query: String) {
        viewModelScope.launch {
            _uiState.value = InvestorSearchUiState.Loading
            try {
                val results = searchInvestorsUseCase(query)
                _uiState.value = InvestorSearchUiState.Success(results)
            } catch (e: Exception) {
                _uiState.value = InvestorSearchUiState.Error(e.message ?: "Failed to load investors")
            }
        }
    }
}

sealed class InvestorSearchUiState {
    object Initial : InvestorSearchUiState()
    object Loading : InvestorSearchUiState()
    data class Success(val investors: List<Investor>) : InvestorSearchUiState()
    data class Error(val message: String) : InvestorSearchUiState()
}