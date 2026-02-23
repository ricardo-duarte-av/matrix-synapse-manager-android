package com.matrix.synapse.feature.rooms.domain

import com.matrix.synapse.feature.rooms.data.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DeleteRoomUseCaseTest {

    private val roomRepository = mockk<RoomRepository>()
    private lateinit var useCase: DeleteRoomUseCase

    @Before
    fun setup() {
        useCase = DeleteRoomUseCase(roomRepository)
    }

    @Test
    fun `delete succeeds when status completes`() = runTest {
        coEvery { roomRepository.deleteRoom(any(), any(), any()) } returns DeleteRoomResponse("del-1")
        coEvery { roomRepository.getDeleteStatus(any(), any()) } returns DeleteStatusResponse(
            results = listOf(DeleteStatus("del-1", "complete"))
        )

        val result = useCase.execute("https://example.com", "!room:example.com", DeleteRoomRequest())
        assertTrue(result.isSuccess)
    }

    @Test
    fun `delete fails when status is failed`() = runTest {
        coEvery { roomRepository.deleteRoom(any(), any(), any()) } returns DeleteRoomResponse("del-1")
        coEvery { roomRepository.getDeleteStatus(any(), any()) } returns DeleteStatusResponse(
            results = listOf(DeleteStatus("del-1", "failed", error = "something broke"))
        )

        val result = useCase.execute("https://example.com", "!room:example.com", DeleteRoomRequest())
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("something broke"))
    }

    @Test
    fun `delete polls until complete`() = runTest {
        coEvery { roomRepository.deleteRoom(any(), any(), any()) } returns DeleteRoomResponse("del-1")
        coEvery { roomRepository.getDeleteStatus(any(), any()) } returnsMany listOf(
            DeleteStatusResponse(results = listOf(DeleteStatus("del-1", "active"))),
            DeleteStatusResponse(results = listOf(DeleteStatus("del-1", "active"))),
            DeleteStatusResponse(results = listOf(DeleteStatus("del-1", "complete"))),
        )

        val result = useCase.execute("https://example.com", "!room:example.com", DeleteRoomRequest())
        assertTrue(result.isSuccess)
        coVerify(exactly = 3) { roomRepository.getDeleteStatus(any(), any()) }
    }

    @Test
    fun `delete fails on API exception`() = runTest {
        coEvery { roomRepository.deleteRoom(any(), any(), any()) } throws RuntimeException("network error")

        val result = useCase.execute("https://example.com", "!room:example.com", DeleteRoomRequest())
        assertTrue(result.isFailure)
    }
}
