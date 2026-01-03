package com.nextserve.serveitpartnernew.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.nextserve.serveitpartnernew.data.model.Notification
import com.nextserve.serveitpartnernew.data.repository.NotificationsRepository

data class NotificationsUiState(
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = false,
    val unreadCount: Int = 0,
    val errorMessage: String? = null
)

class NotificationsViewModel(
    private val providerId: String,
    private val notificationsRepository: NotificationsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
        loadUnreadCount()
    }

    fun loadNotifications() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        notificationsRepository.listenToNotifications(providerId)
            .onEach { notifications ->
                _uiState.value = _uiState.value.copy(
                    notifications = notifications,
                    isLoading = false,
                    unreadCount = notifications.count { !it.isRead }
                )
            }
            .launchIn(viewModelScope)
    }

    fun loadUnreadCount() {
        viewModelScope.launch {
            notificationsRepository.getUnreadCount(providerId).fold(
                onSuccess = { count ->
                    _uiState.value = _uiState.value.copy(unreadCount = count)
                },
                onFailure = { /* Silently fail */ }
            )
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationsRepository.markAsRead(providerId, notificationId).fold(
                onSuccess = {
                    // Update local state
                    val updated = _uiState.value.notifications.map { notif ->
                        if (notif.id == notificationId) notif.copy(isRead = true) else notif
                    }
                    _uiState.value = _uiState.value.copy(
                        notifications = updated,
                        unreadCount = updated.count { !it.isRead }
                    )
                },
                onFailure = { /* Silently fail */ }
            )
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationsRepository.markAllAsRead(providerId).fold(
                onSuccess = {
                    val updated = _uiState.value.notifications.map { it.copy(isRead = true) }
                    _uiState.value = _uiState.value.copy(
                        notifications = updated,
                        unreadCount = 0
                    )
                },
                onFailure = { /* Silently fail */ }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    companion object {
        fun factory(
            providerId: String,
            notificationsRepository: NotificationsRepository
        ): NotificationsViewModelFactory {
            return NotificationsViewModelFactory(providerId, notificationsRepository)
        }
    }
}

class NotificationsViewModelFactory(
    private val providerId: String,
    private val notificationsRepository: NotificationsRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotificationsViewModel(providerId, notificationsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

