package com.matrix.synapse.feature.rooms.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.database.AuditAction
import com.matrix.synapse.database.AuditLogEntry
import com.matrix.synapse.database.AuditLogger
import com.matrix.synapse.feature.rooms.data.*
import com.matrix.synapse.feature.rooms.domain.DeleteRoomUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoomDetailState(
    val room: RoomDetailResponse? = null,
    val members: List<String> = emptyList(),
    val isBlocked: Boolean = false,
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteComplete: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null,
)

@HiltViewModel
class RoomDetailViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val deleteRoomUseCase: DeleteRoomUseCase,
    private val auditLogger: AuditLogger,
) : ViewModel() {

    private val _state = MutableStateFlow(RoomDetailState())
    val state: StateFlow<RoomDetailState> = _state.asStateFlow()

    fun loadRoom(serverUrl: String, serverId: String, roomId: String) {
        _state.value = RoomDetailState(isLoading = true)
        viewModelScope.launch {
            runCatching {
                val room = roomRepository.getRoom(serverUrl, roomId)
                val members = roomRepository.getRoomMembers(serverUrl, roomId)
                val blocked = roomRepository.getBlockStatus(serverUrl, roomId)
                Triple(room, members, blocked)
            }.onSuccess { (room, members, blocked) ->
                _state.value = _state.value.copy(
                    room = room,
                    members = members.members,
                    isBlocked = blocked.block,
                    isLoading = false,
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    fun blockRoom(serverUrl: String, serverId: String, roomId: String, block: Boolean) {
        _state.value = _state.value.copy(error = null, actionMessage = null)
        viewModelScope.launch {
            runCatching {
                roomRepository.blockRoom(serverUrl, roomId, block)
            }.onSuccess {
                _state.value = _state.value.copy(
                    isBlocked = block,
                    actionMessage = if (block) "Room blocked" else "Room unblocked",
                )
                auditLogger.insert(
                    AuditLogEntry(
                        serverId = serverId,
                        action = if (block) AuditAction.BLOCK_ROOM else AuditAction.UNBLOCK_ROOM,
                        details = mapOf("room_id" to roomId),
                    )
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun deleteRoom(serverUrl: String, serverId: String, roomId: String, request: DeleteRoomRequest) {
        _state.value = _state.value.copy(isDeleting = true, error = null, actionMessage = null)
        viewModelScope.launch {
            deleteRoomUseCase.execute(serverUrl, roomId, request)
                .onSuccess {
                    _state.value = _state.value.copy(isDeleting = false, deleteComplete = true, actionMessage = "Room deleted")
                    auditLogger.insert(
                        AuditLogEntry(
                            serverId = serverId,
                            action = AuditAction.DELETE_ROOM,
                            details = mapOf("room_id" to roomId, "purge" to request.purge.toString()),
                        )
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(isDeleting = false, error = e.message)
                }
        }
    }

    fun joinUserToRoom(serverUrl: String, serverId: String, roomId: String, userId: String) {
        _state.value = _state.value.copy(error = null, actionMessage = null)
        viewModelScope.launch {
            runCatching {
                roomRepository.joinUserToRoom(serverUrl, roomId, userId)
            }.onSuccess {
                _state.value = _state.value.copy(actionMessage = "User $userId joined to room")
                auditLogger.insert(
                    AuditLogEntry(
                        serverId = serverId,
                        action = AuditAction.JOIN_USER_TO_ROOM,
                        details = mapOf("room_id" to roomId, "user_id" to userId),
                    )
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun makeRoomAdmin(serverUrl: String, serverId: String, roomId: String, userId: String?) {
        _state.value = _state.value.copy(error = null, actionMessage = null)
        viewModelScope.launch {
            runCatching {
                roomRepository.makeRoomAdmin(serverUrl, roomId, userId)
            }.onSuccess {
                _state.value = _state.value.copy(actionMessage = "Room admin granted")
                auditLogger.insert(
                    AuditLogEntry(
                        serverId = serverId,
                        action = AuditAction.MAKE_ROOM_ADMIN,
                        details = mapOf("room_id" to roomId) + if (userId != null) mapOf("user_id" to userId) else emptyMap(),
                    )
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun clearActionMessage() {
        _state.value = _state.value.copy(actionMessage = null)
    }
}
