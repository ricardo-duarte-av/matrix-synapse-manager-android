package com.matrix.synapse.feature.stats.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.feature.federation.data.FederationRepository
import com.matrix.synapse.feature.jobs.data.JobsRepository
import com.matrix.synapse.feature.moderation.data.ModerationRepository
import com.matrix.synapse.feature.servers.data.ServerRepository
import com.matrix.synapse.feature.stats.data.*
import com.matrix.synapse.model.Server
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardState(
    val currentServer: Server? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val serverVersion: String? = null,
    val totalUsers: Long = 0L,
    val totalRooms: Int = 0,
    val dau: Int = 0,
    val mau: Int = 0,
    val totalMediaBytes: Long? = null,
    val federationDestinations: Int? = null,
    val federationFailures: Int? = null,
    val backgroundUpdatesEnabled: Boolean? = null,
    val backgroundUpdatesJobName: String? = null,
    val openEventReportsCount: Int? = null,
    val largestRooms: List<RoomSizeEntry> = emptyList(),
    val dbStatsUnavailable: Boolean = false,
    val topMediaUsers: List<UserMediaStats> = emptyList(),
)

@HiltViewModel
class ServerDashboardViewModel @Inject constructor(
    private val statsRepository: StatsRepository,
    private val serverRepository: ServerRepository,
    private val federationRepository: FederationRepository,
    private val jobsRepository: JobsRepository,
    private val moderationRepository: ModerationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    fun loadDashboard(serverId: String, serverUrl: String) {
        serverRepository.getServerById(serverId).onEach { server ->
            _state.value = _state.value.copy(currentServer = server)
        }.launchIn(viewModelScope)
        _state.value = _state.value.copy(isLoading = true)
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
                val totalMediaDef = async {
                    runCatching { statsRepository.getTotalMediaStorage(serverUrl) }.getOrNull()
                }
                val federationDef = async {
                    runCatching {
                        val first = federationRepository.listDestinations(serverUrl, limit = 1)
                        val withFailures = federationRepository.listDestinations(serverUrl, limit = 500)
                        Pair(first.total, withFailures.destinations.count { it.failureTs != null })
                    }.getOrNull()
                }
                val jobsDef = async {
                    runCatching { jobsRepository.getStatus(serverUrl) }.getOrNull()
                }
                val reportsDef = async {
                    runCatching { moderationRepository.listEventReports(serverUrl, limit = 1) }.getOrNull()?.total
                }

                val version = versionDef.await()
                val totalUsers = totalUsersDef.await()
                val totalRooms = totalRoomsDef.await()
                val dau = dauDef.await()
                val mau = mauDef.await()
                val dbStats = dbStatsDef.await()
                val media = mediaDef.await()
                val totalMedia = totalMediaDef.await()
                val (federationTotal, federationFailing) = federationDef.await() ?: (null to null)
                val jobsStatus = jobsDef.await()
                val reportsTotal = reportsDef.await()

                _state.value = _state.value.copy(
                    serverVersion = version.serverVersion,
                    totalUsers = totalUsers,
                    totalRooms = totalRooms,
                    dau = dau,
                    mau = mau,
                    totalMediaBytes = totalMedia,
                    federationDestinations = federationTotal,
                    federationFailures = federationFailing,
                    backgroundUpdatesEnabled = jobsStatus?.enabled,
                    backgroundUpdatesJobName = jobsStatus?.currentUpdates?.entries?.firstOrNull()?.value?.name,
                    openEventReportsCount = reportsTotal,
                    largestRooms = dbStats.getOrNull()?.rooms ?: emptyList(),
                    dbStatsUnavailable = dbStats.isFailure,
                    topMediaUsers = media.users,
                    isLoading = false,
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(error = e.message, isLoading = false)
            }
        }
    }
}
