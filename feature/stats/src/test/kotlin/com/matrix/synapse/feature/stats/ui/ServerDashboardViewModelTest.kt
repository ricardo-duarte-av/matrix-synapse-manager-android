package com.matrix.synapse.feature.stats.ui

import app.cash.turbine.test
import com.matrix.synapse.feature.stats.data.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ServerDashboardViewModelTest {

    private val statsRepository = mockk<StatsRepository>()
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
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

        val vm = ServerDashboardViewModel(statsRepository)
        vm.state.test {
            vm.loadDashboard("https://x")
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertEquals("1.100.0", state.serverVersion)
            assertEquals(150L, state.totalUsers)
            assertEquals(42, state.totalRooms)
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

        val vm = ServerDashboardViewModel(statsRepository)
        vm.state.test {
            vm.loadDashboard("https://x")
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertTrue(state.dbStatsUnavailable)
        }
    }
}
