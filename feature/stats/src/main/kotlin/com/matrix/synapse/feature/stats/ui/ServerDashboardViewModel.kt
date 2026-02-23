package com.matrix.synapse.feature.stats.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.feature.stats.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val serverVersion: String? = null,
    val totalUsers: Long = 0L,
    val totalRooms: Int = 0,
    val dau: Int = 0,
    val mau: Int = 0,
    val largestRooms: List<RoomSizeEntry> = emptyList(),
    val dbStatsUnavailable: Boolean = false,
    val topMediaUsers: List<UserMediaStats> = emptyList(),
)

@HiltViewModel
class ServerDashboardViewModel @Inject constructor(
    private val statsRepository: StatsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    fun loadDashboard(serverUrl: String) {
        _state.value = DashboardState(isLoading = true)
        viewModelScope.launch {
            runCatching {
                val versionDef = async { statsRepository.getServerVersion(serverUrl) }
                val totalUsersDef = async { statsRepository.getTotalUsers(serverUrl) }
                val totalRoomsDef = async { statsRepository.getTotalRooms(serverUrl) }
                val dauDef = async { statsRepository.getActiveUserCount(serverUrl, 24 * 60 * 60 * 1000L) }
                val mauDef = async { statsRepository.getActiveUserCount(serverUrl, 30L * 24 * 60 * 60 * 1000L) }
                val dbStatsDef = async {
                    runCatching { statsRepository.getDatabaseRoomStats(serverUrl) }
                }
                val mediaDef = async { statsRepository.getMediaUsage(serverUrl, limit = 50) }

                val version = versionDef.await()
                val totalUsers = totalUsersDef.await()
                val totalRooms = totalRoomsDef.await()
                val dau = dauDef.await()
                val mau = mauDef.await()
                val dbStats = dbStatsDef.await()
                val media = mediaDef.await()

                _state.value = DashboardState(
                    serverVersion = version.serverVersion,
                    totalUsers = totalUsers,
                    totalRooms = totalRooms,
                    dau = dau,
                    mau = mau,
                    largestRooms = dbStats.getOrNull()?.rooms ?: emptyList(),
                    dbStatsUnavailable = dbStats.isFailure,
                    topMediaUsers = media.users,
                )
            }.onFailure { e ->
                _state.value = DashboardState(error = e.message)
            }
        }
    }
}
