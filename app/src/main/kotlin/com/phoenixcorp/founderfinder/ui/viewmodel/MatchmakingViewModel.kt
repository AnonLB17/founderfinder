package com.phoenixcorp.founderfinder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoenixcorp.founderfinder.domain.model.Match
import com.phoenixcorp.founderfinder.domain.model.MatchType
import com.phoenixcorp.founderfinder.domain.usecase.GetRecommendedMatchesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MatchmakingViewModel @Inject constructor(
    private val getRecommendedMatchesUseCase: GetRecommendedMatchesUseCase
) : ViewModel() {

    private val _matchesState = MutableStateFlow<MatchmakingUiState>(MatchmakingUiState.Initial)
    val matchesState = _matchesState.asStateFlow()

    fun loadRecommendedMatches(userId: String, matchType: MatchType) {
        viewModelScope.launch {
            _matchesState.value = MatchmakingUiState.Loading
            try {
                val matches = getRecommendedMatchesUseCase(userId, matchType)
                _matchesState.value = MatchmakingUiState.Success(matches)
            } catch (e: Exception) {
                _matchesState.value = MatchmakingUiState.Error(e.message ?: "Failed to load matches")
            }
        }
    }
}

sealed class MatchmakingUiState {
    object Initial : MatchmakingUiState()
    object Loading : MatchmakingUiState()
    data class Success(val matches: List<Match>) : MatchmakingUiState()
    data class Error(val message: String) : MatchmakingUiState()
}