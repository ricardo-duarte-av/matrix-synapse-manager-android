package com.matrix.synapse.feature.moderation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.feature.moderation.data.EventReportSummary
import com.matrix.synapse.feature.moderation.data.ModerationRepository
import com.matrix.synapse.feature.rooms.data.RoomRepository
import com.matrix.synapse.feature.rooms.data.RoomSummary
import com.matrix.synapse.feature.users.data.UserRepository
import com.matrix.synapse.feature.users.data.UserSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventReportsState(
    val reports: List<EventReportSummary> = emptyList(),
    val total: Int = 0,
    val nextToken: Long? = null,
    val hasMore: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val filterRoomId: String = "",
    val filterUserId: String = "",
    val sortNewestFirst: Boolean = true,
    val rooms: List<RoomSummary> = emptyList(),
    val users: List<UserSummary> = emptyList(),
    val roomsLoading: Boolean = false,
    val usersLoading: Boolean = false,
)

@HiltViewModel
class EventReportsViewModel @Inject constructor(
    private val moderationRepository: ModerationRepository,
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EventReportsState())
    val state: StateFlow<EventReportsState> = _state.asStateFlow()

    private var serverUrl: String = ""

    fun load(serverId: String, serverUrl: String) {
        this.serverUrl = serverUrl
        loadRooms()
        loadUsers()
        loadFirstPage()
    }

    private fun loadRooms() {
        _state.value = _state.value.copy(roomsLoading = true)
        viewModelScope.launch {
            runCatching {
                roomRepository.listRooms(serverUrl, limit = 300)
            }.onSuccess { response ->
                _state.value = _state.value.copy(rooms = response.rooms, roomsLoading = false)
            }.onFailure {
                _state.value = _state.value.copy(roomsLoading = false)
            }
        }
    }

    private fun loadUsers() {
        _state.value = _state.value.copy(usersLoading = true)
        viewModelScope.launch {
            runCatching {
                userRepository.listUsers(serverUrl, limit = 300)
            }.onSuccess { response ->
                _state.value = _state.value.copy(users = response.users, usersLoading = false)
            }.onFailure {
                _state.value = _state.value.copy(usersLoading = false)
            }
        }
    }

    fun setFilters(roomId: String, userId: String) {
        _state.value = _state.value.copy(
            filterRoomId = roomId,
            filterUserId = userId,
            reports = emptyList(),
            nextToken = null,
            hasMore = false,
        )
        loadFirstPage()
    }

    fun setSortNewestFirst(newestFirst: Boolean) {
        _state.value = _state.value.copy(
            sortNewestFirst = newestFirst,
            reports = emptyList(),
            nextToken = null,
            hasMore = false,
        )
        loadFirstPage()
    }

    fun loadNextPage() {
        val current = _state.value
        if (current.isLoadingMore || !current.hasMore) return
        val from = current.nextToken ?: return
        _state.value = current.copy(isLoadingMore = true)
        viewModelScope.launch {
            runCatching {
                moderationRepository.listEventReports(
                    serverUrl = serverUrl,
                    from = from,
                    limit = 50,
                    dir = if (current.sortNewestFirst) "b" else "f",
                    roomId = current.filterRoomId.takeIf { it.isNotBlank() },
                    userId = current.filterUserId.takeIf { it.isNotBlank() },
                )
            }.onSuccess { response ->
                _state.value = _state.value.copy(
                    reports = _state.value.reports + response.eventReports,
                    nextToken = response.nextToken,
                    hasMore = response.nextToken != null,
                    total = response.total,
                    isLoadingMore = false,
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to load",
                    isLoadingMore = false,
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun loadFirstPage() {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            runCatching {
                moderationRepository.listEventReports(
                    serverUrl = serverUrl,
                    from = 0L,
                    limit = 50,
                    dir = if (_state.value.sortNewestFirst) "b" else "f",
                    roomId = _state.value.filterRoomId.takeIf { it.isNotBlank() },
                    userId = _state.value.filterUserId.takeIf { it.isNotBlank() },
                )
            }.onSuccess { response ->
                _state.value = _state.value.copy(
                    reports = response.eventReports,
                    nextToken = response.nextToken,
                    hasMore = response.nextToken != null,
                    total = response.total,
                    isLoading = false,
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to load",
                    isLoading = false,
                )
            }
        }
    }
}
