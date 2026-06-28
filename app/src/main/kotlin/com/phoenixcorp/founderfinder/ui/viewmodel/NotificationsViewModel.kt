package com.phoenixcorp.founderfinder.ui.viewmodel.notifications

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.phoenixcorp.founderfinder.domain.model.Notification
import com.phoenixcorp.founderfinder.domain.repository.NotificationRepository
import com.phoenixcorp.founderfinder.domain.usecase.GetAllNotificationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.delay

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val getAllNotificationsUseCase: GetAllNotificationsUseCase,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var notificationJob: Job? = null

    init {
        loadAllNotifications()
    }

    fun loadAllNotifications() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        notificationJob?.cancel()

        notificationJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                getAllNotificationsUseCase(userId).collectLatest { list ->
                    _notifications.value = list
                    _unreadCount.value = list.count { !it.read }
                }
            } catch (e: Exception) {
                _notifications.value = emptyList()
                _unreadCount.value = 0
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                notificationRepository.markAsRead(notificationId)
                // Force refresh to update UI immediately
                loadAllNotifications()
                Log.d("NotificationsViewModel", "Marked as read and refreshed: $notificationId")
            } catch (e: Exception) {
                Log.e("NotificationsViewModel", "Failed to mark as read", e)
            }
        }
    }

    fun markAllAsRead() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            notificationRepository.markAllAsRead(userId)
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            try {
                notificationRepository.deleteNotification(notificationId)
                Log.d("NotificationsViewModel", "Delete requested for: $notificationId")

                // Force immediate refresh
                delay(500) // Give Firestore a moment to update
                loadAllNotifications()
            } catch (e: Exception) {
                Log.e("NotificationsViewModel", "Failed to delete notification", e)
            }
        }
    }

    override fun onCleared() {
        notificationJob?.cancel()
        super.onCleared()
    }
}