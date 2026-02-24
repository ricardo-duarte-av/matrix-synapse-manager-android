package com.matrix.synapse.feature.rooms.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.feature.rooms.data.RoomRepository
import com.matrix.synapse.feature.rooms.data.RoomSummary
import com.matrix.synapse.feature.rooms.data.mxcToDownloadUrl
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
    val error: String? = null,
    val searchQuery: String = "",
    val nextBatch: Int? = null,
    val hasMore: Boolean = false,
    val totalRooms: Int = 0,
    val sortBy: String = "name",
    val sortDir: String = "f",
)

@HiltViewModel
class RoomListViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val serverRepository: ServerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RoomListState())
    val state: StateFlow<RoomListState> = _state.asStateFlow()

    private var serverUrl: String = ""

    fun init(serverId: String, serverUrl: String) {
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
}
