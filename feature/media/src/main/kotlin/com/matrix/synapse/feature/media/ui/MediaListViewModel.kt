package com.matrix.synapse.feature.media.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.database.AuditAction
import com.matrix.synapse.database.AuditLogEntry
import com.matrix.synapse.database.AuditLogger
import com.matrix.synapse.feature.media.data.MediaRepository
import com.matrix.synapse.feature.rooms.data.RoomRepository
import com.matrix.synapse.feature.rooms.data.RoomSummary
import com.matrix.synapse.feature.servers.data.ServerRepository
import com.matrix.synapse.feature.users.data.UserRepository
import com.matrix.synapse.feature.users.data.UserSummary
import com.matrix.synapse.model.Server
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MediaListItem(
    val mediaId: String,
    val origin: String,
    val isLocal: Boolean,
)

data class MediaListState(
    val currentServer: Server? = null,
    val mediaItems: List<MediaListItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null,
    val filterMode: String = "room",
    val filterValue: String = "",
    val rooms: List<RoomSummary> = emptyList(),
    val users: List<UserSummary> = emptyList(),
    val roomsLoading: Boolean = false,
    val usersLoading: Boolean = false,
    val selectedRoomId: String? = null,
    val selectedUserId: String? = null,
)

@HiltViewModel
class MediaListViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository,
    private val auditLogger: AuditLogger,
    private val serverRepository: ServerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MediaListState())
    val state: StateFlow<MediaListState> = _state.asStateFlow()

    private var serverUrl: String = ""
    private var serverId: String = ""

    fun init(serverUrl: String, serverId: String, filterUserId: String?, filterRoomId: String?) {
        this.serverUrl = serverUrl
        this.serverId = serverId
        serverRepository.getServerById(serverId).onEach { server ->
            _state.value = _state.value.copy(currentServer = server)
        }.launchIn(viewModelScope)
        loadRooms()
        loadUsers()
        when {
            filterRoomId != null -> {
                _state.value = _state.value.copy(
                    filterMode = "room",
                    filterValue = filterRoomId,
                    selectedRoomId = filterRoomId,
                    selectedUserId = null,
                )
                loadRoomMedia(filterRoomId)
            }
            filterUserId != null -> {
                _state.value = _state.value.copy(
                    filterMode = "user",
                    filterValue = filterUserId,
                    selectedRoomId = null,
                    selectedUserId = filterUserId,
                )
                loadUserMedia(filterUserId)
            }
            else -> {
                _state.value = _state.value.copy(filterMode = "room", filterValue = "")
            }
        }
    }

    fun loadRooms() {
        _state.value = _state.value.copy(roomsLoading = true)
        viewModelScope.launch {
            runCatching {
                roomRepository.listRooms(serverUrl, limit = 500)
            }.onSuccess { response ->
                _state.value = _state.value.copy(rooms = response.rooms, roomsLoading = false)
            }.onFailure {
                _state.value = _state.value.copy(roomsLoading = false)
            }
        }
    }

    fun loadUsers() {
        _state.value = _state.value.copy(usersLoading = true)
        viewModelScope.launch {
            runCatching {
                userRepository.listUsers(serverUrl, limit = 500)
            }.onSuccess { response ->
                _state.value = _state.value.copy(users = response.users, usersLoading = false)
            }.onFailure {
                _state.value = _state.value.copy(usersLoading = false)
            }
        }
    }

    fun selectRoom(roomId: String?) {
        _state.value = _state.value.copy(
            selectedRoomId = roomId,
            selectedUserId = null,
            filterMode = "room",
            filterValue = roomId ?: "",
        )
        if (roomId != null) loadRoomMedia(roomId)
        else _state.value = _state.value.copy(mediaItems = emptyList())
    }

    fun selectUser(userId: String?) {
        _state.value = _state.value.copy(
            selectedRoomId = null,
            selectedUserId = userId,
            filterMode = "user",
            filterValue = userId ?: "",
        )
        if (userId != null) loadUserMedia(userId)
        else _state.value = _state.value.copy(mediaItems = emptyList())
    }

    fun loadRoomMedia(roomId: String) {
        _state.value = _state.value.copy(isLoading = true, error = null, filterMode = "room", filterValue = roomId)
        viewModelScope.launch {
            runCatching {
                val serverName = extractServerName(serverUrl)
                val response = mediaRepository.listRoomMedia(serverUrl, roomId)
                val items = response.local.map { MediaListItem(mediaId = it, origin = serverName, isLocal = true) } +
                    response.remote.map { MediaListItem(mediaId = it, origin = serverName, isLocal = false) }
                items
            }.onSuccess { items ->
                _state.value = _state.value.copy(mediaItems = items, isLoading = false)
            }.onFailure { e ->
                _state.value = _state.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    fun loadUserMedia(userId: String) {
        _state.value = _state.value.copy(isLoading = true, error = null, filterMode = "user", filterValue = userId)
        viewModelScope.launch {
            runCatching {
                val serverName = extractServerName(serverUrl)
                val response = userRepository.listUserMedia(serverUrl, userId)
                response.media.map { MediaListItem(mediaId = it.mediaId, origin = serverName, isLocal = true) }
            }.onSuccess { items ->
                _state.value = _state.value.copy(mediaItems = items, isLoading = false)
            }.onFailure { e ->
                _state.value = _state.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    fun bulkDeleteMedia(beforeTs: Long, sizeGt: Long?, keepProfiles: Boolean?) {
        _state.value = _state.value.copy(error = null, actionMessage = null)
        viewModelScope.launch {
            runCatching {
                mediaRepository.bulkDeleteMedia(serverUrl, beforeTs, sizeGt, keepProfiles)
            }.onSuccess { response ->
                _state.value = _state.value.copy(actionMessage = "Deleted ${response.total} media items")
                auditLogger.insert(
                    AuditLogEntry(serverId = serverId, action = AuditAction.BULK_DELETE_MEDIA, details = mapOf("deleted" to response.total.toString()))
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun purgeRemoteMediaCache(beforeTs: Long) {
        _state.value = _state.value.copy(error = null, actionMessage = null)
        viewModelScope.launch {
            runCatching {
                mediaRepository.purgeRemoteMediaCache(serverUrl, beforeTs)
            }.onSuccess { response ->
                _state.value = _state.value.copy(actionMessage = "Purged ${response.deleted} remote media items")
                auditLogger.insert(
                    AuditLogEntry(serverId = serverId, action = AuditAction.PURGE_REMOTE_MEDIA_CACHE, details = mapOf("deleted" to response.deleted.toString()))
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    private fun extractServerName(serverUrl: String): String =
        serverUrl.removePrefix("https://").removePrefix("http://").trimEnd('/')
}
