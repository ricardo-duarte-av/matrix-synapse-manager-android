package com.matrix.synapse.feature.media.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.database.AuditAction
import com.matrix.synapse.database.AuditLogEntry
import com.matrix.synapse.database.AuditLogger
import com.matrix.synapse.feature.media.data.MediaInfoResponse
import com.matrix.synapse.feature.media.data.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MediaDetailState(
    val media: MediaInfoResponse? = null,
    val isLoading: Boolean = false,
    val isActioning: Boolean = false,
    val isDeleted: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null,
)

@HiltViewModel
class MediaDetailViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val auditLogger: AuditLogger,
) : ViewModel() {

    private val _state = MutableStateFlow(MediaDetailState())
    val state: StateFlow<MediaDetailState> = _state.asStateFlow()

    private var serverUrl: String = ""
    private var serverId: String = ""

    fun loadMedia(serverUrl: String, serverId: String, serverName: String, mediaId: String) {
        this.serverUrl = serverUrl
        this.serverId = serverId
        _state.value = MediaDetailState(isLoading = true)
        viewModelScope.launch {
            runCatching { mediaRepository.getMediaInfo(serverUrl, serverName, mediaId) }
                .onSuccess { info -> _state.value = MediaDetailState(media = info) }
                .onFailure { e -> _state.value = MediaDetailState(error = e.message) }
        }
    }

    fun quarantine(serverName: String, mediaId: String) =
        performAction(AuditAction.QUARANTINE_MEDIA, mediaId) {
            mediaRepository.quarantineMedia(serverUrl, serverName, mediaId); "Media quarantined"
        }

    fun unquarantine(serverName: String, mediaId: String) =
        performAction(AuditAction.UNQUARANTINE_MEDIA, mediaId) {
            mediaRepository.unquarantineMedia(serverUrl, serverName, mediaId); "Media removed from quarantine"
        }

    fun protect(mediaId: String) =
        performAction(AuditAction.PROTECT_MEDIA, mediaId) {
            mediaRepository.protectMedia(serverUrl, mediaId); "Media protected from quarantine"
        }

    fun unprotect(mediaId: String) =
        performAction(AuditAction.UNPROTECT_MEDIA, mediaId) {
            mediaRepository.unprotectMedia(serverUrl, mediaId); "Media protection removed"
        }

    fun delete(serverName: String, mediaId: String) {
        _state.value = _state.value.copy(isActioning = true, error = null, actionMessage = null)
        viewModelScope.launch {
            runCatching { mediaRepository.deleteMedia(serverUrl, serverName, mediaId) }
                .onSuccess {
                    _state.value = _state.value.copy(isActioning = false, isDeleted = true, actionMessage = "Media deleted")
                    auditLogger.insert(AuditLogEntry(serverId = serverId, action = AuditAction.DELETE_MEDIA, details = mapOf("media_id" to mediaId)))
                }
                .onFailure { e -> _state.value = _state.value.copy(isActioning = false, error = e.message) }
        }
    }

    private fun performAction(action: AuditAction, mediaId: String, block: suspend () -> String) {
        _state.value = _state.value.copy(isActioning = true, error = null, actionMessage = null)
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { msg ->
                    _state.value = _state.value.copy(isActioning = false, actionMessage = msg)
                    auditLogger.insert(AuditLogEntry(serverId = serverId, action = action, details = mapOf("media_id" to mediaId)))
                    reloadMedia(mediaId)
                }
                .onFailure { e -> _state.value = _state.value.copy(isActioning = false, error = e.message) }
        }
    }

    private fun reloadMedia(mediaId: String) {
        val serverName = extractServerName(serverUrl)
        viewModelScope.launch {
            runCatching { mediaRepository.getMediaInfo(serverUrl, serverName, mediaId) }
                .onSuccess { info -> _state.value = _state.value.copy(media = info) }
        }
    }

    private fun extractServerName(serverUrl: String): String =
        serverUrl.removePrefix("https://").removePrefix("http://").trimEnd('/')
}
