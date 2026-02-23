package com.matrix.synapse.feature.servers.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.feature.servers.data.ServerRepository
import com.matrix.synapse.model.Server
import com.matrix.synapse.security.SecureTokenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ServerListNavEvent {
    data class OpenLogin(val serverId: String, val serverUrl: String) : ServerListNavEvent
    data class OpenUserList(val serverId: String, val serverUrl: String) : ServerListNavEvent
}

@HiltViewModel
class ServerListViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val tokenStore: SecureTokenStore,
) : ViewModel() {

    val servers = serverRepository.servers.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    private val _navigationEvent = MutableSharedFlow<ServerListNavEvent>(replay = 0, extraBufferCapacity = 1)
    val navigationEvent: kotlinx.coroutines.flow.Flow<ServerListNavEvent> = _navigationEvent

    private val _deletingId = MutableStateFlow<String?>(null)
    val deletingId: StateFlow<String?> = _deletingId.asStateFlow()

    fun onServerClick(server: Server) {
        viewModelScope.launch {
            val hasToken = tokenStore.accessTokenFlow(server.id).first() != null
            _navigationEvent.emit(
                if (hasToken) ServerListNavEvent.OpenUserList(server.id, server.homeserverUrl)
                else ServerListNavEvent.OpenLogin(server.id, server.homeserverUrl)
            )
        }
    }

    fun removeServer(serverId: String) {
        viewModelScope.launch {
            _deletingId.value = serverId
            try {
                serverRepository.removeServer(serverId)
                tokenStore.clearTokens(serverId)
            } finally {
                _deletingId.value = null
            }
        }
    }
}
