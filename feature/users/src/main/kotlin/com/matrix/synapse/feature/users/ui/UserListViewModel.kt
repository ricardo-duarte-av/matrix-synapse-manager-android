package com.matrix.synapse.feature.users.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.feature.users.data.UserRepository
import com.matrix.synapse.feature.users.data.UserSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserListState(
    val users: List<UserSummary> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val nextToken: String? = null,
    val hasMore: Boolean = false,
)

@HiltViewModel
class UserListViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(UserListState())
    val state: StateFlow<UserListState> = _state.asStateFlow()

    private var serverUrl: String = ""

    fun init(serverUrl: String) {
        this.serverUrl = serverUrl
        loadFirstPage()
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
}
