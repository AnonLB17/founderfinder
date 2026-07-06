package com.phoenixcorp.founderfinder.ui.viewmodel.notifications

import android.os.Build
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
        startPersistentListener()
    }

    private fun startPersistentListener() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        notificationJob?.cancel()

        notificationJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                getAllNotificationsUseCase(userId).collectLatest { list ->
                    _notifications.value = list
                    _unreadCount.value = list.count { !it.read }
                    Log.d("NotificationsViewModel", "✅ Loaded ${list.size} notifications (device=${Build.MODEL})")
                }
            } catch (e: Exception) {
                Log.w("NotificationsViewModel", "Listener cancelled or error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Public method to refresh (call from MainActivity or when needed)
    fun refreshNotifications() {
        startPersistentListener()
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                notificationRepository.markAsRead(notificationId)
                refreshNotifications()  // Refresh after marking read
                Log.d("NotificationsViewModel", "Marked as read: $notificationId")
            } catch (e: Exception) {
                Log.e("NotificationsViewModel", "Failed to mark as read", e)
            }
        }
    }

    fun markAllAsRead() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                notificationRepository.markAllAsRead(userId)
                refreshNotifications()
            } catch (e: Exception) {
                Log.e("NotificationsViewModel", "Failed to mark all as read", e)
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            try {
                notificationRepository.deleteNotification(notificationId)
                delay(500)
                refreshNotifications()
            } catch (e: Exception) {
                Log.e("NotificationsViewModel", "Failed to delete", e)
            }
        }
    }

    // NEW: Create activity notification
    fun createActivityNotification(
        userId: String,
        activityId: String,
        title: String,
        body: String,
        activityType: String,
        organizationId: String? = null,
        organizationName: String? = null
    ) {
        viewModelScope.launch {
            try {
                notificationRepository.createNotification(
                    userId = userId,
                    senderId = userId,
                    senderName = "System",
                    type = "activity_reminder",
                    title = title,
                    body = body,
                    activityId = activityId,
                    activityType = activityType,
                    organizationId = organizationId,
                    organizationName = organizationName
                )
                Log.d("NotificationsViewModel", "✅ Activity notification created for $activityId")
            } catch (e: Exception) {
                Log.e("NotificationsViewModel", "Failed to create activity notification", e)
            }
        }
    }

    override fun onCleared() {
        notificationJob?.cancel()
        super.onCleared()
    }
}