package com.matrix.synapse.feature.federation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.database.AuditAction
import com.matrix.synapse.database.AuditLogEntry
import com.matrix.synapse.database.AuditLogger
import com.matrix.synapse.feature.federation.data.DestinationRoom
import com.matrix.synapse.feature.federation.data.FederationDestination
import com.matrix.synapse.feature.federation.data.FederationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FederationDetailState(
    val destination: FederationDestination? = null,
    val rooms: List<DestinationRoom> = emptyList(),
    val isLoading: Boolean = false,
    val isResetting: Boolean = false,
    val isLoadingMoreRooms: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null,
    val totalRooms: Int = 0,
    val roomsNextToken: String? = null,
    val hasMoreRooms: Boolean = false,
)

@HiltViewModel
class FederationDetailViewModel @Inject constructor(
    private val federationRepository: FederationRepository,
    private val auditLogger: AuditLogger,
) : ViewModel() {

    private val _state = MutableStateFlow(FederationDetailState())
    val state: StateFlow<FederationDetailState> = _state.asStateFlow()

    private var serverUrl: String = ""
    private var serverId: String = ""

    fun loadDestination(serverUrl: String, serverId: String, destination: String) {
        this.serverUrl = serverUrl
        this.serverId = serverId
        _state.value = FederationDetailState(isLoading = true)
        viewModelScope.launch {
            runCatching {
                val dest = federationRepository.getDestination(serverUrl, destination)
                val rooms = federationRepository.getDestinationRooms(serverUrl, destination)
                dest to rooms
            }.onSuccess { (dest, rooms) ->
                _state.value = FederationDetailState(
                    destination = dest,
                    rooms = rooms.rooms,
                    totalRooms = rooms.total,
                    roomsNextToken = rooms.nextToken,
                    hasMoreRooms = rooms.nextToken != null,
                )
            }.onFailure { e ->
                _state.value = FederationDetailState(error = e.message)
            }
        }
    }

    fun resetConnection(destination: String) {
        _state.value = _state.value.copy(isResetting = true, error = null, actionMessage = null)
        viewModelScope.launch {
            runCatching {
                federationRepository.resetConnection(serverUrl, destination)
            }.onSuccess {
                _state.value = _state.value.copy(isResetting = false, actionMessage = "Connection reset initiated")
                auditLogger.insert(
                    AuditLogEntry(
                        serverId = serverId,
                        action = AuditAction.RESET_FEDERATION_CONNECTION,
                        details = mapOf("destination" to destination),
                    )
                )
                // Reload destination to get updated retry timing
                runCatching { federationRepository.getDestination(serverUrl, destination) }
                    .onSuccess { dest -> _state.value = _state.value.copy(destination = dest) }
            }.onFailure { e ->
                _state.value = _state.value.copy(isResetting = false, error = e.message)
            }
        }
    }

    fun loadMoreRooms(destination: String) {
        val current = _state.value
        if (current.isLoadingMoreRooms || !current.hasMoreRooms) return
        _state.value = current.copy(isLoadingMoreRooms = true)
        viewModelScope.launch {
            runCatching {
                federationRepository.getDestinationRooms(serverUrl, destination, from = current.roomsNextToken)
            }.onSuccess { response ->
                _state.value = _state.value.copy(
                    rooms = _state.value.rooms + response.rooms,
                    roomsNextToken = response.nextToken,
                    hasMoreRooms = response.nextToken != null,
                    totalRooms = response.total,
                    isLoadingMoreRooms = false,
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(error = e.message, isLoadingMoreRooms = false)
            }
        }
    }
}
