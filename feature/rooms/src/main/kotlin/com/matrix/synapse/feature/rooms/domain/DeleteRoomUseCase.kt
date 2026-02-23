package com.matrix.synapse.feature.rooms.domain

import com.matrix.synapse.feature.rooms.data.DeleteRoomRequest
import com.matrix.synapse.feature.rooms.data.RoomRepository
import kotlinx.coroutines.delay
import javax.inject.Inject

class DeleteRoomUseCase @Inject constructor(
    private val roomRepository: RoomRepository,
) {
    suspend fun execute(
        serverUrl: String,
        roomId: String,
        request: DeleteRoomRequest,
        pollIntervalMs: Long = 2_000L,
        maxPolls: Int = 30,
    ): Result<Unit> = runCatching {
        val response = roomRepository.deleteRoom(serverUrl, roomId, request)
        repeat(maxPolls) {
            delay(pollIntervalMs)
            val status = roomRepository.getDeleteStatus(serverUrl, roomId)
            val deleteStatus = status.results.firstOrNull { it.deleteId == response.deleteId }
            when (deleteStatus?.status) {
                "complete" -> return Result.success(Unit)
                "failed" -> throw RuntimeException("Room deletion failed: ${deleteStatus.error}")
            }
        }
        throw RuntimeException("Room deletion timed out after ${maxPolls * pollIntervalMs / 1000}s")
    }
}
