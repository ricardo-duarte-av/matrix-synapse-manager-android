package com.matrix.synapse.feature.servers.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.feature.servers.data.ServerRepository
import com.matrix.synapse.feature.servers.domain.DiscoverServerUseCase
import com.matrix.synapse.model.Server
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ServerFormState {
    data object Idle : ServerFormState
    data object Discovering : ServerFormState
    data class Error(val message: String) : ServerFormState
    data class Success(val server: Server) : ServerFormState
}

@HiltViewModel
class ServerFormViewModel @Inject constructor(
    private val discoverServerUseCase: DiscoverServerUseCase,
    private val serverRepository: ServerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ServerFormState>(ServerFormState.Idle)
    val state: StateFlow<ServerFormState> = _state.asStateFlow()

    fun addServer(urlInput: String, displayName: String) {
        if (urlInput.isBlank()) {
            _state.value = ServerFormState.Error("Server URL cannot be empty")
            return
        }
        viewModelScope.launch {
            _state.value = ServerFormState.Discovering
            val result = discoverServerUseCase.discover(urlInput)
            result.fold(
                onSuccess = { discovered ->
                    val server = Server(
                        displayName = displayName.ifBlank { discovered.homeserverUrl },
                        inputUrl = discovered.inputUrl,
                        homeserverUrl = discovered.homeserverUrl,
                    )
                    serverRepository.addServer(server)
                    _state.value = ServerFormState.Success(server)
                },
                onFailure = { error ->
                    _state.value = ServerFormState.Error(
                        error.message ?: "Unable to reach server. Check the URL and try again."
                    )
                }
            )
        }
    }

    fun resetState() {
        _state.value = ServerFormState.Idle
    }
}
