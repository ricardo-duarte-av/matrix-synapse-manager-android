package com.matrix.synapse.feature.users.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.database.AuditAction
import com.matrix.synapse.database.AuditLogEntry
import com.matrix.synapse.database.AuditLogger
import com.matrix.synapse.feature.servers.data.ServerRepository
import com.matrix.synapse.network.ActiveTokenHolder
import com.matrix.synapse.feature.users.data.UserRepository
import com.matrix.synapse.feature.users.data.UserSummary
import com.matrix.synapse.model.Server
import com.matrix.synapse.security.SecureTokenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserListState(
    val currentServer: Server? = null,
    val users: List<UserSummary> = emptyList(),
    val totalUsers: Long = 0L,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null,
    val searchQuery: String = "",
    val nextToken: String? = null,
    val hasMore: Boolean = false,
    val sortOrder: String = "name_asc",
    val selectionMode: Boolean = false,
    val selectedUserIds: Set<String> = emptySet(),
    val currentUserId: String? = null,
)

@HiltViewModel
class UserListViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val serverRepository: ServerRepository,
    private val auditLogger: AuditLogger,
    private val tokenStore: SecureTokenStore,
    private val activeTokenHolder: ActiveTokenHolder,
) : ViewModel() {

    private val _state = MutableStateFlow(UserListState())
    val state: StateFlow<UserListState> = _state.asStateFlow()

    private var serverUrl: String = ""
    private var serverId: String = ""

    fun init(serverId: String, serverUrl: String) {
        this.serverId = serverId
        this.serverUrl = serverUrl
        serverRepository.getServerById(serverId).onEach { server ->
            _state.value = _state.value.copy(currentServer = server)
        }.launchIn(viewModelScope)
        viewModelScope.launch {
            val current = tokenStore.currentUserIdFlow(serverId).first()
            _state.value = _state.value.copy(currentUserId = current)
        }
        viewModelScope.launch {
            activeTokenHolder.set(tokenStore.accessTokenFlow(serverId).first())
            loadFirstPage()
        }
    }

    fun search(query: String) {
        _state.value = _state.value.copy(
            searchQuery = query,
            users = emptyList(),
            nextToken = null,
            hasMore = false,
        )
        loadFirstPage()
    }

    fun loadNextPage() {
        val current = _state.value
        if (current.isLoadingMore || !current.hasMore) return
        _state.value = current.copy(isLoadingMore = true)
        viewModelScope.launch {
            val query = current.searchQuery.takeIf { it.isNotBlank() }
            runCatching {
                userRepository.listUsers(serverUrl, from = current.nextToken, name = query)
            }.onSuccess { response ->
                _state.value = _state.value.copy(
                    users = _state.value.users + response.users,
                    nextToken = response.nextToken,
                    hasMore = response.nextToken != null,
                    totalUsers = response.total,
                    isLoadingMore = false,
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    error = e.message,
                    isLoadingMore = false,
                )
            }
        }
    }

    private fun loadFirstPage() {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val query = _state.value.searchQuery.takeIf { it.isNotBlank() }
            runCatching {
                userRepository.listUsers(serverUrl, name = query)
            }.onSuccess { response ->
                _state.value = _state.value.copy(
                    users = response.users,
                    nextToken = response.nextToken,
                    hasMore = response.nextToken != null,
                    totalUsers = response.total,
                    isLoading = false,
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    error = e.message,
                    isLoading = false,
                )
            }
        }
    }

    fun setSortOrder(asc: Boolean) {
        _state.value = _state.value.copy(sortOrder = if (asc) "name_asc" else "name_desc")
    }

    fun enterSelectionMode(userId: String?) {
        _state.value = _state.value.copy(
            selectionMode = true,
            selectedUserIds = if (userId != null) setOf(userId) else emptySet(),
        )
    }

    fun exitSelectionMode() {
        _state.value = _state.value.copy(selectionMode = false, selectedUserIds = emptySet())
    }

    fun toggleUserSelection(userId: String) {
        val current = _state.value.selectedUserIds
        _state.value = _state.value.copy(
            selectedUserIds = if (userId in current) current - userId else current + userId,
        )
    }

    fun selectAllUsers() {
        val current = _state.value.currentUserId
        val ids = _state.value.users.map { it.userId }.filter { it != current }.toSet()
        _state.value = _state.value.copy(selectedUserIds = ids)
    }

    fun clearUserSelection() {
        _state.value = _state.value.copy(selectedUserIds = emptySet())
    }

    fun deleteSelectedUsers(erase: Boolean) {
        val currentUserId = _state.value.currentUserId
        val ids = _state.value.selectedUserIds.filter { it != currentUserId }.toList()
        if (ids.isEmpty()) return
        _state.value = _state.value.copy(isDeleting = true, error = null, actionMessage = null)
        viewModelScope.launch {
            var success = 0
            var failed = 0
            ids.forEach { userId ->
                runCatching {
                    userRepository.deactivateUser(serverUrl, userId, erase = erase)
                }.onSuccess {
                    success++
                    auditLogger.insert(
                        AuditLogEntry(
                            serverId = serverId,
                            action = AuditAction.DEACTIVATE_USER,
                            details = mapOf("user_id" to userId, "erase" to erase.toString()),
                        )
                    )
                }.onFailure { failed++ }
            }
            _state.value = _state.value.copy(
                isDeleting = false,
                selectionMode = false,
                selectedUserIds = emptySet(),
                actionMessage = when {
                    failed == 0 -> "Deactivated $success user(s)"
                    success == 0 -> "Deactivate failed for all users"
                    else -> "Deactivated $success, failed $failed"
                },
            )
            loadFirstPage()
        }
    }

    fun clearActionMessage() {
        _state.value = _state.value.copy(actionMessage = null)
    }
}
