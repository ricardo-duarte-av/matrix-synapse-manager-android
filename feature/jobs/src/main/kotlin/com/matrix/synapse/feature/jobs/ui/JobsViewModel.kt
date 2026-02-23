package com.matrix.synapse.feature.jobs.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.feature.jobs.data.CurrentUpdateInfo
import com.matrix.synapse.feature.jobs.data.JobsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackgroundJobsState(
    val enabled: Boolean? = null,
    val currentUpdates: Map<String, CurrentUpdateInfo> = emptyMap(),
    val isLoading: Boolean = false,
    val isToggling: Boolean = false,
    val isStartingJob: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
)

@HiltViewModel
class JobsViewModel @Inject constructor(
    private val jobsRepository: JobsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BackgroundJobsState())
    val state: StateFlow<BackgroundJobsState> = _state.asStateFlow()

    private var serverUrl: String = ""

    fun load(serverId: String, serverUrl: String) {
        this.serverUrl = serverUrl
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            runCatching { jobsRepository.getStatus(serverUrl) }
                .onSuccess { response ->
                    _state.value = _state.value.copy(
                        enabled = response.enabled,
                        currentUpdates = response.currentUpdates,
                        isLoading = false,
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        error = e.message ?: "Failed to load status",
                        isLoading = false,
                    )
                }
        }
    }

    fun setEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(isToggling = true, error = null)
        viewModelScope.launch {
            runCatching { jobsRepository.setEnabled(serverUrl, enabled) }
                .onSuccess { newEnabled ->
                    _state.value = _state.value.copy(
                        enabled = newEnabled,
                        isToggling = false,
                        successMessage = if (newEnabled) "Background updates resumed" else "Background updates paused",
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isToggling = false,
                        error = e.message ?: "Failed to update",
                    )
                }
        }
    }

    fun startJob(jobName: String) {
        _state.value = _state.value.copy(isStartingJob = true, error = null, successMessage = null)
        viewModelScope.launch {
            runCatching { jobsRepository.startJob(serverUrl, jobName) }
                .onSuccess {
                    _state.value = _state.value.copy(
                        isStartingJob = false,
                        successMessage = "Job started: $jobName",
                    )
                    load("", serverUrl)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isStartingJob = false,
                        error = e.message ?: "Failed to start job",
                    )
                }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(error = null, successMessage = null)
    }
}
