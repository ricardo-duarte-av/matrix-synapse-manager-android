package com.matrix.synapse.feature.users.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.feature.users.data.UserRepository
import com.matrix.synapse.feature.users.domain.UpsertUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserEditState(
    val isSaving: Boolean = false,
    val error: String? = null,
    val savedUserId: String? = null,
    val serverName: String? = null,
    val isLoadingServerName: Boolean = false,
)

@HiltViewModel
class UserEditViewModel @Inject constructor(
    private val upsertUserUseCase: UpsertUserUseCase,
    private val userRepository: UserRepository,
) : ViewModel() {

    fun loadServerName(serverUrl: String) {
        if (_state.value.serverName != null) return
        _state.value = _state.value.copy(
            serverName = serverNameFromUrl(serverUrl),
            isLoadingServerName = true,
        )
        viewModelScope.launch {
            val fromWellKnown = runCatching { userRepository.getServerNameFromWellKnown(serverUrl) }.getOrNull()
            val fromApi = fromWellKnown ?: runCatching { userRepository.getServerNameFromApi(serverUrl) }.getOrNull()
            _state.value = _state.value.copy(
                serverName = fromWellKnown ?: fromApi ?: serverNameFromUrl(serverUrl),
                isLoadingServerName = false,
            )
        }
    }

    private fun serverNameFromUrl(serverUrl: String): String {
        return try {
            val uri = java.net.URI(serverUrl)
            var host = uri.host ?: return serverUrl
            if (host.startsWith("matrix.")) host = host.removePrefix("matrix.")
            val port = uri.port
            if (port > 0 && port != 80 && port != 443) "$host:$port" else host
        } catch (_: Exception) {
            val fallback = serverUrl.removePrefix("https://").removePrefix("http://").trimEnd('/')
            if (fallback.startsWith("matrix.")) fallback.removePrefix("matrix.") else fallback
        }
    }

    private val _state = MutableStateFlow(UserEditState())
    val state: StateFlow<UserEditState> = _state.asStateFlow()

    fun createUser(
        serverUrl: String,
        userId: String,
        password: String,
        displayName: String?,
        admin: Boolean,
    ) {
        _state.value = UserEditState(isSaving = true)
        viewModelScope.launch {
            upsertUserUseCase.createUser(serverUrl, userId, password, displayName, admin)
                .onSuccess { _state.value = UserEditState(savedUserId = it.userId) }
                .onFailure { _state.value = UserEditState(error = it.message) }
        }
    }

    fun updateUser(
        serverUrl: String,
        userId: String,
        displayName: String?,
        admin: Boolean?,
    ) {
        _state.value = UserEditState(isSaving = true)
        viewModelScope.launch {
            upsertUserUseCase.updateUser(serverUrl, userId, displayName, admin)
                .onSuccess { _state.value = UserEditState(savedUserId = it.userId) }
                .onFailure { _state.value = UserEditState(error = it.message) }
        }
    }
}
