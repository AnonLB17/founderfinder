package com.phoenixcorp.founderfinder.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.phoenixcorp.founderfinder.domain.usecase.SendChatMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PrivateChatViewModel @Inject constructor(
    val sendChatMessageUseCase: SendChatMessageUseCase
) : ViewModel()