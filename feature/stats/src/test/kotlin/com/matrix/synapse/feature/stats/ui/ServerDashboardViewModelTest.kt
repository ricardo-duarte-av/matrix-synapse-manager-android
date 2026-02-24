package com.matrix.synapse.feature.stats.ui

import app.cash.turbine.test
import com.matrix.synapse.feature.federation.data.FederationDestination
import com.matrix.synapse.feature.federation.data.FederationDestinationsResponse
import com.matrix.synapse.feature.jobs.data.BackgroundUpdatesStatusResponse
import com.matrix.synapse.feature.jobs.data.JobsRepository
import com.matrix.synapse.feature.moderation.data.EventReportsListResponse
import com.matrix.synapse.feature.moderation.data.ModerationRepository
import com.matrix.synapse.feature.servers.data.ServerRepository
import com.matrix.synapse.feature.stats.data.*
import com.matrix.synapse.feature.federation.data.FederationRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ServerDashboardViewModelTest {

    private val statsRepository = mockk<StatsRepository>()
    private val serverRepository = mockk<ServerRepository>()
    private val federationRepository = mockk<FederationRepository>()
    private val jobsRepository = mockk<JobsRepository>()
    private val moderationRepository = mockk<ModerationRepository>()
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        every { serverRepository.getServerById(any()) } returns flowOf(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadDashboard loads all stats`() = runTest {
        coEvery { statsRepository.getServerVersion(any()) } returns ServerVersionResponse("1.100.0")
        coEvery { statsRepository.getTotalUsers(any()) } returns 150L
        coEvery { statsRepository.getTotalRooms(any()) } returns 42
        coEvery { statsRepository.getActiveUserCount(any(), any()) } returnsMany listOf(10, 80)
        coEvery { statsRepository.getDatabaseRoomStats(any()) } returns DatabaseRoomStatsResponse(
            rooms = listOf(RoomSizeEntry("!a:x", 1024L))
        )
        coEvery { statsRepository.getMediaUsage(any(), any()) } returns MediaUsageResponse(
            users = listOf(UserMediaStats("@u:x", "User", 5, 2048L)),
            total = 1,
        )
        coEvery { statsRepository.getTotalMediaStorage(any()) } returns 2048L
        coEvery { federationRepository.listDestinations(any(), limit = 1) } returns FederationDestinationsResponse(total = 10)
        coEvery { federationRepository.listDestinations(any(), limit = 500) } returns FederationDestinationsResponse(
            destinations = listOf(FederationDestination("a.com", failureTs = 1L)),
            total = 10,
        )
        coEvery { jobsRepository.getStatus(any()) } returns BackgroundUpdatesStatusResponse(enabled = true)
        coEvery { moderationRepository.listEventReports(any(), limit = 1) } returns EventReportsListResponse(total = 3)

        val vm = ServerDashboardViewModel(statsRepository, serverRepository, federationRepository, jobsRepository, moderationRepository)
        vm.state.test {
            vm.loadDashboard("s1", "https://x")
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertEquals("1.100.0", state.serverVersion)
            assertEquals(150L, state.totalUsers)
            assertEquals(42, state.totalRooms)
            assertEquals(2048L, state.totalMediaBytes)
            assertEquals(10, state.federationDestinations)
            assertEquals(1, state.federationFailures)
            assertEquals(true, state.backgroundUpdatesEnabled)
            assertEquals(3, state.openEventReportsCount)
            assertEquals(1, state.largestRooms.size)
            assertEquals(1, state.topMediaUsers.size)
        }
    }

    @Test
    fun `database stats shows fallback on error`() = runTest {
        coEvery { statsRepository.getServerVersion(any()) } returns ServerVersionResponse("1.100.0")
        coEvery { statsRepository.getTotalUsers(any()) } returns 0L
        coEvery { statsRepository.getTotalRooms(any()) } returns 0
        coEvery { statsRepository.getActiveUserCount(any(), any()) } returns 0
        coEvery { statsRepository.getDatabaseRoomStats(any()) } throws RuntimeException("SQLite not supported")
        coEvery { statsRepository.getMediaUsage(any(), any()) } returns MediaUsageResponse(total = 0)
        coEvery { statsRepository.getTotalMediaStorage(any()) } returns 0L
        coEvery { federationRepository.listDestinations(any(), limit = any()) } returns FederationDestinationsResponse(total = 0)
        coEvery { jobsRepository.getStatus(any()) } returns BackgroundUpdatesStatusResponse(enabled = true)
        coEvery { moderationRepository.listEventReports(any(), limit = 1) } returns EventReportsListResponse(total = 0)

        val vm = ServerDashboardViewModel(statsRepository, serverRepository, federationRepository, jobsRepository, moderationRepository)
        vm.state.test {
            vm.loadDashboard("s1", "https://x")
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertTrue(state.dbStatsUnavailable)
        }
    }
}
