package com.matrix.synapse.feature.rooms.data

import com.matrix.synapse.network.RetrofitFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepository @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
) {
    private fun api(serverUrl: String): RoomAdminApi = retrofitFactory.create(serverUrl)

    suspend fun listRooms(
        serverUrl: String,
        from: Int? = null,
        limit: Int = 100,
        orderBy: String? = null,
        dir: String? = null,
        searchTerm: String? = null,
    ): RoomListResponse = api(serverUrl).listRooms(from = from, limit = limit, orderBy = orderBy, dir = dir, searchTerm = searchTerm)

    suspend fun getRoom(serverUrl: String, roomId: String): RoomDetailResponse =
        api(serverUrl).getRoom(roomId)

    suspend fun getRoomMembers(serverUrl: String, roomId: String): RoomMembersResponse =
        api(serverUrl).getRoomMembers(roomId)

    suspend fun deleteRoom(serverUrl: String, roomId: String, request: DeleteRoomRequest): DeleteRoomResponse =
        api(serverUrl).deleteRoom(roomId, request)

    suspend fun getDeleteStatus(serverUrl: String, roomId: String): DeleteStatusResponse =
        api(serverUrl).getDeleteStatus(roomId)

    suspend fun blockRoom(serverUrl: String, roomId: String, block: Boolean): BlockRoomResponse =
        api(serverUrl).blockRoom(roomId, BlockRoomRequest(block = block))

    suspend fun getBlockStatus(serverUrl: String, roomId: String): BlockRoomResponse =
        api(serverUrl).getBlockStatus(roomId)

    suspend fun joinUserToRoom(serverUrl: String, roomId: String, userId: String) =
        api(serverUrl).joinUserToRoom(roomId, JoinRoomRequest(userId = userId))

    suspend fun makeRoomAdmin(serverUrl: String, roomId: String, userId: String?) =
        api(serverUrl).makeRoomAdmin(roomId, MakeRoomAdminRequest(userId = userId))
}
