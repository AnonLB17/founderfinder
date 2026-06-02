package com.phoenixcorp.founderfinder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoenixcorp.founderfinder.domain.model.ForumPost
import com.phoenixcorp.founderfinder.domain.model.ForumReply
import com.phoenixcorp.founderfinder.domain.usecase.CreateForumPostUseCase
import com.phoenixcorp.founderfinder.domain.usecase.GetForumPostsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForumViewModel @Inject constructor(
    private val getForumPostsUseCase: GetForumPostsUseCase,
    private val createForumPostUseCase: CreateForumPostUseCase
) : ViewModel() {

    private val _postsState = MutableStateFlow<ForumUiState>(ForumUiState.Initial)
    val postsState = _postsState.asStateFlow()

    fun loadPosts(category: String? = null, school: String? = null) {
        viewModelScope.launch {
            _postsState.value = ForumUiState.Loading
            try {
                val posts = getForumPostsUseCase(category, school)
                _postsState.value = ForumUiState.Success(posts)
            } catch (e: Exception) {
                _postsState.value = ForumUiState.Error(e.message ?: "Failed to load forums")
            }
        }
    }

    fun createPost(post: ForumPost) {
        viewModelScope.launch {
            createForumPostUseCase(post)
            loadPosts() // Refresh list
        }
    }
}

sealed class ForumUiState {
    object Initial : ForumUiState()
    object Loading : ForumUiState()
    data class Success(val posts: List<ForumPost>) : ForumUiState()
    data class Error(val message: String) : ForumUiState()
}