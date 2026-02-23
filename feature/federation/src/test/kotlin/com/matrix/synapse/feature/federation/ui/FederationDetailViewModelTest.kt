package com.matrix.synapse.feature.federation.ui

import app.cash.turbine.test
import com.matrix.synapse.database.AuditAction
import com.matrix.synapse.database.AuditLogger
import com.matrix.synapse.feature.federation.data.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FederationDetailViewModelTest {
    private val federationRepository = mockk<FederationRepository>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val dispatcher = UnconfinedTestDispatcher()

    private fun createVm() = FederationDetailViewModel(federationRepository, auditLogger)

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `loadDestination populates detail and rooms`() = runTest {
        coEvery { federationRepository.getDestination(any(), any()) } returns
            FederationDestination(destination = "matrix.org", failureTs = 1000L, retryInterval = 5000L)
        coEvery { federationRepository.getDestinationRooms(any(), any()) } returns
            DestinationRoomsResponse(rooms = listOf(DestinationRoom(roomId = "!room:matrix.org", streamOrdering = 42)), total = 1)
        val vm = createVm()
        vm.state.test {
            vm.loadDestination("https://example.com", "srv1", "matrix.org")
            val state = expectMostRecentItem()
            assertEquals("matrix.org", state.destination?.destination)
            assertEquals(1, state.rooms.size)
            assertEquals(1000L, state.destination?.failureTs)
        }
    }

    @Test
    fun `resetConnection calls API and logs audit`() = runTest {
        coEvery { federationRepository.getDestination(any(), any()) } returns FederationDestination(destination = "matrix.org")
        coEvery { federationRepository.getDestinationRooms(any(), any()) } returns DestinationRoomsResponse()
        coEvery { federationRepository.resetConnection(any(), any()) } returns Unit
        val vm = createVm()
        vm.loadDestination("https://example.com", "srv1", "matrix.org")
        vm.state.test {
            vm.resetConnection("matrix.org")
            val state = expectMostRecentItem()
            assertFalse(state.isResetting)
            coVerify { auditLogger.insert(match { it.action == AuditAction.RESET_FEDERATION_CONNECTION }) }
        }
    }

    @Test
    fun `loadDestination sets error on failure`() = runTest {
        coEvery { federationRepository.getDestination(any(), any()) } throws RuntimeException("not found")
        val vm = createVm()
        vm.state.test {
            vm.loadDestination("https://example.com", "srv1", "matrix.org")
            val state = expectMostRecentItem()
            assertNotNull(state.error)
        }
    }
}
