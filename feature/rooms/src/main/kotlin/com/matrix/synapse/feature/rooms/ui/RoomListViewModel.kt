package com.matrix.synapse.feature.rooms.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.database.AuditAction
import com.matrix.synapse.database.AuditLogEntry
import com.matrix.synapse.database.AuditLogger
import com.matrix.synapse.feature.rooms.data.DeleteRoomRequest
import com.matrix.synapse.feature.rooms.data.RoomRepository
import com.matrix.synapse.feature.rooms.data.RoomSummary
import com.matrix.synapse.feature.rooms.data.mxcToDownloadUrl
import com.matrix.synapse.feature.rooms.domain.DeleteRoomUseCase
import com.matrix.synapse.feature.servers.data.ServerRepository
import com.matrix.synapse.model.Server
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoomListState(
    val currentServer: Server? = null,
    val rooms: List<RoomSummary> = emptyList(),
    val roomAvatarUrls: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null,
    val searchQuery: String = "",
    val nextBatch: Int? = null,
    val hasMore: Boolean = false,
    val totalRooms: Int = 0,
    val sortBy: String = "name",
    val sortDir: String = "f",
    val selectionMode: Boolean = false,
    val selectedRoomIds: Set<String> = emptySet(),
)

@HiltViewModel
class RoomListViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val serverRepository: ServerRepository,
    private val deleteRoomUseCase: DeleteRoomUseCase,
    private val auditLogger: AuditLogger,
) : ViewModel() {

    private val _state = MutableStateFlow(RoomListState())
    val state: StateFlow<RoomListState> = _state.asStateFlow()

    private var serverUrl: String = ""
    private var serverId: String = ""

    fun init(serverId: String, serverUrl: String) {
        this.serverId = serverId
        this.serverUrl = serverUrl
        serverRepository.getServerById(serverId).onEach { server ->
            _state.value = _state.value.copy(currentServer = server)
        }.launchIn(viewModelScope)
        loadFirstPage()
    }

    fun search(query: String) {
        _state.value = _state.value.copy(searchQuery = query, rooms = emptyList(), nextBatch = null, hasMore = false)
        loadFirstPage()
    }

    fun setSort(orderBy: String, dir: String) {
        _state.value = _state.value.copy(sortBy = orderBy, sortDir = dir, rooms = emptyList(), nextBatch = null, hasMore = false)
        loadFirstPage()
    }

    fun loadNextPage() {
        val current = _state.value
        if (current.isLoadingMore || !current.hasMore) return
        _state.value = current.copy(isLoadingMore = true)
        viewModelScope.launch {
            runCatching {
                roomRepository.listRooms(
                    serverUrl,
                    from = current.nextBatch,
                    searchTerm = current.searchQuery.takeIf { it.isNotBlank() },
                    orderBy = current.sortBy,
                    dir = current.sortDir,
                )
            }.onSuccess { response ->
                val newRooms = _state.value.rooms + response.rooms
                _state.value = _state.value.copy(
                    rooms = newRooms,
                    nextBatch = response.nextBatch,
                    hasMore = response.nextBatch != null,
                    totalRooms = response.totalRooms,
                    isLoadingMore = false,
                )
                loadRoomAvatars(response.rooms.map { it.roomId })
            }.onFailure { e ->
                _state.value = _state.value.copy(error = e.message, isLoadingMore = false)
            }
        }
    }

    private fun loadFirstPage() {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            runCatching {
                roomRepository.listRooms(
                    serverUrl,
                    searchTerm = _state.value.searchQuery.takeIf { it.isNotBlank() },
                    orderBy = _state.value.sortBy,
                    dir = _state.value.sortDir,
                )
            }.onSuccess { response ->
                _state.value = _state.value.copy(
                    rooms = response.rooms,
                    nextBatch = response.nextBatch,
                    hasMore = response.nextBatch != null,
                    totalRooms = response.totalRooms,
                    isLoading = false,
                )
                loadRoomAvatars(response.rooms.map { it.roomId })
            }.onFailure { e ->
                _state.value = _state.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    private fun loadRoomAvatars(roomIds: List<String>) {
        if (roomIds.isEmpty()) return
        viewModelScope.launch {
            val newUrls = coroutineScope {
                roomIds.map { roomId ->
                    async {
                        runCatching {
                            roomRepository.getRoom(serverUrl, roomId)
                        }.getOrNull()?.avatar?.let { mxc ->
                            mxcToDownloadUrl(serverUrl, mxc)
                        }?.let { url -> roomId to url }
                    }
                }.awaitAll().filterNotNull().toMap()
            }
            _state.value = _state.value.copy(
                roomAvatarUrls = _state.value.roomAvatarUrls + newUrls
            )
        }
    }

    fun enterSelectionMode(roomId: String?) {
        _state.value = _state.value.copy(
            selectionMode = true,
            selectedRoomIds = if (roomId != null) setOf(roomId) else emptySet(),
        )
    }

    fun exitSelectionMode() {
        _state.value = _state.value.copy(selectionMode = false, selectedRoomIds = emptySet())
    }

    fun toggleRoomSelection(roomId: String) {
        val current = _state.value.selectedRoomIds
        _state.value = _state.value.copy(
            selectedRoomIds = if (roomId in current) current - roomId else current + roomId,
        )
    }

    fun selectAllRooms() {
        _state.value = _state.value.copy(selectedRoomIds = _state.value.rooms.map { it.roomId }.toSet())
    }

    fun clearRoomSelection() {
        _state.value = _state.value.copy(selectedRoomIds = emptySet())
    }

    fun deleteSelectedRooms(purge: Boolean) {
        val ids = _state.value.selectedRoomIds.toList()
        if (ids.isEmpty()) return
        _state.value = _state.value.copy(isDeleting = true, error = null, actionMessage = null)
        viewModelScope.launch {
            var success = 0
            var failed = 0
            ids.forEach { roomId ->
                deleteRoomUseCase.execute(serverUrl, roomId, DeleteRoomRequest(purge = purge, block = false))
                    .onSuccess {
                        success++
                        auditLogger.insert(
                            AuditLogEntry(
                                serverId = serverId,
                                action = AuditAction.DELETE_ROOM,
                                details = mapOf("room_id" to roomId, "purge" to purge.toString()),
                            )
                        )
                    }
                    .onFailure { failed++ }
            }
            _state.value = _state.value.copy(
                isDeleting = false,
                selectionMode = false,
                selectedRoomIds = emptySet(),
                actionMessage = when {
                    failed == 0 -> "Deleted $success room(s)"
                    success == 0 -> "Delete failed for all rooms"
                    else -> "Deleted $success, failed $failed"
                },
            )
            loadFirstPage()
        }
    }

    fun clearActionMessage() {
        _state.value = _state.value.copy(actionMessage = null)
    }
}
