package com.matrix.synapse.feature.users.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.database.AuditAction
import com.matrix.synapse.database.AuditLogEntry
import com.matrix.synapse.database.AuditLogger
import com.matrix.synapse.feature.users.data.UserDetail
import com.matrix.synapse.feature.users.data.UserRepository
import com.matrix.synapse.feature.users.domain.DeactivateUserUseCase
import com.matrix.synapse.network.CapabilityService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserDetailState(
    val user: UserDetail? = null,
    val isLoading: Boolean = false,
    val isLocking: Boolean = false,
    val isSuspending: Boolean = false,
    val canSuspend: Boolean = false,
    val isDeactivating: Boolean = false,
    val isDeactivated: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
)

@HiltViewModel
class UserDetailViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val capabilityService: CapabilityService,
    private val deactivateUserUseCase: DeactivateUserUseCase,
    private val auditLogger: AuditLogger,
) : ViewModel() {

    private val _state = MutableStateFlow(UserDetailState())
    val state: StateFlow<UserDetailState> = _state.asStateFlow()

    fun loadUser(serverUrl: String, serverId: String, userId: String) {
        _state.value = UserDetailState(isLoading = true)
        viewModelScope.launch {
            runCatching {
                val caps = capabilityService.getCapabilities(serverId, serverUrl)
                val user = userRepository.getUser(serverUrl, userId)
                caps to user
            }.onSuccess { (caps, user) ->
                _state.value = UserDetailState(
                    user = user,
                    canSuspend = caps.canSuspendUsers,
                )
            }.onFailure { e ->
                _state.value = UserDetailState(error = e.message)
            }
        }
    }

    fun setLocked(serverUrl: String, userId: String, locked: Boolean) {
        _state.value = _state.value.copy(isLocking = true, error = null)
        viewModelScope.launch {
            runCatching {
                userRepository.setLocked(serverUrl, userId, locked)
            }.onSuccess {
                _state.value = _state.value.copy(
                    isLocking = false,
                    user = _state.value.user?.copy(locked = locked),
                    successMessage = if (locked) "User locked" else "User unlocked",
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(isLocking = false, error = e.message)
            }
        }
    }

    fun setSuspended(serverUrl: String, userId: String, suspended: Boolean) {
        _state.value = _state.value.copy(isSuspending = true, error = null)
        viewModelScope.launch {
            runCatching {
                userRepository.setSuspended(serverUrl, userId, suspended)
            }.onSuccess {
                _state.value = _state.value.copy(
                    isSuspending = false,
                    successMessage = if (suspended) "User suspended" else "User unsuspended",
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(isSuspending = false, error = e.message)
            }
        }
    }

    fun deactivateUser(serverUrl: String, serverId: String, userId: String, deleteMedia: Boolean) {
        _state.value = _state.value.copy(isDeactivating = true, error = null)
        viewModelScope.launch {
            deactivateUserUseCase.deactivate(
                serverUrl = serverUrl,
                userId = userId,
                deleteMedia = deleteMedia,
                confirmed = true,
            ).onSuccess {
                _state.value = _state.value.copy(
                    isDeactivating = false,
                    isDeactivated = true,
                    successMessage = "User deactivated",
                )
                auditLogger.insert(
                    AuditLogEntry(
                        serverId = serverId,
                        action = AuditAction.DEACTIVATE_USER,
                        targetUserId = userId,
                        details = mapOf("erase" to deleteMedia.toString()),
                    )
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(isDeactivating = false, error = e.message)
            }
        }
    }
}
