package com.matrix.synapse.feature.rooms.ui

import app.cash.turbine.test
import com.matrix.synapse.feature.rooms.data.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoomListViewModelTest {

    private val roomRepository = mockk<RoomRepository>()
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
    fun `init loads first page of rooms`() = runTest {
        val rooms = listOf(RoomSummary(roomId = "!a:example.com", name = "Test Room", joinedMembers = 5))
        coEvery { roomRepository.listRooms(any(), limit = any(), orderBy = any(), dir = any()) } returns RoomListResponse(rooms = rooms, totalRooms = 1)

        val vm = RoomListViewModel(roomRepository)
        vm.state.test {
            vm.init("https://example.com")
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertEquals(1, state.rooms.size)
            assertEquals("Test Room", state.rooms[0].name)
        }
    }

    @Test
    fun `search updates results`() = runTest {
        coEvery { roomRepository.listRooms(any(), limit = any(), orderBy = any(), dir = any()) } returns RoomListResponse(rooms = emptyList(), totalRooms = 0)
        coEvery { roomRepository.listRooms(any(), limit = any(), searchTerm = "test", orderBy = any(), dir = any()) } returns RoomListResponse(
            rooms = listOf(RoomSummary(roomId = "!b:example.com", name = "Test", joinedMembers = 1)),
            totalRooms = 1,
        )

        val vm = RoomListViewModel(roomRepository)
        vm.init("https://example.com")
        vm.state.test {
            vm.search("test")
            val state = expectMostRecentItem()
            assertEquals(1, state.rooms.size)
        }
    }

    @Test
    fun `loadNextPage appends rooms`() = runTest {
        coEvery { roomRepository.listRooms(any(), from = isNull(), limit = any(), orderBy = any(), dir = any()) } returns RoomListResponse(
            rooms = listOf(RoomSummary(roomId = "!a:x", joinedMembers = 1)),
            totalRooms = 2,
            nextBatch = 1,
        )
        coEvery { roomRepository.listRooms(any(), from = 1, limit = any(), orderBy = any(), dir = any()) } returns RoomListResponse(
            rooms = listOf(RoomSummary(roomId = "!b:x", joinedMembers = 1)),
            totalRooms = 2,
        )

        val vm = RoomListViewModel(roomRepository)
        vm.init("https://example.com")
        vm.state.test {
            vm.loadNextPage()
            val state = expectMostRecentItem()
            assertEquals(2, state.rooms.size)
        }
    }

    @Test
    fun `error state set on failure`() = runTest {
        coEvery { roomRepository.listRooms(any(), limit = any(), orderBy = any(), dir = any()) } throws RuntimeException("network error")

        val vm = RoomListViewModel(roomRepository)
        vm.state.test {
            vm.init("https://example.com")
            val state = expectMostRecentItem()
            assertNotNull(state.error)
        }
    }
}
