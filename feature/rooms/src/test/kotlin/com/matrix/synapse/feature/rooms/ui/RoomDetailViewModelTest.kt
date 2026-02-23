package com.matrix.synapse.feature.rooms.ui

import app.cash.turbine.test
import com.matrix.synapse.database.AuditAction
import com.matrix.synapse.database.AuditLogger
import com.matrix.synapse.feature.rooms.data.*
import com.matrix.synapse.feature.rooms.domain.DeleteRoomUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoomDetailViewModelTest {

    private val roomRepository = mockk<RoomRepository>()
    private val deleteRoomUseCase = mockk<DeleteRoomUseCase>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val dispatcher = UnconfinedTestDispatcher()

    private fun createVm() = RoomDetailViewModel(roomRepository, deleteRoomUseCase, auditLogger)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadRoom loads detail and members and block status`() = runTest {
        coEvery { roomRepository.getRoom(any(), any()) } returns RoomDetailResponse(
            roomId = "!a:x", name = "Room A", joinedMembers = 3,
        )
        coEvery { roomRepository.getRoomMembers(any(), any()) } returns RoomMembersResponse(
            members = listOf("@user1:x", "@user2:x"), total = 2,
        )
        coEvery { roomRepository.getBlockStatus(any(), any()) } returns BlockRoomResponse(block = false)

        val vm = createVm()
        vm.state.test {
            vm.loadRoom("https://x", "srv1", "!a:x")
            val state = expectMostRecentItem()
            assertEquals("Room A", state.room?.name)
            assertEquals(2, state.members.size)
            assertFalse(state.isBlocked)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `blockRoom toggles block status and logs audit`() = runTest {
        coEvery { roomRepository.getRoom(any(), any()) } returns RoomDetailResponse(roomId = "!a:x", joinedMembers = 0)
        coEvery { roomRepository.getRoomMembers(any(), any()) } returns RoomMembersResponse(members = emptyList(), total = 0)
        coEvery { roomRepository.getBlockStatus(any(), any()) } returns BlockRoomResponse(block = false)
        coEvery { roomRepository.blockRoom(any(), any(), true) } returns BlockRoomResponse(block = true)
        coEvery { auditLogger.insert(any()) } returns 1L

        val vm = createVm()
        vm.loadRoom("https://x", "srv1", "!a:x")
        vm.state.test {
            vm.blockRoom("https://x", "srv1", "!a:x", block = true)
            val state = expectMostRecentItem()
            assertTrue(state.isBlocked)
            coVerify { auditLogger.insert(match { it.action == AuditAction.BLOCK_ROOM }) }
        }
    }

    @Test
    fun `deleteRoom calls use case and logs audit`() = runTest {
        coEvery { roomRepository.getRoom(any(), any()) } returns RoomDetailResponse(roomId = "!a:x", joinedMembers = 0)
        coEvery { roomRepository.getRoomMembers(any(), any()) } returns RoomMembersResponse(members = emptyList(), total = 0)
        coEvery { roomRepository.getBlockStatus(any(), any()) } returns BlockRoomResponse(block = false)
        coEvery { deleteRoomUseCase.execute(any(), any(), any(), any(), any()) } returns Result.success(Unit)
        coEvery { auditLogger.insert(any()) } returns 1L

        val vm = createVm()
        vm.loadRoom("https://x", "srv1", "!a:x")
        vm.state.test {
            vm.deleteRoom("https://x", "srv1", "!a:x", DeleteRoomRequest())
            val state = expectMostRecentItem()
            assertTrue(state.deleteComplete)
            coVerify { auditLogger.insert(match { it.action == AuditAction.DELETE_ROOM }) }
        }
    }
}
