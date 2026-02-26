package com.matrix.synapse.feature.federation.ui

import app.cash.turbine.test
import com.matrix.synapse.feature.federation.data.*
import com.matrix.synapse.feature.servers.data.ServerRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FederationListViewModelTest {
    private val federationRepository = mockk<FederationRepository>()
    private val serverRepository = mockk<ServerRepository>()
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        every { serverRepository.getServerById(any()) } returns flowOf(null)
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `init loads first page of destinations`() = runTest {
        coEvery { federationRepository.listDestinations(any(), orderBy = any(), dir = any()) } returns
            FederationDestinationsResponse(
                destinations = listOf(
                    FederationDestination(destination = "matrix.org"),
                    FederationDestination(destination = "example.com", failureTs = 1000L),
                ),
                total = 2,
            )
        val vm = FederationListViewModel(federationRepository, serverRepository)
        vm.state.test {
            vm.init("server-id", "https://example.com")
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertEquals(2, state.destinations.size)
            assertEquals("matrix.org", state.destinations[0].destination)
        }
    }

    @Test
    fun `loadNextPage appends destinations`() = runTest {
        coEvery { federationRepository.listDestinations(any(), from = isNull(), orderBy = any(), dir = any()) } returns
            FederationDestinationsResponse(destinations = listOf(FederationDestination(destination = "a.com")), total = 2, nextToken = "token1")
        coEvery { federationRepository.listDestinations(any(), from = "token1", orderBy = any(), dir = any()) } returns
            FederationDestinationsResponse(destinations = listOf(FederationDestination(destination = "b.com")), total = 2)
        val vm = FederationListViewModel(federationRepository, serverRepository)
        vm.init("server-id", "https://example.com")
        vm.state.test {
            vm.loadNextPage()
            val state = expectMostRecentItem()
            assertEquals(2, state.destinations.size)
        }
    }

    @Test
    fun `error state set on failure`() = runTest {
        coEvery { federationRepository.listDestinations(any(), orderBy = any(), dir = any()) } throws RuntimeException("network error")
        val vm = FederationListViewModel(federationRepository, serverRepository)
        vm.state.test {
            vm.init("server-id", "https://example.com")
            val state = expectMostRecentItem()
            assertNotNull(state.error)
        }
    }
}
