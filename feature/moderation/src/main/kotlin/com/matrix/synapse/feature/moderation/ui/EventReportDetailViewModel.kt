package com.matrix.synapse.feature.moderation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.feature.moderation.data.EventReportDetailResponse
import com.matrix.synapse.feature.moderation.data.ModerationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventReportDetailState(
    val report: EventReportDetailResponse? = null,
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
)

@HiltViewModel
class EventReportDetailViewModel @Inject constructor(
    private val moderationRepository: ModerationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EventReportDetailState())
    val state: StateFlow<EventReportDetailState> = _state.asStateFlow()

    private var serverUrl: String = ""

    fun load(serverId: String, serverUrl: String, reportId: Long) {
        this.serverUrl = serverUrl
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            runCatching { moderationRepository.getEventReport(serverUrl, reportId) }
                .onSuccess { report ->
                    _state.value = _state.value.copy(report = report, isLoading = false)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        error = e.message ?: "Failed to load report",
                        isLoading = false,
                    )
                }
        }
    }

    fun deleteReport(reportId: Long) {
        _state.value = _state.value.copy(isDeleting = true, error = null)
        viewModelScope.launch {
            runCatching { moderationRepository.deleteEventReport(serverUrl, reportId) }
                .onSuccess {
                    _state.value = _state.value.copy(
                        isDeleting = false,
                        isDeleted = true,
                        successMessage = "Report deleted",
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isDeleting = false,
                        error = e.message ?: "Failed to delete",
                    )
                }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(error = null, successMessage = null)
    }
}
