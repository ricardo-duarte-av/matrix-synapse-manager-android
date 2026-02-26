package com.matrix.synapse.feature.devices.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.feature.devices.data.DeviceInfo
import com.matrix.synapse.feature.devices.data.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceListState(
    val devices: List<DeviceInfo> = emptyList(),
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
    val pendingDeleteDeviceId: String? = null,
)

@HiltViewModel
class DeviceListViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DeviceListState())
    val state: StateFlow<DeviceListState> = _state.asStateFlow()

    private var serverUrl = ""
    private var userId = ""

    fun init(serverUrl: String, userId: String) {
        this.serverUrl = serverUrl
        this.userId = userId
        loadDevices()
    }

    fun refresh() {
        loadDevices()
    }

    fun requestDelete(deviceId: String) {
        _state.value = _state.value.copy(pendingDeleteDeviceId = deviceId)
    }

    fun cancelDelete() {
        _state.value = _state.value.copy(pendingDeleteDeviceId = null)
    }

    fun confirmDelete() {
        val deviceId = _state.value.pendingDeleteDeviceId ?: return
        _state.value = _state.value.copy(isDeleting = true, pendingDeleteDeviceId = null)
        viewModelScope.launch {
            runCatching {
                deviceRepository.deleteDevice(serverUrl, userId, deviceId)
            }.onSuccess {
                loadDevices()
            }.onFailure { e ->
                _state.value = _state.value.copy(isDeleting = false, error = e.message)
            }
        }
    }

    private fun loadDevices() {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            runCatching {
                deviceRepository.listDevices(serverUrl, userId)
            }.onSuccess { response ->
                _state.value = _state.value.copy(
                    devices = response.devices,
                    isLoading = false,
                    isDeleting = false,
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}
