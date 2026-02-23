package com.matrix.synapse.feature.media.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.database.AuditAction
import com.matrix.synapse.database.AuditLogEntry
import com.matrix.synapse.database.AuditLogger
import com.matrix.synapse.feature.media.data.MediaRepository
import com.matrix.synapse.feature.servers.data.ServerRepository
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
)

@HiltViewModel
class MediaListViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
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
        when {
            filterRoomId != null -> {
                _state.value = _state.value.copy(filterMode = "room", filterValue = filterRoomId)
                loadRoomMedia(filterRoomId)
            }
            filterUserId != null -> {
                _state.value = _state.value.copy(filterMode = "user", filterValue = filterUserId)
            }
            else -> {
                _state.value = _state.value.copy(filterMode = "room", filterValue = "")
            }
        }
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
