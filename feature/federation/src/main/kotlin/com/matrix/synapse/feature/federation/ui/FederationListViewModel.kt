package com.matrix.synapse.feature.federation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.feature.federation.data.FederationDestination
import com.matrix.synapse.feature.federation.data.FederationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FederationListState(
    val destinations: List<FederationDestination> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val nextToken: String? = null,
    val hasMore: Boolean = false,
    val totalDestinations: Int = 0,
    val sortBy: String = "destination",
    val sortDir: String = "f",
)

@HiltViewModel
class FederationListViewModel @Inject constructor(
    private val federationRepository: FederationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FederationListState())
    val state: StateFlow<FederationListState> = _state.asStateFlow()

    private var serverUrl: String = ""

    fun init(serverUrl: String) {
        this.serverUrl = serverUrl
        loadFirstPage()
    }

    fun setSort(orderBy: String, dir: String) {
        _state.value = _state.value.copy(
            sortBy = orderBy, sortDir = dir,
            destinations = emptyList(), nextToken = null, hasMore = false,
        )
        loadFirstPage()
    }

    fun loadNextPage() {
        val current = _state.value
        if (current.isLoadingMore || !current.hasMore) return
        _state.value = current.copy(isLoadingMore = true)
        viewModelScope.launch {
            runCatching {
                federationRepository.listDestinations(
                    serverUrl,
                    from = current.nextToken,
                    orderBy = current.sortBy,
                    dir = current.sortDir,
                )
            }.onSuccess { response ->
                _state.value = _state.value.copy(
                    destinations = _state.value.destinations + response.destinations,
                    nextToken = response.nextToken,
                    hasMore = response.nextToken != null,
                    totalDestinations = response.total,
                    isLoadingMore = false,
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(error = e.message, isLoadingMore = false)
            }
        }
    }

    private fun loadFirstPage() {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            runCatching {
                federationRepository.listDestinations(
                    serverUrl,
                    orderBy = _state.value.sortBy,
                    dir = _state.value.sortDir,
                )
            }.onSuccess { response ->
                _state.value = _state.value.copy(
                    destinations = response.destinations,
                    nextToken = response.nextToken,
                    hasMore = response.nextToken != null,
                    totalDestinations = response.total,
                    isLoading = false,
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(error = e.message, isLoading = false)
            }
        }
    }
}
